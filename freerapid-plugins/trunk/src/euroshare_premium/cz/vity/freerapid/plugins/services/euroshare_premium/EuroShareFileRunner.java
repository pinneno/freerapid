package cz.vity.freerapid.plugins.services.euroshare_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class EuroShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EuroShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        setCookie();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void setCookie() {
        addCookie(new Cookie("euroshare.eu", "lang", "sk", "/", 86400, false));
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher match = PlugUtils.matcher("fileName\\s*:\\s*'(.+?)'", content);
        if (!match.find())
            throw new PluginImplementationException("File name not found");
        httpFile.setFileName(match.group(1).trim());
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        setCookie();
        logger.info("Starting download in TASK " + fileURL);
        login();
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            Matcher match = PlugUtils.matcher("<a[^<>]*?href=\"(.+?)\"[^<>]*?>STIAHNU", contentAsString);
            if (!match.find())
                throw new PluginImplementationException("Download link not found 1");
            HttpMethod httpMethod = getGetMethod(PlugUtils.unescapeHtml(match.group(1).trim()));
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            match = PlugUtils.matcher("<a[^<>]*?href=\"(.+?)\"[^<>]*?>STIAHNU", getContentAsString());
            if (!match.find())
                throw new PluginImplementationException("Download link not found 2");
            httpMethod = getGetMethod(PlugUtils.unescapeHtml(match.group(1).trim()));
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
        if (contentAsString.contains("Požadovaný súbor sa na serveri nenachádza alebo bol odstránený") ||
                contentAsString.contains("súbor bol odstránený") ||
                contentAsString.contains("Súbor neexistuje")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    final static String LOGIN_PAGE = "http://euroshare.eu/login.html";

    private void login() throws Exception {
        synchronized (EuroShareFileRunner.class) {
            EuroShareServiceImpl service = (EuroShareServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No EuroShare account login information");
                }
            }
            if (!makeRedirectedRequest(getGetMethod(LOGIN_PAGE))) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            final Matcher rootMatch = getMatcherAgainstContent("var\\s*WEB_ROOT\\s*=\\s*['\"]([^'\"]+?)['\"]");
            if (!rootMatch.find()) throw new PluginImplementationException("Login page details not found 1");
            final Matcher urlMatch = getMatcherAgainstContent("url:\\s*WEB_ROOT\\s*\\+\\s*['\"]([^'\"]+?)['\"]");
            if (!urlMatch.find()) throw new PluginImplementationException("Login page details not found 2");
            final HttpMethod method = getMethodBuilder()
                    .setReferer(LOGIN_PAGE)
                    .setAction(rootMatch.group(1) + urlMatch.group(1))
                    .setParameter("username", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .setParameter("remember", "false")
                    .setParameter("backlink", "")
                    .setAjax().toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException("Error posting login info");
            }
            if (getContentAsString().contains("\"error\"") && !getContentAsString().contains("\"error\":\"\"")) {
                throw new BadLoginException("ERROR: "+ PlugUtils.getStringBetween(getContentAsString(), "\"error\":\"", "\""));
            }
        }
    }

}