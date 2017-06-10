package cz.vity.freerapid.plugins.services.multiup;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.services.solvemediacaptcha.SolveMediaCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class MultiUpFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MultiUpFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        fileURL = fileURL.replaceFirst("https://", "http://");
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            if (getMethod.getStatusCode() == 404)
                throw new URLNotAvailableAnymoreException("File not found");
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "Filename : ", "<");
        httpFile.setFileName("Extract Link(s): " + httpFile.getFileName());
        final String size = PlugUtils.getStringBetween(content, "Size : ", "<").replaceAll("iB", "B").trim();
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = fileURL.replaceFirst("https://", "http://");
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            fileURL = method.getURI().getURI();
            if (getContentAsString().contains("File is protected by a password")) {
                while (getContentAsString().contains("File is protected by a password")) {
                    final String password = getDialogSupport().askForPassword("MultiUp.org");
                    if (password == null) {
                        throw new PluginImplementationException("This link is protected with a password");
                    }
                    final HttpMethod passMethod = getMethodBuilder().setReferer(fileURL)
                            .setActionFromFormWhereTagContains("password", true)
                            .setParameter("password", password).toPostMethod();
                    if (!makeRedirectedRequest(passMethod)) {
                        checkProblems();//check problems
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                }
            } else {
                HttpMethod httpMethod;
                try {
                    httpMethod = getMethodBuilder().setReferer(fileURL)
                            .setActionFromAHrefWhereATagContains("<h5>DOWNLOAD").toGetMethod();
                } catch(Exception x) {
                    httpMethod = doCaptcha(getMethodBuilder().setReferer(fileURL)
                            .setActionFromFormWhereTagContains("<h5>DOWNLOAD", true)).toPostMethod();
                }
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            }
            final List<URI> list = new LinkedList<URI>();
            final Matcher matcher = getMatcherAgainstContent("href=\"(.+?)\"\\s*target=\"_blank\"\\s*title");
            while (matcher.find()) {
                list.add(new URI((matcher.group(1).trim())));
            }
            // add urls to queue
            if (list.isEmpty()) throw new PluginImplementationException("No links found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            httpFile.setFileName("Link(s) Extracted !");
            httpFile.setState(DownloadState.COMPLETED);
            httpFile.getProperties().put("removeCompleted", true);
        } else {
            checkProblems();
            if (method.getStatusCode() == 404)
                throw new URLNotAvailableAnymoreException("File not found");
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File not found")
                || contentAsString.contains("File might have been deleted")
                || contentAsString.contains("File might have never existed")
                || contentAsString.contains("Link might be incorrect")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("<strong>Error : </strong>Bad captcha"))
            throw new CaptchaEntryInputMismatchException("Bad captcha");
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

    private MethodBuilder doCaptcha(MethodBuilder builder) throws Exception {
        if (getContentAsString().contains("/papi/challenge")) {
            final Matcher captchaKeyMatcher = PlugUtils.matcher("/papi/challenge\\.noscript\\?k=(.+?)\"", getContentAsString());
            if (!captchaKeyMatcher.find())  throw new PluginImplementationException("Captcha key not found");
            final String captchaKey = captchaKeyMatcher.group(1);
            final SolveMediaCaptcha solveMediaCaptcha = new SolveMediaCaptcha(captchaKey, client, getCaptchaSupport(), downloadTask);
            solveMediaCaptcha.askForCaptcha();
            solveMediaCaptcha.modifyResponseMethod(builder);
        }
        else if (getContentAsString().contains("data-sitekey")) {
            final Matcher m = getMatcherAgainstContent("['\"]?site_?key['\"]?\\]?\\s*[:=]\\s*['\"]([^'\"]+?)['\"]");
            if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
            final String reCaptchaKey = m.group(1);
            final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, fileURL);
            r.modifyResponseMethod(builder);
        }
        return builder;
    }
}