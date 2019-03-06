package cz.vity.freerapid.plugins.services.fileboom;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FileBoomFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FileBoomFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        checkUrl();
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
        matcher = getMatcherAgainstContent("['\"]?(?:sitekey|RECAPTCHA_PUBLIC_KEY)['\"]?\\s*[:=]\\s*['\"]([^\"]+)['\"]");
        if (!matcher.find()) throw new PluginImplementationException("captcha key not found");
        reCaptchaKey = matcher.group(1).trim();
        matcher = getMatcherAgainstContent("\\w=\"([^\"]+?api\\.[^\"]+?)\",\\w=\"([^\"]+?)\",\\w=\"([^\"]+?)\",");
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
        checkUrl();
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

    private void checkUrl() {
//        fileURL = fileURL.replace("fboom.me/", "fileboom.me/");
    }

    @Override
    protected String getBaseURL() {
        try {
            return new URL(fileURL).getProtocol() + "://" + new URL(fileURL).getAuthority();
        }
        catch (Exception x) {
            return super.getBaseURL();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File Not Found") || content.contains("File not found")  || content.contains("isDeleted\":true")
                || content.contains("This file is no longer available")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (content.contains("Downloading is not possible")) {
            int time = 60 * 60; // 1 hour
            final Matcher match = PlugUtils.matcher("(\\d+):(\\d+):(\\d+)", content);
            if (match.find()) {
                int hour = new Integer(match.group(1));
                int mins = new Integer(match.group(2));
                int secs = new Integer(match.group(3));
                time = (((hour * 60) + mins) * 60) + secs;
            }
            throw new YouHaveToWaitException("You have to wait before download ", time);
        }

    }

    private MethodBuilder doCaptcha(final MethodBuilder builder) throws Exception {
        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, fileURL);
        return builder.setParameter("captchaType", "recaptcha").setParameter("captchaValue", r.getResponse());
    }
}