package cz.vity.freerapid.plugins.services.uploadbank;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadBankServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadBank";
    }

    @Override
    public String getName() {
        return "uploadbank.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadBankFileRunner();
    }

}