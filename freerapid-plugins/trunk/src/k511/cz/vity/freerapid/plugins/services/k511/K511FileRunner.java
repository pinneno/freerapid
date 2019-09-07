package cz.vity.freerapid.plugins.services.k511;

import cz.vity.freerapid.plugins.exceptions.*;
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
class K511FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(K511FileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "<title>", " - K511");
        Matcher matcher = PlugUtils.matcher(" \\(\\s*(\\d[^\\)]+)\\s*\\)", content);
        if (!matcher.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
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
            checkNameAndSize(contentAsString);
            HttpMethod httpMethod;
            while (getContentAsString().contains("view now<")) {
                httpMethod = getMethodBuilder().setReferer(fileURL)
                        .setActionFromAHrefWhereATagContains("view now").toHttpMethod();
                int wait = PlugUtils.getNumberBetween(getContentAsString(), "var seconds = ", ";");
                downloadTask.sleep(1 + wait);
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }
            Matcher matcher = getMatcherAgainstContent("<a[^>]+href=\"([^\"]+)\"[^>]+>[^<]+download");
            if (!matcher.find())
                throw new PluginImplementationException("Download link not found");
            httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setAction(matcher.group(1).trim()).toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if ((content.contains("pageErrors") && content.contains("File has been removed")) || content.contains("<title>Error")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}