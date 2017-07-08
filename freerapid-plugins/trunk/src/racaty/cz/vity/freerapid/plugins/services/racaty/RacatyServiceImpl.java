package cz.vity.freerapid.plugins.services.racaty;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RacatyServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Racaty";
    }

    @Override
    public String getName() {
        return "racaty.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RacatyFileRunner();
    }

}