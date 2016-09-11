package cz.vity.freerapid.plugins.services.indishare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class IndiShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "IndiShare";
    }

    @Override
    public String getName() {
        return "indishare.me";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new IndiShareFileRunner();
    }

}