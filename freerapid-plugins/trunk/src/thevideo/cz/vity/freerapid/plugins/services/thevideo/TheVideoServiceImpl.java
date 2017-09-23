package cz.vity.freerapid.plugins.services.thevideo;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class TheVideoServiceImpl extends XFilePlayerServiceImpl {
    private final String settingsFile = "plugin_" + getServiceTitle() + "_settings.xml";
    private SettingsConfig settings;

    @Override
    public String getServiceTitle() {
        return "TheVideo";
    }

    @Override
    public String getName() {
        return "thevideo.me";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TheVideoFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        showSettingsDialog();
    }

    @Override
    public PremiumAccount showConfigDialog() throws Exception {
        final PremiumAccount pa = super.showConfigDialog();
        if (pa != null) {
            setConfig(pa);
        }
        return pa;
    }

    public void showSettingsDialog() throws Exception {
        SettingsPanel panel = new SettingsPanel(this);
        if (getPluginContext().getDialogSupport().showOKCancelDialog(panel, getServiceTitle() + " Settings")) {
            synchronized (getClass()) {
                setSettings(panel.getSettings());
                getPluginContext().getConfigurationStorageSupport().storeConfigToFile(settings, settingsFile);
            }
        }
    }

    public void setSettings(SettingsConfig settings) {
        this.settings = settings;
    }

    public SettingsConfig getSettings()  {
        synchronized (getClass()) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (settings == null) {
                if (storage.configFileExists(settingsFile)) {
                    try {
                        settings = storage.loadConfigFromFile(settingsFile, SettingsConfig.class);
                    } catch (Exception x) {
                        settings = new SettingsConfig();
                    }
                } else {
                    settings = new SettingsConfig();
                }
            }
        }
        return settings;
    }
}