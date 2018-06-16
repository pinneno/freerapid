package cz.vity.freerapid.plugins.services.gett;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class GettFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GettFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        checkUrl();
        super.runCheck();
        final HttpMethod method = getGetMethod(getApiUrl(fileURL));
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkUrl() throws Exception {
        if (fileURL.contains("ge.tt/api/")) {
            final Matcher match = PlugUtils.matcher("ge.tt/api/\\d+?/\\w+?/(.+?)/", fileURL);
            if (!match.find()) throw new InvalidURLOrServiceProblemException("Url format error");
            fileURL = "http://ge.tt/" + match.group(1) + "/v/0";
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, getContentAsString(), "filename\":\"", "\",");
        PlugUtils.checkFileSize(httpFile, getContentAsString(), "size\":", ",");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private String getApiUrl(String url) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("ge.tt/(\\w+)", url);
        if (!matcher.find())
            throw new PluginImplementationException("File id not found");
        return "http://api.ge.tt/1/shares/" + matcher.group(1).trim();
    }

    @Override
    public void run() throws Exception {
        checkUrl();
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(getApiUrl(fileURL));
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
            Matcher matcher = PlugUtils.matcher("ge.tt/(\\w+)", fileURL);
            if (!matcher.find())
                throw new PluginImplementationException("File id not found");
            String link = "http://api.ge.tt/1/files/" + matcher.group(1).trim() + "/0/blob?download";
            setFileStreamContentTypes("text/plain");
            method = getMethodBuilder().setReferer(fileURL).setAction(link).toGetMethod();
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}