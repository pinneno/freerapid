package cz.vity.freerapid.plugins.services.takefile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class TakeFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "TakeFile";
    }

    @Override
    public String getName() {
        return "takefile.link";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TakeFileFileRunner();
    }

}