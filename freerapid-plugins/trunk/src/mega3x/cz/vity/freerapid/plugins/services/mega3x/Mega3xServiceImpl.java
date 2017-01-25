package cz.vity.freerapid.plugins.services.mega3x;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Mega3xServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Mega3x";
    }

    @Override
    public String getName() {
        return "mega3x.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Mega3xFileRunner();
    }

}