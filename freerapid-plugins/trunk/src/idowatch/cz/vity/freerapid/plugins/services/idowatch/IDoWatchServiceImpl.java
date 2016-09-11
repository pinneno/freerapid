package cz.vity.freerapid.plugins.services.idowatch;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class IDoWatchServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "IDoWatch";
    }

    @Override
    public String getName() {
        return "idowatch.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new IDoWatchFileRunner();
    }

}