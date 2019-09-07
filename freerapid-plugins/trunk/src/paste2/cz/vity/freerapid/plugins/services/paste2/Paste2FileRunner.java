package cz.vity.freerapid.plugins.services.paste2;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.pastebin.PasteBinFileRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class Paste2FileRunner extends PasteBinFileRunner {
    private final static Logger logger = Logger.getLogger(Paste2FileRunner.class.getName());

    @Override
    protected void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    protected String getLinksText() throws PluginImplementationException {
        String links = "";
        Matcher match = getMatcherAgainstContent("line-[^>]+><div>([^<>]+)<");
        while (match.find()) {
            links += " \n " + match.group(1);
        }
        return links;
    }

    @Override
    protected void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Page Not Found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}