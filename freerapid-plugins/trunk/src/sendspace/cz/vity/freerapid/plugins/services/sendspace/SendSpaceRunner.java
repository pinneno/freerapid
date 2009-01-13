package cz.vity.freerapid.plugins.services.sendspace;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.codec.net.BCodec;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 */
class SendSpaceRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SendSpaceRunner.class.getName());
    public String mLink;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameandSize(getContentAsString());
        } else {
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            final String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);
//            <script type="text/javascript">
//            var count = 60;
//            var link_dec = utf8_decode(enc(base64ToText('grJYHEadssT1nJnYMXaejK4MaUsLCHP9Pi7QnkeTs99WUyQrPs4UPk23"o_PYbQsi_IrrKGmXpqL88yeAOumLnH8UMBDtpjgL_3obEAT"opEikYmY_0Ali2pWrLl2J5G_gfLG2UeswhOip5iyEQhoN_phzCiT7_501Yk0rauHVutY_tKEpcTb2h4UvI6xKQ4VpEICK0J4tFNx"ORMR_QnF5PycA8BNnjvdfVM20t2GkJPF0ol9jb')));
//            var link_updated = 0;



            Matcher matcher = PlugUtils.matcher("var link_dec =[^']+'([^']+)", contentAsString);
            if (matcher.find()) {
                String mCode =  matcher.group(1);
                logger.info("Code :"+mCode);
                
                mLink = new BCodec().decode(matcher.group(1));
                logger.info("Final Link :" + mLink);
                getMethod = getGetMethod(mLink);

                if (!tryDownloadAndSaveFile(getMethod)) {
                    checkProblems();
                    logger.warning(getContentAsString());//something was really wrong, we will explore it from the logs :-)
                    throw new IOException("File input stream is empty.");
                }
            } else {
                throw new PluginImplementationException("Can't find download link");//something is wrong with plugin
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Cant load download link");
        }

    }

    private void checkNameandSize(String content) throws Exception {

        if (!content.contains("sendspace.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("the file you requested is not available")) {
            throw new URLNotAvailableAnymoreException("<b>SendSpace error:</b><br>File doesn't exist");
        }

        Matcher xmatcher = PlugUtils.matcher("<b>Size:</b>((.+?)+)<br>", content);
        if (xmatcher.find()) {
            final String fileSize = xmatcher.group(1).trim();
            logger.info("File size " + fileSize);
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));

        } else {
            logger.warning("File size was not found" + content);
        }

        xmatcher = PlugUtils.matcher("<b>Name:</b>([^<]+)<br>", content);
        if (xmatcher.find()) {
            final String fileName = xmatcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

        } else {
            logger.warning("File name was not found" + content);
        }

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>SendSpace Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>SendSpace Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

}