package cz.vity.freerapid.plugins.services.appcake;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class AppCakeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AppCakeFileRunner.class.getName());

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
        String name = PlugUtils.getStringBetween(content, "<h1>", "</h1>").trim();
        String version = PlugUtils.getStringBetween(content, "<h4>Version:</h4>", "</").trim();
        String size = PlugUtils.getStringBetween(content, "<h4>Size:</h4>", "</").trim();
        httpFile.setFileName(name + " - v" + version);
        try {
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(size));
        } catch(Exception x) { /**/ }
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
            Matcher match = PlugUtils.matcher("<a href=\"(.*download.php.+?)\"", contentAsString);
            if (!match.find())
                throw new PluginImplementationException("Download link not found");
            String url = getMethodBuilder().setReferer(fileURL).setAction(match.group(1)).getEscapedURI();
            if (!makeRedirectedRequest(getGetMethod(url))) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            url = PlugUtils.getStringBetween(getContentAsString(), "URL=", "\"");
            httpFile.setNewURL(new URL(url));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")|| contentAsString.contains("<h1></h1>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}