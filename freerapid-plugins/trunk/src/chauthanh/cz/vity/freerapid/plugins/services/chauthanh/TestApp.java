package cz.vity.freerapid.plugins.services.chauthanh;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author birchie
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            httpFile.setNewURL(new URL("https://chauthanh.info/drama/view/grand-prince-(2018)-[kdrama].html"));
            httpFile.setNewURL(new URL("https://chauthanh.info/drama/download/NR-uavEGmjex83HP6R3hg86xA7RCWb4ZeVv5tU56o45tonR-OapshO8g75blArLi5SBgPDbzFxElCGtgGTqkpg50B-Oc8opCrJgqpdBDrcZTIO6UlMsAWHQqULizXayg--NsK8rHsqCKWN_qnA-Ec0uDIMhoqhuph9egz48n8Ek/Grand.Prince.E02.1080p.Web-DL.AAC.H.264-lk.mkv.html"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final ChauThanhServiceImpl service = new ChauThanhServiceImpl(); //instance of service - of our plugin
            //runcheck makes the validation
            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            //all output goes to the console
        } catch (Exception e) {//catch possible exception
            e.printStackTrace(); //writes error output - stack trace to console
        }
        this.exit();//exit application
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);//starts the application - calls startup() internally
    }
}