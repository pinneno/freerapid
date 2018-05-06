package cz.vity.freerapid.plugins.services.adf;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class AdfFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AdfFileRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            String fileUrlDomain = (new URL(fileURL)).getAuthority();
            String currentDomain = method.getURI().getAuthority();
            if (!fileUrlDomain.equals(currentDomain)){
                changeHttpFileUrl(method.getURI().getURI());
                return;
            }
            if (fileURL.contains("/redirecting/")) {
                String url = PlugUtils.getStringBetween(getContentAsString(), "window.location = '", "'");
                changeHttpFileUrl(url);
                return;
            }
            String url = decodeUrl(PlugUtils.getStringBetween(getContentAsString(), "var ysmm = '", "';"));
            if (url.contains("adf.ly/go.php") || url.contains("/redirecting/")) {
                if (!makeRedirectedRequest(getGetMethod(url))) {
                    throw new ServiceConnectionProblemException();
                }
                checkProblems();
                try {
                    url = PlugUtils.getStringBetween(getContentAsString(), " URL=", "\"");
                } catch (Exception x) {
                    url = PlugUtils.getStringBetween(getContentAsString(), "window.location = '", "'");
                }
            }
            changeHttpFileUrl(url);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void changeHttpFileUrl(String url) throws Exception {
        httpFile.setNewURL(new URL(url));
        httpFile.setPluginID("");
        httpFile.setState(DownloadState.QUEUED);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        if (getContentAsString().contains("that link has been deleted") ||
                getContentAsString().contains("This AdF.ly account has been suspended")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private String decodeUrl(String in) {
        int idx1 = in.indexOf("!HiTommy");
        if (idx1 != -1) {
            in = in.substring(0, idx1);
        }
        String a = "", b = "";
        for (int i = 0; i < in.length(); i++) {
            if (i % 2 == 0) {
                a += in.charAt(i);
            } else {
                b = in.charAt(i) + b;
            }
        }
        String out = a + b;
        char[] ca = out.toCharArray();
        for (int i = 0; i < ca.length; i++) {
            if (!isNaN(ca[i])) {
                for (int j = i + 1; j < ca.length; j++) {
                    if (!isNaN(ca[j])) {
                        int S = ca[i]^ca[j];
                        if (S < 10) {
                            ca[i] = ("" + S).charAt(0);
                        }
                        i = j;
                        j = ca.length;
                    }
                }
            }
        }
        out = new String(ca);
        out = new String(Base64.decodeBase64(out));
        out = out.substring(16, out.length()-16);
        return out;
    }

    public static boolean isNaN(char chr){
        try{
            Integer.parseInt(new String(new char[]{chr}));
            return false;
        } catch (Exception x) {
            return true;
        }
    }

}
