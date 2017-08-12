package cz.vity.freerapid.plugins.services.file_al;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class File_alServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "File_al";
    }

    @Override
    public String getName() {
        return "file.al";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new File_alFileRunner();
    }

}