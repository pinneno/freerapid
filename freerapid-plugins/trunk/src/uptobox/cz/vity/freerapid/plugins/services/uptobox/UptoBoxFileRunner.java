package cz.vity.freerapid.plugins.services.uptobox;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 * <p>
 * Only https fileURL is supported for downloading from cache, otherwise it will not be resumed.
 *
 * @author tong2shot
 * @author birchie
 */
class UptoBoxFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                Matcher matcher = PlugUtils.matcher(">(.+?)\\(([\\s\\d\\.,]+?(?:bytes|.B|.b))\\s*\\)", content);
                if (!matcher.find()) {
                    throw new PluginImplementationException("File name not found");
                }
                httpFile.setFileName(matcher.group(1).trim());
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected void checkFileName() throws ErrorDuringDownloadingException {
        super.checkFileName();
        httpFile.setFileName(PlugUtils.unescapeHtml(httpFile.getFileName()));
    }

    @Override
    protected void correctURL() throws Exception {
        fileURL = fileURL.replaceFirst("http://", "https://");
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        return getXFSMethodBuilder(content, "create-download-link");
    }

    @Override
    protected int getWaitTime() throws Exception {
        Matcher matcher = getMatcherAgainstContent("[Ww]ait.*?<.+?\">.*?(\\d+).*?</span");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        matcher = getMatcherAgainstContent("data-remaining-time=[\"'](\\d+)[\"']");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("the file you want is not available")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("website is currently undergoing maintenance")) {
            throw new ServiceConnectionProblemException("Website is undergoing maintenance");
        }
        super.checkFileProblems();
    }

    @Override
    protected void checkDownloadProblems(final String content) throws ErrorDuringDownloadingException {
        if (content.contains("Vous ne pouvez pas t&eacute;l&eacute;charger des fichiers de taille sup&eacute;rieur &agrave")) {
            throw new NotRecoverableDownloadException(PlugUtils.getStringBetween(content, " class=\"err\">", "<br").replace("Vous ne pouvez pas t&eacute;l&eacute;charger des fichiers de taille sup&eacute;rieur &agrave", "You can not download file sizes greater than"));
        }
        if (content.contains("to launch a new download") || content.contains("you can wait")) {
            final Matcher matcher = getMatcherAgainstContent("(?:(\\d+) hours?,? )?(?:(\\d+) minutes?,? )?(?:(\\d+) seconds?)");
            int waitHours = 0, waitMinutes = 0, waitSeconds = 0;
            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    waitHours = Integer.parseInt(matcher.group(1));
                }
                if (matcher.group(2) != null) {
                    waitMinutes = Integer.parseInt(matcher.group(2));
                }
                waitSeconds = Integer.parseInt(matcher.group(3));
            }
            final int waitTime = (waitHours * 60 * 60) + (waitMinutes * 60) + waitSeconds;
            throw new YouHaveToWaitException("You have to wait " + matcher.group(), waitTime);
        }
        super.checkDownloadProblems(content);
    }

    @Override
    protected boolean stepProcessFolder() throws Exception {
        if (checkDownloadPageMarker()) {
            final String downloadLink = getDownloadLinkFromRegexes();
            HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadLink)
                    .toGetMethod();
            saveDlLinkToCacheAndDownload(method);
            return true;
        }
        return false;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("Click here to start your download");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "product_download_url\\s*?=\\s*?(http.+?" + Pattern.quote(httpFile.getFileName()) + ")[\"']");
        downloadLinkRegexes.add(0, "<a[^<>]*?href\\s*=\\s*[\"'](?:http.+?)?(http.+?" + Pattern.quote(httpFile.getFileName()) + ")[\"']");
        downloadLinkRegexes.add("<a[^<>]*?href\\s*=\\s*[\"'](http[^\"']+)[\"'][^<>]*>\\s*Click here");
        return downloadLinkRegexes;
    }

    @Override
    protected boolean tryDownloadAndSaveFile(HttpMethod method) throws Exception {
        String downloadUrl = method.getURI().getEscapedURI();
        if (fileURL.startsWith("https")) {
            downloadUrl = downloadUrl.replaceFirst("http://", "https://");
        }
        return super.tryDownloadAndSaveFile(getMethodBuilder().setReferer(fileURL).setAction(downloadUrl).toGetMethod());
    }

    private String loginPage = getBaseURL() + "/?op=login&referer=homepage";
    @Override
    protected void doLogin(final PremiumAccount pa) throws Exception {
        HttpMethod method = getMethodBuilder()
                .setReferer(getBaseURL())
                .setAction(loginPage)
                .toGetMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        method = getMethodBuilder()
                .setReferer(loginPage)
                .setActionFromFormByName("login-form", true)
                .setAction(loginPage)
                .setParameter("login", pa.getUsername())
                .setParameter("password", pa.getPassword())
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        if (getContentAsString().contains("Incorrect Login or Password")) {
            throw new BadLoginException("Invalid account login information");
        }
    }

    @Override
    protected long getLongTimeAvailableLinkFromRegexes() {
        return 10 * 60 * 60 * 1000; //assume it's 10 hours, they don't provide dl expiration info.
    }
}