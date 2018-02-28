package cz.vity.freerapid.plugins.services.uplod_cc;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.CaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.captcha.ReCaptchaType;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
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
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Uplod_ccFileRunner extends XFileSharingRunner {


    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkName(httpFile, content, "\"title\">", "<");
            }
        });
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkName(httpFile, content, "name\">", "<");
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a[^<>]+href\\s*=\\s*[\"'](http.+?" + Pattern.quote(httpFile.getFileName()) + ")[\"'].+?Download</a>");
        downloadLinkRegexes.add(0, "<a[^<>]+href\\s*=\\s*[\"'](http.+?" + Pattern.quote(PlugUtils.unescapeHtml(httpFile.getFileName())) + ")[\"'].+?Download</a>");
        return downloadLinkRegexes;
    }

    @Override
    protected void checkFileProblems(final String content) throws ErrorDuringDownloadingException {
        if (content.contains("No File Found")
                || content.contains("file you were looking for could not be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        super.checkFileProblems(content);
    }

    @Override
    protected List<CaptchaType> getCaptchaTypes() {
        final List<CaptchaType> captchaTypes = super.getCaptchaTypes();
        captchaTypes.add(0, new ReCaptchaType2());
        return captchaTypes;
    }

    public class ReCaptchaType2 extends ReCaptchaType {
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

}