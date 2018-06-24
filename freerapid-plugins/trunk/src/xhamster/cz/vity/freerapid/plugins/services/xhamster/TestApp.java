package cz.vity.freerapid.plugins.services.xhamster;

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
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://fr.xhamster.com/movies/4302367/shu_qi_zouk_me.html"));
            httpFile.setNewURL(new URL("https://xhamster.com/movies/7464893/milf_teacher_gets_it_in_her_office.html"));
            //httpFile.setNewURL(new URL("https://xhamster.com/videos/milf-big-tits-and-nipples-7927779"));
            httpFile.setNewURL(new URL("https://xhamster.com/photos/gallery/sexy-beautiful-girls-563-10206387"));     //photo gallery
            //httpFile.setNewURL(new URL("https://xhamster.com/photos/gallery/random-nudity-and-porn-10206239"));       //photo gallery - multi page
            //httpFile.setNewURL(new URL("https://xhamster.com/photos/gallery/10206387/202578489"));                    //photo
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("75.125.40.18", 3128); //eg we can use local proxy to sniff HTTP communication
            final xHamsterServiceImpl service = new xHamsterServiceImpl();
            SettingsConfig config = new SettingsConfig();
            config.setHighestQuality();
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
        Application.launch(TestApp.class, args);//starts the application - calls startup() internally
    }
}