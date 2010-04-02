package cz.vity.freerapid.plugins.services.hellshare;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Ladislav Vitasek & Tom� Proch�zka <to.m.p@atomsoft.cz>
 */
public class TestApp extends PluginDevApplication {
    protected void startup() {

        final HttpFile httpFile = getHttpFile();
        try {

            httpFile.setNewURL(new URL("http://download.cz.hellshare.com/starcraft-ii-beta-enus-13891-installer-part02.rar/981200"));
            testRun(new HellshareServiceImpl(), httpFile, new ConnectionSettings());
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);

        /*try {
            ImageIO.write(ImageIO.read(new File("E:\\projects\\captchatest\\letters1.png")), "png", new File("E:\\projects\\captchatest\\letters.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }
}