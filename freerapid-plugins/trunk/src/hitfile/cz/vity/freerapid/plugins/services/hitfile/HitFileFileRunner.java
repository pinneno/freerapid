package cz.vity.freerapid.plugins.services.hitfile;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.turbobit.TurboBitFileRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class HitFileFileRunner extends TurboBitFileRunner {

    @Override
    protected void checkNameAndSize() throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("<title>\\s*?Download file (.+?) \\(([^\\(]+?)\\)\\s*?\\|\\s*?Hitfile", getContentAsString());
        if (!matcher.find()) {
            throw new PluginImplementationException("File name and size not found");
        }
        String filename = matcher.group(1);
        long filesize = PlugUtils.getFileSizeFromString(matcher.group(2));
        httpFile.setFileName(filename);
        httpFile.setFileSize(filesize);
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    protected void checkFileProblems() throws ErrorDuringDownloadingException {
        try {
            super.checkFileProblems();
        } catch (NotRecoverableDownloadException x) {
            if (!x.getMessage().contains("Limit reached for free download of this file"))
                throw x;
        }

    }
}