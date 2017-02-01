package cz.vity.freerapid.plugins.services.uskip;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class USkipServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "uskip.me";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new USkipFileRunner();
    }

}