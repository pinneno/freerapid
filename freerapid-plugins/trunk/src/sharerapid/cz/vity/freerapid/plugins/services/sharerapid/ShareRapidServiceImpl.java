package cz.vity.freerapid.plugins.services.sharerapid;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Jan Smejkal (edit from CZshare profi to RapidShare)
 */
public class ShareRapidServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "share-rapid.com";
    private static final String PLUGIN_CONFIG_FILE = "plugin_ShareRapid.xml";
    private static final Integer MAX_DOWNLOADS = 3;  //max in PA
    private volatile PremiumAccount config;

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    public int getMaxDownloadsFromOneIP() {
        //don't forget to update this value, in plugin.xml don't forget to update this value too
        return MAX_DOWNLOADS;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareRapidRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "ShareRapid", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (ShareRapidServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }

        return config;
    }

}