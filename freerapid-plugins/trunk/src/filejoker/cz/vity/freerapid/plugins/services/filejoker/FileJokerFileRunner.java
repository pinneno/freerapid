package cz.vity.freerapid.plugins.services.filejoker;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.ReCaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FileJokerFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                final Matcher match = PlugUtils.matcher("class=\"name[^>]*?>(.+?)<", content);
                if (!match.find())
                    throw new PluginImplementationException("File name not found");
                httpFile.setFileName(match.group(1).trim());
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("This link will be available for ");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("<a href\\s?=\\s?(?:\"|')(http[^\"']+)(?:\"|')[^<>]*?>Download File</a>");
        return downloadLinkRegexes;
    }

    @Override
    protected void checkDownloadProblems() throws ErrorDuringDownloadingException {
        super.checkDownloadProblems();
        final String content = getContentAsString();
        final Matcher match = PlugUtils.matcher("This file can be downloaded by (<.+?>)?Premium Members", content);
        if (match.find()) {
            throw new NotRecoverableDownloadException("This file is only available to premium users");
        }
        if (content.contains("No free download slots are available at this time")) {
            throw new YouHaveToWaitException("No free download slots are available at this time", 300);
        }
        if (content.contains("until the next download") || content.contains("to download for free")) {
            final Matcher matcher = getMatcherAgainstContent("(?:(\\d+) hours? )?(?:(\\d+) minutes? )?(?:(\\d+) seconds?)");
            int waitHours = 0, waitMinutes = 0, waitSeconds = 0;
            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    waitHours = Integer.parseInt(matcher.group(1));
                }
                if (matcher.group(2) != null) {
                    waitMinutes = Integer.parseInt(matcher.group(2));
                }
                waitSeconds = Integer.parseInt(matcher.group(3));
            }
            final int waitTime = (waitHours * 60 * 60) + (waitMinutes * 60) + waitSeconds;
            throw new YouHaveToWaitException("You have to wait " + matcher.group(), waitTime);
        }
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("<span[^>]*id=\"count\"[^>]*>[^\\d]*(\\d+).*</span>");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    @Override
    protected boolean stepCaptcha(final MethodBuilder methodBuilder) throws Exception {
        try {
            boolean hasCaptcha = false;
            for (final CaptchaType captchaType : getCaptchaTypes()) {
                if (captchaType.canHandle(getContentAsString())) {
                    hasCaptcha = true;
                }
            }
            if (hasCaptcha) {
                Matcher matcher = getMatcherAgainstContent("\\$\\.post\\(\\s*[\"']([^\"']+?)[\"']");
                if (matcher.find()) {//throw new PluginImplementationException("Not found - action");
                    MethodBuilder builder = getMethodBuilder().setReferer(fileURL).setAjax()
                            .setAction(matcher.group(1).trim());
                    matcher = getMatcherAgainstContent("(?s)\\$\\.post\\(.+?\\{(.+?)\\}");
                    if (!matcher.find()) throw new PluginImplementationException("Not found - parameters");
                    String params = matcher.group(1);
                    matcher = PlugUtils.matcher("[\"']?(\\w+)[\"']?\\s*:\\s*[\"']?(\\w+)[\"']?", params);
                    while (matcher.find()) {
                        builder.setParameter(matcher.group(1).trim(), matcher.group(2).trim());
                    }
                    super.stepCaptcha(builder);
                    HttpMethod method = builder.toPostMethod();
                    client.makeRequest(method, false);
                    return false;
                }
            }
            super.stepCaptcha(methodBuilder);

        } catch (Exception x) {
            super.stepCaptcha(methodBuilder);
        }
        return false;
    }

    @Override
    protected void doDownload(final HttpMethod method) throws Exception {
        final String link = method.getURI().getURI();
        httpFile.setFileName(link.substring(1 + link.lastIndexOf("/")));
        super.doDownload(method);
    }

    @Override
    protected void doLogin(final PremiumAccount pa) throws Exception {
        HttpMethod method = getMethodBuilder().setAjax()
                .setReferer(getBaseURL())
                .setAction(getBaseURL() + "/login")
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        method = getMethodBuilder().setAjax()
                .setReferer(getBaseURL() + "/login")
                .setActionFromFormByName("FL", true)
                .setParameter("email", pa.getUsername())
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
    protected List<CaptchaType> getCaptchaTypes() {
        final List<CaptchaType> captchaTypes = super.getCaptchaTypes();
        captchaTypes.add(0, new ReCaptchaType2());
        return captchaTypes;
    }

    class ReCaptchaType2 extends ReCaptchaType {
        @Override
        protected String getReCaptchaKeyRegex() {
            return "<div class.+?data-sitekey=\"(.+?)\"";
        }

        @Override
        public void handleCaptcha(final MethodBuilder methodBuilder, final HttpDownloadClient client, final CaptchaSupport captchaSupport, final HttpFileDownloadTask downloadTask) throws Exception {
            final String content = client.getContentAsString();
            final Matcher reCaptchaKeyMatcher = PlugUtils.matcher(getReCaptchaKeyRegex(), content);
            if (!reCaptchaKeyMatcher.find()) {
                throw new PluginImplementationException("ReCaptcha key not found");
            }
            final String reCaptchaKey = reCaptchaKeyMatcher.group(1).trim();
            final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, fileURL);
            r.modifyResponseMethod(methodBuilder);
        }
    }
}