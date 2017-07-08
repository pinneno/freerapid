package cz.vity.freerapid.plugins.services.cloudyfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class CloudyFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "CloudyFiles";
    }

    @Override
    public String getName() {
        return "cloudyfiles.org";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CloudyFilesFileRunner();
    }

}