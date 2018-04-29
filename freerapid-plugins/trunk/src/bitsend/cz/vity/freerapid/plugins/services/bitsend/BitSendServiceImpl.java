package cz.vity.freerapid.plugins.services.bitsend;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BitSendServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "bitsend.jp";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BitSendFileRunner();
    }

}