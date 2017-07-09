package cz.vity.freerapid.plugins.webclient.interfaces;

import java.io.File;

/**
 * <p>Class that provides locale storage support for plugins.<br />
 * Plugins use instance of this interface to access local file system to store their information - eg. premium account info.
 * Implementation uses XMLEncoder to store beans data into XML.</p>
 *
 * @author Ladislav Vitasek
 */
public interface ConfigurationStorageSupport {

    /**
     * Load configuration data from file into Object.
     * Internal implementation uses XMLDecoder.
     *
     * @param fileName file name for storing data - it's recommended to use 'plugin_id.xml'
     * @param type     class of the stored object
     * @return returns new instance, null if
     * @throws Exception when reading went wrong
     * @see org.jdesktop.application.LocalStorage#load(String) load method
     */
    <E> E loadConfigFromFile(final String fileName, Class<E> type) throws Exception;


    /**
     * Store plugin's configuration data from Object into file.
     * Internal implementation uses XMLEncoder.
     *
     * @param fileName file name for storing data - it's recommended to use 'plugin_id.xml'
     * @return returns new instance, null if
     * @throws Exception when reading went wrong
     * @see org.jdesktop.application.LocalStorage#save(Object, String) save method
     */
    void storeConfigToFile(final Object object, final String fileName) throws Exception;

    /**
     * Checks whether configuration file exists (if any configuration was created before)
     *
     * @param fileName file name
     * @return true - if there is such file in configuration directory, false otherwise
     */
    boolean configFileExists(String fileName);

    /**
     * Returns the configuration directory. Useful for custom configuration files.
     *
     * @return the configuration directory
     * @since 1.0
     */
    File getConfigDirectory();

}
