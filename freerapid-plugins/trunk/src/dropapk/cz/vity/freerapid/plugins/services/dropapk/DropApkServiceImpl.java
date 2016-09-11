package cz.vity.freerapid.plugins.services.dropapk;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DropApkServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "DropApk";
    }

    @Override
    public String getName() {
        return "dropapk.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DropApkFileRunner();
    }

}