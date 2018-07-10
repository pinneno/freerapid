package cz.vity.freerapid.plugins.services.imdb;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ImdbServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_Imdb.xml";
    private volatile SettingsConfig config;

    @Override
    public String getName() {
        return "imdb.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ImdbFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new SettingsPanel(this), "Imdb settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    SettingsConfig getConfig() throws Exception {
        synchronized (ImdbServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(CONFIG_FILE)) {
                    setConfig(new SettingsConfig());
                } else {
                    setConfig(storage.loadConfigFromFile(CONFIG_FILE, SettingsConfig.class));
                }
            }
            return config;
        }
    }

    void setConfig(final SettingsConfig config) {
        synchronized (ImdbServiceImpl.class) {
            this.config = config;
        }
    }

}