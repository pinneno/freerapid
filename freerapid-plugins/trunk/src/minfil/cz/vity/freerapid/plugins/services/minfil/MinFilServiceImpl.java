package cz.vity.freerapid.plugins.services.minfil;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MinFilServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "minfil.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MinFilFileRunner();
    }

}