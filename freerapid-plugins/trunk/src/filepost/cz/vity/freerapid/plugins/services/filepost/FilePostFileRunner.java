package cz.vity.freerapid.plugins.services.filepost;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author CrazyCoder
 * @author ntoskrnl
 */
class FilePostFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilePostFileRunner.class.getName());
    private final static Lock LOCK = new ReentrantLock(true);
    private static volatile long lastRequest;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequestWithSleep(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>FilePost.com: Download", "- fast &amp; secure!</title>");
        PlugUtils.checkFileSize(httpFile, content, "<span>Size:</span>", "</li>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRequestWithSleep(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);

            fileURL = method.getURI().toString();

            final Cookie sid = getCookieByName("SID");
            if (sid == null) {
                throw new PluginImplementationException("SID cookie not found");
            }

            final Matcher matcher = PlugUtils.matcher("/files/([^/]+)", fileURL);
            if (!matcher.find()) {
                throw new PluginImplementationException("Error parsing file URL");
            }
            final String code = matcher.group(1);
            logger.info("Code: " + code);

            final String captchaKey = PlugUtils.getStringBetween(getContentAsString(), "key:\t\t\t'", "',");
            logger.info("Captcha key: " + captchaKey);

            HttpMethod ajax = null;

            while (true) {
                if (ajax == null) ajax = ajaxBuilder(sid, code, true).toPostMethod();

                if (!makeRequestWithSleep(ajax)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }

                String content = getContentAsString();
                logger.info(content);

                if (content.contains("wait_time")) {
                    int wait = 60;
                    try {
                        // please don't use PlugUtils.getNumberBetween, as it will throw exception on negative time value
                        // which is perfectly valid, see the logic below
                        wait = Integer.parseInt(PlugUtils.getStringBetween(content, "\"wait_time\":\"", "\"}}"));
                    } catch (NumberFormatException ignored) {
                    }

                    logger.info("wait: " + wait);

                    if (wait > 0) {
                        downloadTask.sleep(wait + 1);
                        ajax = null;
                    } else {
                        // show captcha
                        final ReCaptcha r = new ReCaptcha(captchaKey, client);
                        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
                        if (captcha == null) {
                            throw new CaptchaEntryInputMismatchException();
                        }
                        r.setRecognized(captcha);
                        ajax = r.modifyResponseMethod(ajaxBuilder(sid, code, false)).toPostMethod();
                    }
                } else if (content.contains("{\"answer\":{\"link\":\"")) {
                    final String fileUrl = unescape(PlugUtils.getStringBetween(content, "{\"link\":\"", "\"}}"));
                    logger.info("FILE: " + fileUrl);
                    method = getMethodBuilder().setReferer(fileURL).setAction(fileUrl).toGetMethod();
                    setFileStreamContentTypes("\"application/octet-stream\"");
                    if (!tryDownloadAndSaveFile(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                    break;
                } else {
                    checkProblems();
                    throw new PluginImplementationException();
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String unescape(String string) {
        return string.replaceAll("\\\\", "");
    }

    private MethodBuilder ajaxBuilder(Cookie sid, String code, boolean start) throws URIException, PluginImplementationException {
        final Cookie time = getCookieByName("time");
        if (time == null) {
            throw new PluginImplementationException("Time cookie not found");
        }

        final String startUrl = "http://filepost.com/files/get/?SID=" + sid.getValue() + "&JsHttpRequest=" + time.getValue() + "-xml";
        logger.info("Start URL: " + startUrl);

        MethodBuilder mb = getMethodBuilder();
        mb = mb.setReferer(fileURL)
                .setAction(startUrl)
                .setParameter("code", code);
        if (start) mb = mb.setParameter("action", "set_download");
        return mb;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        } else if (contentAsString.contains("already downloading a file")) {
            throw new YouHaveToWaitException("Your IP address is already downloading a file at the moment", 60);
        } else if (contentAsString.contains("{\"error\":\"")) {
            final String error = unescape(PlugUtils.getStringBetween(contentAsString, "{\"error\":\"", "\"},"));
            logger.warning(error);
            if (error.contains("You entered a wrong CAPTCHA code")) {
                throw new YouHaveToWaitException(error, 4);
            } else {
                throw new ServiceConnectionProblemException(error);
            }
        }
    }

    private boolean makeRequestWithSleep(final HttpMethod method) throws Exception {
        LOCK.lockInterruptibly();
        try {
            final long interval = 500L;
            if (lastRequest > System.currentTimeMillis() - interval) {
                Thread.sleep(interval);
            }
            final boolean b = makeRedirectedRequest(method);
            lastRequest = System.currentTimeMillis();
            return b;
        } finally {
            LOCK.unlock();
        }
    }

}
