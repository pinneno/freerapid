package cz.vity.freerapid.plugins.services.file_al;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import org.apache.commons.httpclient.HttpMethod;

import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class File_alFileRunner extends XFileSharingRunner {

    @Override
    protected void correctURL() throws Exception {
        if (fileURL.contains("/public/")) {
            final HttpMethod method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                checkFileProblems();
                throw new ServiceConnectionProblemException();
            }
            fileURL = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("window.open('", "')").getEscapedURI();
        }
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("id=\"countdown\".*?<span.*?\">.*?(\\d+).*?</span");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }
}