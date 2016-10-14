package cz.vity.freerapid.plugins.services.wipfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;

import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class WipFilesFileRunner extends XFileSharingRunner {

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("id=\"countdown\".*?<span.*?>.*?(\\d+).*?</span");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }

}
