package cz.vity.freerapid.plugins.services.fmdisk;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FmDiskFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FmDiskFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1>", "</h1>");
        Matcher match = PlugUtils.matcher("文件大小：(.+?)&nbsp;&nbsp;&nbsp;", content);
        if (!match.find())
            throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1).trim() + "b"));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        httpFile.setResumeSupported(true);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            fileURL = method.getURI().toString();
            final String fileId = PlugUtils.getStringBetween(fileURL, "-", ".");
            logger.info("fileId= " + fileId + "; fileURL = "+fileURL);

            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setAction("https://www.feemoo.com/yythems_ajax_file.php").setAjax()
                    .setParameter("action", "load_down_addr2")
                    .setParameter("file_id", fileId)
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            httpMethod = getMethodBuilder().setActionFromTextBetween("href=\"", "\"").toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String finalURL = PlugUtils.getStringBetween(getContentAsString(), "file_url= '", "'");
            logger.info("finalURL = "+finalURL);
            final HttpMethod finalMethod = getMethodBuilder().setAction(finalURL).setReferer(finalURL).toGetMethod();
            if (!tryDownloadAndSaveFile(finalMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("<h1></h1>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}