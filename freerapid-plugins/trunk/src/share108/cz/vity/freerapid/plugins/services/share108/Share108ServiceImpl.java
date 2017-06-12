package cz.vity.freerapid.plugins.services.share108;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Share108ServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Share108";
    }

    @Override
    public String getName() {
        return "share108.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Share108FileRunner();
    }

}