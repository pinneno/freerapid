package cz.vity.freerapid.plugins.services.fistfast;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FistFastServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FistFast";
    }

    @Override
    public String getName() {
        return "fistfast.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FistFastFileRunner();
    }

}