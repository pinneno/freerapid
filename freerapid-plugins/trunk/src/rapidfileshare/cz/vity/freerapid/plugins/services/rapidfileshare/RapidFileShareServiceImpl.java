package cz.vity.freerapid.plugins.services.rapidfileshare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RapidFileShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RapidFileShare";
    }

    @Override
    public String getName() {
        return "rapidfileshare.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RapidFileShareFileRunner();
    }

}