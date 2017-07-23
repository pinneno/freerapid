package cz.vity.freerapid.plugins.services.upera;

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
class UperaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UperaFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "title\" content=\"", "\"");
        PlugUtils.checkFileSize(httpFile, content, "Size:</b>", "<br>");
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
                    .setActionFromFormWhereTagContains("Download", true)
                    .setAction(fileURL).toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            Matcher match = getMatcherAgainstContent("var counter\\s*=\\s*(\\d+)");
            if (match.find()) {
                downloadTask.sleep(1 + Integer.parseInt(match.group(1)));
                String captchaStepUrl = PlugUtils.getStringBetween(getContentAsString(), "url: '", "',");
                httpMethod = getMethodBuilder().setAction(captchaStepUrl).setReferer(fileURL).setAjax().toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                do{
                    httpMethod = doCaptcha(getMethodBuilder().setActionFromFormWhereTagContains("Download", true)
                            .setAction(fileURL).setReferer(fileURL).setAjax()).toPostMethod();
                    if (!makeRedirectedRequest(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                } while (getContentAsString().contains("CAPTCHA was Incorrect"));
                checkProblems();
            }
            httpMethod = getMethodBuilder().setActionFromAHrefWhereATagContains("Download Link").toGetMethod();
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
        if (contentAsString.contains("Invalid or Deleted File")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private MethodBuilder doCaptcha(MethodBuilder builder) throws Exception {
        if (getContentAsString().contains("g-recaptcha-response")) {
            final CaptchaSupport captchaSupport = getCaptchaSupport();
            final String captchaSrc = getMethodBuilder().setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
            final String captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            return builder.setParameter("g-recaptcha-response", captcha);
        }
        return builder;
    }
}