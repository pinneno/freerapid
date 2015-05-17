package cz.vity.freerapid.plugins.services.iprima;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author JPEXS
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://play.iprima.cz/zazraky-vedy-techniky/atom-3-5"));//georestricted
            //httpFile.setNewURL(new URL("http://play.iprima.cz/cesko-na-taliri/cesko-na-taliri-24-0"));//non-georestricted
            //httpFile.setNewURL(new URL("http://play.iprima.cz/all/55946/all"));//stream.cz
            //httpFile.setNewURL(new URL("http://play.iprima.cz/tajemstvi-zeme-vesmiru/cestovani-cervi-dirou-s-morganem-freemanem-i-5"));
            //httpFile.setNewURL(new URL("http://play.iprima.cz/ano-sefe/ano-sefe-i-1")); //non-georestricted
            httpFile.setNewURL(new URL("http://play.iprima.cz/top-star-magazin/top-star-magazin-2015-15"));  //non-geo HD
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final iPrimaServiceImpl service = new iPrimaServiceImpl(); //instance of service - of our plugin
            //runcheck makes the validation
            //setUseTempFiles(true);
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