package cz.vity.freerapid.plugins.services.linktl;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class LinkTLServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "link.tl";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinkTLFileRunner();
    }

}