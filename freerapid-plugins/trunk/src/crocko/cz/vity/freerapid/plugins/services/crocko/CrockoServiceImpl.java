package cz.vity.freerapid.plugins.services.crocko;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class CrockoServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "crocko.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CrockoFileRunner();
    }

}