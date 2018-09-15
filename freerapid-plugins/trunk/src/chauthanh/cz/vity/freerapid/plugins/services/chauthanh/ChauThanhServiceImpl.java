package cz.vity.freerapid.plugins.services.chauthanh;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ChauThanhServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "chauthanh.info";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ChauThanhFileRunner();
    }

}