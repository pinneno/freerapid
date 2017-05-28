package cz.vity.freerapid.plugins.services.twoddl;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class TwoDdlFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TwoDdlFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        if (!fileURL.contains("://linx.")) {
            correctUrl();
            final GetMethod getMethod = getGetMethod(fileURL);//make first request
            if (makeRedirectedRequest(getMethod)) {
                checkProblems();
                checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void correctUrl() {
        fileURL = fileURL.replaceFirst("(twoddl\\.(tv|eu|com|org)|2ddl\\.(la|link))", "iiddl.com");
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, ">", "</a></h2>");
        if (httpFile.getFileName().contains(">"))
            httpFile.setFileName("Get Links: " + httpFile.getFileName().substring(1 + httpFile.getFileName().lastIndexOf(">")));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        if (!fileURL.contains("://linx."))
            correctUrl();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems

            if (fileURL.contains("://linx.")) {
                fileURL = method.getURI().getURI();
                do {
                HttpMethod httpMethod = doCaptcha(getMethodBuilder().setReferer(fileURL)
                        .setActionFromFormWhereTagContains("submit", true)
                        .setAction(fileURL)).toPostMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                }while (getContentAsString().contains("Security Code"));
                checkProblems();
                final Matcher matcher = getMatcherAgainstContent("\"?(http[^\"]+?)s*</a");
                List<URI> list = new LinkedList<URI>();
                while (matcher.find()) {
                    list.add(new URI(matcher.group(1).trim()));
                }
                if (list.isEmpty())
                    throw new PluginImplementationException("No link found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName("Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            }
            else {
                checkNameAndSize(contentAsString);
                final Matcher match = getMatcherAgainstContent("rel=\"nofollow\">(http[^<>]+)");
                List<URI> list = new LinkedList<URI>();
                while (match.find()) {
                    list.add(new URI(match.group(1).trim()));
                }
                if (list.isEmpty()) throw new PluginImplementationException("No link(s) found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(this.httpFile, list);
                this.httpFile.setFileName("Link(s) Extracted !");
                this.httpFile.setState(DownloadState.COMPLETED);
                this.httpFile.getProperties().put("removeCompleted", true);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("ID was not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Moved Permanently")) {
            throw new URLNotAvailableAnymoreException("Moved Permanently"); //let to know user in FRD
        }
    }

    private MethodBuilder doCaptcha(MethodBuilder builder) throws Exception {
        final String baseUrl = new URL(fileURL).getProtocol() + "://" + new URL(fileURL).getAuthority();
        final String image = getMethodBuilder().setActionFromImgSrcWhereTagContains("Security").setBaseURL(baseUrl).getEscapedURI();
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captcha = captchaSupport.getCaptcha(image);
        if (captcha == null)
            throw new CaptchaEntryInputMismatchException();
        return builder.setParameter("security_code", captcha);
    }
}