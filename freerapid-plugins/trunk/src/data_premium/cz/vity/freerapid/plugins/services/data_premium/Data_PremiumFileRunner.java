package cz.vity.freerapid.plugins.services.data_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author Javi
 */
class Data_PremiumFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Data_PremiumFileRunner.class.getName());
    private boolean badConfig = false;


    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
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

        final HttpMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            fileURL = getMethod.getURI().getURI();
            Login();

            makeRedirectedRequest(getMethod);
            Matcher matcher = getMatcherAgainstContent("Nincs érvényes prémium elõfizetésed!");
            if (matcher.find()) {
                logger.info("No premium");
                throw new NotRecoverableDownloadException("Not premium account!");
            }
			matcher = getMatcherAgainstContent("<div class=\"download_it\">\\s+<a class=\"btn btn-primary\" href=\"(.*?)\">");
            if (!matcher.find()) {
                throw new PluginImplementationException("Download link not found!");
            }
            String downURL = matcher.group(1);
            logger.info("downURL: " + downURL);
            final GetMethod getmethod = getGetMethod(downURL);
            httpFile.setState(DownloadState.GETTING);
            if (!tryDownloadAndSaveFile(getmethod)) {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }

        } else
            throw new ServiceConnectionProblemException();
    }

    private void Login() throws Exception {
        logger.info("Starting login");
        synchronized (Data_PremiumFileRunner.class) {
            Data_PremiumServiceImpl service = (Data_PremiumServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No data.hu login information!");
                }
                badConfig = false;
            }
            if (!makeRedirectedRequest(getGetMethod("https://data.hu/bejelentkezes"))) {  // login page
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final String passTag = PlugUtils.getStringBetween(getContentAsString(), "<input type=\"password\" name=\"", "\"");
            HttpMethod postMethod = getMethodBuilder()
                    .setActionFromFormWhereActionContains("login", true)
                    .setParameter("username", pa.getUsername())
                    .setParameter(passTag, pa.getPassword())
                    .setParameter("login_passfield", passTag)
                    .setParameter("target", fileURL)
                    .setParameter("url_for_login", fileURL)
                    .setAjax().toPostMethod();
            if (makeRedirectedRequest(postMethod)) {
                if (getContentAsString().contains("error\":1")) {
                    badConfig = true;
                    logger.info("bad info");
                    throw new PluginImplementationException("Bad data.hu login information!");
                }
            } else {
                throw new ServiceConnectionProblemException("Error logging in");
            }
        }
        logger.info("Logged in");
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("nem létezik")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    @Override
    protected String getBaseURL() {
        try {
            return new URL(fileURL).getProtocol() + "://" + new URL(fileURL).getAuthority();
        }
        catch (Exception x) {
            return super.getBaseURL();
        }
    }
}