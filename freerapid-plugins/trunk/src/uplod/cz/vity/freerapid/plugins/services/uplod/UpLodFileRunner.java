package cz.vity.freerapid.plugins.services.uplod;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class UpLodFileRunner extends XFileSharingRunner {

    @Override
    protected void correctURL() throws Exception {
        fileURL = fileURL.replaceFirst("uplod\\.it/", "uploads.to/");
    }

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkName(httpFile, content, "&q=", "\"");
                httpFile.setFileName(httpFile.getFileName().replaceAll("\\s+", "."));
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("CLICK HERE TO DOWNLOAD");
        return downloadPageMarkers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("<a[^>]*href\\s*=\\s*[\"'](.+?)[\"'][^>]*>CLICK HERE TO DOWNLOAD");
        return downloadLinkRegexes;
    }

    @Override
    protected void doDownload(final HttpMethod method) throws Exception {
        final Matcher match = PlugUtils.matcher("<a[^>]*href\\s*=\\s*[\"'].+?/([^/]+?)[\"'][^>]*>CLICK HERE TO DOWNLOAD", getContentAsString());
        if (match.find())
            httpFile.setFileName(match.group(1));
        super.doDownload(method);
    }
}