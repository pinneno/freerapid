package cz.vity.freerapid.plugins.services.nbc;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.adobehds.AdjustableBitrateHdsDownloader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class NbcFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NbcFileRunner.class.getName());
    private static final String VideoType = ".flv";
    private static final String SubtitleType = ".tt";
    private SettingsConfig config;


    private void setConfig() throws Exception {
        NbcServiceImpl service = (NbcServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
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

        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            MethodBuilder mb;
            try {
                mb = getMethodBuilder().setActionFromIFrameSrcWhereTagContains("player");
            } catch (BuildMethodException e) {
                throw new PluginImplementationException("Player URL not found");
            }
            String playerUrl = mb.getAction().replaceFirst("^//", "http://");
            method = getMethodBuilder().setReferer(fileURL).setAction(playerUrl).toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String releaseUrl;
            try {
                releaseUrl = PlugUtils.replaceEntities(PlugUtils.getStringBetween(getContentAsString(), "releaseUrl=\"", "\""));
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Release URL not found");
            }
            method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(releaseUrl)
                    .setParameter("Embedded", "true")
                    .setParameter("Tracking", "true")
                    .setParameter("format", "SMIL")
                    .setAndEncodeParameter("formats", "MPEG4,F4M,FLV,MP3")
                    .setParameter("manifest", "f4m")
                    .toGetMethod();
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();

            String manifestUrl;
            try {
                manifestUrl = PlugUtils.getStringBetween(getContentAsString(), "<video src=\"", "\"");
            } catch (PluginImplementationException e) {
                throw new PluginImplementationException("Manifest URL not found");
            }
            manifestUrl += (!manifestUrl.contains("?") ? "?" : "&") + "g=FGBNBTGJLYGU&hdcore=3.3.0";
            String subtitleLink = null;
            Matcher match = getMatcherAgainstContent("<textstream[^<>]+?src=\"(.+?" + SubtitleType + ")\"");
            if (match.find()) {
                subtitleLink = match.group(1).trim();
            }
            setConfig();
            logger.info("Settings config: " + config);
            if (!config.isOnlySubtitles()) {
                AdjustableBitrateHdsDownloader downloader = new AdjustableBitrateHdsDownloader(client, httpFile, downloadTask, config.getVideoQuality().getBitrate());
                downloader.tryDownloadAndSaveFile(manifestUrl);
            }
            if (config.isDownloadSubtitles()) {
                if (subtitleLink != null) {
                    httpFile.setFileName(httpFile.getFileName().replace(VideoType, SubtitleType));
                    setFileStreamContentTypes("text/plain");
                    if (!tryDownloadAndSaveFile(getGetMethod(subtitleLink))) {
                        checkProblems();//if downloading failed
                        throw new ServiceConnectionProblemException("Error starting subtitle download");//some unknown problem
                    }
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("Page not found") || getContentAsString().contains(" - All Videos ")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (getContentAsString().contains("This content is not available in your location")) {
            throw new NotRecoverableDownloadException("This content is not available in your location");
        }
    }

}