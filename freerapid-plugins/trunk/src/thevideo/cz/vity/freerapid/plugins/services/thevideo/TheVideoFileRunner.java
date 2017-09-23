package cz.vity.freerapid.plugins.services.thevideo;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
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
class TheVideoFileRunner extends XFilePlayerRunner {

    @Override
    protected void correctURL() throws Exception {
        fileURL = fileURL.replaceFirst("(www\\.)?tvad\\.me/", "thevideo.me/");
        fileURL = fileURL.replaceFirst("(www\\.)?thevideo\\.me/", "thevideo.me/");
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        return super.getXFSMethodBuilder().setAction(fileURL)
                .setParameter("_vhash", "i1102394cE")
                .setParameter("gfk", "i22abd2449");
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("var _playerconfig");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        TheVideoServiceImpl service = (TheVideoServiceImpl) getPluginService();
        SettingsConfig settings = service.getSettings();
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, ", ['\"]?file['\"]?\\s*?:\\s*?['\"](http[^'\"]+?)['\"]");
        downloadLinkRegexes.add(0, "['\"]?file['\"]?\\s*:\\s*['\"]?(http[^'\",]+)['\"]?\\s*,\\s*['\"]?label['\"]?\\s*:\\s*['\"]?" + settings);
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        String link = super.getDownloadLinkFromRegexes();
        return link + getPlayerSignature();
    }

    private String getPlayerSignature() throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("/vsign/player.+?\\+(.+?)\\+");
        if (!matcher.find()) throw new PluginImplementationException("script key not found 1");
        String key = matcher.group(1).trim();
        matcher = getMatcherAgainstContent(key + "\\s*=\\s*[\"'](.+?)[\"']");
        if (!matcher.find()) throw new PluginImplementationException("script key not found 2");
        key = matcher.group(1).trim();
        HttpMethod method = getMethodBuilder().setReferer(fileURL)
                .setAction("/vsign/player/" + key).toGetMethod();
        try {
            if (!makeRedirectedRequest(method))
                throw new ServiceConnectionProblemException();
        } catch (Exception x) {
            throw new ErrorDuringDownloadingException(x.getMessage());
        }
        final ScriptEngineManager mgr = new ScriptEngineManager();
        final ScriptEngine engine = mgr.getEngineByName("JavaScript");
        if (engine == null) {
            throw new RuntimeException("JavaScript engine not found");
        }
        String out;
        try {
            out = (String) engine.eval(getContentAsString().substring(4));
        } catch (Exception x) {
            throw new ErrorDuringDownloadingException(x.getMessage());
        }
        String a1 = PlugUtils.getStringBetween(out, ")+\"", "\"");
        String a2 = PlugUtils.getStringBetween(out, "var b=\"", "\"");
        String a3 = PlugUtils.getStringBetween(out, "c=\"", "\"");
        return "?" + a1 + a2 + "&" + a3;
    }

}