package cz.vity.freerapid.plugins.services.indoshares;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class IndoSharesServiceImpl extends AbstractFileShareService {

    private final String configFile = "plugin_IndoShares.xml";
    private PremiumAccount config;

    @Override
    public String getName() {
        return "indoshares.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new IndoSharesFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        final PremiumAccount pa = showConfigDialog();
        if (pa != null) {
            setConfig(pa);
        }
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "IndoShares", configFile);
    }

    public PremiumAccount getConfig() throws Exception {
        synchronized (getClass()) {
            if (config == null) {
                config = getAccountConfigFromFile(configFile);
            }
            return config;
        }
    }

    public void setConfig(final PremiumAccount config) {
        synchronized (getClass()) {
            this.config = config;
        }
    }
}