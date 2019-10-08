package cz.vity.freerapid.plugins.services.clicknupload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ClicknUploadFileRunner extends XFileSharingRunner {

    @Override
    protected void correctURL() throws Exception {
        fileURL = fileURL.replaceFirst("clicknupload\\.com", "clicknupload.me");
        fileURL = fileURL.replaceFirst("clicknupload\\.me", "clicknupload.link");
        fileURL = fileURL.replaceFirst("clicknupload\\.link", "clicknupload.org");
        fileURL = fileURL.replaceFirst("http://", "https://");
        skipDDoSProtection();
    }

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                final Matcher match = PlugUtils.matcher("(?:\\[URL=|href=\").+?(?:\\]|\">)(.+?) - (.+?)[\\[<]", content);
                if (!match.find())
                    throw new PluginImplementationException("File name not found");
                httpFile.setFileName(PlugUtils.unescapeHtml(match.group(1)).trim());
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("window.open\\([\"'](http.+?" + Pattern.quote(httpFile.getFileName()) + ")[\"']\\)");
        downloadLinkRegexes.add("window.open\\([\"'](http.+?)[\"']\\)");
        return downloadLinkRegexes;
    }

    private void skipDDoSProtection() throws Exception{HttpMethod method = getGetMethod(fileURL);
        makeRedirectedRequest(method);
        int loopCount = 0;
        while (getContentAsString().contains("<title>Just a moment...</title>")) {
            if (loopCount++ > 5) {
                throw new PluginImplementationException("Unable to pass DDoS protection");
            }
            Matcher match = getMatcherAgainstContent("var (?:\\w,)* ([^;]+;)");
            if (!match.find()) throw new PluginImplementationException("DDoS Protection bypass error 1");
            String script = match.group(1);
            script += "t=\"" + (new URL(fileURL)).getAuthority() + "\";";
            match = getMatcherAgainstContent("  ;(.+?;) ");
            if (!match.find()) throw new PluginImplementationException("DDoS Protection bypass error 2");
            script += match.group(1).replace("a.value", "answer");

            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            String answer = "" + engine.eval(script).toString();
            match = getMatcherAgainstContent("toFixed\\((\\d+)\\)");
            if (!match.find()) throw new PluginImplementationException("DDoS Protection bypass error 3");
            int precision = Integer.parseInt(match.group(1).trim());
            answer = answer.substring(0, answer.lastIndexOf(".")+ 1 + precision);
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