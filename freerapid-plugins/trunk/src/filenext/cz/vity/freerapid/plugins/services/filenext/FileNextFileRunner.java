package cz.vity.freerapid.plugins.services.filenext;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class FileNextFileRunner extends XFileSharingRunner {

    protected void checkDownloadProblems(final String content) throws ErrorDuringDownloadingException {
        super.checkDownloadProblems(content);
        if (content.contains("file is available for Premium Users only")) {
            throw new NotRecoverableDownloadException("This file is only available to premium users");
        }
    }
}