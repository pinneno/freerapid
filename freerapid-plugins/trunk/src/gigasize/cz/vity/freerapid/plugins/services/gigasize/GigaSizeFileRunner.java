package cz.vity.freerapid.plugins.services.gigasize;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda
 * @author ntoskrnl
 */
class GigaSizeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GigaSizeFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<title>", "- GigaSize.com");
        PlugUtils.checkFileSize(httpFile, content, "File size: <strong>", "</strong>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        checkNameAndSize();

        String fileId = PlugUtils.getParameter("fileId", getContentAsString());
        method = getMethodBuilder().setReferer(fileURL).setAction("/getoken").setParameter("fileId", fileId).toPostMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        downloadTask.sleep(31);
        method = getMethodBuilder().setReferer(fileURL).setAction("/formtoken").toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        final String token = getContentAsString().trim();
        method = getMethodBuilder().setReferer(fileURL).setAction("/getoken").setParameter("fileId", fileId).setParameter("token", token).toPostMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        final Matcher matcher = getMatcherAgainstContent("\"redirect\"\\s*:\\s*\"(.+?)\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Download URL not found");
        }
        final String url = matcher.group(1).replace("\\/", "/");
        method = getMethodBuilder().setReferer(fileURL).setAction(url).toGetMethod();
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("The file you are looking for is not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

}