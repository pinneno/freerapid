package cz.vity.freerapid.plugins.services.fourshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class FourShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FourShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            if (PlugUtils.matcher("http://(?:www\\.|up\\.)?4share\\.vn/f/.+", fileURL).find()) { //file URL
                checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final String regexRule = "<h4>.+?:\\s*<strong>(.+?)<.+?<strong>(.+?)<";
        final Matcher matcher = PlugUtils.matcher(regexRule, content);
        if (matcher.find()) {
            httpFile.setFileName(matcher.group(1).trim());
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(2)));
        } else {
            throw new PluginImplementationException("File name and file size not found");
        }
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
            if (PlugUtils.matcher("http://(?:www\\.|up\\.)?4share\\.vn/d/.+", fileURL).find()) { //list URL
                final List<java.net.URI> uriList = new LinkedList<java.net.URI>();
                stepBuildList("<a href='(http://up\\.4share\\.vn/[fd]/.+?)'", uriList);
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
                httpFile.getProperties().put("removeCompleted", true);

            } else if (PlugUtils.matcher("http://(?:www\\.|up\\.)?4share\\.vn/dlist/.+", fileURL).find()) { //list URL
                final List<java.net.URI> uriList = new LinkedList<java.net.URI>();
                stepBuildList(">(http://up\\.4share\\.vn/[fd]/.+?)<", uriList);
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
                httpFile.getProperties().put("removeCompleted", true);
            } else if (PlugUtils.matcher("http://(?:www\\.|up\\.)?4share\\.vn/f/.+", fileURL).find()) {  //file URL
                checkNameAndSize(contentAsString);//extract file name and size from the page

                if (contentAsString.contains("var counter=")) {
                    Matcher matcher = getMatcherAgainstContent("var counter=(\\d+);");
                    if (matcher.find()) {
                        final int waitTime = Integer.parseInt(matcher.group(1));
                        downloadTask.sleep(waitTime);
                    }
                }

                MethodBuilder methodBuilder = getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromFormWhereTagContains("recaptcha", true)
                        .setAction(fileURL);
                methodBuilder = stepCaptcha(methodBuilder);

                final HttpMethod httpMethod = methodBuilder.toPostMethod();

                //here is the download link extraction
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            } else
                throw new InvalidURLOrServiceProblemException("Invalid URL");
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void stepBuildList(String urlListRegex, List<java.net.URI> uriList) throws Exception {
        final Matcher urlListMatcher = getMatcherAgainstContent(urlListRegex);
        while (urlListMatcher.find()) {
            uriList.add(new java.net.URI(new URI(urlListMatcher.group(1), false, "UTF-8").toString()));
        }
    }

    private MethodBuilder stepCaptcha(MethodBuilder methodBuilder) throws Exception {
        final String reCaptchaKey = PlugUtils.getStringBetween(getContentAsString(), "api/challenge?k=", "\"");
        final ReCaptcha r = new ReCaptcha(reCaptchaKey, client);
        final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
        if (captcha == null)
            throw new CaptchaEntryInputMismatchException();
        r.setRecognized(captcha);
        r.modifyResponseMethod(methodBuilder);
        return methodBuilder;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Link không hợp lệ")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Error: Not valid ID")) {
            throw new PluginImplementationException("Not valid ID");
        }
    }

}