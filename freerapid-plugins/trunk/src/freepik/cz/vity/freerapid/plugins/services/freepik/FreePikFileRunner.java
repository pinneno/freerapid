package cz.vity.freerapid.plugins.services.freepik;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FreePikFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FreePikFileRunner.class.getName());

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
        PlugUtils.checkName(httpFile, content, "title\" content=\"", "\"");
        if (!isDownloadPage(content))
            httpFile.setFileName("Collection >>  " + httpFile.getFileName());
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private boolean isDownloadPage(String content) {
        Matcher match = PlugUtils.matcher("/index\\.php\\?goto.+?idfoto", content);
        return match.find();
    }


    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        doLogin();
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);

            if (isDownloadPage(content)) {
                Matcher match = PlugUtils.matcher("<a[^>]+?href=\"(https?://(www\\.)?freepik.com/index\\.php\\?goto[^>\"]+?idfoto[^>\"]+?)\"", content);
                if (!match.find())
                    throw new PluginImplementationException("Download link not found");
                if (!makeRedirectedRequest(getGetMethod(match.group(1)))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                        .setActionFromTextBetween("URL=", "\"").toGetMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
            else {
                LinkedList<URI> list = new LinkedList<URI>();
                Matcher match = PlugUtils.matcher("<a[^>]+?href=\"([^\"]+?)\"[^>]+?class=\"preview[^>]+?onclick", content);
                while (match.find()) {
                    list.add(new URI(match.group(1).trim()));
                }
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                logger.info(list.size() + " links added");
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("File Not Found") || content.contains("title\" content=\"Freepik - Free Graphic resources for everyone\"")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (content.contains("<title>Freepik Premium")) {    //Premium Download</title>
            throw new NotRecoverableDownloadException("Premium content only");
        }
    }

    private void doLogin() throws Exception {
        logger.info("Starting login");
        synchronized (FreePikFileRunner.class) {
            FreePikServiceImpl service = (FreePikServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (pa.isSet()) {
                String accountsApiKey = PlugUtils.getStringBetween(getContentAsString(), "var ACCOUNTS_API_KEY = '", "';");
                String time = "" + System.currentTimeMillis();
                String action = "https://profile.freepik.com/request/login?o=" + accountsApiKey + "&kfc=" + time;
                HttpMethod postMethod = getMethodBuilder().setAjax()
                        .setReferer("https://profile.freepik.com/login?lang=en")
                        .setAction(action)
                        .setParameter("username", pa.getUsername())
                        .setParameter("password", pa.getPassword())
                        .setParameter("o", accountsApiKey)
                        .setParameter("kfc", time)
                        .setParameter("register_callback", "")
                        .setParameter("rememberme", "false")
                        .setParameter("token_recaptcha", "false")
                        .toPostMethod();
                if (!makeRedirectedRequest(postMethod)) {
                    throw new ServiceConnectionProblemException("Error logging in");
                }
                if (getContentAsString().contains("\"status\":false")) {
                    throw new BadLoginException("Invalid login information!");
                }
                logger.info("Logged in");
            } else {
                logger.info("No login details");
            }
        }
    }
}