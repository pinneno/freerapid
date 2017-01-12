package cz.vity.freerapid.plugins.services.sharehost;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
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
class ShareHostFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShareHostFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "<h2>", "</h2>");
        PlugUtils.checkFileSize(httpFile, content, "Size: <span>", "</span>");
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

            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setActionFromAHrefWhereATagContains("Slow download").toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            int waitTime = 1 + PlugUtils.getNumberBetween(getContentAsString(), "DOWNLOAD_WAIT=", ";");
            httpMethod = stepCaptcha(getMethodBuilder().setReferer(fileURL)
                    .setActionFromFormWhereTagContains("dwn_free", true)).toPostMethod();
            downloadTask.sleep(waitTime);
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
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File is unavailable")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("You can't download any more files at this moment"))
            throw new ServiceConnectionProblemException("You can't download any more files at this moment. \nYou can download maximum 1 file in every 60 minutes.");
        if (contentAsString.contains("Time to wait for download has not elapsed"))
            throw new ServiceConnectionProblemException("Error waiting for download");

        if (contentAsString.contains("Errors occured")) {
            String errs = PlugUtils.getStringBetween(getContentAsString(), "Errors occured", "<h2>");
            String errMsg = "";
            Matcher match = PlugUtils.matcher("<li>(.+?)</li>", errs);
            while (match.find())
                errMsg += "\n" + match.group(1);
            throw new ServiceConnectionProblemException("Errors occured:" + errMsg);
        }
    }

    private MethodBuilder stepCaptcha(MethodBuilder builder) throws Exception {
        final String captchaImage = getMethodBuilder().setReferer(fileURL).setActionFromImgSrcWhereTagContains("Captcha").getEscapedURI();
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaText = captchaSupport.getCaptcha(captchaImage);
        if (captchaText == null) throw new CaptchaEntryInputMismatchException();
        return builder.setParameter("cap_key", captchaText);
    }
}