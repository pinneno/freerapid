package cz.vity.freerapid.plugins.services.u2s;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class U2sServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "u2s.io";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new U2sFileRunner();
    }

}