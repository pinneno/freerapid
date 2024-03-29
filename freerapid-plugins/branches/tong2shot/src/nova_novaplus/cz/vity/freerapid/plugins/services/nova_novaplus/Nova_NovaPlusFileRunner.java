package cz.vity.freerapid.plugins.services.nova_novaplus;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.RtmpDownloader;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class Nova_NovaPlusFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Nova_NovaPlusFileRunner.class.getName());
    private final static String TIME_SERVICE_URL = "http://tn.nova.cz/lbin/time.php";
    private SettingsConfig config;

    private void setConfig() throws Exception {
        Nova_NovaPlusServiceImpl service = (Nova_NovaPlusServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        String filename;
        try {
            filename = PlugUtils.getStringBetween(content, "<h1>", "</h1>", 2);
        } catch (PluginImplementationException e) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(filename + ".flv");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            Matcher matcher = PlugUtils.matcher("url:\\s*?\"(http://[^\"]+?config\\.php[^\"]+?)\"", getContentAsString());
            if (!matcher.find()) {
                throw new PluginImplementationException("Config URL not found");
            }
            String configUrl = matcher.group(1);
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(configUrl).toGetMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String base64Config;
            try {
                base64Config = PlugUtils.getStringBetween(getContentAsString(), "'", "';");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Base64 config not found");
            }
            String configDecrypted = new Crypto().decrypt(base64Config);
            setConfig();
            if (configDecrypted.contains("\"nacevi-resolver\":{")) { //RTMP
                String mediaListContent = getMediaListContent(configDecrypted);
                Nova_NovaPlusVideo selectedVideo = getSelectedRtmpVideo(mediaListContent);
                RtmpSession rtmpSession = new RtmpSession(selectedVideo.baseUrl, selectedVideo.url);
                new RtmpDownloader(client, downloadTask).tryDownloadAndSaveFile(rtmpSession);
            } else { //HTTP
                Nova_NovaPlusVideo selectedVideo = getSelectedHttpVideo(configDecrypted);
                httpFile.setFileName(httpFile.getFileName().replaceFirst("\\..{3,4}$", ".mp4"));
                httpMethod = getMethodBuilder().setReferer(fileURL).setAction(selectedVideo.url).toHttpMethod();
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("ale hledáte stránku, která neexistuje")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String getTimeString() throws Exception {
        if (!makeRequest(getGetMethod(TIME_SERVICE_URL))) {
            throw new PluginImplementationException("Time service not available");
        }
        return getContentAsString().substring(0, 14);
    }

    private String getMediaListContent(String configDecrypted) throws Exception {
        String timeStr = getTimeString();
        String mediaId;
        String resolverContent;
        String serviceUrl;
        String appId;
        String secret;
        try {
            mediaId = PlugUtils.getStringBetween(configDecrypted, "\"mediaId\":", ",");
            resolverContent = PlugUtils.getStringBetween(configDecrypted, "\"nacevi-resolver\":{", "},");
            serviceUrl = PlugUtils.getStringBetween(resolverContent, "\"serviceUrl\":\"", "\"").replace("\\/", "/");
            appId = PlugUtils.getStringBetween(resolverContent, "\"appId\":\"", "\"");
            secret = PlugUtils.getStringBetween(resolverContent, "\"secret\":\"", "\"");
        } catch (PluginImplementationException e) {
            logger.warning(configDecrypted);
            throw new PluginImplementationException("Error parsing media config content", e);
        }
        String hashString = appId + "|" + mediaId + "|" + timeStr + "|" + secret;
        String base64FromBA = Base64.encodeBase64String(DigestUtils.md5(hashString));
        HttpMethod method = getMethodBuilder()
                .setAction(serviceUrl)
                        //.setAction("http://voyo.nova.cz/lbin/cdn-cra-r.php")
                .setParameter("c", appId + "|" + mediaId)
                .setParameter("h", "0")
                .setParameter("t", timeStr)
                .setParameter("s", base64FromBA)
                .setParameter("tm", "nova")
                .setParameter("d", "1")
                .setEncodeParameters(true)
                .toGetMethod();
        if (!makeRedirectedRequest(method) || !getContentAsString().contains("<status>Ok</status>")) {
            throw new PluginImplementationException("Error getting media list content");
        }
        return getContentAsString();
    }

    private Nova_NovaPlusVideo getSelectedRtmpVideo(String mediaListContent) throws PluginImplementationException {
        List<Nova_NovaPlusVideo> videoList = new ArrayList<Nova_NovaPlusVideo>();
        String baseUrl = PlugUtils.getStringBetween(mediaListContent, "<baseUrl>", "</baseUrl>").replace("<![CDATA[", "").replace("]]>", "");
        Matcher mediaMatcher = PlugUtils.matcher("(?s)<media>(.+?)</media>", mediaListContent);
        while (mediaMatcher.find()) {
            String mediaContent = mediaMatcher.group(1);
            Matcher qualityMatcher = PlugUtils.matcher("(?s)<quality>(.+?)</quality>", mediaContent);
            Matcher urlMatcher = PlugUtils.matcher("(?s)<url>(.+?)</url>", mediaContent);
            if (!qualityMatcher.find() || !urlMatcher.find()) {
                logger.warning(mediaListContent);
                throw new PluginImplementationException("Error parsing media");
            }
            String quality = qualityMatcher.group(1).replace("<![CDATA[", "").replace("]]>", "");
            String url = urlMatcher.group(1).replace("<![CDATA[", "").replace("]]>", "");
            for (VideoQuality videoQuality : VideoQuality.values()) {
                if (videoQuality.getLabel().contains(quality)) {
                    Nova_NovaPlusVideo novaPlusVideo = new Nova_NovaPlusVideo(videoQuality, baseUrl, url);
                    videoList.add(novaPlusVideo);
                    logger.info("Found video: " + novaPlusVideo);
                    break;
                }
            }
        }
        if (videoList.isEmpty()) {
            logger.warning(mediaListContent);
            throw new PluginImplementationException("No available videos");
        }
        Nova_NovaPlusVideo selectedVideo = Collections.min(videoList);
        logger.info("Config settings : " + config);
        logger.info("Selected video  : " + selectedVideo);
        return selectedVideo;
    }

    private Nova_NovaPlusVideo getSelectedHttpVideo(String configDecrypted) throws Exception {
        String url;
        String bitratesString;
        String urlPattern;
        try {
            url = PlugUtils.getStringBetween(configDecrypted, "\"url\":\"", "\"").replace("\\/", "/");
            urlPattern = PlugUtils.getStringBetween(configDecrypted, "\"urlPattern\":\"", "\"").replace("\\/", "/");
            bitratesString = PlugUtils.getStringBetween(configDecrypted, "\"bitrates\":{", "}");
        } catch (PluginImplementationException e) {
            logger.warning(configDecrypted);
            throw new PluginImplementationException("Error parsing media config content", e);
        }
        List<Nova_NovaPlusVideo> videoList = new LinkedList<Nova_NovaPlusVideo>();
        for (VideoQuality videoQuality : VideoQuality.getItems()) {
            String qualityLabel = videoQuality.getLabel();
            if (bitratesString.contains(qualityLabel)) {
                Nova_NovaPlusVideo video = new Nova_NovaPlusVideo(videoQuality, urlPattern, getVideoUrl(urlPattern, url, qualityLabel));
                videoList.add(video);
                logger.info("Found: " + video);
            }
        }
        if (videoList.isEmpty()) {
            logger.warning(configDecrypted);
            throw new PluginImplementationException("No available videos");
        }
        Nova_NovaPlusVideo selectedVideo = Collections.min(videoList);
        logger.info("Config settings : " + config);
        logger.info("Selected video  : " + selectedVideo);
        return selectedVideo;
    }

    private String getVideoUrl(String urlPattern, String url, String qualityLabel) {
        return urlPattern.replace("{0}", url).replace("{1}", qualityLabel) + "?start=0";
    }

    private class Nova_NovaPlusVideo implements Comparable<Nova_NovaPlusVideo> {
        private final static int LOWER_QUALITY_PENALTY = 10;
        private final VideoQuality videoQuality;
        private final String baseUrl;
        private final String url;
        private int weight;

        public Nova_NovaPlusVideo(final VideoQuality videoQuality, String baseUrl, final String url) {
            this.videoQuality = videoQuality;
            this.baseUrl = baseUrl;
            this.url = url;
            calcWeight();
        }

        private void calcWeight() {
            VideoQuality configQuality = config.getVideoQuality();
            int deltaQ = videoQuality.getQuality() - configQuality.getQuality();
            weight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ); //prefer nearest better if the same quality doesn't exist
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(final Nova_NovaPlusVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "Nova_NovaPlusVideo{" +
                    "videoQuality=" + videoQuality +
                    ", baseUrl='" + baseUrl + '\'' +
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

}
