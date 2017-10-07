package cz.vity.freerapid.plugins.services.rarefile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileSizeHandler;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class RareFileFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new RareFileFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected List<FileSizeHandler> getFileSizeHandlers() {
        final List<FileSizeHandler> fileSizeHandlers = super.getFileSizeHandlers();
        fileSizeHandlers.add(0, new RareFileFileSizeHandler());
        return fileSizeHandlers;
    }

    protected List<String> getFalseProblemRegexes() {
        final List<String> falseProblemRegexes = super.getFalseProblemRegexes();
        falseProblemRegexes.add("(?s)<div style=\"display:none\">.+?</div>");
        return falseProblemRegexes;
    }

    @Override
    protected void checkFileProblems(final String content) throws ErrorDuringDownloadingException {
        super.checkFileProblems(content);
        if (content.contains("server has crashed but we are still working for this server")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}