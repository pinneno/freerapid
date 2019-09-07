package cz.vity.freerapid.plugins.services.spaste;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.pastebin.PasteBinFileRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class SPasteFileRunner extends PasteBinFileRunner {

    @Override
    protected void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    protected void stepCaptcha(HttpMethod method) throws Exception {
        String sCaptcha = PlugUtils.getStringBetween(getContentAsString(), "myCaptchaHash='", "';");
        String[] options = PlugUtils.getStringBetween(getContentAsString(), "myCaptchaQuestions=[", "];").replace("\"", "").replace("\\s", "").split(",");
        String[] answers = PlugUtils.getStringBetween(getContentAsString(), "myCaptchaAns=[", "];").replace("\"", "").replace("\\s", "").split(",");
        String response = "";
        for (String ans : answers) {
            for (int i=0; i<options.length; i++) {
                if (options[i].equalsIgnoreCase(ans)) {
                    response += i;
                    break;
                }
            }
        }
        downloadTask.sleep(3);
        HttpMethod httpMethod = getMethodBuilder().setActionFromFormWhereTagContains("Captcha", true)
                .setAction(method.getURI().getURI()).setReferer(method.getURI().getURI())
                .setParameter("sPasteCaptcha", sCaptcha)
                .setParameter("userEnterHashHere", response)
                .setParameter("detector", "http://www.spaste.com/site/checkBlockedDotCom")
                .setParameter("pasteUrlForm[submit]", "submit")
                .toPostMethod();

        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    protected String getLinksText() throws PluginImplementationException {
        String links = "";
        Matcher match = getMatcherAgainstContent("<a target=\"[^\"]*\" style[^>]+>([^<>]+)<");
        while (match.find()) {
            links += " \n " + match.group(1);
        }
        return links;
    }

    @Override
    protected void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("404 Not Found") || contentAsString.contains("requested paste has been deleted")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}