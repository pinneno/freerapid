package cz.vity.freerapid.plugins.services.btafile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BtaFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "BtaFile";
    }

    @Override
    public String getName() {
        return "btafile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BtaFileFileRunner();
    }

}