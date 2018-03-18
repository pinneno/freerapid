package cz.vity.freerapid.plugins.services.linkget;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class LinkGetServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "linkget.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinkGetFileRunner();
    }

}