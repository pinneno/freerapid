package cz.vity.freerapid.plugins.services.uploadcomua;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class UploadComUaServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "upload.com.ua";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadComUaFileRunner();
    }

}