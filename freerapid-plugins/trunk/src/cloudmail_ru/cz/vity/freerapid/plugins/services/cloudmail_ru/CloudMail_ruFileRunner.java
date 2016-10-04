package cz.vity.freerapid.plugins.services.cloudmail_ru;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.JsonMapper;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 * @since 0.9u4
 */
class CloudMail_ruFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CloudMail_ruFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            fileURL = getMethod.getURI().toString(); // /weblink/ redirected to /public/
            checkProblems();
            String fileId = getFileId(fileURL);
            JsonNode folderNode = getFolderNode(getContentAsString(), new JsonMapper().getObjectMapper());
            checkNameAndSize(isFolder(folderNode, fileId), folderNode, fileId);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(boolean isFolder, JsonNode folderNode, String fileId) throws Exception {
        String filename;
        if (!isFolder) {
            JsonNode fileNode = getSelectedListNode(folderNode, fileId);
            filename = fileNode.findPath("name").getTextValue();
            if (filename == null) {
                throw new PluginImplementationException("File name not found");
            }
            httpFile.setFileName(filename);
            long filesize = fileNode.findPath("size").getLongValue();
            if (filesize == 0) {
                throw new PluginImplementationException("File size not found");
            }
            httpFile.setFileSize(filesize);
        } else {
            filename = folderNode.findPath("name").getTextValue();
            if (filename == null) {
                throw new PluginImplementationException("File name not found");
            }
            httpFile.setFileName(filename + " >> Ready to extract");
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            fileURL = method.getURI().toString(); // /weblink/ redirected to /public/
            checkProblems();
            String fileId = getFileId(fileURL);
            ObjectMapper mapper = new JsonMapper().getObjectMapper();
            JsonNode folderNode = getFolderNode(getContentAsString(), mapper);
            boolean isFolder = isFolder(folderNode, fileId);
            checkNameAndSize(isFolder, folderNode, fileId);
            if (isFolder) {
                parseFolder(folderNode);
            } else {
                JsonNode downloadRootNode = getDownloadRootNode(getContentAsString(), mapper);
                String downloadUrl = getDownloadUrl(downloadRootNode, fileId);
                String downloadToken = downloadRootNode.findPath("tokens").findPath("download").getTextValue();
                if (downloadToken == null) {
                    throw new PluginImplementationException("Download token not found");
                }
                method = getMethodBuilder()
                        .setReferer(fileURL)
                        .setAction(downloadUrl)
                        .setParameter("key", downloadToken)
                        .toHttpMethod();
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

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("\"error\":\"not_exists\"")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkFileProblemsApi(String fileId) throws Exception {
        HttpMethod method = getMethodBuilder()
                .setReferer(fileURL)
                .setAction("https://cloud.mail.ru/api/v2/batch")
                .setAjax()
                .setParameter("weblink", fileId)
                .setParameter("batch", "[{\"method\":\"folder/tree\"},{\"method\":\"folder\"}]")
                .setParameter("sort", "{\"type\":\"name\",\"order\":\"asc\"}")
                .setParameter("api", "2")
                .setParameter("build", "hotfix_CLOUDWEB-7036_38-0-1.201609220328")
                .toPostMethod();
        if (!makeRedirectedRequest(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
        checkProblems();
    }

    private String getFileId(String fileUrl) throws MalformedURLException, UnsupportedEncodingException {
        URL url = new URL(fileUrl);
        String fileId = URLDecoder.decode(url.getPath().replaceFirst("^/public/", "").replaceFirst("/$", ""), "UTF-8");
        logger.info("File ID: " + fileId);
        return fileId;
    }

    private JsonNode getFolderNode(String content, ObjectMapper mapper) throws ErrorDuringDownloadingException {
        Matcher matcher = PlugUtils.matcher("(?s)cloudBuilder\\(.+?(\\{\\s*?\"tree\".+?),undefined.+?\\);", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting JSON content");
        }
        String jsonContent = matcher.group(1);
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(jsonContent);
        } catch (Exception e) {
            throw new PluginImplementationException("Error parsing JSON", e);
        }
        return rootNode.findPath("folder");
    }

    private JsonNode getSelectedListNode(JsonNode folderNode, String fileId) throws Exception {
        JsonNode listNodes = folderNode.findPath("list");
        if (listNodes.isMissingNode()) {
            checkFileProblemsApi(fileId);
            throw new PluginImplementationException("Error getting list node");
        }
        JsonNode selectedListNode = null;
        for (JsonNode listNode : listNodes) {
            if (listNode.findPath("id").getTextValue().equals(fileId)) {
                selectedListNode = listNode;
                break;
            }
        }
        if (selectedListNode == null) {
            throw new PluginImplementationException("Unable to select list node");
        }
        return selectedListNode;
    }

    private boolean isFolder(JsonNode folderNode, String fileId) {
        JsonNode id, kind;
        return (!folderNode.isMissingNode() &&
                ((id = folderNode.get("id")) != null && id.getTextValue().equals(fileId)) &&
                ((kind = folderNode.get("kind")) != null && kind.getTextValue().equals("folder")));
    }

    private void parseFolder(JsonNode folderNode) throws Exception {
        List<URI> list = new LinkedList<URI>();
        JsonNode listNode = folderNode.findPath("list");
        for (JsonNode listItem : listNode) {
            try {
                list.add(new URI("https://cloud.mail.ru/public/" + listItem.findPath("id").getTextValue()));
            } catch (Exception e) {
                LogUtils.processException(logger, e);
            }
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
        logger.info(list.size() + " links added");
        httpFile.setState(DownloadState.COMPLETED);
        httpFile.getProperties().put("removeCompleted", true);
    }

    private JsonNode getDownloadRootNode(String content, ObjectMapper mapper) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("(?s)window.+?(\\{\"storages\".+?\\}\\};)", content);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error getting JSON content");
        }
        String jsonContent = matcher.group(1).replace("\\x3c", "");  //remove illegal char
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(jsonContent);
        } catch (Exception e) {
            throw new PluginImplementationException("Error parsing JSON", e);
        }
        return rootNode;
    }

    private String getDownloadUrl(JsonNode downloadRootNode, String fileId) throws ErrorDuringDownloadingException {
        String webLinkGetUrl = downloadRootNode.findPath("dispatcher").findPath("weblink_get").findPath("url").getTextValue();
        if (webLinkGetUrl == null) {
            throw new PluginImplementationException("Error getting download URL");
        }
        String downloadUrl;
        try {
            downloadUrl = webLinkGetUrl + (webLinkGetUrl.endsWith("/") || fileId.startsWith("/") ? "" : "/") + URIUtil.encodePathQuery(fileId);
        } catch (URIException e) {
            throw new PluginImplementationException("Error parsing download URL", e);
        }
        return downloadUrl;
    }
}
