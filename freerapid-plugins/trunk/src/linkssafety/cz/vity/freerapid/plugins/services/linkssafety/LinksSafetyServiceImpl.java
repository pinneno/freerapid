package cz.vity.freerapid.plugins.services.linkssafety;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class LinksSafetyServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "links-safety.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinksSafetyFileRunner();
    }

}