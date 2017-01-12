package cz.vity.freerapid.plugins.services.suprafiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SupraFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SupraFiles";
    }

    @Override
    public String getName() {
        return "suprafiles.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SupraFilesFileRunner();
    }

}