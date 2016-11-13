package cz.vity.freerapid.plugins.services.zeetv;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.applehls.AdjustableBitrateHlsDownloader;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class ZeeTvFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(ZeeTvFileRunner.class.getName());

    private SettingsConfig config;

    private void setConfig() throws Exception {
        ZeeTvServiceImpl service = (ZeeTvServiceImpl) getPluginService();
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
        httpFile.setFileName(httpFile.getFileName() + ".ts");
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

            Matcher matcher = getMatcherAgainstContent("(var hlsplayurl\\s*?=\\s*?'.+?';)");
            if (!matcher.find()) {
                throw new PluginImplementationException("Cipher playlist URL not found");
            }
            String hlsplayurl = matcher.group(1);

            matcher = getMatcherAgainstContent("var dailytoday\\s*?=\\s*?[\"']([^\"']+)[\"']");
            String dailytoday = null;
            while (matcher.find()) {
                dailytoday = matcher.group(1);
            }
            if (dailytoday == null) {
                throw new PluginImplementationException("Cipher password not found");
            }

            ScriptEngine engine = initScriptEngine();
            engine.eval(hlsplayurl);
            engine.put("dailytoday", dailytoday);
            engine.eval("var videourl = JSON.parse(CryptoJS.AES.decrypt(hlsplayurl, dailytoday, {format: CryptoJSAesJson}).toString(CryptoJS.enc.Utf8))");
            String playlistUrl = (String) engine.get("videourl");

            setConfig();
            logger.info("Config settings : " + config);
            AdjustableBitrateHlsDownloader downloader = new AdjustableBitrateHlsDownloader(client, httpFile, downloadTask, config.getVideoQuality().getBitrate());
            downloader.tryDownloadAndSaveFile(playlistUrl);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("This page is currently unavailable")
                || contentAsString.contains("An Internal Server Error Occurred")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private static ScriptEngine initScriptEngine() throws Exception {
        final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        if (engine == null) {
            throw new RuntimeException("JavaScript engine not found");
        }
        final Reader reader = new InputStreamReader(ZeeTvFileRunner.class.getResourceAsStream("/resources/crypto.js"), "UTF-8");
        try {
            engine.eval(reader);
        } finally {
            reader.close();
        }
        return engine;
    }
}
