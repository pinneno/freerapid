package cz.vity.freerapid.plugins.services.filehippo;

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
class FileHippoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileHippoFileRunner.class.getName());

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
        Matcher match = PlugUtils.matcher("File\\s*name\\s*:\\s*(?:<[^>]+>\\s*)*([^<>]+?)<", content);
        if (match.find())
            httpFile.setFileName(match.group(1).trim());
        else
            PlugUtils.checkName(httpFile, content, "title\" content=\"", "\" />");
        match = PlugUtils.matcher("File\\s*size\\s*:\\s*(?:<[^>]+>\\s*)*([^<>]+?)<", content);
        if (match.find())
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1).trim()));
        else
            PlugUtils.checkFileSize(httpFile, content, "<span class=\"normal\">(", ")</span>");
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
            final Matcher match = PlugUtils.matcher("(?s)<a[^>]+?program-header-download-link.+?href=\"([^\"]+?)\"", contentAsString);
            if (!match.find())
                throw new PluginImplementationException("Download page not found");
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL).setAction(match.group(1)).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            HttpMethod dlMethod = getMethodBuilder().setActionFromAHrefWhereATagContains("downloading-icon").toGetMethod();
            if (!tryDownloadAndSaveFile(dlMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("the page you requested could not be found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}