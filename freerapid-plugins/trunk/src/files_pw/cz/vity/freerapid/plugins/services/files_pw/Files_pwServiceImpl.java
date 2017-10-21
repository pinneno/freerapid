package cz.vity.freerapid.plugins.services.files_pw;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Files_pwServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Files";
    }

    @Override
    public String getName() {
        return "files.pw";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Files_pwFileRunner();
    }

}