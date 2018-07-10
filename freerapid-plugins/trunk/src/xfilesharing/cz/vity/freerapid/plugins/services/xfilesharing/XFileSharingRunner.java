package cz.vity.freerapid.plugins.services.xfilesharing;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.*;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @author ntoskrnl
 * @author birchie
 */
public abstract class XFileSharingRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(XFileSharingRunner.class.getName());

    private final static Map<Class<?>, LoginData> LOGIN_CACHE = new WeakHashMap<Class<?>, LoginData>(2);
    private final static String DOWNLOAD_LINK_DATA = "downloadLinkData";

    private final List<CaptchaType> captchaTypes = getCaptchaTypes();
    protected boolean directDownload = false;
    protected boolean downloadFromCache = false;

    protected List<CaptchaType> getCaptchaTypes() {
        final List<CaptchaType> captchaTypes = new LinkedList<CaptchaType>();
        captchaTypes.add(new ReCaptchaType());
        captchaTypes.add(new FourTokensCaptchaType());
        captchaTypes.add(new CaptchasCaptchaType());
        captchaTypes.add(new SolveMediaCaptchaType());
        return captchaTypes;
    }

    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = new LinkedList<FileNameHandler>();
        fileNameHandlers.add(new FileNameHandlerA());
        fileNameHandlers.add(new FileNameHandlerB());
        fileNameHandlers.add(new FileNameHandlerC());
        return fileNameHandlers;
    }

    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = new LinkedList<FileSizeHandler>();
        fileSizeHandlers.add(new FileSizeHandlerA());
        fileSizeHandlers.add(new FileSizeHandlerB());
        fileSizeHandlers.add(new FileSizeHandlerC());
        fileSizeHandlers.add(new FileSizeHandlerD());
        return fileSizeHandlers;
    }

    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = new LinkedList<String>();
        downloadPageMarkers.add("File Download Link Generated");
        downloadPageMarkers.add("This direct link will be ");
        return downloadPageMarkers;
    }

    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = new LinkedList<String>();
        downloadLinkRegexes.add("<a href\\s?=\\s?(?:\"|')(http.+?" + Pattern.quote(httpFile.getFileName()) + ")(?:\"|')");
        downloadLinkRegexes.add("<a href\\s?=\\s?(?:\"|')(http.+?" + Pattern.quote(PlugUtils.unescapeHtml(httpFile.getFileName())) + ")(?:\"|')");
        return downloadLinkRegexes;
    }

    protected List<String> getFalseProblemRegexes() {
        final List<String> falseProblemRegexes = new LinkedList<String>();
        falseProblemRegexes.add("<font[^<>]+?visibility:hidden.+?</font>");
        falseProblemRegexes.add("<font[^<>]+?font-size:0.+?</font>");
        falseProblemRegexes.add("<div[^<>]*display\\s*:\\s*none.+?</div>");
        falseProblemRegexes.add("(?s)<div[^<>]*color\\s*:\\s*transparent.+?</div>");
        falseProblemRegexes.add("(?s)<td[^<>]*color\\s*:\\s*transparent.+?</td>");
        return falseProblemRegexes;
    }

    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        return getXFSMethodBuilder(getContentAsString());
    }

    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        return getXFSMethodBuilder(content, "method_");
    }

    protected MethodBuilder getXFSMethodBuilder(final String content, final String tag) throws Exception {
        final MethodBuilder methodBuilder = getMethodBuilder(content)
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains(tag, true)
                .setAction(fileURL)
                .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        if ((methodBuilder.getParameters().get("method_free") != null) && (!methodBuilder.getParameters().get("method_free").isEmpty())) {
            methodBuilder.removeParameter("method_premium");
        }
        return methodBuilder;
    }

    protected void correctURL() throws Exception {
    }

    protected boolean stepProcessFolder() throws Exception {
        return false;
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        correctURL();
        setLanguageCookie();
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
        super.run();
        correctURL();
        setLanguageCookie();
        logger.info("Starting download in TASK " + fileURL);
        if (handleDownloadFromCache()) {
            return;
        }
        login();
        HttpMethod method = getGetMethod(fileURL);
        int httpStatus = client.makeRequest(method, false);
        if (httpStatus / 100 == 3) {
            if (handleRedirection(method)) {
                return;
            }
        } else if (httpStatus != 200) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        checkFileProblems();
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
            httpStatus = client.makeRequest(method, false);
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
        saveDlLinkToCacheAndDownload(method);
    }

    protected void doDownload(final HttpMethod method) throws Exception {
        logger.info("DirectDownload: " + directDownload);
        logger.info("DownloadFromCache: " + downloadFromCache);
        setFileStreamContentTypes("text/plain");
        //some servers prefer to GZIP certain downloads, which we don't want
        method.removeRequestHeader("Accept-Encoding");
        if (!tryDownloadAndSaveFile(method)) {
            checkDownloadProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    /**
     * Handle redirection for the first request
     *
     * @param method method to be checked/handled its redirection
     * @return true if it's handled as direct download, otherwise false
     * @throws Exception when there's file problem, connection problem, etc
     */
    protected boolean handleRedirection(HttpMethod method) throws Exception {
        HttpMethod redirectMethod = redirectToLocation(method);
        String location = redirectMethod.getURI().getEscapedURI();

        URI fileUri = new URI(fileURL, true, "UTF-8");
        URI locationUri = new URI(location, true, "UTF-8");

        //Two kinds of redirection will be checked:
        //1. http -> https redirection
        //2. New (sub) domain redirection
        if (location.replaceFirst("^https?://", "").equals(fileURL.replaceFirst("^https?://", ""))) {
            logger.info("http -> https redirection");
        } else if (new URI(locationUri.getScheme(), locationUri.getAuthority(), fileUri.getPath(), fileUri.getQuery(), fileUri.getFragment())
                .equals(locationUri)) {
            logger.info("New (sub) domain redirection: " + fileUri.getScheme() + "://" + fileUri.getAuthority() + " -> " +
                    locationUri.getScheme() + "://" + locationUri.getAuthority());
        } else {
            return handleDirectDownload(method);
        }

        fileURL = location;
        int httpStatus = client.makeRequest(redirectMethod, false);
        if (httpStatus / 100 == 3) {
            return handleDirectDownload(redirectMethod);
        } else if (httpStatus != 200) {
            checkFileProblems();
            throw new ServiceConnectionProblemException();
        }
        return false;
    }

    @Override
    protected String getBaseURL() {
        try {
            URL url = new URL(fileURL);
            return url.getProtocol() + "://" + url.getAuthority();
        } catch (MalformedURLException e) {
            return super.getBaseURL();
        }
    }

    /**
     * Handle direct download
     *
     * @param method HttpMethod to be passed on to the next step
     * @return true if it's handled as direct download, false otherwise
     * @throws Exception something goes wrong
     */
    protected boolean handleDirectDownload(HttpMethod method) throws Exception {
        logger.info("Direct download");
        if (httpFile.getFileName() == null) {
            //if runCheck hasn't been run yet, we need to do it here, without login cookies
            final Cookie[] cookies = client.getHTTPClient().getState().getCookies();
            client.getHTTPClient().getState().clearCookies();
            runCheck();
            client.getHTTPClient().getState().clearCookies();
            client.getHTTPClient().getState().addCookies(cookies);
        }
        method = redirectToLocation(method);
        directDownload = true;
        saveDlLinkToCacheAndDownload(method);
        return true;
    }

    protected String getCookieDomain() throws Exception {
        String host = new URL(getBaseURL()).getHost();
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        return "." + host;
    }

    protected void setLanguageCookie() throws Exception {
        addCookie(new Cookie(getCookieDomain(), "lang", "english", "/", 86400, false));
    }

    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        checkFileName();
        checkFileSize();
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    protected void checkFileName() throws ErrorDuringDownloadingException {
        for (final FileNameHandler fileNameHandler : getFileNameHandlers()) {
            try {
                fileNameHandler.checkFileName(httpFile, getContentAsString());
                logger.info("Name handler: " + fileNameHandler.getClass().getSimpleName());
                return;
            } catch (final ErrorDuringDownloadingException e) {
                //failed
            }
        }
        throw new PluginImplementationException("File name not found");
    }

    protected void checkFileSize() throws ErrorDuringDownloadingException {
        for (final FileSizeHandler fileSizeHandler : getFileSizeHandlers()) {
            try {
                fileSizeHandler.checkFileSize(httpFile, getContentAsString());
                logger.info("Size handler: " + fileSizeHandler.getClass().getSimpleName());
                return;
            } catch (final ErrorDuringDownloadingException e) {
                //failed
            }
        }
        throw new PluginImplementationException("File size not found");
    }

    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    protected void sleepWaitTime(final int waitTime, final long startTime) throws Exception {
        if (waitTime > 0) {
            //time taken to input password and captcha - no need to wait this time twice
            final int diffTime = (int) ((System.currentTimeMillis() - startTime) / 1000);
            if (waitTime > diffTime) {
                downloadTask.sleep(waitTime - diffTime);
            }
        }
    }

    protected void stepPassword(final MethodBuilder methodBuilder) throws Exception {
        if (getContentAsString().contains("<input type=\"password\" name=\"password\" class=\"myForm\">")) {
            final String serviceTitle = ((XFileSharingServiceImpl) getPluginService()).getServiceTitle();
            final String password = getDialogSupport().askForPassword(serviceTitle);
            if (password == null) {
                throw new NotRecoverableDownloadException("This file is secured with a password");
            }
            methodBuilder.setParameter("password", password);
        }
    }

    protected boolean stepCaptcha(final MethodBuilder methodBuilder) throws Exception {
        for (final CaptchaType captchaType : captchaTypes) {
            if (captchaType.canHandle(getContentAsString())) {
                client.setReferer(fileURL);
                logger.info("Captcha type: " + captchaType.getClass().getSimpleName());
                captchaType.handleCaptcha(methodBuilder, client, getCaptchaSupport(), downloadTask);
                return (captchaType instanceof ReCaptchaType);
            }
        }
        return false;
    }

    protected boolean checkDownloadPageMarker() {
        for (final String downloadPageMarker : getDownloadPageMarkers()) {
            if (getContentAsString().contains(downloadPageMarker)) {
                return true;
            }
        }
        return false;
    }

    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        for (final String downloadLinkRegex : getDownloadLinkRegexes()) {
            final Matcher matcher = getMatcherAgainstContent(downloadLinkRegex);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        throw new PluginImplementationException("Download link not found");
    }

    protected HttpMethod redirectToLocation(final HttpMethod method) throws Exception {
        final Header locationHeader = method.getResponseHeader("Location");
        if (locationHeader == null) {
            throw new PluginImplementationException("Invalid redirect");
        }
        return getMethodBuilder()
                .setReferer(fileURL)
                .setAction(locationHeader.getValue())
                .toGetMethod();
    }

    protected String removeFalseProblemsByRegexes(final String content) {
        String newContent = content;
        for (final String falseProblemRegex : getFalseProblemRegexes()) {
            newContent = newContent.replaceAll(falseProblemRegex, "");
        }
        return newContent;
    }

    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        checkFileProblems(removeFalseProblemsByRegexes(getContentAsString()));
    }

    protected void checkFileProblems(final String content) throws ErrorDuringDownloadingException {
        if (content.contains("File Not Found")
                || content.contains("file was removed")
                || content.contains("file has been removed")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("server is in maintenance mode")) {
            throw new ServiceConnectionProblemException("This server is in maintenance mode. Please try again later.");
        }
    }

    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        checkDownloadProblems(removeFalseProblemsByRegexes(getContentAsString()));
    }

    protected void checkDownloadProblems(final String content) throws ErrorDuringDownloadingException {
        if (content.contains("till next download") || content.contains("You have to wait")) {
            final Matcher matcher = getMatcherAgainstContent("(?:(\\d+) hours?, )?(?:(\\d+) minutes?, )?(?:(\\d+) seconds?)");
            int waitHours = 0, waitMinutes = 0, waitSeconds = 0;
            String waitStr = "";
            if (matcher.find()) {
                waitStr = matcher.group();
                if (matcher.group(1) != null) {
                    waitHours = Integer.parseInt(matcher.group(1));
                }
                if (matcher.group(2) != null) {
                    waitMinutes = Integer.parseInt(matcher.group(2));
                }
                waitSeconds = Integer.parseInt(matcher.group(3));
            }
            final int waitTime = (waitHours * 60 * 60) + (waitMinutes * 60) + waitSeconds;
            throw new YouHaveToWaitException("You have to wait " + waitStr, waitTime);
        }
        if (content.contains("Undefined subroutine")) {
            throw new PluginImplementationException("Plugin is broken - Undefined subroutine");
        }
        if (content.contains("Skipped countdown")) {
            throw new PluginImplementationException("Plugin is broken - Skipped countdown");
        }
        if (content.contains("file reached max downloads limit")) {
            throw new ServiceConnectionProblemException("This file reached max downloads limit");
        }
        if (content.contains("You can download files up to")) {
            throw new NotRecoverableDownloadException(PlugUtils.getStringBetween(content, "<div class=\"err\">", "<br>"));
        }
        if (content.contains("have reached the download limit")
                || content.contains("have reached the download-limit")) {
            throw new YouHaveToWaitException("You have reached the download limit", 10 * 60);
        }
        if (content.contains("Error happened when generating Download Link")) {
            throw new YouHaveToWaitException("Error happened when generating download link", 60);
        }
        if (content.contains("Free Download Closed")) {
            throw new ServiceConnectionProblemException("Reached free download limit, wait or try premium");
        }
        if (content.contains("file is available to premium users only")
                || content.contains("this file requires premium to download")) {
            throw new NotRecoverableDownloadException("This file is only available to premium users");
        }
        if (content.contains("Wrong password")) {
            throw new ServiceConnectionProblemException("Wrong password");
        }
    }

    protected boolean login() throws Exception {
        synchronized (getClass()) {
            final PremiumAccount pa = ((XFileSharingServiceImpl) getPluginService()).getConfig();
            if (pa == null || !pa.isSet()) {
                LOGIN_CACHE.remove(getClass());
                logger.info("No account data set, skipping login");
                return false;
            }
            final LoginData loginData = LOGIN_CACHE.get(getClass());
            if (loginData == null || !pa.equals(loginData.getPa()) || loginData.isStale()) {
                logger.info("Logging in");
                doLogin(pa);
                final Cookie xfss = getCookieByName("xfss");
                if (xfss == null) {
                    throw new PluginImplementationException("Login cookie not found");
                }
                LOGIN_CACHE.put(getClass(), new LoginData(pa, xfss.getValue()));
            } else {
                logger.info("Login data cache hit");
                addCookie(new Cookie(getCookieDomain(), "xfss", loginData.getXfss(), "/", 86400, false));
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
                .setActionFromFormByName("FL", true)
                .setParameter("login", pa.getUsername())
                .setParameter("password", pa.getPassword())
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        if (getContentAsString().contains("Incorrect Login or Password")) {
            throw new BadLoginException("Invalid account login information");
        }
    }

    protected boolean isErrorWithLongTimeAvailableLink() {
        return getContentAsString().contains("Wrong IP");
    }

    protected List<String> getLongTimeAvailableLinkRegexes() {
        final List<String> longTimeAvailableLinkRegexes = new LinkedList<String>();
        longTimeAvailableLinkRegexes.add("direct link will be available for your IP next (.+)");
        return longTimeAvailableLinkRegexes;
    }

    /**
     * Get time available for download link cache.
     * <p>
     * Direct download should never read the stream's content, as it is not text type.
     * So direct download should return an arbitrary value.
     * <p>
     * General case of overriding for direct download would look like:
     * <pre><code>
     *
     *     protected long getLongTimeAvailableLinkFromRegexes() {
     *       if (directDownload)
     *         return 10 * 60 * 60 * 1000; //10 hours;
     *       return super.getLongTimeAvailableFromRegexes();
     *     }
     * </code></pre>
     *
     * @return Time available for download link cache in milliseconds
     */
    protected long getLongTimeAvailableLinkFromRegexes() {
        if (!directDownload) {
            try {
                for (final String longTimeAvailableLinkRegexes : getLongTimeAvailableLinkRegexes()) {
                    final Matcher match = getMatcherAgainstContent(longTimeAvailableLinkRegexes);
                    if (match.find()) {
                        final Matcher matcher = PlugUtils.matcher("(?:(\\d+) hours?)?[,\\s]*(?:(\\d+) minutes?)?[,\\s]*(?:(\\d+) seconds?)?", match.group(1));
                        if (matcher.find()) {
                            int waitHours = 0, waitMinutes = 0, waitSeconds = 0;
                            if (matcher.group(1) != null)
                                waitHours = Integer.parseInt(matcher.group(1));
                            if (matcher.group(2) != null)
                                waitMinutes = Integer.parseInt(matcher.group(2));
                            if (matcher.group(3) != null)
                                waitSeconds = Integer.parseInt(matcher.group(3));

                            return ((waitHours * 60 * 60) + (waitMinutes * 60) + waitSeconds) * 1000; //in millisec
                        }
                    }
                }
            } catch (Exception e) {
                LogUtils.processException(logger, e);
            }
            return -1;
        } else {
            return 8 * 60 * 60 * 1000; //8 hours for direct download.
        }
    }

    /**
     * Handling download from cache
     *
     * @return true if the file will be downloaded from cache, otherwise false.
     */
    protected boolean handleDownloadFromCache() throws Exception {
        Cookie[] cookies = getCookies();
        String downloadLink = null;
        DownloadLinkData downloadLinkData = null;
        try {
            String downloadLinkDataStr = (String) httpFile.getProperties().get(DOWNLOAD_LINK_DATA);
            if (downloadLinkDataStr != null) {
                downloadLinkData = loadDlDataFromString(downloadLinkDataStr);
            }
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }
        if (downloadLinkData != null && downloadLinkData.isExpired()) {
            downloadLinkData = null;
            logger.info("Download link cache is expired");
            logger.info("Removing download link cache");
            httpFile.getProperties().remove(DOWNLOAD_LINK_DATA);
        }
        if (downloadLinkData != null) {
            downloadLink = downloadLinkData.getDownloadLink();
        }
        if (downloadLink != null) {
            try {
                logger.info("Trying to download using download link cache");
                client.getHTTPClient().getState().clearCookies();
                client.getHTTPClient().getState().addCookies(downloadLinkData.getCookies());
                HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(downloadLinkData.getDownloadLink()).toGetMethod();
                downloadFromCache = true;
                doDownload(httpMethod);
                return true;
            } catch (Exception e) {
                logger.warning("Downloading using download link cache failed");
                handleDownloadFromCacheFailure(e);
            }
        }
        client.getHTTPClient().getState().clearCookies();
        client.getHTTPClient().getState().addCookies(cookies);
        downloadFromCache = false;
        return false;
    }

    protected void handleDownloadFromCacheFailure(Exception e) throws Exception {
        if (e.getMessage().equals("Error starting download")) {
            if (isErrorWithLongTimeAvailableLink()) {
                logger.warning("Download link cache - wrong IP address");
                logger.info("Removing download link cache");
                httpFile.getProperties().remove(DOWNLOAD_LINK_DATA);
            } else {
                LogUtils.processException(logger, e);
            }
        } else {
            LogUtils.processException(logger, e);
        }
    }

    /**
     * Save download link to cache and then download the file
     *
     * @param method method to be passed on to {@link #doDownload}
     * @throws Exception when download link data conversion failed or
     *                   when download process is failed.
     */
    protected void saveDlLinkToCacheAndDownload(HttpMethod method) throws Exception {
        long linkAvailTime = getLongTimeAvailableLinkFromRegexes();
        if (linkAvailTime > 0) {
            long created = System.currentTimeMillis();
            String downloadLink = method.getURI().toString();
            DownloadLinkData downloadLinkData = new DownloadLinkData(downloadLink, created, linkAvailTime, getCookies());
            String downloadLinkDataStr = convertDlDataToString(downloadLinkData);
            logger.info("Saving download link cache: " + downloadLink);
            httpFile.getProperties().put(DOWNLOAD_LINK_DATA, downloadLinkDataStr);
        }
        doDownload(method);
    }

    /**
     * Load download link data from XML string
     * <p>
     * In the future, can be simplified with: {@link cz.vity.freerapid.plugins.webclient.AbstractFileShareService#loadConfigFromString(String, Class)} call
     *
     * @param content XML string representation of download link data
     * @return download link data object
     * @throws Exception when download link data binaries/formats are incompatible between versions.
     */
    @SuppressWarnings("unchecked")
    protected DownloadLinkData loadDlDataFromString(String content) throws Exception {
        XMLDecoder xmlDecoder = null;
        try {
            xmlDecoder = new XMLDecoder(new ByteArrayInputStream(content.getBytes()), null, null, DownloadLinkData.class.getClassLoader());
            return (DownloadLinkData) xmlDecoder.readObject();
        } catch (Exception e) {
            LogUtils.processException(logger, e);
            throw e;
        } finally {
            if (xmlDecoder != null) {
                try {
                    xmlDecoder.close();
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
    }

    /**
     * Convert download link data to XML string
     * <p>
     * In the future, can be simplified with: {@link cz.vity.freerapid.plugins.webclient.AbstractFileShareService#convertConfigToString(Object)} call
     *
     * @param object Downlink link data object
     * @return XML string representation of download link data
     * @throws Exception Conversion went wrong
     */
    protected String convertDlDataToString(Object object) throws Exception {
        XMLEncoder xmlEncoder = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            xmlEncoder = new XMLEncoder(baos);
            xmlEncoder.writeObject(object);
        } catch (Exception e) {
            LogUtils.processException(logger, e);
            throw e;
        } finally {
            if (xmlEncoder != null) {
                try {
                    xmlEncoder.close();
                } catch (Exception e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        String result = new String(baos.toByteArray());
        try {
            baos.close();
        } catch (IOException e) {
            LogUtils.processException(logger, e);
        }
        return result;
    }

    private static class LoginData {
        private final static long MAX_AGE = 86400000;//1 day
        private final long created;
        private final PremiumAccount pa;
        private final String xfss;

        public LoginData(final PremiumAccount pa, final String xfss) {
            this.created = System.currentTimeMillis();
            this.pa = pa;
            this.xfss = xfss;
        }

        public boolean isStale() {
            return System.currentTimeMillis() - created > MAX_AGE;
        }

        public PremiumAccount getPa() {
            return pa;
        }

        public String getXfss() {
            return xfss;
        }
    }

}