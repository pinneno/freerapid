package cz.vity.freerapid.plugins.services.share108;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Share108FileRunner extends XFileSharingRunner {
    private final static Logger logger = Logger.getLogger(Share108FileRunner.class.getName());

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkName(httpFile, content, "dfree-filename\">", "&nbsp;");
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkFileSize(httpFile, content, "Size &nbsp; ", "<");
            }
        });
        return fileSizeHandlers;
    }

    private boolean isShortUrl() {
        return fileURL.contains("108.to/");
    }
    private void stepShortUrl() throws Exception {
        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        method = getMethodBuilder().setActionFromFormByIndex(1, true).toPostMethod();
        if (!makeRedirectedRequest(method)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        Matcher match = getMatcherAgainstContent("(?:\\[URL=|<a href=\")(.+?)(?:\\]\\[|\" target=_blank>)");
        if (!match.find())
            throw new PluginImplementationException("Long link not found");
        String url = match.group(1).trim();
        httpFile.setNewURL(new URL(url));
        httpFile.setState(DownloadState.QUEUED);
    }

    @Override
    public void runCheck() throws Exception {
        correctURL();
        setLanguageCookie();
        if (isShortUrl()) {
            return;
        }
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkFileProblems();
            checkNameAndSize();
        } else {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        correctURL();
        setLanguageCookie();
        logger.info("Starting download in TASK " + fileURL);
        if (isShortUrl()) {
            stepShortUrl();
            return;
        }
        login();
        HttpMethod method = getGetMethod(fileURL);
        if (!makeRedirectedRequest(method)) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
        fileURL = method.getURI().getURI();
        checkNameAndSize();
        checkDownloadProblems();
        if (stepProcessFolder()) {
            return;
        }
        for (int loopCounter = 0; ; loopCounter++) {
            if (loopCounter >= 8) {
                //avoid infinite loops
                throw new PluginImplementationException("Cannot proceed to download link");
            }
            final MethodBuilder methodBuilder = getXFSMethodBuilder();
            final int waitTime = getWaitTime();
            final long startTime = System.currentTimeMillis();
            stepPassword(methodBuilder);
            //skip the wait time if it is on the same page as a captcha of type ReCaptcha
            if (!stepCaptcha(methodBuilder)) {
                sleepWaitTime(waitTime, startTime);
            }
            method = methodBuilder.toPostMethod();
            int httpStatus = client.makeRequest(method, false);
            if (httpStatus / 100 == 3) {
                //redirect to download file location
                method = redirectToLocation(method);
                break;
            } else if (checkDownloadPageMarker()) {
                //page containing download link
                final String downloadLink = getDownloadLinkFromRegexes();
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(downloadLink)
                        .toGetMethod();
                break;
            }
            checkDownloadProblems();
        }
        doDownload(method);
    }

}