package cz.vity.freerapid.plugins.services.hotfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Kajda & Arthur Gunawan
 * @since 0.82
 */
class HotfileFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(HotfileFileRunner.class.getName());
    private static final String SERVICE_WEB = "http://hotfile.com";

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = checkURL(fileURL); //added support for http://hotfile.com/links/....
        final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).toHttpMethod();
        if (makeRedirectedRequest(httpMethod)) {
            checkSeriousProblems();
            checkNameAndSize();
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = checkURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkAllProblems();
            checkNameAndSize();

            if (getContentAsString().contains("var timerend=0;")) {
                processDownloadWithForm();
            } else {
                downloadFile();
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL or service problem");
        }
    }

    private void checkSeriousProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();

        if (contentAsString.isEmpty() || contentAsString.contains("404 - Not Found") || contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File was not found");
        }
    }

    private void checkAllProblems() throws ErrorDuringDownloadingException {
        checkSeriousProblems();
        final String contentAsString = getContentAsString();

        if (contentAsString.contains("Your download expired")) {
            throw new YouHaveToWaitException("Your download expired", 60);
        }

        final Matcher matcher = getMatcherAgainstContent("([0-9]+?);\\s*document.getElementById\\('dwltmr");

        if (matcher.find()) {
            final int waitTime = Integer.parseInt(matcher.group(1)) / 1000;

            if (waitTime > 0) {
                throw new YouHaveToWaitException("You reached your hourly traffic limit", waitTime);
            }
        }
    }

    private String checkURL(String cURL) throws Exception {   //added support for http://hotfile.com/links/....
        if (cURL.contains("hotfile.com/links/")) {
            final HttpMethod httpMethod = getMethodBuilder().setAction(cURL).toHttpMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkAllProblems();
                throw new PluginImplementationException();
            }
            final String xURL = PlugUtils.getStringBetween(getContentAsString(), "<input type=text size=85 value=\"", "\">");
            final String escapedURI = getMethodBuilder().setAction(xURL).toHttpMethod().getURI().getEscapedURI();
            logger.info("New Link : " + escapedURI);     //Debug purpose, show the new found link
            this.httpFile.setNewURL(new URL(escapedURI));
            return escapedURI;
        } else return cURL;
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {

        PlugUtils.checkFileSize(httpFile, getContentAsString(), "<span class=\"size\">| ", "</span>");
        PlugUtils.checkName(httpFile, getContentAsString(), "Downloading <b>", "</b>");

    }

    private void processDownloadWithForm() throws Exception {
        final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromFormByName("f", true).setBaseURL(SERVICE_WEB).toHttpMethod();
        final int waitTime = PlugUtils.getWaitTimeBetween(getContentAsString(), "timerend=d.getTime()+", ";", TimeUnit.MILLISECONDS);
        downloadTask.sleep(waitTime);

        if (makeRedirectedRequest(httpMethod)) {
            downloadFile();
        } else {
            throw new ServiceConnectionProblemException();
        }
    }

    private void downloadFile() throws Exception {

        if (getContentAsString().contains("Enter word above:")) {
            stepCaptcha();
            if (getContentAsString().contains("var timerend=0;")) {
                processDownloadWithForm();
                return;
            }
        }
        HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Click here to download").toHttpMethod();

        if (!tryDownloadAndSaveFile(httpMethod)) {
            checkAllProblems();
            logger.warning(getContentAsString());
            throw new IOException("File input stream is empty");
        }
    }

    private void stepCaptcha() throws Exception {

        CaptchaSupport captchaSupport = getCaptchaSupport();
        String host = "http://" + httpFile.getFileUrl().getHost();
        String s = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getAction();
        s = host + s;
        logger.info("Captcha URL " + s);
        String captcha = captchaSupport.getCaptcha(s);
        if (captcha == null) {
            throw new CaptchaEntryInputMismatchException();
        } else {
            final HttpMethod postMethod = getMethodBuilder().setBaseURL(SERVICE_WEB).setActionFromFormWhereTagContains("method=\"post\"", true).
                    setParameter("captcha", captcha).toPostMethod();

            if (!makeRedirectedRequest(postMethod)) {

                throw new ServiceConnectionProblemException();
            }
        }


    }

}
