package cz.vity.freerapid.plugins.services.ally;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptchaNoCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class AllyFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AllyFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            MethodBuilder methodBuilder = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormWhereTagContains("captcha", true)
                    .setAction(fileURL);
            do {
                method = doCaptcha(methodBuilder).toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
            } while(getContentAsString().contains("captcha"));
            final Matcher match = getMatcherAgainstContent("attr\\(\"href\",\"(http[^\"]+?)\"");
            if (!match.find())
                throw new PluginImplementationException("Download link not found");
            httpFile.setNewURL(new URL(match.group(1).trim()));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private MethodBuilder doCaptcha(MethodBuilder builder) throws Exception {
        final Matcher reCaptchaKeyMatcher = PlugUtils.matcher("sitekey['\"]?\\s*[:=]\\s*['\"]([^'\"]+)['\"]", getContentAsString());
        if (!reCaptchaKeyMatcher.find()) {
            throw new PluginImplementationException("ReCaptcha key not found");
        }
        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKeyMatcher.group(1).trim(), fileURL);
        return r.modifyResponseMethod(builder);
    }
}