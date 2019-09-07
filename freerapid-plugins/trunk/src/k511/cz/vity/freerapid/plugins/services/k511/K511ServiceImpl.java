package cz.vity.freerapid.plugins.services.k511;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class K511ServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "k511.me";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new K511FileRunner();
    }

}