package cz.vity.freerapid.plugins.services.douploads;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DoUploadsServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "DoUploads";
    }

    @Override
    public String getName() {
        return "douploads.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DoUploadsFileRunner();
    }

}