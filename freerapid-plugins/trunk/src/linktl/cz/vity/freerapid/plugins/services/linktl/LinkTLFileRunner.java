package cz.vity.freerapid.plugins.services.linktl;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class LinkTLFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(LinkTLFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkProblems();//check problems
            Matcher match = getMatcherAgainstContent("skip_ad\\.click\\(function\\(\\) \\{\\s*\\$\\.post\\('(.+?)',\\s*(.+?})");
            if (!match.find())
                throw new PluginImplementationException("Skip link not found");
            String action = match.group(1).trim();
            String params = match.group(2).trim();
            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAjax()
                    .setAction(action)
                    .setParameter("opt", getParamValue("opt", params))
                    .setParameter("args[aid]", getParamValue("aid", params))
                    .setParameter("args[lid]", getParamValue("lid", params))
                    .setParameter("args[oid]", getParamValue("aid", params))
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            final Matcher m = getMatcherAgainstContent("[\"']url[\"']:[\"'](.+?)[\"']");
            if (!m.find()) throw new PluginImplementationException("Link not found");
            this.httpFile.setNewURL(new URL(m.group(1).replace("\\", "")));
            this.httpFile.setPluginID("");
            this.httpFile.setState(DownloadState.QUEUED);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getParamValue(String param, String content) throws PluginImplementationException {
        final Matcher match = PlugUtils.matcher("'?" + param + "'?:'?(.+?)'?[,}]", content);
        if (!match.find())
            throw new PluginImplementationException("Parameter '" + param + "' not found");
        return match.group(1).trim();
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found") || contentAsString.contains("\"url\":null")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}