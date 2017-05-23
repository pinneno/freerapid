package cz.vity.freerapid.plugins.services.uploadgig;

import cz.vity.freerapid.plugins.exceptions.*;
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
class UploadGigFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadGigFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "name\">", "<");
        PlugUtils.checkFileSize(httpFile, content, "size\">[", "]<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            do {
                if (getContentAsString().length() < 5) {
                    if (!makeRedirectedRequest(method)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                }
                final HttpMethod httpMethod = stepCaptcha(getMethodBuilder().setAjax()
                        .setReferer(fileURL)
                        .setActionFromFormWhereActionContains("free_dl", true)
                        , fileURL).toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                if (getContentAsString().trim().equals("fl"))
                    throw new NotRecoverableDownloadException("This file can be downloaded by Premium Member only");
                else if (getContentAsString().trim().equals("rfd"))
                    throw new NotRecoverableDownloadException("This file reached the maximum number of free downloads");
                else if (getContentAsString().trim().equals("m"))
                    throw new YouHaveToWaitException("You have reached the max. number of possible free downloads for this hour", 60*60);
            } while (!getContentAsString().contains("\"ur"));
            checkProblems();

            Matcher match = PlugUtils.matcher("\"ur.+?\":\"(.+?)\"", getContentAsString());
            if (!match.find())
                throw new PluginImplementationException("Download link not found");
            String dlUrl = match.group(1);
            int wait = 1 + Integer.parseInt(PlugUtils.getStringBetween(getContentAsString(), "cd\":", "}"));
            downloadTask.sleep(wait);
            if (!tryDownloadAndSaveFile(getGetMethod(dlUrl))) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private MethodBuilder stepCaptcha(MethodBuilder builder, final String referrer) throws Exception {
        final Matcher m = getMatcherAgainstContent("['\"]?sitekey['\"]?\\s*[:=]\\s*['\"]([^\"]+)['\"]");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
        final String reCaptchaKey = m.group(1);
        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, referrer);
        return r.modifyResponseMethod(builder);
    }
}