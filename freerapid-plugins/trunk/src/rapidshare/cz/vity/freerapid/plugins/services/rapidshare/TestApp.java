package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.dev.PluginApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Ladislav Vitasek
 */
public class TestApp extends PluginApplication {
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://rapidshare.com/files/169450403/samantha.who.s02e08.hdtv.xvid-xor.avi"));
            testRun(new RapidShareServiceImpl(), httpFile, new ConnectionSettings());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
