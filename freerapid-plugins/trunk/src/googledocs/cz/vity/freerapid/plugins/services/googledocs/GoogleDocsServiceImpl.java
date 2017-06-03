package cz.vity.freerapid.plugins.services.googledocs;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class GoogleDocsServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_GoogleDocs_Account.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "docs.google.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GoogleDocsFileRunner();
    }

    @Override
    public void showOptions() {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() {
        return showAccountDialog(getConfig(), "Google", PLUGIN_CONFIG_FILE);
    }

    public PremiumAccount getConfig() {
        synchronized (GoogleDocsServiceImpl.class) {
            if (config == null) {
                setConfig(getAccountConfigFromFile(PLUGIN_CONFIG_FILE));
            }
        }
        return config;
    }

    public void setConfig(PremiumAccount config) {
        this.config = config;
    }
}