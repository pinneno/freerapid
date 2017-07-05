package cz.vity.freerapid.plugins.services.iprima;

import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;

/**
 * @author tong2shot
 */
class iPrimaAccountRequiredException extends NotRecoverableDownloadException {
    public iPrimaAccountRequiredException() {
        super("iPrimaAccountRequiredException");
    }

    public iPrimaAccountRequiredException(String message) {
        super(message);
    }

    public iPrimaAccountRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public iPrimaAccountRequiredException(Throwable cause) {
        super(cause);
    }
}
