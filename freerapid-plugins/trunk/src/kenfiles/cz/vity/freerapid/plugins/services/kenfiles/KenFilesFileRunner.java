package cz.vity.freerapid.plugins.services.kenfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class KenFilesFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.remove(0);
        return fileNameHandlers;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        return getXFSMethodBuilder(content, "method_free");
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add(0, "<a[^<>]*href\\s*=\\s*[\"'].+(https?:[^<>\"':]+?:[^<>\"':]+?" + Pattern.quote(httpFile.getFileName()) + ")[\"'][^<>]*>[^<>]*" + Pattern.quote(httpFile.getFileName()));
        return downloadLinkRegexes;
    }
}