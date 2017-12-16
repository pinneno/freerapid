package cz.vity.freerapid.plugins.services.mupload;

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
class MUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MUploadFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        if (!fileURL.matches(".+?mupload.net/\\w\\w_[\\w/]+")) {
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

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("File:(?:\\s*<[^<>]+>)*([^<>]+)<", content);
        if (!matcher.find())
            throw new PluginImplementationException("File name not found");
        httpFile.setFileName("Extract Link(s): " + matcher.group(1).trim());
        matcher = PlugUtils.matcher("Size:(?:\\s*<[^<>]+>)*([^<>]+)<", content);
        if (!matcher.find())
            throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).trim()));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            if (fileURL.matches(".+?mupload.net/\\w\\w_[\\w/]+")) {
                HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromTextBetween("<frame src='", "'>").toGetMethod();
                if (!makeRedirectedRequest(httpMethod))
                    throw new ServiceConnectionProblemException("1");
                int wait = 1 + PlugUtils.getNumberBetween(getContentAsString(), "var count = ", ";");
                downloadTask.sleep(wait);
                httpMethod = getMethodBuilder().setActionFromFormWhereTagContains("parent", true).setAction(fileURL).toPostMethod();
                if (!makeRedirectedRequest(httpMethod))
                    throw new ServiceConnectionProblemException("2");
                this.httpFile.setNewURL(new URL(httpMethod.getResponseHeader("Location").getValue()));
                this.httpFile.setPluginID("");
                this.httpFile.setState(DownloadState.QUEUED);
                return;
            }
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final List<URI> list = new LinkedList<URI>();
            final Matcher matcher = getMatcherAgainstContent("<a href=['\"](http[^'\"]+?)['\"] target=['\"]_blank['\"]>http");
            while (matcher.find()) {
                list.add(new URI((matcher.group(1).trim())));
            }
            if (list.isEmpty()) throw new PluginImplementationException("No links found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            httpFile.setFileName("Link(s) Extracted !");
            httpFile.setState(DownloadState.COMPLETED);
            httpFile.getProperties().put("removeCompleted", true);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}