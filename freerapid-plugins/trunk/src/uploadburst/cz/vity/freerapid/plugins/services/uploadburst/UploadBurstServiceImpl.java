package cz.vity.freerapid.plugins.services.uploadburst;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadBurstServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadBurst";
    }

    @Override
    public String getName() {
        return "uploadburst.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadBurstFileRunner();
    }

}