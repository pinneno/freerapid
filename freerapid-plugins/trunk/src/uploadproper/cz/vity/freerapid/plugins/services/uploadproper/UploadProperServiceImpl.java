package cz.vity.freerapid.plugins.services.uploadproper;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadProperServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadProper";
    }

    @Override
    public String getName() {
        return "uploadproper.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadProperFileRunner();
    }

}