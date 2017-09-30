package cz.vity.freerapid.plugins.services.rapidvideo_2;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RapidVideo_2ServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "rapidvideo.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RapidVideo_2FileRunner();
    }

}