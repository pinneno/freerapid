package cz.vity.freerapid.plugins.services.mexashare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class MexaShareFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                Matcher match = PlugUtils.matcher("File Size(?:&nbsp;|\\s)*:(?:&nbsp;|\\s)*(.+?)(?:&nbsp;|\\s)*<", content);
                if (!match.find())
                    throw new PluginImplementationException("File size not found");
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1).trim()));
            }
        });
        return fileSizeHandlers;
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("<span class=\"seconds\">(\\d+)</span");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    @Override
    protected boolean stepCaptcha(final MethodBuilder methodBuilder) throws Exception {
        super.stepCaptcha(methodBuilder);
        return false;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Start Download");
        return downloadPageMarkers;
    }

    @Override
    protected void correctURL() throws Exception {
        skipDDoSProtection();
    }

    protected void skipDDoSProtection() throws Exception {
        HttpMethod method = getGetMethod(fileURL);
        makeRedirectedRequest(method);
        if (getContentAsString().contains("<title>Just a moment...</title>")) {
            Matcher match = getMatcherAgainstContent("var (?:\\w,)* ([^;]+;)");
            if (!match.find()) throw new PluginImplementationException("DDoS Protection bypass error 1");
            String script = match.group(1);
            script += "t=\"www.mexashare.com\";";
            match = getMatcherAgainstContent("  ;(.+?;) ");
            if (!match.find()) throw new PluginImplementationException("DDoS Protection bypass error 2");
            script += match.group(1).replace("a.value", "answer");

            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            String answer = "" + new Float(engine.eval(script).toString()).intValue();

            method = getMethodBuilder().setReferer(fileURL)
                    .setActionFromFormByName("challenge-form", true)
                    .setParameter("jschl_answer", answer)
                    .toGetMethod();
            downloadTask.sleep(5);
            if (!makeRedirectedRequest(method)) {
                checkFileProblems();
                throw new ServiceConnectionProblemException();
            }
        }
    }
}