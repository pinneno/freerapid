package cz.vity.freerapid.plugins.services.pixhost;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class PixHostFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PixHostFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        checkUrl();
        if (!fileURL.contains("/images/")) {
            final GetMethod getMethod = getGetMethod(fileURL);//make first request
            if (makeRedirectedRequest(getMethod)) {
                checkProblems();
                checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } else
            checkNameAndSize(fileURL);
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (fileURL.equals(content))
            httpFile.setFileName(content.substring(1 + content.lastIndexOf("/")));
        else {
            Matcher matcher = PlugUtils.matcher("<img id=[^<>]+?alt=\"([^\"]+?)\"", content);
            if (!matcher.find())
                throw new PluginImplementationException("File name not found");
            httpFile.setFileName(matcher.group(1));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkUrl() {
        fileURL = fileURL.replaceFirst("https://", "http://");
        fileURL = fileURL.replaceFirst("pixhost.org/", "pixhost.to/");
        fileURL = fileURL.replaceFirst("/thumbs/", "/show/");
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkUrl();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod method = getGetMethod(fileURL); //create GET request

        if (!fileURL.contains("/images/")) {
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);
            Matcher matcher = PlugUtils.matcher("<img id=[^<>]+?src=\"([^\"]+?)\"", contentAsString);
            if (!matcher.find())
                throw new PluginImplementationException("Image source not found");
            method = getGetMethod(matcher.group(1));
        } else
            checkNameAndSize(fileURL);
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();//if downloading failed
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Picture doesn't exist")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}