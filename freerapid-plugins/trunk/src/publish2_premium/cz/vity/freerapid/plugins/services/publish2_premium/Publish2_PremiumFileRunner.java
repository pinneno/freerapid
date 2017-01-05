package cz.vity.freerapid.plugins.services.publish2_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Publish2_PremiumFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Publish2_PremiumFileRunner.class.getName());

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
        final Matcher match = PlugUtils.matcher("Download file:(?:<[^<>]*>|\\s)*(.+?)\\s*<", content);
        if (!match.find())
            throw new PluginImplementationException("File name not found");
        httpFile.setFileName(match.group(1).trim());
        PlugUtils.checkFileSize(httpFile, content, "File size: ", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        HttpMethod method = getGetMethod(fileURL);
        final int status = client.makeRequest(method, false);
        if (status / 100 == 3) {
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else if (status == 200) {
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download").toGetMethod();
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
        if (contentAsString.contains("This file is no longer available")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Traffic limit exceed")) {
            throw new ServiceConnectionProblemException("Traffic limit exceed");
        }
    }

    private final static String loginURL = "http://publish2.me/login.html";
    private final static long MAX_AGE = 6 * 3600000;//6 hours
    private static long created = 0;
    private static Cookie sessionId;
    private static PremiumAccount pa0 = null;

    public void setLoginData(final PremiumAccount pa) {
        pa0 = pa;
        sessionId = getCookieByName("sessid");
        created = System.currentTimeMillis();
    }

    public boolean isLoginStale(final PremiumAccount pa) {
        return (System.currentTimeMillis() - created > MAX_AGE) || (!pa0.getUsername().matches(pa.getUsername())) || (!pa0.getPassword().matches(pa.getPassword()));
    }

    private void login() throws Exception {
        synchronized (Publish2_PremiumFileRunner.class) {
            Publish2_PremiumServiceImpl service = (Publish2_PremiumServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No Publish2 Premium account login information!");
                }
            }
            if (!isLoginStale(pa)) {
                addCookie(sessionId);
                logger.info("Logged in... with cookie");
            } else {
                final GetMethod getMethod = getGetMethod(loginURL);//make first request
                if (!makeRedirectedRequest(getMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                int verificationCount = 0;
                do {
                    final HttpMethod httpMethod = doCaptcha(getMethodBuilder().setReferer(loginURL)
                            .setActionFromFormWhereTagContains("login", true)
                            .setParameter("LoginForm[username]", pa.getUsername())
                            .setParameter("LoginForm[password]", pa.getPassword())
                            .setParameter("LoginForm[rememberMe]", "1")
                            ).toPostMethod();
                    final int status = client.makeRequest(httpMethod, false);
                    if (status / 100 == 3) {
                        // successfully logged in (trying to redirect to dl url)
                        setLoginData(pa);
                        logger.info("Logged in...");
                        return;
                    } else if (status / 100 != 2) {
                        throw new ServiceConnectionProblemException("Error posting login info");
                    }
                    verificationCount++;
                    if (verificationCount > 5)
                        throw new PluginImplementationException("Excessive incorrect verification codes entered");
                } while (getContentAsString().contains("The verification code is incorrect"));
                if (getContentAsString().contains("Incorrect username or password"))
                    throw new BadLoginException("Incorrect username or password!");
                if (getContentAsString().contains("Your account has been banned"))
                    throw new BadLoginException("Your account has been banned!");
                if (getContentAsString().contains("Please fix the following input errors"))
                    throw new BadLoginException("Login error occurred!");
                logger.info("Logged in !");
                setLoginData(pa);
            }
        }
    }
    private MethodBuilder doCaptcha(final MethodBuilder builder) throws Exception {
        if (getContentAsString().contains("/auth/captcha.html")) {
            final String captchaImg = getMethodBuilder().setActionFromImgSrcWhereTagContains("/auth/captcha.html").getEscapedURI();
            final CaptchaSupport captchaSupport = getCaptchaSupport();
            final String captchaTxt = captchaSupport.getCaptcha(captchaImg);
            if (captchaTxt == null)
                throw new CaptchaEntryInputMismatchException();
            builder.setParameter("LoginForm[verifyCode]", captchaTxt);
        }
        return builder;
    }
}