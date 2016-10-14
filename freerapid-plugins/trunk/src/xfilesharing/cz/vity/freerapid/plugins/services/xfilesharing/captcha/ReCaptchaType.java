package cz.vity.freerapid.plugins.services.xfilesharing.captcha;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * @author tong2shot
 * @author ntoskrnl
 * @author birchie
 */
public class ReCaptchaType implements CaptchaType {

    protected String getReCaptchaKeyRegex() {
        return "recaptcha/api/challenge\\?k=(.+?)\"|recaptcha\" data-sitekey=\"(.+?)\"";
    }

    @Override
    public boolean canHandle(final String content) {
        return PlugUtils.find(getReCaptchaKeyRegex(), content);
    }

    @Override
    public void handleCaptcha(final MethodBuilder methodBuilder, final HttpDownloadClient client, final CaptchaSupport captchaSupport, final HttpFileDownloadTask downloadTask) throws Exception {
        final Matcher reCaptchaKeyMatcher = PlugUtils.matcher(getReCaptchaKeyRegex(), client.getContentAsString());
        if (!reCaptchaKeyMatcher.find()) {
            throw new PluginImplementationException("ReCaptcha key not found");
        }
        if (client.getContentAsString().contains("/api/challenge")) {
            final String reCaptchaKey = reCaptchaKeyMatcher.group(1).trim();
            final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
            final String captcha = captchaSupport.getCaptcha(r.getImageURL());
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            r.setRecognized(captcha);
            r.modifyResponseMethod(methodBuilder);
        }
        else {
            final String reCaptchaKey = reCaptchaKeyMatcher.group(2).trim();
            final String fileUrl = downloadTask.getDownloadFile().getFileUrl().toString();
            final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, fileUrl);
            r.modifyResponseMethod(methodBuilder);
        }
    }

}
