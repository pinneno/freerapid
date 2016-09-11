package cz.vity.freerapid.plugins.services.upload_af;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Upload_afServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Upload";
    }

    @Override
    public String getName() {
        return "upload.af";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Upload_afFileRunner();
    }

}