package cz.vity.freerapid.plugins.services.kumpulbagi;

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
            httpFile.setNewURL(new URL("http://kumpulbagi.com/avesena-arif-nugroho/getter-robo-armageddon-end-288428/getter-robo-armageddon-ep-01,2110588,gallery,1,1.mkv"));
            //httpFile.setNewURL(new URL("http://copiapop.com/ovagames/campur15-45753/629842991-seloebrevo-part01,427090,list,1,1.rar"));
            httpFile.setNewURL(new URL("http://kbagi.com/wachid-fatkhu-rk/film-indonesia-151255/18-forever-love,1002781,gallery,1,1.mkv"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final KumpulBagiServiceImpl service = new KumpulBagiServiceImpl(); //instance of service - of our plugin
            /*
            final PremiumAccount config = new PremiumAccount();
            config.setUsername("*******");
            config.setPassword("*******");
            service.setConfig(config);
            //*/
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