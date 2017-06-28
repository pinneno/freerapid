package cz.vity.freerapid.plugins.services.exload;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ExLoadFileRunner extends XFileSharingRunner {
    private final static Logger logger = Logger.getLogger(ExLoadFileRunner.class.getName());

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new FileNameHandler() {
            @Override
            public void checkFileName(HttpFile httpFile, String content) throws ErrorDuringDownloadingException {
                final Matcher match = PlugUtils.matcher("<h1.+?>([^<>]+?)</h1>", content);
                if (!match.find())
                    throw new PluginImplementationException("File name not found");
                httpFile.setFileName(match.group(1).trim());
            }
        });
        return fileNameHandlers;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder(final String content) throws Exception {
        return getXFSMethodBuilder(content, "download2");
    }

    @Override
    protected int getWaitTime() throws Exception {
        try {
            Matcher matcher = getMatcherAgainstContent("parseInt\\(\\s*$$\\('([^\\(\\)]+)'\\)\\.inner");
            if (matcher.find()) {
                String waitTimeVar = matcher.group(1);
                Matcher waitTimeMatcher = getMatcherAgainstContent("<span [^<>]*?id=\"" + waitTimeVar + "\"[^<>]*?>(\\d+)</");
                if (waitTimeMatcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }
            }
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }
        return 60;
    }
}