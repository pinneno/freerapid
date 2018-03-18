package cz.vity.freerapid.plugins.services.linkget;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class LinkGetFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinkGetFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems(getMethod);
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems(getMethod);
logger.info("######   "+getMethod.getStatusText());
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<title>", "- Linkget");
        Matcher matcher = PlugUtils.matcher("\\((\\d.+?)\\)<", content);
        if (!matcher.find())
            throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems(method);//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            int wait = Integer.parseInt(PlugUtils.getStringBetween(contentAsString, "var seconds = ", ";"));
            downloadTask.sleep(wait + 1);

            Matcher matcher = PlugUtils.matcher("<a[^>]*free[^>]*href=['\"]([^\"]+?)['\"][^>]*>", contentAsString);
            if (!matcher.find())
                throw new PluginImplementationException("Download page link not found");
            String dlPage = matcher.group(1).trim();
            HttpMethod httpMethod = getGetMethod(dlPage);
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems(httpMethod);
                throw new ServiceConnectionProblemException();
            }
            checkProblems(httpMethod);
            httpMethod = doCaptcha(getMethodBuilder().setReferer(dlPage)
                    .setActionFromFormWhereTagContains("captcha", true)).toPostMethod();
            client.makeRequest(httpMethod, false);
            if (!tryDownloadAndSaveFile(getGetMethod(httpMethod.getResponseHeader("Location").getValue()))) {
                checkProblems(httpMethod);//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems(method);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems(HttpMethod method) throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || (method.getStatusCode() == 404)) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private MethodBuilder doCaptcha(MethodBuilder builder) throws Exception {
        final Matcher reCaptchaKeyMatcher = PlugUtils.matcher("sitekey['\"]?\\s*[:=]\\s*['\"]([^'\"]+)['\"]", getContentAsString());
        if (!reCaptchaKeyMatcher.find())  throw new PluginImplementationException("ReCaptcha key not found");
        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKeyMatcher.group(1).trim(), fileURL);
        return r.modifyResponseMethod(builder);
    }
}