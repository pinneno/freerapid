package cz.vity.freerapid.plugins.services.hotlink;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class HotLinkServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "HotLink";
    }

    @Override
    public String getName() {
        return "hotlink.cc";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HotLinkFileRunner();
    }

}