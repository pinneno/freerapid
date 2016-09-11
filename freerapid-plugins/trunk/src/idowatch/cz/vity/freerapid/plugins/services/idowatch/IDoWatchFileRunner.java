package cz.vity.freerapid.plugins.services.idowatch;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class IDoWatchFileRunner extends XFilePlayerRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkName(httpFile, content, "<title>Watch ", "</title>");
                httpFile.setFileName(httpFile.getFileName().replace(" ", "."));
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected boolean stepProcessFolder() throws Exception {
        if (checkDownloadPageMarker())    {
            //page containing download link
            final String downloadLink = getDownloadLinkFromRegexes();
            HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setAction(downloadLink)
                    .toGetMethod();
            doDownload(method);
            return true;
        }
        return false;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "['\"]?file['\"]?\\s*?:\\s*?['\"](http[^'\"]+?[^lt])['\"]");
        return downloadLinkRegexes;
    }
}