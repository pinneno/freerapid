package cz.vity.freerapid.xmlimport;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import cz.vity.freerapid.xmlimport.ver2.Plugins;

import java.io.IOException;

/**
 * @author Ladislav Vitasek
 */
public class XMLBind {
    /**
     * Parses updates plugin info list
     *
     * @param xmlData xml xml as string
     * @return instance of Plugins list
     * @throws IOException - in case we were not able to parse the xml data list
     */
    public Plugins loadPluginList(final String xmlData) throws IOException {
        final XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.setAnnotationIntrospector(new JacksonXmlAnnotationIntrospector());
        return xmlMapper.readValue(xmlData, Plugins.class);
    }


}
