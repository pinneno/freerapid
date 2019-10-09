package cz.vity.freerapid.plugins.services.ouo;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class OuoFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OuoFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = fileURL.substring(0, fileURL.lastIndexOf("/")) + "/fbc" + fileURL.substring(fileURL.lastIndexOf("/"));
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(httpMethod)) { //we make the main request
            fileURL = httpMethod.getURI().getURI();
            checkProblems(httpMethod);//check problems
            do {
                httpMethod = stepCaptcha(fileURL);
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems(httpMethod);
                    throw new ServiceConnectionProblemException();
                }
            } while (getContentAsString().contains("form-captcha"));
            httpMethod = getMethodBuilder().setActionFromFormWhereTagContains("Get Link", true).toPostMethod();
            int status = client.makeRequest(httpMethod, false);
            if (status == 200)
                this.httpFile.setNewURL(new URL(httpMethod.getURI().getURI()));
            else if (status/100 == 3)
                this.httpFile.setNewURL(new URL(httpMethod.getResponseHeader("Location").getValue()));
            else
                throw new PluginImplementationException("Error getting redirected link");
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems(httpMethod);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems(HttpMethod method) throws Exception {
        if (method.getURI().getURI().equals("http://ouo.press/") ||
                method.getURI().getURI().equals("http://ouo.io/")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private HttpMethod stepCaptcha(final String referrer) throws Exception {
        final Matcher m = getMatcherAgainstContent("['\"]?sitekey['\"]?\\s*[:=]\\s*['\"]([^\"]+)['\"]");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
        final String reCaptchaKey = m.group(1);

        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, referrer);
        return r.modifyResponseMethod(
                getMethodBuilder()
                        .setReferer(referrer)
                        .setActionFromFormWhereTagContains("captcha", true)
                ).toPostMethod();
    }
}