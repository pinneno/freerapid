package cz.vity.freerapid.plugins.services.servupcoil;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Frishrash
 */
public class ServUpCoIlServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "servupcoil";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ServUpCoIlFileRunner();
    }

}