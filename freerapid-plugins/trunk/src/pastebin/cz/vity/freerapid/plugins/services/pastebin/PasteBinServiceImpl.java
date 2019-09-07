package cz.vity.freerapid.plugins.services.pastebin;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class PasteBinServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "pastebin.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PasteBinFileRunner();
    }

}