package cz.vity.freerapid.plugins.services.tune_pk;

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
            //httpFile.setNewURL(new URL("https://tune.pk/video/6577742/the-kapil-sharma-show-25th-june-2016-episode-19"));
            //httpFile.setNewURL(new URL("http://tune.pk/video/6613265/wfatw-tnaimpact-21072016"));
            //httpFile.setNewURL(new URL("https://tune.pk/video/6999965/wfatw-tnaimpact-21072016")); //not found
            //httpFile.setNewURL(new URL("https://tune.pk/video/6641886/comedy-nights-bachao-14th-august-2016-episode"));
            httpFile.setNewURL(new URL("http://tune.pk/video/6577742/the-kapil-sharma-show-25th-june-2016-episode-19"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final Tune_pkServiceImpl service = new Tune_pkServiceImpl();
            SettingsConfig config = new SettingsConfig();
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