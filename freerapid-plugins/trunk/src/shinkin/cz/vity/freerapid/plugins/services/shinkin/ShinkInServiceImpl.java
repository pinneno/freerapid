package cz.vity.freerapid.plugins.services.shinkin;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShinkInServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "shink.in";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShinkInFileRunner();
    }

}