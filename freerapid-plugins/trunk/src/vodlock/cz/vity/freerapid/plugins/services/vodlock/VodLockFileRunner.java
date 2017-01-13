package cz.vity.freerapid.plugins.services.vodlock;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class VodLockFileRunner extends XFilePlayerRunner {

    @Override
    protected void checkFileProblems(final String content) throws ErrorDuringDownloadingException {
        super.checkFileProblems();
        if (content.contains("The file was deleted")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }
}