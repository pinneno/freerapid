package cz.vity.freerapid.plugins.services.shortixssl;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShortixSslServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "shortixssl.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShortixSslFileRunner();
    }

}