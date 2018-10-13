package cz.vity.freerapid.plugins.services.keep2share;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
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
class Keep2ShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Keep2ShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        addCookie(new Cookie(".keep2share.cc", "lang", "en", "/", 86400, false));
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            loadTokens();
            checkProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws Exception {
        HttpMethod infoMethod = getMethodBuilder().setReferer(fileURL).setAjax()
                .setAction(baseApiUrl + "/files/" + fileId + "?referer=").toGetMethod();
        if (!makeRedirectedRequest(infoMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        Matcher match = getMatcherAgainstContent("\"name\"\\:\"([^\"]+?)\".+?false,\"size\"\\:\"?(\\d+)");
        if (!match.find())
            throw new PluginImplementationException("File name/size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    String fileId;
    String baseApiUrl;
    String reCaptchaKey;
    private void loadTokens() throws Exception {
        Matcher matcher = PlugUtils.matcher("file/(\\w+)", fileURL);
        if (!matcher.find()) throw new PluginImplementationException("File ID not found");
        fileId = matcher.group(1).trim();
        matcher = getMatcherAgainstContent("src=\"([^\"]+?spa[^\"]+?\\.js)\"");
        if (!matcher.find()) throw new PluginImplementationException("Jscript not found");
        if (!makeRedirectedRequest(getMethodBuilder().setAction(matcher.group(1).trim()).setReferer(fileURL).toGetMethod())) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        matcher = getMatcherAgainstContent("['\"]?sitekey['\"]?\\s*[:=]\\s*['\"]([^\"]+)['\"]");
        if (!matcher.find()) throw new PluginImplementationException("captcha key not found");
        reCaptchaKey = matcher.group(1);
        matcher = getMatcherAgainstContent("d=\"([^\"]+?api\\.[^\"]+?)\",m=\"([^\"]+?)\",f=\"([^\"]+?)\",");
        if (!matcher.find()) throw new PluginImplementationException("token keys not found");
        baseApiUrl = matcher.group(1);
        String c_id = matcher.group(2);
        String c_secret = matcher.group(3);
        String tokenUrl = baseApiUrl + "/auth/token";

        HttpMethod tokenMethod = getMethodBuilder().setReferer(fileURL).setAjax()
                .setAction(tokenUrl)
                .setParameter("grant_type", "client_credentials")
                .setParameter("client_id", c_id)
                .setParameter("client_secret", c_secret)
                .toPostMethod();
        if (!makeRedirectedRequest(tokenMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".keep2share.cc", "lang", "en", "/", 86400, false));
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            loadTokens();
            checkNameAndSize();//extract file name and size from the page

            HttpMethod downloadMethod = getMethodBuilder().setReferer(fileURL).setAjax()
                    .setAction(baseApiUrl + "/files/" + fileId + "/download?referer=").toGetMethod();
            makeRedirectedRequest(downloadMethod);
            checkProblems();
            while (getContentAsString().contains("errors\":{\"captcha")) {
                downloadMethod = doCaptcha(getMethodBuilder().setReferer(fileURL).setAjax()
                        .setAction(baseApiUrl + "/files/" + fileId + "/download")
                ).setParameter("?referer=", "").toGetMethod();
                makeRedirectedRequest(downloadMethod);
            }
            checkProblems();
            if (getContentAsString().contains("need_to_wait")) {
                Matcher matcher = getMatcherAgainstContent("['\"]?timeRemain['\"]?\\s*[:=]\\s*['\"]?(\\d+)['\"]?");
                if (matcher.find())
                    downloadTask.sleep(1 + Integer.parseInt(matcher.group(1).trim()));
                downloadMethod = getMethodBuilder().setReferer(fileURL).setAjax()
                        .setAction(baseApiUrl + "/files/" + fileId + "/download?referer=").toGetMethod();
                makeRedirectedRequest(downloadMethod);
            }
            checkProblems();
            Matcher matcher = getMatcherAgainstContent("['\"]?downloadUrl['\"]?\\s*[:=]\\s*['\"]?([^'\"]+)['\"]?");
            if (!matcher.find())
                throw new PluginImplementationException("Download url not found");
            final HttpMethod httpMethod = getGetMethod(matcher.group(1).trim());
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
        final String content = getContentAsString();
        if (content.contains("File not found") || content.contains("<title>Error 404</title>") || content.contains("isDeleted\":true")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (content.contains("File size to large") ||
                content.contains("Free user can't download large files")) {
            throw new NotRecoverableDownloadException("This file size is only for Premium members");
        }
        if (content.contains("only for premium members")) {
            throw new NotRecoverableDownloadException("This file is only for Premium members");
        }
        final Matcher waitMatch = PlugUtils.matcher("Please wait (\\d+?):(\\d+?):(\\d+?) to download this file", content);
        if (waitMatch.find()) {
            final int waitTime = Integer.parseInt(waitMatch.group(3)) + 60 * (Integer.parseInt(waitMatch.group(2)) + 60 * Integer.parseInt(waitMatch.group(1)));
            throw new YouHaveToWaitException("Please wait for download", waitTime);
        }
    }

    private MethodBuilder doCaptcha(final MethodBuilder builder) throws Exception {
        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, fileURL);
        return builder.setParameter("captchaType", "recaptcha").setParameter("captchaValue", r.getResponse());
    }

}