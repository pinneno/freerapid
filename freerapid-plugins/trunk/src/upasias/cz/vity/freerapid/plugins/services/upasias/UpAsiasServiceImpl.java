package cz.vity.freerapid.plugins.services.upasias;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UpAsiasServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UpAsias";
    }

    @Override
    public String getName() {
        return "upasias.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UpAsiasFileRunner();
    }

}