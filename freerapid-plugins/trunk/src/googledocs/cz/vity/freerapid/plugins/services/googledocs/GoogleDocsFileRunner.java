package cz.vity.freerapid.plugins.services.googledocs;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u3
 */
class GoogleDocsFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(GoogleDocsFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        correctURL();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            fileURL = getMethod.getURI().getURI();
            checkProblems();
            doLogin();
            checkNameAndSize(getContentAsString());
        } else {
            if (getMethod.getStatusCode() / 100 == 4) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        if (fileURL.contains("/folder")) {
            Matcher matcher = getMatcherAgainstContent("<title>(.+?)\\s*–\\s*Google");
            if (!matcher.find())
                throw new PluginImplementationException("Folder name not found");
            httpFile.setFileName("Folder > " + matcher.group(1).trim());
        } else {
            PlugUtils.checkName(httpFile, content, "\"og:title\" content=\"", "\"");
            Matcher matcher = getMatcherAgainstContent("\\[\\w+,\\w+,\"(\\d+)\"\\]");
            if (matcher.find())
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).trim()));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void correctURL() {
          fileURL = fileURL.replaceFirst("/uc\\?id=", "/file/d/").split("&")[0];
    }

    @Override
    public void run() throws Exception {
        super.run();
        correctURL();
        if (!fileURL.contains("confirm=no_antivirus")) {
            if (!fileURL.contains("?")) {
                fileURL += "?confirm=no_antivirus";
            } else {
                fileURL += "&confirm=no_antivirus";
            }
        }
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            fileURL = method.getURI().getURI();
            checkProblems();
            doLogin();
            checkNameAndSize(getContentAsString());
            if (fileURL.contains("/folder")) {
                List<URI> list = new LinkedList<URI>();
                Matcher match = getMatcherAgainstContent("\\\\x5b\\\\x22([^\\\\,]+)\\\\x22,\\\\x5b\\\\x22");
                while (match.find()) {
                    list.add(new URI(getMethodBuilder().setReferer(fileURL).setAction("/open?id=" + match.group(1).trim()).getEscapedURI()));
                }
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);

            } else {
                Matcher matcher = getMatcherAgainstContent("\"(https?://[^\"]+?download)\"");
                if (!matcher.find()) {
                    throw new PluginImplementationException("Download URL not found");
                }
                String downloadUrl = PlugUtils.unescapeUnicode(matcher.group(1));
                setClientParameter(DownloadClientConsts.NO_CONTENT_LENGTH_AVAILABLE, true);
                httpFile.setResumeSupported(true);
                HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(downloadUrl).toGetMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    httpMethod = getMethodBuilder().setReferer(fileURL).setAction(downloadUrl).toGetMethod();
                    if (!makeRedirectedRequest(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    checkProblems();

                    String referer = httpMethod.getURI().toString();
                    URL url = new URL(referer);
                    matcher = getMatcherAgainstContent("<a id=\"uc-download-link\".*? href=\"(.+?)\"");
                    if (!matcher.find()) {
                        throw new PluginImplementationException("Download link not found");
                    }
                    String downloadLink = PlugUtils.replaceEntities(URLDecoder.decode(matcher.group(1).trim(), "UTF-8"));
                    String baseUrl = url.getProtocol() + "://" + url.getAuthority();
                    httpMethod = getMethodBuilder()
                            .setReferer(referer)
                            .setBaseURL(baseUrl)
                            .setAction(downloadLink)
                            .toGetMethod();
                    if (!tryDownloadAndSaveFile(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException("Error starting download");
                    }
                }
            }
        } else {
            if (method.getStatusCode() / 100 == 4) {
                throw new URLNotAvailableAnymoreException("File not found");
            }
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("file you have requested does not exist")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void doLogin() throws Exception {
        if (getContentAsString().contains("Sign in to continue to Google")) {
            synchronized (GoogleDocsFileRunner.class) {
                GoogleDocsServiceImpl service = (GoogleDocsServiceImpl) getPluginService();
                PremiumAccount pa = service.getConfig();
                if (!pa.isSet()) {
                    pa = service.showConfigDialog();
                    if (pa == null || !pa.isSet()) {
                        throw new BadLoginException("Login Required - No Google account information!");
                    }
                }

                HttpMethod method = getMethodBuilder()
                        .setActionFromFormWhereActionContains("signin", true)
                        .setParameter("Email", pa.getUsername())
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                if (getContentAsString().contains("class=\"has-error\""))
                    throw new BadLoginException("Google doesn't recognize your email");

                method = getMethodBuilder()
                        .setActionFromFormWhereActionContains("signin", true)
                        .setParameter("Passwd", pa.getPassword())
                        .toPostMethod();
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                if (getContentAsString().contains("class=\"form-error\""))
                    throw new BadLoginException("Google doesn't recognize your password");

                String url = fileURL;
                Matcher match = getMatcherAgainstContent("URL=([^\"']+)");
                if (match.find())
                    url = match.group(1).trim();
                if (!makeRedirectedRequest(getGetMethod(url))) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                logger.info("Logged in !!!");
            }
        }
    }

}
