package cz.vity.freerapid.plugins.services.vodlock;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VodLockServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VodLock";
    }

    @Override
    public String getName() {
        return "vodlock.co";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VodLockFileRunner();
    }

}