package cz.vity.freerapid.plugins.services.data;

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
 * @author Javi
 */
class DataFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(DataFileRunner.class.getName());


    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher matchN = PlugUtils.matcher("<h1>([^<>\\s]+)", content);
        if (!matchN.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(matchN.group(1).trim());
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            final String contentAsString = getContentAsString();
            checkProblems();
            checkNameAndSize(contentAsString);
            HttpMethod httpMethod = stepCaptcha(getMethodBuilder().setReferer(fileURL)
                    .setActionFromFormWhereActionContains("free", true).setAjax()
                    , fileURL).toPostMethod();
            do {
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
            } while (!getContentAsString().contains("redirect"));

            final Matcher matcher = getMatcherAgainstContent("redirect\":\"(.+?)\"");
            if (matcher.find()) {
                final String downURL = matcher.group(1).replace("\\/", "/");
                logger.info("downURL: " + downURL);
                final GetMethod getmethod = getGetMethod(downURL);
                if (!tryDownloadAndSaveFile(getmethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new ServiceConnectionProblemException();
                }
            } else {
                checkProblems();
                throw new PluginImplementationException("Download link not found");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("nem létezik")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("200 MB-nál nagyobb")) {
            throw new NotRecoverableDownloadException("Premium account needed for files >200MB");
        }

    }


    private MethodBuilder stepCaptcha(MethodBuilder builder, final String referrer) throws Exception {
        final Matcher m = getMatcherAgainstContent("['\"]?sitekey['\"]?\\s*[:=]\\s*['\"]([^'\"]+)['\"]");
        if (!m.find()) throw new PluginImplementationException("ReCaptcha key not found");
        final String reCaptchaKey = m.group(1);
        final ReCaptchaNoCaptcha r = new ReCaptchaNoCaptcha(reCaptchaKey, referrer);
        return r.modifyResponseMethod(builder);
    }
}