package cz.vity.freerapid.plugins.services.uploadgig;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadGigServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "uploadgig.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadGigFileRunner();
    }

}