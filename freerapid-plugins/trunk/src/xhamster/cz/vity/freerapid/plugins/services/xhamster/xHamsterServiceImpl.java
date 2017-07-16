package cz.vity.freerapid.plugins.services.xhamster;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class xHamsterServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_xHamster.xml";
    private volatile SettingsConfig config;

    @Override
    public String getName() {
        return "xhamster.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new xHamsterFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new SettingsPanel(this), "xHamster settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    SettingsConfig getConfig() throws Exception {
        synchronized (xHamsterServiceImpl.class) {
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
        this.config = config;
    }
}