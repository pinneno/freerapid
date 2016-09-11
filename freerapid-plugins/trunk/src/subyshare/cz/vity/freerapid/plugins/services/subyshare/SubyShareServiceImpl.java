package cz.vity.freerapid.plugins.services.subyshare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SubyShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SubyShare";
    }

    @Override
    public String getName() {
        return "subyshare.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SubyShareFileRunner();
    }

}