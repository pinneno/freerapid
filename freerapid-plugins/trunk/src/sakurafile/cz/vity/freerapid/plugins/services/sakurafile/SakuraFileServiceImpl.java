package cz.vity.freerapid.plugins.services.sakurafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SakuraFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SakuraFile";
    }

    @Override
    public String getName() {
        return "sakurafile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SakuraFileFileRunner();
    }

}