package cz.vity.freerapid.plugins.services.tezfiles;

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
class TezFilesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TezFilesFileRunner.class.getName());

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
        Matcher match = PlugUtils.matcher("<h1[^>]*>\\s*(.+?)\\s*<[^>]*>\\((.+?)\\)<", content);
        if (!match.find())
            throw new PluginImplementationException("File name and size not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(2).trim()));
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
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();//check problems

            if (getContentAsString().contains(">Download now<")) {
                httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Download now").toGetMethod();
            }
            else {
                do {
                    httpMethod = stepCaptcha(getMethodBuilder().setReferer(fileURL)
                            .setActionFromFormWhereTagContains("Download", true)
                            ).toPostMethod();
                    if (!makeRedirectedRequest(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();//check problems
                } while (getContentAsString().contains("verification code is incorrect"));

                int wait = 0;
                String timer = PlugUtils.getStringBetween(getContentAsString(), "var timer = $('#", "'");
                Matcher match = PlugUtils.matcher(timer + "[^>]*>\\s*(\\d+)", getContentAsString());
                if (match.find())
                    wait = 1 + Integer.parseInt(match.group(1).trim());
                downloadTask.sleep(wait);
                httpMethod = getMethodBuilder().setReferer(fileURL).setAjax()
                        .setActionFromTextBetween("url: '", "'")
                        .setParameter("uniqueId", PlugUtils.getStringBetween(getContentAsString(), "uniqueId: '", "'"))
                        .setParameter("free", "1")
                        .toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("this link").toGetMethod();
            }
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
        if (contentAsString.contains("file is no longer available")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private MethodBuilder stepCaptcha(MethodBuilder builder) throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = getMethodBuilder().setReferer(fileURL).setActionFromImgSrcWhereTagContains("captcha").getEscapedURI();
        final String captcha = captchaSupport.getCaptcha(captchaSrc);
        if (captcha == null) throw new CaptchaEntryInputMismatchException();
        return builder.setParameter("CaptchaForm[code]", captcha);
    }
}