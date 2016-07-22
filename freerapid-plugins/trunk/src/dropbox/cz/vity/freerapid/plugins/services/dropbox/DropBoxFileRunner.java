package cz.vity.freerapid.plugins.services.dropbox;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class DropBoxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DropBoxFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>Dropbox - ", "<");
        final Matcher match = PlugUtils.matcher("<div class=\"meta\">.+? ([0-9].+?)</div>", content);
        if (!match.find()) {
            PlugUtils.checkFileSize(httpFile, content, "\"size\": \"", "\"");
        } else {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            HttpMethod httpMethod;
            try {
                httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download").toHttpMethod();
            } catch (BuildMethodException e) {
                fileURL = fileURL.replaceFirst("^https?://(www\\.)?", "https://dl.");
                httpMethod = getGetMethod(fileURL);
                int httpStatus = client.makeRequest(httpMethod, false);
                if (httpStatus / 100 == 3) {
                    httpMethod = getDirectDownload(httpMethod);
                } else if (httpStatus != 200) {
                    checkProblems();
                    throw new PluginImplementationException("Unable to get direct download URL");
                }
            }
            setFileStreamContentTypes("text/plain");
            if (!tryDownloadAndSaveFile(httpMethod)) {
                setClientParameter(DownloadClientConsts.NO_CONTENT_LENGTH_AVAILABLE, true);
                if (!tryDownloadAndSaveFile(getMethodBuilder().setReferer(fileURL).setAction(httpMethod.getURI().toString()).toGetMethod())) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Invalid Link") ||
                contentAsString.contains("The file you&rsquo;re looking for has been moved or deleted") ||
                contentAsString.contains("That file isn’t here anymore")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("links are generating too much traffic and have been temporarily disabled"))
            throw new ServiceConnectionProblemException("This account's public links are generating too much traffic and have been temporarily disabled!");
    }

    protected HttpMethod getDirectDownload(final HttpMethod method) throws Exception {
        final Header locationHeader = method.getResponseHeader("Location");
        if (locationHeader == null) {
            throw new PluginImplementationException("Invalid redirect");
        }
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction(locationHeader.getValue())
                .toGetMethod();
    }

}