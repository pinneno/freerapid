package cz.vity.freerapid.plugins.services.zstream;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class ZStreamFileRunner extends XFilePlayerRunner {

    @Override
    protected void checkFileProblems(final String content) throws ErrorDuringDownloadingException {
        super.checkFileProblems(content);
        if (content.contains("<h3>The file was deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}