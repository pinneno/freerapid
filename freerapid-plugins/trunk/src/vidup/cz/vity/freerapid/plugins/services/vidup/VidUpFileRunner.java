package cz.vity.freerapid.plugins.services.vidup;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 * @author tong2shot
 */
class VidUpFileRunner extends XFilePlayerRunner {

    @Override
    protected MethodBuilder getXFSMethodBuilder(String content) throws Exception {
        MethodBuilder mb = super.getXFSMethodBuilder(content);
        Matcher matcher = PlugUtils.matcher("'gfk',\\s*?value:\\s*?'([^']+)'", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("'gfk' parameter not found");
        }
        String gfk = matcher.group(1);

        matcher = PlugUtils.matcher("'_vhash',\\s*?value:\\s*?'([^']+)'", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("'_vhash' parameter not found");
        }
        String _vhash = matcher.group(1);

        mb.setParameter("gfk", gfk);
        mb.setParameter("_vhash", _vhash);
        mb.setParameter("imhuman", "");
        return mb;
    }

    @Override
    protected List<String> getDownloadPageMarkers() {
        List<String> result = super.getDownloadPageMarkers();
        result.add(0, "VideoPlayer = jwplayer(\"vplayer\")");
        return result;
    }

    @Override
    protected List<String> getDownloadLinkRegexes() {
        List<String> result = super.getDownloadLinkRegexes();
        result.add(0, "sources\\s*?:\\s*?\\[\\{\"file\":\"(http[^\"]+)\""); //avoid getting subtitle
        return result;
    }
}