package cz.vity.freerapid.plugins.services.streamin;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.rtmp.AbstractRtmpRunner;
import cz.vity.freerapid.plugins.services.rtmp.RtmpSession;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 * @author tong2shot
 */
class StreaminFileRunner extends AbstractRtmpRunner {
    private final static Logger logger = Logger.getLogger(StreaminFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        final Matcher match = PlugUtils.matcher("<Title>Watch(.+?)</Title>", content);
        if (!match.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(match.group(1).trim().replaceAll("\\s", "."));
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL)
                    .setActionFromFormWhereTagContains("download", true)
                    .setAction(fileURL).toPostMethod();
            Matcher matcher = PlugUtils.matcher("Wait\\s*?<.+?>(\\d+?)<", getContentAsString());
            if (matcher.find())
                downloadTask.sleep(1 + Integer.parseInt(matcher.group(1)));
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            if (getContentAsString().contains("streamer: \"")) {
                final String streamer = PlugUtils.getStringBetween(getContentAsString(), "streamer: \"", "\"");
                final String file = PlugUtils.getStringBetween(getContentAsString(), "file: \"", "\"");
                final String playName = ((file.contains(".mp4") && !file.startsWith("mp4:")) ? "mp4:" + file : file);
                final RtmpSession rtmpSession = new RtmpSession(streamer, playName);
                tryDownloadAndSaveFile(rtmpSession);
            } else {
                contentAsString = getContentAsString();
                if (contentAsString.contains("eval(function(p,a,c,k,e,d)")) {
                    contentAsString = unPackJavaScript();
                }
                matcher = PlugUtils.matcher("file\\s*?:\\s*?\"([^\"]+)\"", contentAsString);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Video URL not found");
                }
                final String videoURL = matcher.group(1);
                final String filename = PlugUtils.suggestFilename(videoURL);
                if (filename.lastIndexOf(".") != -1) {
                    final String fileExt = filename.substring(filename.lastIndexOf("."));
                    if (!fileExt.isEmpty()) {
                        httpFile.setFileName(httpFile.getFileName().replaceFirst("\\..{2,4}$", fileExt));
                    }
                }
                httpMethod = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(videoURL)
                        .toGetMethod();
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
        final String content = getContentAsString();
        if (content.contains("File Not Found") || content.contains("file was deleted") ||
                content.contains(">File Removed") || content.contains(">File Deleted")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private String unPackJavaScript() throws ErrorDuringDownloadingException {
        final Matcher jsMatcher = getMatcherAgainstContent("<script type='text/javascript'>\\s*?(" + Pattern.quote("eval(function(p,a,c,k,e,d)") + ".+?)\\s*?</script>");
        String jsString = null;
        while (jsMatcher.find()) {
            jsString = jsMatcher.group(1).replaceFirst(Pattern.quote("eval(function(p,a,c,k,e,d)"), "function test(p,a,c,k,e,d)")
                    .replaceFirst(Pattern.quote("return p}"), "return p};test").replaceFirst(Pattern.quote(".split('|')))"), ".split('|'));");
            if (jsString.contains("jwplayer"))
                break;
        }
        if (jsString == null) {
            throw new PluginImplementationException("Javascript not found");
        }
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            return (String) engine.eval(jsString);
        } catch (ScriptException e) {
            throw new PluginImplementationException("JavaScript eval failed");
        }
    }
}