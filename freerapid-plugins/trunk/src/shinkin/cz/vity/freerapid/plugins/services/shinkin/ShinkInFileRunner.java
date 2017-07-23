package cz.vity.freerapid.plugins.services.shinkin;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
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
class ShinkInFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShinkInFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        do {
            if (!makeRedirectedRequest(method)) { //we make the main request
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();//check problems
            HttpMethod httpMethod = stepCaptcha();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        } while (getContentAsString().contains("Error captcha"));

        Matcher matcher = getMatcherAgainstContent("\"timer\"[^>]*>\\s*(\\d+)\\s*</");
        if (matcher.find()) {
            downloadTask.sleep(1 + Integer.parseInt(matcher.group(1).trim()));
        }
        HttpMethod httpMethod = getMethodBuilder().setActionFromFormWhereTagContains("GET LINK", true).toPostMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        matcher = PlugUtils.matcher("url=(.+);?", httpMethod.getResponseHeader("Refresh").getValue());
        if (!matcher.find())
            throw new PluginImplementationException("Target url not found");

        this.httpFile.setNewURL(new URL(matcher.group(1)));
        this.httpFile.setPluginID("");
        this.httpFile.setState(DownloadState.QUEUED);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("page you are looking for cannot be found") ||
                contentAsString.contains("Page Cannot Be Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        final Matcher m = getMatcherAgainstContent("['\"]?sitekey['\"]?\\s*[:=]\\s*['\"]([^\"]+)['\"]");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
        final String reCaptchaKey = m.group(1);

        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, fileURL);
        return r.modifyResponseMethod(
                getMethodBuilder()
                        .setReferer(fileURL)
                        .setActionFromFormWhereTagContains("g-recaptcha", true)
        ).toPostMethod();
    }
}