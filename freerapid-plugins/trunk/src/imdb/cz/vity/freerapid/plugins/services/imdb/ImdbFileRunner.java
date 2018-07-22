package cz.vity.freerapid.plugins.services.imdb;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.xml.ws.Service;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ImdbFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ImdbFileRunner.class.getName());
    private SettingsConfig config;

    private void loadConfig() throws Exception {
        ImdbServiceImpl service = (ImdbServiceImpl) getPluginService();
        config = service.getConfig();
    }
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
        PlugUtils.checkName(httpFile, content, "title\" content=\"", "\"");
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
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
            loadConfig();
            final String videoUrl = getPreferredQualityURL();
            final HttpMethod httpMethod = getGetMethod(videoUrl);
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
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("requested URL was not found on our server")
                || contentAsString.contains("404 Error") || contentAsString.contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private String getPreferredQualityURL() throws PluginImplementationException {
        String QualityMatcher = "definition\":\"%s\"[^{}]*Url\":\"(.+?)\"";
        for (VideoQuality pref : config.getVideoQuality()) {
            Matcher match = PlugUtils.matcher(String.format(QualityMatcher, pref.toString()), getContentAsString().replace("\\", ""));
            if (match.find()) {
                return match.group(1).trim();
            }
        }
        throw new PluginImplementationException("No videos found");
    }
}