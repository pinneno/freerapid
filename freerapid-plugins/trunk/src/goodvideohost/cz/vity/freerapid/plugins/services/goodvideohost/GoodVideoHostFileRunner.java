package cz.vity.freerapid.plugins.services.goodvideohost;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class GoodVideoHostFileRunner extends XFilePlayerRunner {

    @Override
    protected void correctURL() throws Exception {
        fileURL = fileURL.replaceFirst("/embed-", "/");
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        return getXFSMethodBuilder(getContentAsString());
    }
}