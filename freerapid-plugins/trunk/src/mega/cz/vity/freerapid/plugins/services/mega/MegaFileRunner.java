package cz.vity.freerapid.plugins.services.mega;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.*;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaFileRunner.class.getName());

    private static enum LinkType {
        P, N, FOLDER;

        public String parameter() {
            return toString().toLowerCase(Locale.ENGLISH);
        }
    }

    private MegaApi api;
    private String id;
    private byte[] folderKey;
    private String parentFolderId;
    private LinkType type = LinkType.P;

    private void init() throws Exception {
        fileURL = fileURL.replace("%21", "!").replace("%23", "#");
        if (id == null) {
            Matcher matcher = PlugUtils.matcher("#(N)?!([a-zA-Z\\d]{8})!([a-zA-Z\\d\\-_]{43})(?:!([a-zA-Z\\d]{8}))?", fileURL);
            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    type = LinkType.N;
                }
                id = matcher.group(2);
                api = new MegaApi(client, matcher.group(3));
                parentFolderId = matcher.group(4);
            } else {
                matcher = PlugUtils.matcher("#F!([a-zA-Z\\d]{8})!([a-zA-Z\\d\\-_]{22})(?:\\?([a-zA-Z\\d]{8}))?", fileURL);
                if (!matcher.find()) {
                    throw new PluginImplementationException("Error parsing file URL");
                }
                id = matcher.group(1);
                folderKey = Base64.decodeBase64(matcher.group(2));
                type = LinkType.FOLDER;

                final String nodeId = matcher.group(3);
                if (nodeId != null) {
                    for (final MegaNode node : getFolderLinks()) {
                        if (nodeId.equals(node.id)) {
                            fileURL = node.toString();
                            logger.info("Setting new file URL to " + fileURL);
                            folderKey = null;
                            type = LinkType.N;
                            id = node.id;
                            api = new MegaApi(client, node.key);
                            parentFolderId = node.parentFolderId;
                            return;
                        }
                    }
                    throw new URLNotAvailableAnymoreException("File not found in folder");
                }
            }
        }
    }

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        init();
        if (type != LinkType.FOLDER) {
            final String content = api.request("[{\"a\":\"g\",\"" + type.parameter() + "\":\"" + id + "\",\"ssl\":\"1\"}]", parentFolderId);
            checkNameAndSize(content);
        }
    }

    private void checkNameAndSize(final String content) throws Exception {
        try {
            Matcher matcher = PlugUtils.matcher("\"s\"\\s*:\\s*(\\d+)", content);
            if (!matcher.find()) {
                throw new PluginImplementationException("File size not found");
            }
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
            matcher = PlugUtils.matcher("\"n\"\\s*:\\s*\"(.+?)\"", content);
            if (!matcher.find()) {
                throw new PluginImplementationException("File name not found");
            }
            httpFile.setFileName(matcher.group(1));
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } catch (final Exception e) {
            logger.warning("Content from API request:\n" + content);
            throw e;
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        init();
        logger.info("Starting download in TASK " + fileURL);
        if (type == LinkType.FOLDER) {
            final List<MegaNode> list = getFolderLinks();
            final List<URI> uriList = new ArrayList<URI>(list.size());
            for (final MegaNode node : list) {
                try {
                    uriList.add(node.toUri());
                } catch (final URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
            }
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
            httpFile.getProperties().put("removeCompleted", true);
            return;
        }
        final String content = api.request("[{\"a\":\"g\",\"g\":1,\"ssl\":1,\"" + type.parameter() + "\":\"" + id + "\"}]", parentFolderId);
        checkNameAndSize(content);
        final Matcher matcher = PlugUtils.matcher("\"g\"\\s*:\\s*\"(.+?)\"", content);
        if (!matcher.find()) {
            logger.warning("Content from API request:\n" + content);
            throw new PluginImplementationException("Download URL not found");
        }
        final String url = matcher.group(1);
        //the server doesn't send Accept-Ranges, but supports resume
        setClientParameter(DownloadClientConsts.IGNORE_ACCEPT_RANGES, true);
        final HttpMethod method = getMethodBuilder()
                .setAction(url.replaceFirst("https", "http"))
                .toPostMethod();
        if (!tryDownloadAndSaveFile(method, api.getDownloadCipher(getStartPosition()))) {
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private long getStartPosition() throws Exception {
        long position = 0;
        final File storeFile = httpFile.getStoreFile();
        if (storeFile != null && storeFile.exists()) {
            position = Math.max(httpFile.getRealDownload(), 0);
            if (position != 0) {
                logger.info("Download start position: " + position);
            }
        }
        return position;
    }

    private boolean tryDownloadAndSaveFile(final HttpMethod method, final Cipher cipher) throws Exception {
        if (httpFile.getState() == DownloadState.PAUSED || httpFile.getState() == DownloadState.CANCELLED)
            return false;
        else
            httpFile.setState(DownloadState.GETTING);
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Download link URI: " + method.getURI().toString());
            logger.info("Making final request for file");
        }

        try {
            InputStream inputStream = client.makeFinalRequestForFile(method, httpFile, true);
            if (inputStream != null) {
                inputStream = new CipherInputStream(inputStream, cipher);
                logger.info("Saving to file");
                downloadTask.saveToFile(inputStream);
                return true;
            } else {
                logger.info("Saving file failed");
                return false;
            }
        } finally {
            method.abort();
            method.releaseConnection();
        }
    }

    private List<MegaNode> getFolderLinks() throws Exception {
        final HttpMethod method = new MethodBuilder(client)
                .setAction("https://g.api.mega.co.nz/cs?id=" + new Random().nextInt(0x10000000) + "&n=" + id)
                .toPostMethod();
        ((PostMethod) method).setRequestEntity(new StringRequestEntity("[{\"a\":\"f\",\"c\":\"1\",\"r\":\"1\"}]", "text/plain", "UTF-8"));
        if (!makeRedirectedRequest(method)) {
            throw new ServiceConnectionProblemException();
        }
        MegaApi.checkProblems(getContentAsString());
        final List<MegaNode> list = parseFolderContent(getContentAsString());
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        return list;
    }

    private List<MegaNode> parseFolderContent(final String content) throws Exception {
        final List<MegaNode> list = new ArrayList<MegaNode>();
        final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(folderKey, "AES"));
        final Matcher matcher = PlugUtils.matcher("\\{\\s*(\"h\".+?)\\s*\\}", content);
        while (matcher.find()) {
            final NodeData data = new NodeData(matcher.group(1));
            if (data.getField("s") != null) {
                final String nodeId = data.getField("h");
                final String nodeKey = data.getField("k");
                if (nodeId == null || nodeKey == null) {
                    throw new PluginImplementationException("Error parsing server response");
                }
                final String[] keyParts = nodeKey.split(":");
                if (keyParts.length != 2) {
                    throw new PluginImplementationException("Error parsing server response");
                }
                final String key = Base64.encodeBase64URLSafeString(cipher.doFinal(Base64.decodeBase64(keyParts[1])));
                list.add(new MegaNode(nodeId, key, id));
            }
        }
        return list;
    }

    private static class NodeData {
        private String content;

        public NodeData(final String content) {
            this.content = content;
        }

        public String getField(final String field) {
            final Matcher matcher = PlugUtils.matcher("\"" + Pattern.quote(field) + "\":\\s*?\"?(.+?)[\",]", content);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return null;
            }
        }
    }

    private static class MegaNode {
        final String id;
        final String key;
        final String parentFolderId;

        public MegaNode(final String id, final String key, final String parentFolderId) {
            this.id = id;
            this.key = key;
            this.parentFolderId = parentFolderId;
        }

        public URI toUri() throws Exception {
            return new URI(toString());
        }

        public String toString() {
            return "https://mega.nz/#N!" + id + "!" + key + "!" + parentFolderId;
        }
    }

}