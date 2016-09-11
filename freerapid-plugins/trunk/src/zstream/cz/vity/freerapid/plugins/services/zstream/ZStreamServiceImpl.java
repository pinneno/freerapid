package cz.vity.freerapid.plugins.services.zstream;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ZStreamServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ZStream";
    }

    @Override
    public String getName() {
        return "zstream.to";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ZStreamFileRunner();
    }

}