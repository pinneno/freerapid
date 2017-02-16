package cz.vity.freerapid.plugins.services.sendit;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SendItServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SendIt";
    }

    @Override
    public String getName() {
        return "sendit.cloud";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SendItFileRunner();
    }

}