package cz.vity.freerapid.plugins.services.upload4earn;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Upload4EarnServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Upload4Earn";
    }

    @Override
    public String getName() {
        return "upload4earn.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Upload4EarnFileRunner();
    }

}