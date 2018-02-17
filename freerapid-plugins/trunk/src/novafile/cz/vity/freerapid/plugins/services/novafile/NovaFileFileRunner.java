package cz.vity.freerapid.plugins.services.novafile;

import cz.vity.freerapid.plugins.exceptions.BadLoginException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.ReCaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class NovaFileFileRunner extends XFileSharingRunner {

    @Override
    protected void correctURL() throws Exception {
        fileURL = fileURL.replaceFirst("https?://", "https://");
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new NovaFileFileSizeHandler());
        return fileSizeHandlers;
    }

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkName(httpFile, content, "class=\"name\">", "</");
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("This download link will be ");
        return downloadPageMarkers;
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("id=\"count\"[^>]*>(\\d+)<");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("can only be downloaded by Premium")) {
            throw new PluginImplementationException("This file is only available to premium users");
        }
        super.checkDownloadProblems();
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        final MethodBuilder methodBuilder = getMethodBuilder()
                .setReferer(fileURL)
                .setActionFromFormWhereTagContains("method_", true)
                .setAction(fileURL);
        if ((methodBuilder.getParameters().get("method_free") != null) && (!methodBuilder.getParameters().get("method_free").isEmpty())) {
            methodBuilder.removeParameter("method_premium");
        }
        return methodBuilder;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        final int index = httpFile.getFileName().indexOf("&#8230;");
        if (index > 0) {
            final String s = httpFile.getFileName().substring(0, index);
            downloadLinkRegexes.add("<a href\\s?=\\s?(?:\"|')(http.+?" + Pattern.quote(s) + ".+?)(?:\"|')");
        }
        return downloadLinkRegexes;
    }

    @Override
    protected void doLogin(PremiumAccount pa) throws Exception {
        HttpMethod method = getMethodBuilder()
                .setReferer(getBaseURL())
                .setAction(getBaseURL() + "/login")
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        method = getMethodBuilder()
                .setReferer(getBaseURL() + "/login")
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

    @Override
    protected boolean stepCaptcha(final MethodBuilder methodBuilder) throws Exception {
        final Matcher reCaptchaKeyMatcher = PlugUtils.matcher("data-sitekey=['\"]([^'\"]+?)['\"]", getContentAsString());
        if (!reCaptchaKeyMatcher.find())
            throw new PluginImplementationException("ReCaptcha key not found");

        Matcher captchaFormMatcher = getMatcherAgainstContent("post\\(\\s*\"([^\"]+)\",\\s*\\{((?:\\s*\"?[^\"{}:,]+\"?\\s*:\\s*\"?[^\"{}:,]+\"?,?)+)");
        if (!captchaFormMatcher.find())
            throw new PluginImplementationException("Captcha verification form not found");
        MethodBuilder builder = getMethodBuilder().setReferer(fileURL)
                .setAction(captchaFormMatcher.group(1).trim());
        Matcher captchaFormParametersMatcher = PlugUtils.matcher("\"?([^\"{}:,]+)\"?\\s*:\\s*\"?([^\"{}:,]+)\"?", captchaFormMatcher.group(2));
        while (captchaFormParametersMatcher.find())
            builder.setParameter(captchaFormParametersMatcher.group(1).trim(), captchaFormParametersMatcher.group(2).trim());

        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKeyMatcher.group(1).trim(), fileURL);
        r.modifyResponseMethod(builder);
        if (!makeRedirectedRequest(builder.toPostMethod()))
            throw new ServiceConnectionProblemException("Error submitting captcha");
        return true;
    }

    @Override
    protected List<CaptchaType> getCaptchaTypes() {
        final List<CaptchaType> captchaTypes = super.getCaptchaTypes();
        captchaTypes.add(new ReCaptchaType2());
        return captchaTypes;
    }

    public class ReCaptchaType2 extends ReCaptchaType {
        @Override
        protected String getReCaptchaKeyRegex() {
            return "(?:recaptcha/api/challenge\\?k=|data-sitekey=\")(.+?)\"";
        }
    }
}