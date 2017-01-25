package cz.vity.freerapid.plugins.services.uploading_site;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandlerNoSize;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Uploading_siteFileRunner extends XFileSharingRunner {

    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                //file name not displayed
            }
        });
        return fileNameHandlers;
    }

    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(new FileSizeHandlerNoSize());
        return fileSizeHandlers;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("<a[^<>]*href\\s*=\\s*[\"'](http[^<>\"']+?)[\"'][^<>]*>\\s*(?:Click here to download|Direct Download)");
        return downloadLinkRegexes;
    }

    @Override
    protected String getDownloadLinkFromRegexes() throws ErrorDuringDownloadingException {
        httpFile.setFileName("File_Name.temp");
        final String file = super.getDownloadLinkFromRegexes();
        httpFile.setFileName(file.substring(1 + file.lastIndexOf("/")));
        return file;
    }

    @Override
    protected void checkFileProblems(final String content) throws ErrorDuringDownloadingException {
        super.checkFileProblems(content);
        if (content.contains("No such file")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    protected void checkDownloadProblems(final String content) throws ErrorDuringDownloadingException {
        super.checkDownloadProblems(content);
        if (content.contains("No such file")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }


}