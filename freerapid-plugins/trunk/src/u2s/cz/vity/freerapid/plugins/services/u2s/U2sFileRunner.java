package cz.vity.freerapid.plugins.services.u2s;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
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
class U2sFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(U2sFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            fileURL = method.getURI().getURI();
            do {
logger.info("##### "+getContentAsString());
                HttpMethod httpMethod1 = stepCaptcha(getMethodBuilder()
                        .setReferer(fileURL).setActionFromFormByIndex(1, true).setAction(fileURL)
                        , fileURL).toPostMethod();
                if (!makeRedirectedRequest(httpMethod1)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } while (getContentAsString().contains("CAPTCHA was incorrect"));
            checkProblems();
            int wait = PlugUtils.getNumberBetween(getContentAsString(), "var time = ", ",") / 1000;
            downloadTask.sleep(1 + wait);
            HttpMethod httpMethod2 = getMethodBuilder().setAjax()
                    .setReferer(fileURL).setActionFromFormByIndex(1, true)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod2)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            if (!getContentAsString().contains("\"status\":\"success\""))
                throw new PluginImplementationException("Error loading link");

            final Matcher m = getMatcherAgainstContent("\"url\":\"([^\"]+?)\"");
            if (!m.find()) throw new PluginImplementationException("Link not found");
            this.httpFile.setNewURL(new URL(m.group(1).replace("\\", "")));
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private MethodBuilder stepCaptcha(MethodBuilder builder, final String referrer) throws Exception {
        final Matcher m = getMatcherAgainstContent("['\"]?site_?key['\"]?\\]?\\s*[:=]\\s*['\"]([^'\"]+?)['\"]");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
        final String reCaptchaKey = m.group(1);
        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, referrer);
        return r.modifyResponseMethod(builder);
    }
}