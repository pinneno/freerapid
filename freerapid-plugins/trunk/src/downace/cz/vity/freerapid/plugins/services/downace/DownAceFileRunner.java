package cz.vity.freerapid.plugins.services.downace;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class DownAceFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DownAceFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems(getMethod);
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems(getMethod);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("Filename\\s*:(?:\\s*<[^>]+>)*\\s*(.+?)\\s*<", content);
        if (!matcher.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(matcher.group(1).trim());
        matcher = PlugUtils.matcher("Filesize\\s*:(?:\\s*<[^>]+>)*\\s*(.+?)\\s*<", content);
        if (!matcher.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems(method);//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setActionFromFormWhereTagContains("Download", true)
                    .setAction(fileURL)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems(httpMethod);
                throw new ServiceConnectionProblemException();
            }
            checkProblems(httpMethod);
            httpMethod = getMethodBuilder().setActionFromAHrefWhereATagContains("Click Here!").toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems(httpMethod);//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems(method);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems(HttpMethod method) throws Exception {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") ||
                method.getURI().getURI().contains("File+has+been+removed") ||
                (""+method.getResponseHeader("Location")).contains("File+has+been+removed")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}