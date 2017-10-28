package cz.vity.freerapid.plugins.services.imgcredit;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ImgCreditServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "imgcredit.xyz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ImgCreditFileRunner();
    }

}