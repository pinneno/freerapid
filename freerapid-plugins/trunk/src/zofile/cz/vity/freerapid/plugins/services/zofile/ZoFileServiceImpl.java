package cz.vity.freerapid.plugins.services.zofile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ZoFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ZoFile";
    }

    @Override
    public String getName() {
        return "zofile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ZoFileFileRunner();
    }

}