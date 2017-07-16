package cz.vity.freerapid.plugins.services.rockfile;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class RockFileFileRunner extends XFileSharingRunner {

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
            script += "t=\"rockfile.eu\";";
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

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkFileSize(httpFile, content, "iniFileSize = ", ";");
            }
        });
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Click to Download");
        return downloadPageMarkers;
    }

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                httpFile.setFileName(PlugUtils.suggestFilename(fileURL));
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("<a[^<>]+?href\\s*?=\\s*?[\"'](http.+?" + Pattern.quote(httpFile.getFileName()) + ")[\"']");
        downloadLinkRegexes.add(0, "<a[^<>]+?downloadStarted[^<>]+?href\\s*=\\s*[\"'](http[^\"']+?)[\"']");
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        String link = super.getDownloadLinkFromRegexes();
        httpFile.setFileName(1 + link.substring(link.lastIndexOf("/")));
        return link;
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("(?s)id=\"countdown_str\".*?<span[^<>]*id=\".*?\">.*?(\\d+).*?</span");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    @Override
    protected void checkDownloadProblems(final String content) throws ErrorDuringDownloadingException {
        super.checkDownloadProblems(content);
        if (content.contains("Maintenance in progress")) {
            throw new ServiceConnectionProblemException("Maintenance in progress");
        }
    }
}