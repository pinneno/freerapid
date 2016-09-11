package cz.vity.freerapid.plugins.services.camwhores;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class CamWhoresServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "camwhores.tv";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CamWhoresFileRunner();
    }

}