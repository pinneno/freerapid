package cz.vity.freerapid.plugins.services.beeg;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class BeegFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(BeegFileRunner.class.getName());
    private SettingsConfig config;

    private void setConfig() throws Exception {
        BeegServiceImpl service = (BeegServiceImpl) getPluginService();
        config = service.getConfig();
    }

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(getInfoUrl());//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String infoUrl;
    private String salt;

    private String getInfoUrl() throws Exception {
        if (infoUrl == null) {
            Matcher match = PlugUtils.matcher("beeg\\.com/(\\d+)", fileURL);
            if (!match.find()) throw new PluginImplementationException("Video ID not found");
            String videoID = match.group(1);
            if (!makeRedirectedRequest(getGetMethod(fileURL))) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            match = PlugUtils.matcher("/static/cpl/(\\d+)\\.js", getContentAsString());
            if (!match.find()) throw new PluginImplementationException("Beeg Version not found");
            String beegVer = match.group(1);
            if (!makeRedirectedRequest(getMethodBuilder().setReferer(fileURL).setAction(match.group(0)).toGetMethod())) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            match = PlugUtils.matcher("/api/([^/]+/)", getContentAsString());
            if (!match.find()) throw new PluginImplementationException("API Version not found");
            String apiVer = match.group(1);
            infoUrl =  "http://beeg.com/api/" + apiVer + beegVer + "/video/" + videoID;

            match = PlugUtils.matcher("_salt=\"([^\"]+)\"", getContentAsString());
            if (!match.find()) throw new PluginImplementationException("API Version not found");
            salt = match.group(1);
        }
        return infoUrl;
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "\"title\":\"", "\"");
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(getInfoUrl()); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            setConfig();
            final String quality = config.toString();
            logger.info("Preferred Quality : " + quality);
            String fileUrl;
            final Matcher match = PlugUtils.matcher("\""+quality+"\":\"(.+?)\"", contentAsString);
            if (match.find()) {
                fileUrl = match.group(1);
                fileUrl = fileUrl.replaceFirst(".+?video.beeg.com", "http://video.beeg.com");
                fileUrl = fileUrl.replace("{DATA_MARKERS}", "data=pc_US");
                Matcher matcher = PlugUtils.matcher("key=(.+)%2Cend", fileUrl);
                if (!matcher.find()) throw new PluginImplementationException("Video Key not found");
                fileUrl = fileUrl.replace(matcher.group(1), decodeKey(matcher.group(1)));
            }
            else // default quality on page
                fileUrl = PlugUtils.getStringBetween(contentAsString, "'file': '", "',");

            //here is the download link extraction
            if (!tryDownloadAndSaveFile(getGetMethod(fileUrl))) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

    private String decodeKey(String key) throws Exception {
        String script = "jsalt = \""+ salt +"\";\n"+
                "String.prototype.str_split=function(e,t){var n=this;e=e||1,t=!!t;var r=[];if(t){var a=n.length%e;a>0&&(r.push(n.substring(0,a)),n=n.substring(a))}for(;n.length>e;)r.push(n.substring(0,e)),n=n.substring(e);return r.push(n),r};\n"+
                "  function jsaltDecode(e) {\n"+
                "    e = decodeURIComponent(e);\n"+
                "    for (var s = jsalt.length, t = \"\", o = 0; o < e.length; o++) {\n"+
                "      var l = e.charCodeAt(o)\n"+
                "        , n = o % s\n"+
                "        , i = jsalt.charCodeAt(n) % 21;\n"+
                "      t += String.fromCharCode(l - i)\n"+
                "    }\n"+
                "    return t.str_split(3, !0).reverse().join(\"\")\n"+
                "  }\n"+
                "OUTPUT = jsaltDecode(\""+ key +"\");";
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        engine.eval(script);
        return engine.get("OUTPUT").toString();
    }
}