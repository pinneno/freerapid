package cz.vity.freerapid.plugins.services.ausfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AusFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "AusFile";
    }

    @Override
    public String getName() {
        return "ausfile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AusFileFileRunner();
    }

}