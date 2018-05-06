package cz.vity.freerapid.plugins.services.xhamster;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadClientConsts;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class xHamsterFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(xHamsterFileRunner.class.getName());
    private SettingsConfig config;

    private final static String QualityMatcher = "\"%s\":\\[\"([^\"]+)\"";
    private String PreferredQuality;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        loadConfig();
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
        Matcher matcher = PlugUtils.matcher("<h1[^<>]*>(.+?)</h1>", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName(matcher.group(1).replace("- xHamster", "").trim() + ".mp4");
        PlugUtils.checkFileSize(httpFile, content, getPreferredQuality() + " quality (", ")");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        loadConfig();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
            Matcher match = PlugUtils.matcher(String.format(QualityMatcher, getPreferredQuality()), getContentAsString().replace("\\", ""));
            if (!match.find())
                throw new PluginImplementationException(getPreferredQuality() + " Video not found");
            final String file = match.group(1).replace("\\", "");
            String videoURL;
            if (file.startsWith("http")) {
                videoURL = URLDecoder.decode(file, "UTF-8");
            } else {
                final String srv = PlugUtils.getStringBetween(getContentAsString(), "&srv=", "&");
                videoURL = URLDecoder.decode(srv + "/key=" + file, "UTF-8");
            }
            videoURL = videoURL.replaceAll("\\s", "+");
            final HttpMethod httpMethod = getMethodBuilder()
                    .setReferer(fileURL.replace("88.208.24.43", "xhamster.com"))
                    .setAction(videoURL)
                    .toGetMethod();
            setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
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
        if (contentAsString.contains("not found on this server") || contentAsString.contains("The requested content is no longer available") ||
                contentAsString.contains("This video was deleted") || contentAsString.contains("Video Was Deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (contentAsString.contains("<title>Restricted access to video")) {
            throw new NotRecoverableDownloadException("Access to video is restricted");
        }
    }

    private void checkURL() {
        fileURL = fileURL.replaceFirst("https", "http");
        fileURL = fileURL.replaceFirst("//\\w+\\.xhamster", "//xhamster");
        fileURL = fileURL.replaceFirst("//88\\.208\\.24\\.43", "//xhamster.com");
    }

    private void loadConfig() throws Exception {
        xHamsterServiceImpl service = (xHamsterServiceImpl) getPluginService();
        config = service.getConfig();
    }

    private String getPreferredQuality() throws PluginImplementationException {
        if (PreferredQuality == null) {
            for (VideoQuality pref : config.getVideoQuality()) {
            Matcher match = PlugUtils.matcher(String.format(QualityMatcher, pref.toString()), getContentAsString().replace("\\", ""));
            if (match.find()) {
                PreferredQuality = pref.toString();
                return PreferredQuality;
            }
        }
            throw new PluginImplementationException("No videos found");
        }
        return PreferredQuality;
    }

}
