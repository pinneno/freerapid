package cz.vity.freerapid.plugins.services.letsupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class LetsUploadServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "letsupload.co";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LetsUploadFileRunner();
    }

}