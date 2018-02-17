package cz.vity.freerapid.plugins.services.downace;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DownAceServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "downace.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DownAceFileRunner();
    }

}