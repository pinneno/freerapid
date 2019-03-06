package cz.vity.freerapid.plugins.services.bitly;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BitlyServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "bitly.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BitlyFileRunner();
    }

}