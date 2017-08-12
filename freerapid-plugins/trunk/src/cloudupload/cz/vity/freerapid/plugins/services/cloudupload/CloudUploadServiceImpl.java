package cz.vity.freerapid.plugins.services.cloudupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class CloudUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "CloudUpload";
    }

    @Override
    public String getName() {
        return "cloudupload.co";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CloudUploadFileRunner();
    }

}