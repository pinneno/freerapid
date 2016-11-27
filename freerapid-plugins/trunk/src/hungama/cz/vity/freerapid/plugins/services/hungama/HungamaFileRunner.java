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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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
            final GetMethod getMethod = getGetMethod(fileURL);
            if (makeRedirectedRequest(getMethod)) {
                checkProblems();
                checkNameAndSize(getContentAsString());
            } else {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        String album = null;
        Matcher matcher = PlugUtils.matcher("data\\.albumName\\s*?=\\s*?\"'?([^\"]+)'?\"", content);
        if (matcher.find()) {
            album = matcher.group(1).trim().replaceFirst("^'", "").replaceFirst("'$", "");
        }
        String song;
        matcher = PlugUtils.matcher("data\\.songName\\s*?=\\s*?\"'?([^\"]+)'?\"", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("Song name not found");
        }
        song = matcher.group(1).trim().replaceFirst("^'", "").replaceFirst("'$", "");
        String filename = (album != null ? album + " - " + song : song) + ".mp3";
        httpFile.setFileName(filename);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        checkUrl();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            if (!isAlbum(fileURL)) {
                final String contentAsString = getContentAsString();
                checkProblems();
                checkNameAndSize(contentAsString);

                Matcher matcher = PlugUtils.matcher("data\\.trackId\\s*?=\\s*?\"([^\"]+)\"", getContentAsString());
                if (!matcher.find()) {
                    throw new PluginImplementationException("Track id not found");
                }
                String trackId = matcher.group(1).trim();

                String param;
                try {
                    ScriptEngine engine = initScriptEngine();
                    engine.put("trackId", trackId);
                    engine.eval("var data = c2sencrypt(trackId, 'HUNG123#');"); //http://www.hungama.com/themes/hungamaTheme/js/autoplay.js
                    param = (String) engine.get("data");
                } catch (Exception e) {
                    throw new PluginImplementationException("Unable to decrypt song path parameter");
                }

                HttpMethod httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction("http://www.hungama.com/mediaPlayer/getsongpath")
                        .setParameter("param", param)
                        .setAjax()
                        .toGetMethod();
                if (!makeRedirectedRequest(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();

                String downloadUrl = getContentAsString();
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(downloadUrl)
                        .toGetMethod();
                setClientParameter(DownloadClientConsts.DONT_USE_HEADER_FILENAME, true);
                if (!tryDownloadAndSaveFile(httpMethod)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else {
                processAlbum();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
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

    private boolean isAlbum(String fileUrl) {
        return fileUrl.contains("/music/album-");
    }

    private ScriptEngine initScriptEngine() throws Exception {
        final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        if (engine == null) {
            throw new RuntimeException("JavaScript engine not found");
        }
        final Reader reader = new InputStreamReader(HungamaFileRunner.class.getResourceAsStream("/resources/crypto.js"), "UTF-8");
        try {
            engine.eval(reader);
        } finally {
            reader.close();
        }
        return engine;
    }

    private void processAlbum() throws Exception {
        setTextContentTypes("application/json");
        final HttpMethod httpMethod = getMethodBuilder()
                .setReferer("http://www.hungama.com/")
                .setAction(fileURL.replaceFirst("/#/music/", "/music/"))
                .setAjax()
                .toGetMethod();
        if (!makeRedirectedRequest(httpMethod)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }

        ObjectMapper objectMapper = new JsonMapper().getObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(getContentAsString());
        } catch (IOException e) {
            throw new PluginImplementationException("Error parsing JSON (1)");
        }
        JsonNode contentNode = rootNode.get("#content");
        if (contentNode == null) {
            throw new PluginImplementationException("Error parsing JSON (2)");
        }

        String jsonContent = contentNode.getTextValue();
        Matcher matcher = PlugUtils.matcher("(?s)\"track\":\\s*?(\\[\\{.+?\\}\\])", jsonContent);
        if (!matcher.find()) {
            throw new PluginImplementationException("Tracks data not found");
        }
        String tracksData = matcher.group(1).trim();

        List<URI> uriList = new LinkedList<URI>();
        try {
            rootNode = objectMapper.readTree(tracksData);
        } catch (IOException e) {
            throw new PluginImplementationException("Error parsing tracks data");
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