package cz.vity.freerapid.plugins.services.kenfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class KenFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "KenFiles";
    }

    @Override
    public String getName() {
        return "kenfiles.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new KenFilesFileRunner();
    }

}