package cz.vity.freerapid.plugins.services.nbcnews;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class NbcNewsFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NbcNewsFileRunner.class.getName());
    private static final String VideoType = ".mp4";
    private static final String SubtitleType = ".srt";
    private cz.vity.freerapid.plugins.services.nbcnews.SettingsConfig config;


    private void getConfig() throws Exception {
        NbcNewsServiceImpl service = (NbcNewsServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize();//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("\"og:title\" content=\"(?:Watch)?([^\"]+?)(?:on NBC\\.com)?\"");
        if (!matcher.find()) {
            throw new PluginImplementationException("Video title not found");
        }
        final String name = PlugUtils.unescapeHtml(PlugUtils.unescapeHtml(PlugUtils.unescapeUnicode(matcher.group(1).trim()))) + VideoType;
        httpFile.setFileName(name);
        logger.info("File name : " + name);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        getConfig();
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            checkNameAndSize();//extract file name and size from the page

            Matcher match = PlugUtils.matcher("videoAssets\":\\[([^\\]]+?)\\]", getContentAsString());
            if (!match.find())
                throw new PluginImplementationException("Video list not found");
            String videoDetails = match.group(1);
            match = PlugUtils.matcher(",\\{\"format\"[^\\}]+?publicUrl\":\"([^\"]+)\"[^\\}]+?bitRate\":([^\\}]+?),", videoDetails);
            HashMap<Integer, String> videos = new HashMap<Integer, String>();
            while (match.find()) {
                videos.put(Integer.parseInt(match.group(2).trim()), match.group(1).trim());
            }
            if (videos.size() == 0)
                throw new PluginImplementationException("No videos found");
            int lowBR=Integer.MAX_VALUE, highBR=0;
            for (int bitRate : videos.keySet()) {
                if (bitRate < lowBR) lowBR = bitRate;
                if (bitRate > highBR) highBR = bitRate;
            }
            String settingsLink;
            if (config.isHighVideoQuality())
                settingsLink = videos.get(highBR);
            else
                settingsLink = videos.get(lowBR);
            if (!makeRedirectedRequest(getGetMethod(settingsLink))) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String videoLink = PlugUtils.getStringBetween(getContentAsString(), "<video src=\"", "\" title");
            String subtitleLink = null;
            match = getMatcherAgainstContent("<textstream[^<>]+?src=\"(.+?" + SubtitleType + ")\"");
            if (match.find()) {
                subtitleLink = match.group(1).trim();
            }

            if (!config.isOnlySubtitles()) {
                if (!tryDownloadAndSaveFile(getGetMethod(videoLink))) {
                    checkProblems();//if downloading failed
                    throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
                }
            }
            if (config.isDownloadSubtitles()) {
                if (subtitleLink != null) {
                    SubtitleDownloader subtitleDownloader = new SubtitleDownloader();
                    try {
                        subtitleDownloader.downloadSubtitle(client, httpFile, subtitleLink, httpFile.getFileName().replace(VideoType, ""), SubtitleType);
                    } catch (Exception e) {
                        throw new ServiceConnectionProblemException("Error downloading subtitle");
                    }
                }
            }


        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("requested video was not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}