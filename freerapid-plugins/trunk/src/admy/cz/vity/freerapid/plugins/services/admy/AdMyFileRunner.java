package cz.vity.freerapid.plugins.services.admy;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
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
class AdMyFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AdMyFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(httpMethod)) { //we make the main request
            checkProblems();//check problems
            do {
                if ((""+httpMethod.getResponseHeader("Location")).contains("goo.gl")) {
                    httpMethod = getGetMethod(httpMethod.getResponseHeader("Location").getValue());
                    if (!makeRedirectedRequest(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();
                }
                httpMethod = getMethodBuilder().setReferer(fileURL)
                        .setActionFromFormWhereTagContains("SKIP", true).toPostMethod();
                int status = client.makeRequest(httpMethod, false);
                if (status / 100 != 3) {
                    throw new PluginImplementationException("Error getting target link. " + status);
                }
            } while ((""+httpMethod.getResponseHeader("Location")).contains("goo.gl"));

            httpFile.setNewURL(new URL(httpMethod.getResponseHeader("Location").getValue()));
            httpFile.setPluginID("");
            httpFile.setState(DownloadState.QUEUED);

        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("we didn't find what you were looking for")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}