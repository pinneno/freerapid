package cz.vity.freerapid.plugins.services.tumblr;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class TumblrFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(TumblrFileRunner.class.getName());

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
        if (!fileURL.contains("/post/")) {
            PlugUtils.checkName(httpFile, content, "<title>", "</title>");
            httpFile.setFileName("Tumblr: " + httpFile.getFileName());
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);
            if (!fileURL.contains("/post/")) {
                fileURL = fileURL.replaceFirst("tumblr\\.com/.*", "tumblr\\.com/");
                List<URI> list = new LinkedList<URI>();
                int page = 1;
                while (getContentAsString().contains("\"@type\":\"ListItem\"") && httpFile.getState() == DownloadState.GETTING) {
                    final Matcher match = PlugUtils.matcher("\"@type\":\"ListItem\",\"position\":\\d+,\"url\":\"(.+?)\"", getContentAsString());
                    while (match.find()) {
                        list.add(new URI(getMethodBuilder().setAction(match.group(1).replace("\\/", "/")).getEscapedURI()));
                    }
                    httpFile.setFileSize(list.size());  //links found
                    page = page + 1;
                    final HttpMethod nextPageMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL + "page/" + page).toGetMethod();
                    if (!makeRedirectedRequest(nextPageMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                }
                if (httpFile.getState() != DownloadState.GETTING) {
                    httpFile.setFileSize(-1);
                    return;
                }
                if (list.isEmpty()) throw new PluginImplementationException("No posts found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName("Post(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);

            } else {
                String dlUrl = PlugUtils.getStringBetween(content, "\"image\":\"", "\",\"").replace("\\/", "/");
                httpFile.setFileName(dlUrl.substring(1 + dlUrl.lastIndexOf("/")));
                //here is the download link extraction
                if (!tryDownloadAndSaveFile(getGetMethod(dlUrl))) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The URL you requested could not be found") ||
                contentAsString.contains("Not Found</title>")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}