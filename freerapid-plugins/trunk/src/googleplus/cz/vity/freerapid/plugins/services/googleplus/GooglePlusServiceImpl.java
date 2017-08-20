package cz.vity.freerapid.plugins.services.googleplus;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class GooglePlusServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "plus.google.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GooglePlusFileRunner();
    }

}