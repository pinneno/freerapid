package cz.vity.freerapid.plugins.services.megaup;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class MegaUpFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaUpFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems(getMethod);
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems(getMethod);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, " File:", "<");
        PlugUtils.checkFileSize(httpFile, content, " Size:", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems(method);//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            int wait = 1 + PlugUtils.getNumberBetween(contentAsString, "var seconds = ", ";");
            downloadTask.sleep(wait);
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("ownload now").toHttpMethod();
            client.makeRequest(httpMethod, false);
            httpMethod = getGetMethod(httpMethod.getResponseHeader("Location").getValue());
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems(httpMethod);//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems(method);
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems(HttpMethod method) throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if ((method.getStatusCode() == 404) || contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}