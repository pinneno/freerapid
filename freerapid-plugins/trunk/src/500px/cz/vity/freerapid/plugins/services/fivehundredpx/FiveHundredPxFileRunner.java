package cz.vity.freerapid.plugins.services.fivehundredpx;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
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
class FiveHundredPxFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FiveHundredPxFileRunner.class.getName());

    private static final String API_URL_ID = "https://api.500px.com/v1/photos?ids=";
    private static final String API_URL_PARAMS = "&image_size%5B%5D=2048&include_states=1&expanded_user_info=true&include_tags=true&include_geo=true&is_following=true&include_equipment_info=true&include_licensing=true&include_releases=true&liked_by=1&include_vendor_photos=true";
    private String ImageID;

    private void updateFileUrl() throws Exception {
        Matcher matcher = PlugUtils.matcher("500px\\.com/photo/(\\d+)", fileURL);
        if (!matcher.find())
            throw new InvalidURLOrServiceProblemException("Error getting file ID");
        ImageID = matcher.group(1).trim();
        fileURL = API_URL_ID + ImageID + API_URL_PARAMS;
    }

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        updateFileUrl();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) {
        String name = "";
        Matcher matcher = PlugUtils.matcher("\"fullname\"\\s*:\\s*\"([^\"]+?)\"", content);
        if (matcher.find())
            name = matcher.group(1).trim();
        matcher = PlugUtils.matcher("\\]\\s*,\\s*\"name\"\\s*:\\s*\"([^\"]+?)\"", content);
        String title = "";
        if (matcher.find())
            title = matcher.group(1).trim();
        httpFile.setFileName(title + "__" + name + "__" + ImageID + ".jpg");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        updateFileUrl();
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page

            Matcher matcher = PlugUtils.matcher("\"image_url\"\\s*:\\s*\\[\\s*\"([^\"]+?)\"", contentAsString);
            if (!matcher.find())
                throw new PluginImplementationException("Image not found");

            final HttpMethod httpMethod = getGetMethod(matcher.group(1).trim());
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Sorry, no such page") || contentAsString.contains("{\"photos\":{}}")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}