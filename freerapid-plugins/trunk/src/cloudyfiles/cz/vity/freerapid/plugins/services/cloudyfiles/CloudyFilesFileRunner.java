package cz.vity.freerapid.plugins.services.cloudyfiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.ReCaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class CloudyFilesFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                Matcher matcher = PlugUtils.matcher("Size(?:<[^>]+>\\s*)*(.+?)\\s*<", content);
                if (!matcher.find()) throw new PluginImplementationException("File size not found");
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
            }
        });
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
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
            return "<div.+?data-sitekey=\"(.+?)\"";
        }

        @Override
        public void handleCaptcha(final MethodBuilder methodBuilder, final HttpDownloadClient client, final CaptchaSupport captchaSupport, final HttpFileDownloadTask downloadTask) throws Exception {
            final String content = client.getContentAsString().replaceAll("(?s)<!--.+?-->", "").replaceAll("(?s)<div.+?visibility:hidden.+?</div>", "");
            final Matcher reCaptchaKeyMatcher = PlugUtils.matcher(getReCaptchaKeyRegex(), content);
            if (!reCaptchaKeyMatcher.find()) {
                throw new PluginImplementationException("ReCaptcha key not found");
            }
            final String reCaptchaKey = reCaptchaKeyMatcher.group(1).trim();
            final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, fileURL);
            r.modifyResponseMethod(methodBuilder);
        }
    }

    @Override
    protected List<String> getFalseProblemRegexes() {
        final List<String> falseProblemRegexes = super.getFalseProblemRegexes();
        falseProblemRegexes.add("<font[^<>]+?font-size:1p.+?</font>");
        return falseProblemRegexes;
    }
}