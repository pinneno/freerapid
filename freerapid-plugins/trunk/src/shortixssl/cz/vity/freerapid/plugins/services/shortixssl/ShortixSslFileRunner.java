package cz.vity.freerapid.plugins.services.shortixssl;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ShortixSslFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ShortixSslFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems(method);//check problems
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Generate").toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems(httpMethod);
                throw new ServiceConnectionProblemException();
            }
            String dlLink = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("Visit").getEscapedURI().trim();
            this.httpFile.setNewURL(new URL(dlLink));
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems(method);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems(HttpMethod method) throws Exception {
        if (method.getURI().getURI().contains("index.php")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}