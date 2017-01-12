package cz.vity.freerapid.plugins.services.sharehost;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShareHostServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "sharehost.eu";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareHostFileRunner();
    }

}