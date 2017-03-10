package cz.vity.freerapid.plugins.services.xeupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class XeUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "XeUpload";
    }

    @Override
    public String getName() {
        return "xeupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new XeUploadFileRunner();
    }

}