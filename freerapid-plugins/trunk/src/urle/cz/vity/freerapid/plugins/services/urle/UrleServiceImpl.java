package cz.vity.freerapid.plugins.services.urle;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UrleServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "urle.co";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UrleFileRunner();
    }

}