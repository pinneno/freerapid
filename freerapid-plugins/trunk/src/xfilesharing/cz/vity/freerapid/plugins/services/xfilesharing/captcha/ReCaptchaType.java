package cz.vity.freerapid.plugins.services.xfilesharing.captcha;

import com.sun.org.apache.xml.internal.resolver.helpers.FileURL;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
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
        return "(?:recaptcha/api/challenge\\?k=|recaptcha\" data-sitekey=\")(.+?)\"";
    }

    @Override
    public boolean canHandle(final String content) {
        return PlugUtils.find(getReCaptchaKeyRegex(), content);
    }

    @Override
    public void handleCaptcha(final MethodBuilder methodBuilder, final HttpDownloadClient client, final CaptchaSupport captchaSupport, final HttpFileDownloadTask downloadTask) throws Exception {
        final String content = client.getContentAsString();
        final Matcher reCaptchaKeyMatcher = PlugUtils.matcher(getReCaptchaKeyRegex(), content);
        if (!reCaptchaKeyMatcher.find()) {
            throw new PluginImplementationException("ReCaptcha key not found");
        }
        final String reCaptchaKey = reCaptchaKeyMatcher.group(1).trim();
        final String fileUrl = downloadTask.getDownloadFile().getFileUrl().toString();
        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, fileUrl);
        r.modifyResponseMethod(methodBuilder);
    }

}
