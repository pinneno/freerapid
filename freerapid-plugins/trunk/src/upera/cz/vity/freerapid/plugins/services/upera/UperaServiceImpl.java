package cz.vity.freerapid.plugins.services.upera;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UperaServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "upera.co";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UperaFileRunner();
    }

}