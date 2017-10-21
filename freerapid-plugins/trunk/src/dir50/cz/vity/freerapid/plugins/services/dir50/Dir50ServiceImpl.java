package cz.vity.freerapid.plugins.services.dir50;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Dir50ServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Dir50";
    }

    @Override
    public String getName() {
        return "dir50.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Dir50FileRunner();
    }

}