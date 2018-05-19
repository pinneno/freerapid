package cz.vity.freerapid.plugins.services.applehls;

import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.util.URIUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author tong2shot
 */
class HlsPlaylist {

    private static final Logger logger = Logger.getLogger(HlsPlaylist.class.getName());

    private final List<HlsMedia> mediaList;
    private final boolean master;
    private Crypto crypto;

    public HlsPlaylist(final HttpDownloadClient client, final String playlistUrl, final boolean master, final int bandwidth, final int quality) throws IOException {
        try {
            this.master = master;
            final List<HlsMedia> mediaList = getMediaList(client, playlistUrl, bandwidth, quality);
            if (mediaList.isEmpty()) {
                throw new IOException("No media found");
            }
            logger.info("Found " + (!master ? "segment " : "") + "media list: " + mediaList.toString());
            this.mediaList = Collections.unmodifiableList(mediaList);
        } catch (final Exception e) {
            throw new IOException("Failed to parse playlist", e);
        }
    }

    public HlsPlaylist(final HttpDownloadClient client, final String playlistUrl) throws IOException {
        this(client, playlistUrl, true, 0, 0);
    }


    private List<HlsMedia> getMediaList(final HttpDownloadClient client, String playlistUrl, int bandwidth, int quality) throws Exception {
        logger.info("Playlist URL: " + playlistUrl);
        final HttpMethod method = client.getGetMethod(playlistUrl);
        if (client.makeRequest(method, true) != HttpStatus.SC_OK) {
            throw new ServiceConnectionProblemException();
        }
        playlistUrl = method.getURI().toString();
        logger.info("New playlist URL: " + playlistUrl);

        final String content = client.getContentAsString();
        final Scanner scanner = new Scanner(content);
        final List<HlsMedia> mediaList = new ArrayList<HlsMedia>();
        crypto = new Crypto(getBaseUrl(playlistUrl));
        Matcher matcher;
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (master) { //master playlist
                if (line.contains("BANDWIDTH")) {
                    quality = 0;
                    matcher = PlugUtils.matcher("BANDWIDTH=(\\d+)", line);
                    if (!matcher.find()) {
                        throw new IOException("Error parsing bandwidth");
                    }
                    bandwidth = Integer.parseInt(matcher.group(1)) / 1000;
                    matcher = PlugUtils.matcher("RESOLUTION=(\\d+)x(\\d+)", line);
                    if (matcher.find()) {
                        quality = Integer.parseInt(matcher.group(2));
                    }

                    try {
                        line = scanner.nextLine();
                    } catch (Exception e) {
                        continue;
                    }
                    if (line.startsWith("#"))
                        continue;
                    mediaList.add(new HlsMedia(getUrl(playlistUrl, line), bandwidth, quality));
                }
            } else { //segments playlist
                if ((line.length() > 0) && line.contains("#EXT-X-KEY")) {
                    crypto.updateKeyString(line);
                    logger.info("Current key: " + crypto.getCurrentKey());
                    logger.info("Current IV: " + crypto.getCurrentIV());
                }
                if ((line.length() > 0) && (!line.startsWith("#"))) {
                    mediaList.add(new HlsMedia(getUrl(playlistUrl, line), bandwidth, quality));
                }
            }
        }
        return mediaList;
    }

    private String getUrl(final String playlistUrl, final String url) throws Exception {
        String ret;
        try {
            ret = new URI(playlistUrl).resolve(new URI(url)).toString();
        } catch (URISyntaxException e) {
            logger.warning("Invalid URI detected: " + playlistUrl + ". Trying to reencode");
            ret = new URI(URIUtil.encodePathQuery(playlistUrl)).resolve(new URI(url)).toString();
        }
        return ret;
    }

    private String getBaseUrl(String urlString) {
        int index = urlString.lastIndexOf('/');
        return urlString.substring(0, ++index);
    }

    public boolean isMaster() {
        return master;
    }

    public List<HlsMedia> getMediaList() {
        return mediaList;
    }

    public Crypto getCrypto() {
        return crypto;
    }

}
