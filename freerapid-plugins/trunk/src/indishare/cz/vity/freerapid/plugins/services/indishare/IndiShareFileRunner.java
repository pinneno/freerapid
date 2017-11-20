package cz.vity.freerapid.plugins.services.indishare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class IndiShareFileRunner extends XFileSharingRunner {

    @Override

    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkName(httpFile, content, "<h1>", "</h1>");
            }
        });
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                final Matcher matcher = PlugUtils.matcher("copy\\(this\\);\">(?:<a[^<>]+>|\\[[^\\[\\]]+\\])(.+?) - \\d[^-<>\\[\\]]+", content);
                if (!matcher.find())
                    throw new PluginImplementationException("Name not found");
                httpFile.setFileName(matcher.group(1).trim());
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandler() {
            @Override
            public void checkFileSize(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                PlugUtils.checkFileSize(httpFile, content, "<b>Size:</b>", "</");
            }
        });
        return fileSizeHandlers;
    }

}