package cz.vity.freerapid.plugins.services.appletrailers;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class AppleTrailersServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_AppleTrailers.xml";
    private volatile SettingsConfig config;

    @Override
    public String getName() {
        return "trailers.apple.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AppleTrailersFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new SettingsPanel(this), "Apple Trailers settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    SettingsConfig getConfig() throws Exception {
        synchronized (AppleTrailersServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(CONFIG_FILE)) {
                    config = new SettingsConfig();
                } else {
                    try {
                        config = storage.loadConfigFromFile(CONFIG_FILE, SettingsConfig.class);
                    } catch (Exception e) {
                        config = new SettingsConfig();
                    }
                }
            }
            return config;
        }
    }

    void setConfig(final SettingsConfig config) {
        synchronized (AppleTrailersServiceImpl.class) {
            this.config = config;
        }
    }

}