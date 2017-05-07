package cz.vity.freerapid.plugins.services.nhk_vod;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.applehls.AdjustableBitrateHlsDownloader;
import cz.vity.freerapid.plugins.services.applehls.HlsDownloader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class NHK_vodFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NHK_vodFileRunner.class.getName());

    private final static String GET_ALL_API_URL = "https://api.nhk.or.jp/nhkworld/vodesdlist/v1/all/all/all.json?apikey=EJfK8jdS57GqlupFgAfAAwr573q01y6k";
    //private final static String PLAYER_BRANDING_ID = "43e68966aa77408bb5cfeb054861e73a";
    private final static String P_CODE = "lqcGIyOmhVSXLwE8dLZfLLZRPaye";
    private NHK_vodSettingsConfig config;

    private void setConfig() throws Exception {
        final NHK_vodServiceImpl service = (NHK_vodServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkNameAndSize(getEpisodeNode());
    }

    private void checkNameAndSize(JsonNode episodeNode) throws ErrorDuringDownloadingException {
        String prgName = episodeNode.findPath("title_clean").getTextValue();
        String episodeName = episodeNode.findPath("sub_title_clean").getTextValue();
        if (episodeName == null) {
            throw new PluginImplementationException("Episode name not found");
        }
        String fileName = (prgName == null ? "" : prgName + " - ") + episodeName + ".ts";
        httpFile.setFileName(fileName);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        JsonNode episodeNode = getEpisodeNode();
        checkNameAndSize(episodeNode);
        String vodId = episodeNode.findPath("vod_id").getTextValue();
        if (vodId == null) {
            throw new PluginImplementationException("VOD ID not found");
        }

        GetMethod getMethod = getGetMethod(String.format("https://player.ooyala.com/sas/player_api/v2/authorization/embed_code/%s/%s?device=html5&domain=www3.nhk.or.jp", P_CODE, vodId));
        if (!makeRedirectedRequest(getMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();

        JsonNode rootNode;
        try {
            rootNode = new JsonMapper().getObjectMapper().readTree(getContentAsString());
        } catch (IOException e) {
            throw new PluginImplementationException("Error parsing VOD player data (1)");
        }
        String data = rootNode.findPath("data").getTextValue();
        if (data == null) {
            throw new PluginImplementationException("Error parsing VOD player data (2)");
        }
        String playlistUrl = new String(Base64.decodeBase64(data));
        if (!playlistUrl.startsWith("http")) { //in case it's not in encoded form
            throw new PluginImplementationException("Error decoding player URL");
        }

        setConfig();
        logger.info("Settings config: " + config);
        HlsDownloader hlsDownloader = new AdjustableBitrateHlsDownloader(client, httpFile, downloadTask, config.getVideoQuality().getBitrate());
        hlsDownloader.tryDownloadAndSaveFile(playlistUrl);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Error: 404 Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private JsonNode getEpisodeNode() throws Exception {
        GetMethod method = getGetMethod(GET_ALL_API_URL);
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();

        JsonNode rootNode;
        try {
            rootNode = new JsonMapper().getObjectMapper().readTree(getContentAsString());
        } catch (IOException e) {
            throw new PluginImplementationException("Error parsing episodes json data (1)");
        }

        JsonNode episodesNodes = rootNode.findPath("episodes");
        if (episodesNodes.isMissingNode()) {
            throw new PluginImplementationException("Error parsing episodes json data (2)");
        }

        JsonNode episodeNode = null;
        String path = downloadTask.getDownloadFile().getFileUrl().getPath();
        for (JsonNode tempEpisodeNode : episodesNodes) {
            JsonNode urlNode = tempEpisodeNode.get("url");
            if (urlNode != null && urlNode.getTextValue().equals(path)) {
                episodeNode = tempEpisodeNode;
                break;
            }
        }
        if (episodeNode == null) {
            throw new PluginImplementationException("Episode not found in json data");
        }
        return episodeNode;
    }

}
