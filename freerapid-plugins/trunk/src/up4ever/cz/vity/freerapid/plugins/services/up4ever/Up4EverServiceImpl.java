package cz.vity.freerapid.plugins.services.up4ever;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Up4EverServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Up-4Ever";
    }

    @Override
    public String getName() {
        return "up-4ever.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Up4EverFileRunner();
    }

}