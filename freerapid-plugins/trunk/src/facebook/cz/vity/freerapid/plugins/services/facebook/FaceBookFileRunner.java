package cz.vity.freerapid.plugins.services.facebook;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 * @author tong2shot
 */
class FaceBookFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FaceBookFileRunner.class.getName());
    private static boolean isLoggedIn = false;
    private static Cookie[] cookies;

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".facebook.com", "locale", "en_US", "/", 86400, false));
        addCookie(new Cookie(".facebook.com", "datr", "ABCDEFG", "/", 86400, false)); //so we can get full items in album, instead of 28 items
        eatCookies();
        client.setReferer(fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (getContentAsString().contains("content is currently unavailable")) {
                login();
                eatCookies(); // to make sure other threads eat cookies
                method = getGetMethod(fileURL);
                if (!makeRedirectedRequest(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException();
                }
                if (getContentAsString().contains("content is currently unavailable")) {
                    throw new URLNotAvailableAnymoreException("The link may have expired, or you may not have permission");
                }
                checkProblems();
            }
            if (isAlbumUrl()) {
                processAlbum();
                return;
            }
            if (getContentAsString().contains("\"videoData\":[{") || getContentAsString().contains("videoData:[{")) { //video
                String videoId = getVideoId();
                if (getContentAsString().contains("\"status\":\"invalid\"")) {
                    throw new URLNotAvailableAnymoreException("This video either has been removed or is not visible due to privacy settings");
                }
                final String content = getContentAsString();
                Matcher matcher = PlugUtils.matcher("(?s)<title id=\"pageTitle\">(.+?)(?:\\| Facebook)?</title>", content);
                if (!matcher.find()) {
                    throw new PluginImplementationException("File name not found");
                }
                String name = matcher.group(1).trim();
                if (name.length() > 100) {
                    name = name.substring(0, 100);
                }
                httpFile.setFileName(name + ".mp4");

                String videoDataContent = null;
                Matcher videoDataMatcher = PlugUtils.matcher("\"?videoData\"?:\\[\\{(.+?)\\}\\]", content);
                while (videoDataMatcher.find()) {
                    Matcher videoIdMatcher = PlugUtils.matcher(String.format("\"?video_id\"?\\s*?:\\s*?\"%s\"", videoId), videoDataMatcher.group(1));
                    if (videoIdMatcher.find()) {
                        videoDataContent = unescapeUnicode(videoDataMatcher.group(1));
                        break;
                    }
                }
                if (videoDataContent == null) {
                    throw new PluginImplementationException("Video data content not found");
                }

                List<FacebookVideoPattern> videoPatternList = new ArrayList<FacebookVideoPattern>();
                // If the no_ratelimit version is not present, then that quality is very likely not available at all.
                // For example, if there is no hd_src_no_ratelimit and you download the hd_src, it will be 480p without audio.
                videoPatternList.add(new FacebookVideoPattern("\"?hd_src_no_ratelimit\"?\\s*?:\\s*?\"(http[^\"]+)\"", VideoQuality.HD));
                //videoPatternList.add(new FacebookVideoPattern("\"?hd_src\"?\\s*?:\\s*?\"(http[^\"]+)\"", VideoQuality.HD));
                videoPatternList.add(new FacebookVideoPattern("\"?sd_src_no_ratelimit\"?\\s*?:\\s*?\"(http[^\"]+)\"", VideoQuality.SD));
                //videoPatternList.add(new FacebookVideoPattern("\"?sd_src\"?\\s*?:\\s*?\"(http[^\"]+)\"", VideoQuality.SD));
                List<FacebookVideo> facebookVideos = new ArrayList<FacebookVideo>();
                for (FacebookVideoPattern facebookVideoPattern : videoPatternList) {
                    matcher = PlugUtils.matcher(facebookVideoPattern.pattern, videoDataContent);
                    if (matcher.find()) {
                        FacebookVideo video = new FacebookVideo(facebookVideoPattern.videoQuality, matcher.group(1).replace("\\/", "/"));
                        logger.info("Found video: " + video);
                        facebookVideos.add(video);
                    }
                }
                if (facebookVideos.isEmpty()) {
                    throw new PluginImplementationException("No available videos");
                }

                boolean succeed = false;
                for (FacebookVideo facebookVideo : facebookVideos) {
                    method = getMethodBuilder().setReferer(fileURL).setAction(facebookVideo.url).toGetMethod();
                    if (tryDownloadAndSaveFile(method)) {
                        succeed = true;
                        break;
                    } else {
                        logger.warning("Failed to download " + facebookVideo);
                    }
                }
                if (!succeed) {
                    logger.warning(content);
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            } else { //pic
                final MethodBuilder methodBuilder;
                //language cookie doesn't seem to work, search link from regex, instead of grabbing link that contains "Download" token.
                Matcher matcher = getMatcherAgainstContent("<a class=\"fbPhotosPhotoActionsItem\" href=\"(https?://[^>]+?(?:akamaihd\\.net|fbcdn\\.net)/.+?)\"");
                if (matcher.find()) {
                    methodBuilder = getMethodBuilder()
                            .setReferer(fileURL)
                            .setAction(matcher.group(1));
                } else {
                    methodBuilder = getMethodBuilder()
                            .setReferer(fileURL)
                            .setActionFromImgSrcWhereTagContains("fbPhotoImage");
                }
                matcher = PlugUtils.matcher("https?://.+?/([^/]+?)(?:\\?.+?)?$", methodBuilder.getAction());
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error parsing picture url");
                }
                httpFile.setFileName(matcher.group(1));
                method = methodBuilder.toGetMethod();
                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    throw new ServiceConnectionProblemException("Error starting download");
                }
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String getVideoId() throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("(?:video\\.php\\?v=|/videos/(?:vb\\.\\d+/)?)(\\d+)(?:/|&|$)", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Video ID not found");
        }
        return matcher.group(1);
    }

    //If already logged-in, add the cookies. This way we only have to login once for entire FRD session (until we close FRD)
    private void eatCookies() {
        synchronized (FaceBookFileRunner.class) {
            if (isLoggedIn && (cookies.length > 0)) {
                for (Cookie cookie : cookies) {
                    addCookie(cookie);
                }
            }
        }
    }

    //Login is optional, if the content is public then we don't have to login. If content is detected as private then we have to login.
    //Once login, will stay logged-in for entire FRD session, until FRD is closed that is.
    private void login() throws Exception {
        synchronized (FaceBookFileRunner.class) {
            if (isLoggedIn) return; //receiving isLoggedIn signal from other thread
            logger.info("Entering login subroutine...");
            FaceBookServiceImpl service = (FaceBookServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet()) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new BadLoginException("No FaceBook account login information!");
                }
            }
            HttpMethod method = getGetMethod("https://www.facebook.com/login.php");
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            method = getMethodBuilder()
                    .setActionFromFormByIndex(1, true)
                    .setReferer(method.getURI().toString())
                    .setParameter("email", pa.getUsername())
                    .setParameter("pass", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(method))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("Incorrect username") || getContentAsString().contains("The password you entered is incorrect") || getContentAsString().contains("Incorrect Email"))
                throw new BadLoginException("Invalid FaceBook account login information!");
            isLoggedIn = true;
            cookies = getCookies();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        //
    }

    private boolean isAlbumUrl() {
        return fileURL.matches("https?://(?:www\\.)?facebook\\.com/(media/set/.+|.+?/videos|video/\\?id=.+)");
    }

    private void processAlbum() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("href=\"(https?://(?:www\\.)?facebook\\.com/(?:photo\\.php\\?[^#]+?|video/video\\.php\\?[^#]+?))\"");
        final List<URI> uriList = new LinkedList<URI>();
        while (matcher.find()) {
            URI uri = new URI(PlugUtils.unescapeHtml(matcher.group(1)));
            if (!uriList.contains(uri)) {
                uriList.add(uri);
            }
        }
        if (uriList.isEmpty()) {
            throw new PluginImplementationException("No picture/video links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        httpFile.getProperties().put("removeCompleted", true);
        logger.info(String.valueOf(uriList.size()));
    }

    private String unescapeUnicode(final String str) throws PluginImplementationException, UnsupportedEncodingException {
        final StringBuilder buf = new StringBuilder();
        for (int i = 0, len = str.length(); i < len; i++) {
            char c = str.charAt(i);
            label0:
            switch (c) {
                case '\\':
                    if (i == str.length() - 1) {
                        buf.append('\\');
                        break;
                    }
                    c = str.charAt(++i);
                    switch (c) {
                        case 'n':
                            buf.append('\n');
                            break label0;
                        case 't':
                            buf.append('\t');
                            break label0;
                        case 'r':
                            buf.append('\r');
                            break label0;
                        case 'u':
                            int value = 0;
                            for (int j = 0; j < 4; j++) {
                                c = str.charAt(++i);
                                switch (c) {
                                    case '0':
                                    case '1':
                                    case '2':
                                    case '3':
                                    case '4':
                                    case '5':
                                    case '6':
                                    case '7':
                                    case '8':
                                    case '9':
                                        value = ((value << 4) + c) - 48;
                                        break;
                                    case 'a':
                                    case 'b':
                                    case 'c':
                                    case 'd':
                                    case 'e':
                                    case 'f':
                                        value = ((value << 4) + 10 + c) - 97;
                                        break;
                                    case 'A':
                                    case 'B':
                                    case 'C':
                                    case 'D':
                                    case 'E':
                                    case 'F':
                                        value = ((value << 4) + 10 + c) - 65;
                                        break;
                                    default:
                                        throw new PluginImplementationException("Malformed \\uxxxx encoding: " + str);
                                }
                            }
                            buf.append(URLEncoder.encode(String.valueOf((char) value), "UTF-8"));
                            break;
                        default:
                            buf.append(c);
                            break;
                    }
                    break;
                default:
                    buf.append(c);
                    break;
            }
        }
        return buf.toString();
    }


    enum VideoQuality {
        SD(480),
        HD(720);
        private int quality;

        VideoQuality(int quality) {
            this.quality = quality;
        }
    }

    private class FacebookVideoPattern {
        private final String pattern;
        private final VideoQuality videoQuality;

        public FacebookVideoPattern(String pattern, VideoQuality videoQuality) {
            this.pattern = pattern;
            this.videoQuality = videoQuality;
        }
    }

    private class FacebookVideo implements Comparable<FacebookVideo> {
        private final VideoQuality videoQuality;
        private final String url;

        public FacebookVideo(final VideoQuality videoQuality, final String url) {
            this.videoQuality = videoQuality;
            this.url = url;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int compareTo(final FacebookVideo that) {
            return Integer.valueOf(videoQuality.quality).compareTo(that.videoQuality.quality);
        }

        @Override
        public String toString() {
            return "FacebookVideo{" +
                    "videoQuality=" + videoQuality +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

}