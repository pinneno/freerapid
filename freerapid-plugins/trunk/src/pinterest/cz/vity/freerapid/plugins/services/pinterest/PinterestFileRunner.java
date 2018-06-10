package cz.vity.freerapid.plugins.services.pinterest;

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
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author bircie
 */
class PinterestFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PinterestFileRunner.class.getName());

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
        if (fileURL.contains("/pin/")) {
            final Matcher match = PlugUtils.matcher("name=\"og:image\"\\s*content=\".+?/([^/\"]+?)\"", content);
            if (!match.find())
                throw new PluginImplementationException("File name not found");
            httpFile.setFileName(match.group(1).trim());
        } else {
            httpFile.setFileName("Board: " + PlugUtils.getStringBetween(content, "name=\"og:title\" content=\"", "\""));
            final Matcher match = PlugUtils.matcher("\"pin_count\"\\s*:\\s*(\\d+?)\\D", content);
            if (match.find())
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            fileURL =  method.getURI().getURI();
            checkProblems();//check problems
            final String content = getContentAsString();
            checkNameAndSize(content);//extract file name and size from the page
            if (fileURL.contains("/pin/")) {
                final Matcher match = PlugUtils.matcher("name=\"og:image\"\\s*content=\"(.+?)\"", content);
                if (!match.find())
                    throw new PluginImplementationException("Download link not found");
                final String dlLink = match.group(1).trim();
                if (!tryDownloadAndSaveFile(getGetMethod(dlLink))) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            } else {
                List<URI> list = new LinkedList<URI>();
                Matcher matchBoardPath = PlugUtils.matcher("\"pathname\": \"(.+?)\"", content);
                if (!matchBoardPath.find())
                    throw new PluginImplementationException("Error BP1");
                String subContent = content.substring(0, content.indexOf("bookmarks\":"));
                final Matcher matchPin = PlugUtils.matcher("\"id\"\\s*:\\s*\"([^\"]+?)\",\\s*\"description_html\"", subContent);
                while (matchPin.find())
                    list.add(new URI(getBaseURL() + "/pin/" + matchPin.group(1)));

                // check for more links
                final String source = PlugUtils.getStringBetween(content, "<link rel=\"canonical\" href=\"", "\">");
                final Matcher matchD1 = PlugUtils.matcher("name\"\\s*:\\s*\"ReactBoardFeedResource\", (\"options\": \\{\"board_id\":.+?)[\\},]", content);
                if (!matchD1.find()) throw new PluginImplementationException("Error D1");
                final Matcher matchD2 = PlugUtils.matcher("\"bookmarks\"\\s*:\\s*\"(.+?)\",", content);
                if (!matchD2.find()) throw new PluginImplementationException("Error D2");
                String data = "{" + matchD1.group(1).replaceAll("\\s", "") + ",\"board_url\":\"" +
                        matchBoardPath.group(1).replaceAll("\\s", "") + "\",\"bookmarks\":[\"" +
                        matchD2.group(1).replaceAll("\\s", "") + "\"],\"field_set_key\":\"react_grid_pin\",\"filter_section_pins\":true,\"layout\":\"default\",\"page_size\":25,\"redux_normalize_feed\":true},\"context\":{}}";
                long time = System.currentTimeMillis();

                do {
                    httpFile.setDownloaded(list.size());  //links found
                    time += 1;
                    final HttpMethod httpMethod = getMethodBuilder()
                            .setReferer(fileURL).setAjax()
                            .setAction("/resource/BoardFeedResource/get/")
                            .setParameter("source_url", URLEncoder.encode(source, "UTF-8"))
                            .setParameter("data", URLEncoder.encode(data, "UTF-8"))
                            .setParameter("_", "" + time)
                        .toGetMethod();
                    if (!makeRedirectedRequest(httpMethod)) {
                        checkProblems();
                        throw new ServiceConnectionProblemException();
                    }
                    subContent = getContentAsString().substring(0, getContentAsString().indexOf("bookmarks\":"));
                    final Matcher matchP2 = PlugUtils.matcher("}},\"id\":\"([^\"]+?)\",\"", subContent);
                    while (matchP2.find())
                        list.add(new URI(getBaseURL() + "/pin/" + matchP2.group(1)));
                    final Matcher matchB = PlugUtils.matcher("bookmarks\"\\s*:\\s*\\[\"(.+?)\"\\]", getContentAsString());
                    if (!matchB.find()) throw new PluginImplementationException("Error B");

                    final String d1 = data.substring(0, data.indexOf("bookmarks\":[\"") + ("bookmarks\":[\"").length());
                    final String d2 = data.substring(data.indexOf("\"]"));
                    data = d1 + matchB.group(1) + d2;
                } while (!getContentAsString().contains("\"bookmarks\":[\"-end-\"]"));

                if (list.isEmpty()) throw new PluginImplementationException("No links found");
                getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
                logger.info("Added " + list.size() + " links");
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
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("We couldn't find that page") || contentAsString.contains("/?redirected=1")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
        if (contentAsString.contains("Something went wrong!") ||
                contentAsString.contains("Our server is experiencing a mild case of the hiccups")) {
            throw new ErrorDuringDownloadingException("Internal Pinterest error");
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