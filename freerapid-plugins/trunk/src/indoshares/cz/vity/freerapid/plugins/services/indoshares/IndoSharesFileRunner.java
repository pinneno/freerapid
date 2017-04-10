package cz.vity.freerapid.plugins.services.indoshares;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class IndoSharesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(IndoSharesFileRunner.class.getName());
    private final static Map<Class<?>, LoginData> LOGIN_CACHE = new WeakHashMap<Class<?>, LoginData>(2);

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("(?s)<strong class=\"custom-file-text\"[^<>]+>([^<>]+)\\(([\\s\\d\\.,]+?(?:bytes|.B|.b))\\s*\\)", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name or size not found");
        }
        httpFile.setFileName(matcher.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);

            int waitTime = 20;
            Matcher matcher = getMatcherAgainstContent("var seconds\\s*?=\\s*?(\\d+);");
            if (matcher.find()) {
                waitTime = Integer.parseInt(matcher.group(1));
            }
            downloadTask.sleep(waitTime + 1);

            matcher = getMatcherAgainstContent("<a[^<>]*? href=['\"]([^'\"]+)['\"][^<>]*?>Download</a");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download page not found");
            }

            final String downloadPage = matcher.group(1);
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(downloadPage).toGetMethod();
            int httpStatus = client.makeRequest(httpMethod, false);
            if (httpStatus / 100 != 3) { //no login => no direct download
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();

                final String referer = httpMethod.getURI().toString();
                final boolean hasCaptcha = getContentAsString().contains("data-sitekey=\"");
                MethodBuilder mb = getMethodBuilder().setReferer(referer).setActionFromFormWhereTagContains("form-join", true);
                if (hasCaptcha) {
                    matcher = getMatcherAgainstContent("data-sitekey\\s*?=\\s*?\"([^\"]+)\"");
                    if (!matcher.find()) {
                        throw new PluginImplementationException("ReCaptcha key not found");
                    }
                    String captchaKey = matcher.group(1);
                    ReCaptchaNoCaptcha reCaptchaNoCaptcha = new ReCaptchaNoCaptcha(captchaKey, referer);
                    reCaptchaNoCaptcha.modifyResponseMethod(mb);
                }

                //workaround for bug in DownloadClient.makeRequestFile() for https redirection
                httpMethod = mb.toPostMethod();
                httpStatus = client.makeRequest(httpMethod, false);
                if (httpStatus / 100 != 3) {
                    throw new PluginImplementationException("Download link not found");
                }
            }

            final Header locationHeader = httpMethod.getResponseHeader("Location");
            if (locationHeader == null) {
                throw new PluginImplementationException("Invalid redirect");
            }
            httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(locationHeader.getValue())
                    .toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) { //they don't provide "File Not Found" page
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    protected boolean login() throws Exception {
        synchronized (getClass()) {
            final PremiumAccount pa = ((IndoSharesServiceImpl) getPluginService()).getConfig();
            if (pa == null || !pa.isSet()) {
                LOGIN_CACHE.remove(getClass());
                logger.info("No account data set, skipping login");
                return false;
            }
            final LoginData loginData = LOGIN_CACHE.get(getClass());
            if (loginData == null || !pa.equals(loginData.getPa()) || loginData.isStale()) {
                logger.info("Logging in");
                doLogin(pa);
                LOGIN_CACHE.put(getClass(), new LoginData(pa));
            } else {
                logger.info("Login data cache hit");
            }
            return true;
        }
    }

    protected void doLogin(final PremiumAccount pa) throws Exception {
        HttpMethod method = getMethodBuilder()
                .setReferer(getBaseURL())
                .setAction(getBaseURL() + "/login.html")
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        method = getMethodBuilder()
                .setReferer(getBaseURL() + "/login.html")
                .setAction(getBaseURL())
                .setActionFromFormWhereTagContains("form_login", true)
                .setParameter("username", pa.getUsername())
                .setParameter("password", pa.getPassword())
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        if (getContentAsString().contains("\"error-message-container\">Your username and password are invalid")) {
            throw new BadLoginException("Invalid account login information");
        }
    }

    private static class LoginData {
        private final static long MAX_AGE = 86400000;//1 day
        private final long created;
        private final PremiumAccount pa;

        LoginData(final PremiumAccount pa) {
            this.created = System.currentTimeMillis();
            this.pa = pa;
        }

        boolean isStale() {
            return System.currentTimeMillis() - created > MAX_AGE;
        }

        public PremiumAccount getPa() {
            return pa;
        }
    }

}