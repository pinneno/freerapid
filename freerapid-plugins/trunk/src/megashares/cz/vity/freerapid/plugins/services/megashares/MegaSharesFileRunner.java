package cz.vity.freerapid.plugins.services.megashares;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaSharesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaSharesFileRunner.class.getName());
    private final static String SERVICE_WEB = "http://d01.megashares.com/";
    private final static int CAPTCHA_MAX = 5;
    private int captchaCounter = 1;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        PlugUtils.checkName(httpFile, content, "<h1 class=\"black xxl\" style=\"letter-spacing: -1px\">", "</h1>");
        PlugUtils.checkFileSize(httpFile, content, "Filesize:</span></strong>", "<br");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            /*
             * Captcha only needs to be posted once every 2 hours.
             * Synchronize this part to make sure it's posted only once.
             */
            synchronized (MegaSharesFileRunner.class) {
                while (getContentAsString().contains("id=\"replace_sec_pprenewal\"")) {
                    if (!makeRedirectedRequest(stepCaptcha())) {
                        throw new ServiceConnectionProblemException("Error posting captcha");
                    }
                    if (!makeRedirectedRequest(method)) {
                        throw new ServiceConnectionProblemException();
                    }
                }
            }

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setActionFromAHrefWhereATagContains("download_file").toGetMethod();
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("Could not download file") || content.contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("All download slots for this link are currently filled")) {
            throw new ServiceConnectionProblemException("All download slots for this link are currently filled. Please try again momentarily.");
        }
        /*
         * There's no real need to check for download limits,
         * because they are just lousy client-side JS functions.
         * Once the "passport" (captcha function) is validated,
         * you can freely download as much as you want
         * if you simply ignore the JS-hide-element commands.
         */
    }

    private HttpMethod stepCaptcha() throws Exception {
        final String content = getContentAsString();
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaSrc = SERVICE_WEB + getMethodBuilder().setBaseURL(SERVICE_WEB).setActionFromImgSrcWhereTagContains("Security Code").getAction();
        logger.info("Captcha URL " + captchaSrc);

        final String captcha;
        if (captchaCounter <= CAPTCHA_MAX) {
            final BufferedImage captchaImage = prepareCaptchaImage(captchaSupport.getCaptchaImage(captchaSrc));
            captcha = PlugUtils.recognize(captchaImage, "-d -1 -C 0-9");
            logger.info("OCR attempt " + captchaCounter + " of " + CAPTCHA_MAX + ", recognized " + captcha);
            captchaCounter++;
        } else {
            captcha = captchaSupport.getCaptcha(captchaSrc);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        //http://d01.megashares.com/?d01=ZuOELFW&rs=check_passport_renewal&rsargs[]=2586&rsargs[]=762853&rsargs[]=f91f9a3918b8f803888f3f4a015ecca1&rsargs[]=replace_sec_pprenewal&rsrnd=1270641006310

        final String request_uri = PlugUtils.getStringBetween(content, "var request_uri = \"/", "\";");
        final String random_num = PlugUtils.getStringBetween(content, "id=\"random_num\" value=\"", "\"");
        final String passport_num = PlugUtils.getStringBetween(content, "id=\"passport_num\" value=\"", "\"");

        final StringBuilder sb = new StringBuilder()
                .append(SERVICE_WEB).append(request_uri)
                .append("&rs=check_passport_renewal")
                .append("&rsargs[]=").append(captcha)
                .append("&rsargs[]=").append(random_num)
                .append("&rsargs[]=").append(passport_num)
                .append("&rsargs[]=replace_sec_pprenewal")
                .append("&rsrnd=").append(System.currentTimeMillis());

        return getMethodBuilder().setReferer(fileURL).setAction(sb.toString()).toGetMethod();
    }

    private BufferedImage prepareCaptchaImage(final BufferedImage input) {
        final int colorLimit = 40;
        final int w = input.getWidth();
        final int h = input.getHeight();
        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        final Graphics g = output.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int red = new Color(input.getRGB(x, y)).getRed();
                final int color = red > colorLimit ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                output.setRGB(x, y, color);
            }
        }
        return output;
    }

}