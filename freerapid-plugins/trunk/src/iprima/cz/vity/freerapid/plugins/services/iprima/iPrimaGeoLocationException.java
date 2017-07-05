package cz.vity.freerapid.plugins.services.iprima;

import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;

/**
 * @author tong2shot
 */
class iPrimaGeoLocationException extends NotRecoverableDownloadException {
    public iPrimaGeoLocationException() {
        super("iPrimaGeoLocationException");
    }

    public iPrimaGeoLocationException(String message) {
        super(message);
    }

    public iPrimaGeoLocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public iPrimaGeoLocationException(Throwable cause) {
        super(cause);
    }
}
