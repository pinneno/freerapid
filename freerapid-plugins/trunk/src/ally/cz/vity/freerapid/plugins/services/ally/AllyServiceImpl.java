package cz.vity.freerapid.plugins.services.ally;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AllyServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "al.ly";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AllyFileRunner();
    }

}