package cz.vity.freerapid.plugins.services.nhk_vod;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author tong2shot
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://www3.nhk.or.jp/nhkworld/en/vod/asiainsight/20160226.html"));
            httpFile.setNewURL(new URL("https://www3.nhk.or.jp/nhkworld/en/vod/japaneasy2/20170409/"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final NHK_vodServiceImpl service = new NHK_vodServiceImpl();
            NHK_vodSettingsConfig config = new NHK_vodSettingsConfig();
            config.setVideoQuality(VideoQuality._360);
            service.setConfig(config);
            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}