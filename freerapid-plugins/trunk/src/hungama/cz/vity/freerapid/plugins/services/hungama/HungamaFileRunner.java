package cz.vity.freerapid.plugins.services.hungama;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class HungamaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(HungamaFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkUrl();
        if (!isAlbum(fileURL)) {
            final String trackId = getTrackId();
            final JsonNode audioPlayerData = getAudioPlayerData(trackId, new JsonMapper().getObjectMapper());
            checkNameAndSize(audioPlayerData);
        }
    }

    private void checkNameAndSize(JsonNode audioPlayerData) throws ErrorDuringDownloadingException {
        String album = audioPlayerData.findPath("album_name").getTextValue();
        String song = audioPlayerData.findPath("song_name").getTextValue();
        if (song == null) {
            throw new PluginImplementationException("Song name not found");
        }
        String filename = (album != null ? album + " - " + song : song) + ".mp4";
        httpFile.setFileName(filename);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        checkUrl();
        final ObjectMapper objectMapper = new JsonMapper().getObjectMapper();
        if (!isAlbum(fileURL)) {
            final String trackId = getTrackId();
            final JsonNode audioPlayerData = getAudioPlayerData(trackId, objectMapper);
            checkNameAndSize(audioPlayerData);

            final String songUrl = audioPlayerData.findPath("file").getTextValue();
            if (songUrl == null) {
                throw new PluginImplementationException("Song URL not found");
            }

            HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(songUrl)
                    .toGetMethod();
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            GetMethod getMethod = getGetMethod(fileURL);
            if (!makeRedirectedRequest(getMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            processAlbum(objectMapper);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("content is not available in your country")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkUrl() {
        //fileURL = fileURL.replaceFirst("/#/music/", "/music/");
    }

    private String getTrackId() throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("/(\\d{3,})/?$", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Track ID not found");
        }
        return matcher.group(1);
    }

    private JsonNode getAudioPlayerData(String trackId, ObjectMapper objectMapper) throws ErrorDuringDownloadingException, IOException {
        HttpMethod method = getMethodBuilder()
                .setAction("http://www.hungama.com/audio-player-data/track/" + trackId)
                .setAjax()
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
        JsonNode ret;
        try {
            ret = objectMapper.readTree(getContentAsString());
        } catch (IOException e) {
            throw new PluginImplementationException("Error parsing audio player data");
        }
        return ret;
    }

    private boolean isAlbum(String fileUrl) {
        return fileUrl.contains("/album/");
    }

    private void processAlbum(ObjectMapper objectMapper) throws Exception {
        Matcher matcher = getMatcherAgainstContent("(?s)\"track\":\\s*?(\\[\\{.+?\\}\\])");
        if (!matcher.find()) {
            throw new PluginImplementationException("Tracks data not found");
        }
        String tracksData = matcher.group(1).trim();

        JsonNode rootNode;
        List<URI> uriList = new LinkedList<URI>();
        try {
            rootNode = objectMapper.readTree(tracksData);
        } catch (IOException e) {
            throw new PluginImplementationException("Error parsing tracks data", e);
        }
        for (JsonNode trackNode : rootNode) {
            String url = trackNode.findPath("url").getTextValue();
            if (url != null) {
                try {
                    uriList.add(new URI(url));
                } catch (URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(uriList.size() + " links added");
    }

}