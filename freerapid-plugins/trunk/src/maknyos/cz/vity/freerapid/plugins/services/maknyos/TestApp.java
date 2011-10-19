package cz.vity.freerapid.plugins.services.maknyos;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * Test application for maknyos.com
 *
 * @author zid
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.maknyos.com/lrqmmufhllvm/A_LITTLE_THING_CALLED_LOVE-maknyos.com.srt.html"));
            httpFile.setNewURL(new URL("http://www.maknyos.com/s205fpjohm8m/indicator2-maknyos.com.gif.html"));

            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final MaknyosServiceImpl service = new MaknyosServiceImpl(); //instance of service - of our plugin
            final PremiumAccount config = new PremiumAccount();
            config.setUsername("zzz123");
            config.setPassword("123456");
            service.setConfig(config);
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
