package cz.vity.freerapid.plugins.services.up4ever;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Up4EverFileRunner extends XFileSharingRunner {
    @Override
    protected List<String> getDownloadPageMarkers() {
        final List<String> downloadPageMarkers = super.getDownloadPageMarkers();
        downloadPageMarkers.add("btn_downloadLink");
        downloadPageMarkers.add("Click to Download");
        return downloadPageMarkers;
    }

    protected List<String> getDownloadLinkRegexes() {
        final List<String> downloadLinkRegexes = super.getDownloadLinkRegexes();
        downloadLinkRegexes.add("<a[^<>]+href\\s*=\\s*[\"'](http[^\"']+?" + Pattern.quote(httpFile.getFileName()) + ")[\"']");
        return downloadLinkRegexes;
    }
}