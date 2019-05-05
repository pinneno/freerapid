package cz.vity.freerapid.plugins.services.linkshrink;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class LinkShrinkFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinkShrinkFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            final HttpMethod getMethod = getMethodBuilder().setReferer(fileURL)
                    .setAction(decodeNextLink()).toGetMethod();
            addCookie(new Cookie("linkshrink.net", "s32", "1", "/", 86400, false));
            addCookie(new Cookie("linkshrink.net", "_gat", "1", "/", 86400, false));
            try {
                makeRedirectedRequest(getMethod);
            } catch (Exception x) {/**/}
            this.httpFile.setNewURL(new URL(getMethod.getURI().getURI()));
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Link does not exist") ||
                contentAsString.contains("LinkShrink is a free URL shortening service")) {//errors redirect to main page
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private String decodeNextLink() throws Exception {
        Matcher match = getMatcherAgainstContent("<script>(function.+?)</script><script>(.+?(?:location\\.href\\s*=\\s*|window\\.open\\()(.+?)[,;].+?)</script>");
        if (!match.find()) throw new PluginImplementationException("Script not found");
        try {
            Matcher subMatch1 = PlugUtils.matcher("location\\.href\\s*=\\s*(.+?)[,;]", match.group(2));
            Matcher subMatch2 = PlugUtils.matcher("window\\.open\\((.+?)[,;]", match.group(2));
            String evalFunct;
            if (subMatch1.find() && !subMatch1.group(1).contains("ads.linkshrink"))
                evalFunct = subMatch1.group(1);
            else if (subMatch2.find() && !subMatch2.group(1).contains("ads.linkshrink"))
                evalFunct = subMatch2.group(1);
            else
                throw new PluginImplementationException("Error finding javascript");
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            return engine.eval(match.group(1) + evalFunct).toString();
        } catch (Exception e) {
            throw new PluginImplementationException("JS evaluation error " + e.getLocalizedMessage());
        }
    }
}