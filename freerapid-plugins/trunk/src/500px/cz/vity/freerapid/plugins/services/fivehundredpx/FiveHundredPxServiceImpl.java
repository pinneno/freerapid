package cz.vity.freerapid.plugins.services.fivehundredpx;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FiveHundredPxServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "500px.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FiveHundredPxFileRunner();
    }

}