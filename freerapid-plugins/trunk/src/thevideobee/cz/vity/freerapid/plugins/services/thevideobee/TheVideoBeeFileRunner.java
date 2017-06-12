package cz.vity.freerapid.plugins.services.thevideobee;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;

import java.util.List;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class TheVideoBeeFileRunner extends XFilePlayerRunner {

    @Override
    protected List<String> getDownloadLinkRegexes() {
        List<String> ret = super.getDownloadLinkRegexes();
        ret.add(0, "sources\\s*?:\\s*?\\[[\"'](http[^\"']+?)[\"']");
        return ret;
    }
}