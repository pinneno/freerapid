package cz.vity.freerapid.plugins.services.onefichier_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class OneFichierFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(OneFichierFileRunner.class.getName());
    private final static String loginUrl = "http://www.1fichier.com/en/login.pl";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        setEnglishURL();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void setEnglishURL() {
        if (!fileURL.contains("/en")) {
            String[] temp = fileURL.split(".com");
            fileURL = temp[0] + ".com/en";
            if (temp.length > 1) fileURL += temp[1];
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "File name :</th><td>", "</td>");
        PlugUtils.checkFileSize(httpFile, content, "File size :</th><td>", "</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setEnglishURL();
        login();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        final int status = client.makeRequest(method, false);
        if (status / 100 == 3) {
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else if (status == 200) {
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setActionFromFormWhereTagContains("Download the file", true).toPostMethod(); //  2do ?

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(httpMethod)) {
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
        if (contentAsString.contains("file could not be found") ||
                contentAsString.contains("The requested file has been deleted")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("you can download only one file at a time")) {
            throw new YouHaveToWaitException("You can download only one file at a time and you must wait up to 5 minutes between each downloads", 300);
        }
    }

    private void login() throws Exception {
        synchronized (OneFichierFileRunner.class) {
            OneFichierServiceImpl service = (OneFichierServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No 1Fichier account login information!");
                }
            }
            final HttpMethod method = getMethodBuilder()
                    .setAction(loginUrl).setReferer(loginUrl)
                    .setParameter("mail", pa.getUsername())
                    .setParameter("pass", pa.getPassword())
                    .setParameter("Login", "Login")
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("Invalid username or password") ||
                    getContentAsString().contains("Invalid email address") ||
                    getContentAsString().contains("Invalid password")) {
                throw new BadLoginException("Invalid 1Fichier account login information!");
            }
        }
    }

}