package cz.vity.freerapid.plugins.services.appletrailers;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import jlibs.core.net.URLUtil;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class AppleTrailersFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AppleTrailersFileRunner.class.getName());
    private final static String TITLE = "title";
    private SettingsConfig config;

    private void setConfig() throws Exception {
        AppleTrailersServiceImpl service = (AppleTrailersServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);

        if (fileURL.contains(".mov") || fileURL.contains(".m4v")) {
            URL url = new URL(fileURL);
            String filename = null;
            try {
                filename = URLUtil.getQueryParams(url.toString(), "UTF-8").get(TITLE);
            } catch (Exception e) {
                //
            }
            if (filename == null) {
                throw new PluginImplementationException("File name not found");
            }

            fileURL = url.getProtocol() + "://" + url.getAuthority() + url.getPath();
            String fileExt = fileURL.substring(fileURL.lastIndexOf("."));
            httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(URLDecoder.decode(filename, "UTF-8"), "_") + fileExt);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

            GetMethod method = new GetMethod(fileURL);
            setTextContentTypes("video/quicktime");
            if (fileURL.contains(".mov")) {
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                String downloadURL;
                Matcher matcher = PlugUtils.matcher("(https?://.+\\.mov)", getContentAsString());
                if (!matcher.find()) {
                    throw new PluginImplementationException("Download URL not found");
                }
                downloadURL = matcher.group(1);
                method = getGetMethod(downloadURL);
            }
            setFileStreamContentTypes("video/quicktime");
            if (!tryDownloadAndSaveFile(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            setConfig();
            fileURL = fileURL.replaceFirst("#.+", "");
            if (!makeRedirectedRequest(getGetMethod(fileURL))) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            try {
                PlugUtils.checkName(httpFile, getContentAsString(), "<title>", "- Movie Trailers");
            } catch (PluginImplementationException e) {
                LogUtils.processException(logger, e);
            }

            if (!makeRedirectedRequest(getMethodBuilder().setReferer(fileURL).setAction(fileURL + "data/page.json").toGetMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            JsonNode rootNode;
            try {
                rootNode = new JsonMapper().getObjectMapper().readTree(getContentAsString());
            } catch (IOException e) {
                throw new PluginImplementationException("Error parsing trailer data (1)");
            }

            logger.info("Settings config: " + config);
            final List<AppleTrailersVideo> videoList = getSelectedVideoList(rootNode);
            final List<URI> uriList = new ArrayList<URI>();
            for (AppleTrailersVideo video : videoList) {
                try {
                    uriList.add(new URI(video.url + "?" + TITLE + "=" + URLEncoder.encode(httpFile.getFileName() + " - " + video.title, "UTF-8")));
                } catch (URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }

            if (uriList.isEmpty()) throw new PluginImplementationException("Videos not found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            httpFile.getProperties().put("removeCompleted", true);
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("the page you’re looking for can’t be found")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private List<AppleTrailersVideo> getSelectedVideoList(JsonNode rootNode) throws PluginImplementationException {
        JsonNode clipsNodes = rootNode.get("clips");
        if (clipsNodes == null) {
            throw new PluginImplementationException("Error parsing trailer data (2)");
        }

        List<AppleTrailersVideo> videoList = new ArrayList<AppleTrailersVideo>();
        for (JsonNode clipNode : clipsNodes) {
            String title = clipNode.findPath("title").getTextValue();
            if (title == null) {
                throw new PluginImplementationException("Error parsing trailer data (3)");
            }
            JsonNode sizes = clipNode.findPath("sizes");
            List<AppleTrailersVideo> videoListTemp = new ArrayList<AppleTrailersVideo>();
            if (!sizes.isMissingNode()) {
                for (VideoQuality videoQuality : VideoQuality.getItems()) {
                    JsonNode size = sizes.get(videoQuality.getQualityToken());
                    if (size != null) {
                        String src = size.get("src").getTextValue();
                        if (src == null) {
                            continue;
                        }
                        AppleTrailersVideo video = new AppleTrailersVideo(title, videoQuality, src);
                        logger.info("Found video: " + video);
                        videoListTemp.add(video);

                        String srcAlt = size.get("srcAlt").getTextValue();
                        if (srcAlt != null) {
                            video = new AppleTrailersVideo(title, videoQuality, srcAlt);
                            logger.info("Found video: " + video);
                            videoListTemp.add(video);
                        }
                    }
                }

                AppleTrailersVideo selectedVideo = null;
                //select quality
                final int LOWER_QUALITY_PENALTY = 10;
                int weight = Integer.MAX_VALUE;
                int selectedVideoQuality = -1;
                for (AppleTrailersVideo video : videoListTemp) {
                    int deltaQ = video.videoQuality.getQuality() - config.getVideoQuality().getQuality();
                    int tempWeight = (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
                    if (tempWeight < weight) {
                        weight = tempWeight;
                        selectedVideoQuality = video.videoQuality.getQuality();
                    }
                }

                if (selectedVideoQuality != -1) {
                    //select format
                    weight = Integer.MIN_VALUE;
                    for (AppleTrailersVideo video : videoListTemp) {
                        if (video.videoQuality.getQuality() == selectedVideoQuality) {
                            int tempWeight;
                            if (config.getVideoFormat() == video.videoFormat) {
                                tempWeight = 100;
                            } else if (video.videoFormat == VideoFormat.MOV) {
                                tempWeight = 50;
                            } else {
                                tempWeight = 49;
                            }
                            if (tempWeight > weight) {
                                weight = tempWeight;
                                selectedVideo = video;
                            }
                        }
                    }
                    if (selectedVideo != null) {
                        videoList.add(selectedVideo);
                    }
                }
            }
        }
        return videoList;
    }

    private class AppleTrailersVideo {
        private final String title;
        private final VideoQuality videoQuality;
        private final VideoFormat videoFormat;
        private final String url;

        private AppleTrailersVideo(final String title, final VideoQuality videoQuality, final String url) {
            this.title = title;
            this.videoQuality = videoQuality;
            this.url = url;
            this.videoFormat = (url.endsWith(".mov") ? VideoFormat.MOV : VideoFormat.M4V);
        }

        @Override
        public String toString() {
            return "AppleTrailersVideo{" +
                    "title='" + title + '\'' +
                    ", videoQuality=" + videoQuality +
                    ", videoFormat=" + videoFormat +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

}