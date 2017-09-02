package cz.vity.freerapid.plugins.services.fileupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileUpload";
    }

    @Override
    public String getName() {
        return "file-upload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileUploadFileRunner();
    }

}