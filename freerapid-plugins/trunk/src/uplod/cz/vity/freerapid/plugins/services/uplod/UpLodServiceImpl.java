package cz.vity.freerapid.plugins.services.uplod;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UpLodServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UpLod";
    }

    @Override
    public String getName() {
        return "uplod.it";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UpLodFileRunner();
    }

}