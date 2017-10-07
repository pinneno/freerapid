package cz.vity.freerapid.plugins.services.uplod_cc;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Uplod_ccServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Uplod";
    }

    @Override
    public String getName() {
        return "uplod.cc";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Uplod_ccFileRunner();
    }

}