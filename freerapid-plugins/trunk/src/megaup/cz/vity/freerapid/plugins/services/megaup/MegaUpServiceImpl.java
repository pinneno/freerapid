package cz.vity.freerapid.plugins.services.megaup;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MegaUpServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "megaup.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegaUpFileRunner();
    }

}