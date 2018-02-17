package cz.vity.freerapid.plugins.services.kumpulbagi;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class KumpulBagiServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_KumpulBagi_Account.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "kumpulbagi.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new KumpulBagiFileRunner();
    }


    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) setConfig(pa);
    }

    PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "KumpulBagi", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        synchronized (KumpulBagiServiceImpl.class) {
            if (config == null) {
                setConfig(getAccountConfigFromFile(PLUGIN_CONFIG_FILE));
            }
        }
        return config;
    }

    void setConfig(final PremiumAccount config) {
        this.config = config;
    }
}