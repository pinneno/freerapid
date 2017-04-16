package cz.vity.freerapid.plugins.services.owndrives;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class OwnDrivesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "OwnDrives";
    }

    @Override
    public String getName() {
        return "owndrives.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OwnDrivesFileRunner();
    }

}