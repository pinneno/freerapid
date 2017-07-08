package cz.vity.freerapid.xmlimport;

import cz.vity.freerapid.utilities.Utils;
import org.junit.Test;

import java.io.File;
import java.net.URI;

/**
 * @author VitasekL
 */
public class XMLBindTest {
    @Test
    public void loadPluginList() throws Exception {
        final URI uri = XMLBindTest.class.getResource("resources/pluginsList.xml").toURI();
        String content = Utils.loadFile(new File(uri), "UTF-8");

        new XMLBind().loadPluginList(content);
    }

}