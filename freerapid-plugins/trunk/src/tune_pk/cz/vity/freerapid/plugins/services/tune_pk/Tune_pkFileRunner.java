package cz.vity.freerapid.plugins.services.tune_pk;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class Tune_pkFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Tune_pkFileRunner.class.getName());
    private SettingsConfig config;

    private void setConfig() throws Exception {
        Tune_pkServiceImpl service = (Tune_pkServiceImpl) getPluginService();
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
        PlugUtils.checkName(httpFile, content, "\"og:title\" content=\"", "\"");
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
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

            setConfig();
            Tune_pkVideo selectedVideo = getSelectedVideo(getContentAsString());
            logger.info("Config settings : " + config);
            logger.info("Selected video  : " + selectedVideo);
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(selectedVideo.url).toHttpMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        Matcher matcher = getMatcherAgainstContent("<p>\\s*Video does not exist\\s*</p>");
        if (contentAsString.contains("Not available!") && matcher.find()) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private Tune_pkVideo getSelectedVideo(String content) throws PluginImplementationException {
        List<Tune_pkVideo> videoList = new LinkedList<Tune_pkVideo>();
        if (content.contains("only uploader friends can view this video")) {
            final String[] qualityLabels = new String[]{"240", "360", "480", "720"};
            Matcher matcher = PlugUtils.matcher("\"twitter:player:stream\" content=\"([^\"]+)\"", content);
            if (!matcher.find()) {
                throw new PluginImplementationException("Video URL not found");
            }
            String videoUrl = matcher.group(1);
            matcher = PlugUtils.matcher("-\\d{3}\\.mp4", videoUrl);
            if (!matcher.find()) {
                throw new PluginImplementationException("Invalid video URL");
            }
            for (String qualityLabel : qualityLabels) {
                Tune_pkVideo video = new Tune_pkVideo(Integer.parseInt(qualityLabel), videoUrl.replaceFirst("-\\d{3}\\.mp4", "-" + qualityLabel + ".mp4"));
                videoList.add(video);
                logger.info("Found: " + video);
            }
        } else {
            Matcher videoFilesMatcher = Pattern.compile("(?s)var video_files\\s*?=\\s*?(\\[.+?\\];)").matcher(content);
            Matcher videoFileMatcher = Pattern.compile("(?s)\\{(.+?)\\}").matcher(content);
            Matcher fileMatcher = Pattern.compile("file\\s*?:\\s*?[\"'](.+?)[\"']").matcher(content);
            Matcher labelMatcher = Pattern.compile("label\\s*?:\\s*?[\"'](.+?)[\"']").matcher(content);
            if (!videoFilesMatcher.find()) {
                throw new PluginImplementationException("Video files not found");
            }
            videoFileMatcher.region(videoFilesMatcher.start(1), videoFilesMatcher.end(1));
            while (videoFileMatcher.find()) {
                fileMatcher.region(videoFileMatcher.start(1), videoFileMatcher.end(1));
                labelMatcher.region(videoFileMatcher.start(1), videoFileMatcher.end(1));
                if (fileMatcher.find() && labelMatcher.find()) {
                    String url = fileMatcher.group(1);
                    int quality = Integer.parseInt(labelMatcher.group(1).replace("p", ""));
                    Tune_pkVideo video = new Tune_pkVideo(quality, url);
                    videoList.add(video);
                    logger.info("Found: " + video);
                }
            }
        }
        if (videoList.isEmpty()) {
            throw new PluginImplementationException("No available video");
        }
        return Collections.min(videoList);
    }

    private class Tune_pkVideo implements Comparable<Tune_pkVideo> {
        private final static int LOWER_QUALITY_PENALTY = 10;
        private final int videoQuality;
        private final String url;
        private final int weight;

        public Tune_pkVideo(final int videoQuality, final String url) {
            this.videoQuality = videoQuality;
            this.url = url;
            this.weight = calcWeight();
        }

        private int calcWeight() {
            VideoQuality configQuality = config.getVideoQuality();
            int deltaQ = videoQuality - configQuality.getQuality();
            return (deltaQ < 0 ? Math.abs(deltaQ) + LOWER_QUALITY_PENALTY : deltaQ);
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(final Tune_pkVideo that) {
            return Integer.valueOf(this.weight).compareTo(that.weight);
        }

        @Override
        public String toString() {
            return "Tune_pkVideo{" +
                    "videoQuality=" + videoQuality +
                    ", url='" + url + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }

}
