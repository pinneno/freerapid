package cz.vity.freerapid.plugins.services.chauthanh;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
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
class ChauThanhFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ChauThanhFileRunner.class.getName());

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
        if (fileURL.contains("/view/")) {
            Matcher matcher = getMatcherAgainstContent("<h2[^>]+?>(.+?)<");
            if (!matcher.find()) throw new PluginImplementationException("File name not found");
        }
        else if (fileURL.contains("/download/")) {
            PlugUtils.checkName(httpFile, content, "description\" content=\"Download file ", "\"");
        }
        else {
            throw new InvalidURLOrServiceProblemException("Unrecognised Url");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
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

            if (fileURL.contains("/view/")) {
                final LinkedList<URI> list = new LinkedList<URI>();
                final Matcher matcher = getMatcherAgainstContent("<a href=\"([^\"]+?/download/[^\"]+?)\">");
                while (matcher.find()) {
                    list.add(new URI(getMethodBuilder().setAction(getLink(matcher.group(1).trim())).getEscapedURI()));
                }
                // add urls to queue
                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName(list.size() + " Link(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            }
            else if (fileURL.contains("/download/")) {
                final Matcher matcher = getMatcherAgainstContent("<a href=\"([^\"]+?/eri/[^\"]+?)\">");
                if (!matcher.find())
                    throw new PluginImplementationException("Download link not found");

                final HttpMethod httpMethod = getMethodBuilder().setAction(getLink(matcher.group(1).trim())).toGetMethod();
                setFileStreamContentTypes("text/plain");
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
            else {
                throw new InvalidURLOrServiceProblemException("Unrecognised Url");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getLink(String ref) {
        String link = "ERR";
        if (ref.startsWith("http"))
            link = ref;
        if (ref.startsWith("../")) {
            link = fileURL.substring(0, fileURL.lastIndexOf("/", fileURL.lastIndexOf("/")-1)) + ref.substring(ref.indexOf("/"));
        }
        return link;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}