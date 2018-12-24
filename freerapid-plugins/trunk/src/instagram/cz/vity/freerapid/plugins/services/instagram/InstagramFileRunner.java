package cz.vity.freerapid.plugins.services.instagram;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.digest.DigestUtils;
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
class InstagramFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(InstagramFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        fileURL = checkURL(fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getDownloadLink(getContentAsString()));//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String downloadLink) throws ErrorDuringDownloadingException {
        if (fileURL.contains("/p/")) {
            String filename;
            try {
                filename = PlugUtils.suggestFilename(downloadLink);
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("File name not found");
            }
            logger.info("File name: " + filename);
            httpFile.setFileName(filename);
        } else {
            final String content = getContentAsString();
            Matcher match = PlugUtils.matcher("content=\"(.+?)\\s.\\sInstagram", content);
            if (!match.find())
                throw new PluginImplementationException("User's name not found");
            httpFile.setFileName("User: " + match.group(1));
            match = PlugUtils.matcher("timeline_media\"\\s*:\\s*\\{\"count\"\\s*:\\s*(\\d+),\\s*\"page_info\"\\s*:\\s*\\{", content);
            if (!match.find())
                throw new PluginImplementationException("Media count not found");
            httpFile.setFileSize(Long.parseLong(match.group(1)));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = checkURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            if (fileURL.contains("/p/")) {  // post
                String downloadLink = getDownloadLink(getContentAsString());
                checkNameAndSize(downloadLink);
                if (!tryDownloadAndSaveFile(getGetMethod(downloadLink))) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            } else {  // user
                fileURL = method.getURI().getURI();
                checkNameAndSize(fileURL);
                List<URI> list = new LinkedList<URI>();
                String content = getContentAsString();
                Matcher matcher = PlugUtils.matcher("\"id\"\\s*:\\s*\"([^\"]+?)\"", content);
                if (!matcher.find())  throw new PluginImplementationException("Owner ID not found");
                final String userID = matcher.group(1);
                matcher = PlugUtils.matcher("\"rhx_gis\"\\s*:\\s*\"([^\"]+?)\"", content);
                if (!matcher.find()) throw new PluginImplementationException("RHX_GIS not found");
                final String rhxGis = matcher.group(1);
                matcher = PlugUtils.matcher("type=\"text/javascript\"[^>]*src=\"([^\"]+?Container[^\"]+?)\"", content);
                if (!matcher.find())  throw new PluginImplementationException("Query id not found 1");
                if (!makeRedirectedRequest(getGetMethod(getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1).trim()).getEscapedURI()))) {
                    throw new ServiceConnectionProblemException();
                }
                matcher = PlugUtils.matcher("profilePosts.byUserId.get[^}]*},queryId\\s*:\\s*\"([^\"]+?)\"", getContentAsString());
                if (!matcher.find())  throw new PluginImplementationException("Query id not found 2");
                final String queryId = matcher.group(1);
                boolean nextPage;
                do {
                    nextPage = false;
                    final Matcher match = PlugUtils.matcher("\"(?:short)?code\"\\s*:\\s*\"(.+?)\"", content);
                    while (match.find()) {
                        list.add(new URI(getBaseURL() + "/p/" + match.group(1)));
                    }
                    httpFile.setDownloaded(list.size());  //links found
                    if (content.replaceAll("\\s+", "").contains("timeline_media\":{\"count\":" + httpFile.getFileSize() + ",\"page_info\":{\"has_next_page\":true")) {
                        nextPage = true;
                        final Matcher lastMatch = PlugUtils.matcher("end_cursor\"\\s*:\\s*\"(.+?)\"", content);
                        if (!lastMatch.find()) throw new PluginImplementationException("Error getting next page details");
                        final String lastPost = lastMatch.group(1);
                        final String queryUrl = "/graphql/query/";
                        final String queryVariables = "{\"id\":\"" + userID + "\",\"first\":\"50\",\"after\":\"" + lastPost + "\"}";
                        final String gisHeaderValue = DigestUtils.md5Hex(rhxGis + ":" + queryVariables);
                        final HttpMethod nextPageMethod = getMethodBuilder(content).setReferer(fileURL)
                                .setAction(queryUrl)
                                .setParameter("query_hash", queryId)
                                .setAndEncodeParameter("variables", queryVariables)
                                .setHeader("X-Instagram-GIS", gisHeaderValue)
                                .setAjax()
                                .toGetMethod();
                        if (!makeRedirectedRequest(nextPageMethod)) {
                            checkProblems();
                            throw new ServiceConnectionProblemException();
                        }
                        content = getContentAsString();
                    }
                } while (nextPage);
                if (list.isEmpty()) throw new PluginImplementationException("No posts found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                httpFile.setFileName(list.size() + " Post(s) Extracted !");
                httpFile.setState(DownloadState.COMPLETED);
                httpFile.getProperties().put("removeCompleted", true);
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getDownloadLink(String content) throws PluginImplementationException {
        final Matcher match;
        if (content.contains(":type\" content=\"video"))
            match = PlugUtils.matcher("\"video_url\"\\s*:\\s*\"([^\"]+?)\"", content);
        else
            match = PlugUtils.matcher("\"display_(?:src|url)\"\\s*:\\s*\"([^\"]+?)\"", content);
        if (!match.find())
            throw new PluginImplementationException("Download link not found");
        return match.group(1);
    }

    private String checkURL(final String fileURL) {
        return fileURL.replace("http://", "https://").replace("://instagram.", "://www.instagram.");
    }

    @Override
    protected String getBaseURL() {
        return "https://www.instagram.com";
    }

}