package cz.vity.freerapid.plugins.services.nhk_vod;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;
import cz.vity.freerapid.utilities.LogUtils;

import java.util.logging.Logger;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class NHK_vodServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "NHK_VOD_Settings.xml";
    private static final Logger logger = Logger.getLogger(NHK_vodServiceImpl.class.getName());
    private volatile NHK_vodSettingsConfig config;

    @Override
    public String getName() {
        return "nhk_vod.jp";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NHK_vodFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();

        if (getPluginContext().getDialogSupport().showOKCancelDialog(new NHK_vodSettingsPanel(this), "NHK VOD settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public NHK_vodSettingsConfig getConfig() throws Exception {
        synchronized (NHK_vodServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(CONFIG_FILE)) {
                    config = new NHK_vodSettingsConfig();
                } else {
                    try {
                        config = storage.loadConfigFromFile(CONFIG_FILE, NHK_vodSettingsConfig.class);
                    } catch (Exception e) {
                        LogUtils.processException(logger, e);
                        logger.warning("Broken plugin config file detected. Using default settings.");
                        config = new NHK_vodSettingsConfig();
                    }
                }
            }
            return config;
        }
    }

    public void setConfig(final NHK_vodSettingsConfig config) {
        synchronized (NHK_vodServiceImpl.class) {
            this.config = config;
        }
    }

}