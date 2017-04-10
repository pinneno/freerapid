package cz.vity.freerapid.plugins.services.nbc;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author birchie, based on tong2shot's DailyMotion SubtitleDownloader
 */
class SubtitleDownloader {
    private final static Logger logger = Logger.getLogger(SubtitleDownloader.class.getName());

    public void downloadSubtitle(HttpDownloadClient client, HttpFile httpFile, String subtitleUrl, String subFileName, String subFileExt) throws Exception {
        if ((subtitleUrl == null) || subtitleUrl.isEmpty()) {
            return;
        }
        logger.info("Downloading subtitle");
        HttpMethod method = client.getGetMethod(subtitleUrl);
        if (200 != client.makeRequest(method, true)) {
            throw new PluginImplementationException("Failed to request subtitle");
        }
        String subtitle = client.getContentAsString();
        String fnameOutput = subFileName + subFileExt;
        File outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
        BufferedWriter bw = null;
        int outputFileCounter = 2;
        try {
            while (outputFile.exists()) {
                fnameOutput = subFileName + "-" + outputFileCounter++ + subFileExt;
                outputFile = new File(httpFile.getSaveToDirectory(), fnameOutput);
            }
            logger.info("File name : "+fnameOutput);
            logger.info("File size : "+method.getResponseHeader("Content-Length"));
            bw = new BufferedWriter(new FileWriter((outputFile)));
            bw.write(subtitle);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    LogUtils.processException(logger, e);
                }
            }
        }
    }

}
