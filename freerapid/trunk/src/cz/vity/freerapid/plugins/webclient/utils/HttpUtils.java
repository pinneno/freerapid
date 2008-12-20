package cz.vity.freerapid.plugins.webclient.utils;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Helpful utilities for parsing http headers
 *
 * @author Ladislav Vitasek
 */
final public class HttpUtils {
    private final static Logger logger = Logger.getLogger(HttpUtils.class.getName());

    /**
     * Do not instantiate HttpUtils.
     */
    private HttpUtils() {
    }


    /**
     * Extracts file name from response header Content-Disposition
     * Eg for <code>//Content-Disposition: =?UTF-8?attachment;filename="Two Peaks Personal Vehicle Manager 2005 3.2.zip";?=</code>
     * it returns <code>Two Peaks Personal Vehicle Manager 2005 3.2.zip</code>
     *
     * @param method executed HttpMethod with Content-Disposition header
     * @return null if there was now header Content-Disposition or parsed file name
     */
    public static String getFileName(final HttpMethod method) {

        final Header disposition = method.getResponseHeader("Content-Disposition");
        if (disposition != null && disposition.getValue().toLowerCase(Locale.ENGLISH).contains("attachment")) {
            final String value = disposition.getValue();
            String str = "filename=";
            final String lowercased = value.toLowerCase();
            int index = lowercased.lastIndexOf(str);
            if (index >= 0) {
                String s = value.substring(index + str.length());
                final int secondQuoteIndex = s.lastIndexOf('\"');
                if (s.startsWith("\"") && secondQuoteIndex > 0)
                    s = s.substring(1, secondQuoteIndex);
                // napr. pro xtraupload je jeste treba dekodovat
                if (s.matches(".*%[0-9A-Fa-f]+.*"))
                    try {
                        s = URLDecoder.decode(s, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        logger.warning("Unsupported encoding");
                    }
                return s;
            } else {
                //test na buggove Content-Disposition
                str = "filename\\*=UTF-8''";
                index = lowercased.lastIndexOf(str);
                if (index >= 0) {
                    final String s = value.substring(index + str.length());
                    if (!s.isEmpty())
                        try {
                            return URLDecoder.decode(s, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            logger.warning("Unsupported encoding");
                        }
                } else {
                    logger.warning("File name was not found in:" + value);
                }
            }
        }
        return null;
    }
}
