package cz.vity.freerapid.plugins.services.applehls;

/**
 * @author Christopher A Longo (https://github.com/chrislongo)
 */
public class CryptoException extends RuntimeException {
    public CryptoException() {
        super();
    }

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoException(Throwable cause) {
        super(cause);
    }
}
