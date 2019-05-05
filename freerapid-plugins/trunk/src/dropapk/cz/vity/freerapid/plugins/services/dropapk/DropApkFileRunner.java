package cz.vity.freerapid.plugins.services.dropapk;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class DropApkFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                Matcher match = PlugUtils.matcher("\\[<[^<>]+>(.+?)<[^<>]+>\\]", content);
                if (match.find())
                    httpFile.setFileSize(PlugUtils.getFileSizeFromString(match.group(1)));
                else throw new PluginImplementationException("File size not found");
            }
        });
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        return getXFSMethodBuilder(content, "method_free");
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a[^<>]+href\\s*=\\s*[\"'](http.+?" + Pattern.quote(httpFile.getFileName()) + ")[\"'][^<>]*>\\s*Click");
        return downloadLinkRegexes;
    }
}