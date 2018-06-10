package cz.vity.freerapid.plugins.services.usersdrive;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UsersDriveServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UsersDrive";
    }

    @Override
    public String getName() {
        return "usersdrive.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UsersDriveFileRunner();
    }

}