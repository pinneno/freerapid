package cz.vity.freerapid.plugins.services.ausfile;

import cz.vity.freerapid.plugins.exceptions.BadLoginException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class AusFileFileRunner extends XFileSharingRunner {

    @Override
    protected void correctURL() throws Exception {
        fileURL = fileURL.replaceFirst("https://", "http://");
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkFileSize(httpFile, content, " :", "</h2>");
            }
        });
        return fileSizeHandlers;
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("Wait(?:<br/>)?\\s*<span[^<>]+?class=\"count\">(.+?)</span>");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("final_download.png");
        return downloadPageMarkers;
    }

    @Override
    protected void checkDownloadProblems(final String content) throws ErrorDuringDownloadingException {
        super.checkDownloadProblems(content);
        if (content.contains("file is available for Premium Users only")) {
            throw new NotRecoverableDownloadException("This file is only available to premium users");
        }
    }

    @Override
    protected void doLogin(final PremiumAccount pa) throws Exception {
        final String baseUrl = (new URL(fileURL)).getProtocol() + "://" + (new URL(fileURL)).getAuthority();
        HttpMethod method = getMethodBuilder()
                .setReferer(baseUrl)
                .setAction(baseUrl + "/login.html")
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        method = getMethodBuilder()
                .setReferer(baseUrl + "/login.html")
                .setAction(baseUrl)
                .setActionFromFormByName("FL", true)
                .setParameter("login", pa.getUsername())
                .setParameter("password", pa.getPassword())
                .setAction(baseUrl)
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        if (getContentAsString().contains("Incorrect Login or Password")) {
            throw new BadLoginException("Invalid account login information");
        }
    }
}