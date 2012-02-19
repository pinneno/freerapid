package cz.vity.freerapid.plugins.webclient;

import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.URIUtil;

import java.io.*;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Robot to browse on the web.
 *
 * @author Vity
 * @see cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient
 */
public class DownloadClient implements HttpDownloadClient {
    /**
     * Field logger
     */
    private final static Logger logger = Logger.getLogger(DownloadClient.class.getName());

    /**
     * Field client
     *
     * @see org.apache.commons.httpclient.HttpClient
     */
    protected HttpClient client;
    /**
     * Field referer  - HTTP referer
     */
    protected String referer = "";
    /**
     * string content of last request
     */
    protected String asString;
    /**
     * checks whether redirect is used
     */
    private int redirect;
    /**
     * connection settings those are used for creating TCP/HTTP connections
     */
    private volatile ConnectionSettings settings;
    public static final String START_POSITION = "startPosition";
    public static final String SUPPOSE_TO_DOWNLOAD = "supposeToDownload";


    /**
     * Constructor - creates a new DownloadClient instance.
     */
    public DownloadClient() {
        this.client = new HttpClient();
    }

    @Override
    public void initClient(final ConnectionSettings settings) {
        if (settings == null)
            throw new NullPointerException("Internet connection settings cannot be null");
        this.settings = settings;
        final HttpClientParams clientParams = client.getParams();
        clientParams.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        clientParams.setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
        clientParams.setSoTimeout(60 * 1000);
        clientParams.setConnectionManagerTimeout(60 * 1000);

        clientParams.setHttpElementCharset("UTF-8");
        this.client.setHttpConnectionManager(new SimpleHttpConnectionManager(true));
        this.client.getHttpConnectionManager().getParams().setConnectionTimeout(60 * 1000);

        HttpState initialState = new HttpState();
        if (settings.isProxySet()) {
            HostConfiguration configuration = new HostConfiguration();
            configuration.setProxy(settings.getProxyURL(), settings.getProxyPort());
            client.setHostConfiguration(configuration);
            if (settings.getUserName() != null)
                initialState.setProxyCredentials(AuthScope.ANY, new NTCredentials(settings.getUserName(), settings.getPassword(), "", ""));
        } else client.setHostConfiguration(new HostConfiguration());

        clientParams.setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        // Get initial state object

        client.setState(initialState);
    }

    private boolean hasAuthentification() {
        if (settings == null)
            throw new IllegalStateException("Client not initialized");
        return settings.isProxySet() && settings.getUserName() != null;
    }

    /**
     * Method setDefaultsForMethod sets default header request values - emulates Mozilla Firefox
     *
     * @param method
     */
    protected void setDefaultsForMethod(HttpMethod method) {
        method.setRequestHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.0.8) Gecko/2009032609 Firefox/3.0.8 (.NET CLR 3.5.30729)");
        method.setRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        method.setRequestHeader("Accept-Language", "cs,en-us;q=0.7,en;q=0.3");
        method.setRequestHeader("Accept-Charset", "windows-1250,utf-8;q=0.7,*;q=0.7");
        //method.setRequestHeader("Accept-Charset", "utf-8, windows-1250;q=0.7,*;q=0.7");
        method.setRequestHeader("Accept-Encoding", "gzip,deflate");
        method.setRequestHeader("Keep-Alive", "30");
        if (referer != null && referer.length() > 0)
            method.setRequestHeader("Referer", referer);
        method.setFollowRedirects(false);
    }

    @Override
    public PostMethod getPostMethod(final String uri) {
        PostMethod m;
        try {
            m = new PostMethod(uri);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid URI detected for PostMethod: " + uri + " Trying to reencode ");
            try {
                m = new PostMethod(URIUtil.encodePathQuery(uri));
            } catch (URIException e1) {
                throw e;
            }
        }
        setDefaultsForMethod(m);
        m.setDoAuthentication(hasAuthentification());
        return m;
    }

    @Override
    public String getReferer() {
        return referer;
    }


    @Override
    public InputStream makeFinalRequestForFile(HttpMethod method, HttpFile file, boolean allowRedirect) throws IOException {
        if (method == null)
            throw new NullPointerException("HttpMethod cannot be null");
        if (file == null)
            throw new NullPointerException("File cannot be null");
        file.getProperties().remove(START_POSITION);
        file.getProperties().remove(SUPPOSE_TO_DOWNLOAD);
        return makeRequestFile(method, file, 0, allowRedirect);
    }

    private InputStream makeRequestFile(final HttpMethod method, final HttpFile file, final int deep, boolean allowRedirect) throws IOException {
        asString = "";
        toString(method);


        if (allowRedirect && method instanceof GetMethod) {
            method.setFollowRedirects(true); //autoredirects for GetMethod, it's not working for PostMethod
        }
        addRangeHeader(file, method);
        client.executeMethod(method);

        final int statuscode = method.getStatusCode();

        if (statuscode == HttpStatus.SC_INTERNAL_SERVER_ERROR || statuscode == HttpStatus.SC_FORBIDDEN) {//bezpecnost
            logger.severe("Status code je 500");
            updateAsString(method);
            return null;
        } else if (statuscode >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            logger.severe("Status code > 500:" + statuscode);
            updateAsString(method);
            return null;
        }

        final boolean isRedirect = isRedirect(statuscode);

        if (statuscode != HttpStatus.SC_OK && statuscode != HttpStatus.SC_PARTIAL_CONTENT && !isRedirect) { //selhalo pripojeni
            logger.warning("Loading file failed - invalid HTTP return status code:" + statuscode);
            updateAsString(method);
            return null;
        }

        if (allowRedirect && isRedirect && deep < 2) {
            Header header = method.getResponseHeader("location");
            if (header != null) {
                String newuri = header.getValue();
                if ((newuri == null) || ("".equals(newuri))) {
                    newuri = "/";
                }
                if (!newuri.contains("http://"))
                    newuri = "http://" + method.getURI().getHost() + newuri;

                logger.info("Redirect target: " + newuri);
                if (client.getParams().getBooleanParameter("useRefererWhenRedirect", false)) {
                    setReferer(newuri);
                }

                method.releaseConnection();
                GetMethod redirect = getGetMethod(newuri);
                final InputStream inputStream = makeRequestFile(redirect, file, deep + 1, allowRedirect);
                logger.info("Redirect: " + redirect.getStatusLine().toString());
                return inputStream;
// release any connection resources used by the method
            } else {
                logger.warning("Invalid redirect");
                return null;
            }
        } else {
            return processFileForDownload(method, file);
        }
    }

    private void addRangeHeader(HttpFile file, HttpMethod method) {
        if (!file.isResumeSupported())
            return;
        final File storeFile = file.getStoreFile();
        if (storeFile != null && storeFile.exists()) {
            //velikost souboru muze byt preddelana, proto bereme minimum
            final long l = Math.max(file.getRealDownload(), 0);
            if (l != 0) {
                method.addRequestHeader("Range", "bytes=" + l + "-");
            }
        }
    }

    private InputStream processFileForDownload(HttpMethod method, HttpFile file) throws IOException {
        boolean isStream = true;
        final Header contentType = method.getResponseHeader("Content-Type");
        if (contentType == null) {
            isStream = false;
            logger.warning("No Content-Type!");
        } else {
            final String value = contentType.getValue().toLowerCase(Locale.ENGLISH);
            final boolean isImage = value.startsWith("image/");
            final boolean isAudioVideo = value.startsWith("audio/") || value.startsWith("video/");
            if (!value.startsWith("application/") && !isImage && !isAudioVideo) {
                isStream = false;
                logger.warning("Suspicious Content-Type:" + contentType.getValue());
            }
        }
        final String fileName = HttpUtils.getFileName(method);
        if (fileName != null && !fileName.isEmpty()) {
            if (!client.getParams().isParameterTrue("dontUseHeaderFilename"))
                file.setFileName(fileName);
            if (client.getParams().isParameterTrue("noContentTypeInHeader"))
                isStream = true;
        } else {
            if (method.getResponseHeader("Content-Range") == null)
                logger.warning("No Content-Disposition (filename) header in file");
        }

        final String fn = file.getFileName();
        if (fn == null)
            throw new IOException("No defined file name");
        file.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(fn), "_"));

        //server sends eg. text/plain for binary data
        if (!isStream && contentType != null && client.getParams().isParameterSet("considerAsStream")) {
            final String ct = client.getParams().getParameter("considerAsStream").toString();
            if (contentType.getValue().equalsIgnoreCase(ct)) {
                logger.info("considering as stream '" + ct + "'");
                isStream = true;
            }
        }

        if (isStream) {
            final Header contentLength = method.getResponseHeader("Content-Length");
            if (contentLength == null) {
                isStream = false;
                logger.warning("No Content-Length in header");
            } else {

                final Long contentResponseLength = Long.valueOf(contentLength.getValue());
                final Header contentRange = method.getResponseHeader("Content-Range");
                if (contentRange != null) {
                    final String val = contentRange.getValue();
                    final Matcher matcher = Pattern.compile("(\\d+)-\\d+/(\\d+)").matcher(val);
                    if (matcher.find()) {
                        file.getProperties().put(START_POSITION, Long.valueOf(matcher.group(1)));
                        file.setFileSize(Long.valueOf(matcher.group(2)));
                    } else
                        file.getProperties().put(START_POSITION, 0L);
                    file.setResumeSupported(true);
                } else {
                    if (!client.getParams().isParameterTrue("ignoreAcceptRanges")) {
                        final Header acceptRangesHeader = method.getResponseHeader("Accept-Ranges");
                        if (file.isResumeSupported())
                            file.setResumeSupported(acceptRangesHeader != null && "bytes".equals(acceptRangesHeader.getValue()));
                    }
                    file.setFileSize(contentResponseLength);
                }
                file.getProperties().put(SUPPOSE_TO_DOWNLOAD, contentResponseLength);
            }
        }

        if (isStream) {
            return method.getResponseBodyAsStream();
        } else {
            logger.warning("Loading file failed");
            updateAsString(method);
        }
        return null;
    }


    @Override
    public InputStream makeRequestForFile(HttpMethod method) throws IOException {
        toString(method);
        client.executeMethod(method);

        int statuscode = method.getStatusCode();

        if (statuscode == HttpStatus.SC_OK) {
            final Header contentType = method.getResponseHeader("Content-Type");
            if (contentType == null) {
                logger.warning("No Content-Type!");
            } else {
                final String contentTypeValue = contentType.getValue();
                if (!contentTypeValue.startsWith("application/") && !contentTypeValue.startsWith("image/")) {
                    logger.warning("Suspicious Content-Type:" + contentTypeValue);
                }
            }

            Header hce = method.getResponseHeader("Content-Encoding");
            if (null != hce) {
                if ("gzip".equals(hce.getValue())) {
                    logger.info("Found gzip Stream");
                    return new GZIPInputStream(method.getResponseBodyAsStream());
                } else {
                    //better hope this never happens
                }
            }
            return method.getResponseBodyAsStream();
        } else {
            logger.warning("Loading file failed");
            updateAsString(method);
        }

        return null;
    }


    @Override
    public int makeRequest(HttpMethod method, boolean allowRedirect) throws IOException {
        //toString(method);
        asString = ""; //pro sichr aby tam nebylo nikdy null
        if (allowRedirect && method instanceof GetMethod) {
            method.setFollowRedirects(true);
        }

        client.executeMethod(method);

        int statuscode = method.getStatusCode();
        final boolean isRedirect = isRedirect(statuscode);
        if (!isRedirect)
            redirect = 0;
        if (statuscode == HttpStatus.SC_INTERNAL_SERVER_ERROR || statuscode == HttpStatus.SC_FORBIDDEN) {//bezpecnost
            logger.severe("Status code je 500");
        } else if (statuscode >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            logger.severe("Status code > 500:" + statuscode);
        }

        //TODO overeni, ze 
        if (allowRedirect && isRedirect && redirect != 1) {
            redirect = 1;
            Header header = method.getResponseHeader("location");
            if (header != null) {
                String newuri = header.getValue();
                if ((newuri == null) || ("".equals(newuri))) {
                    newuri = "/";
                }
                if (!newuri.contains("http://"))
                    newuri = "http://" + method.getURI().getHost() + newuri;

                logger.info("Redirect target: " + newuri);
                if (client.getParams().getBooleanParameter("useRefererWhenRedirect", false)) {
                    setReferer(newuri);
                }
                GetMethod redirect = getGetMethod(newuri);
                final int i = makeRequest(redirect, allowRedirect);
                logger.info("Redirect: " + redirect.getStatusLine().toString());
// release any connection resources used by the method
                return i;
            } else {
                logger.info("Invalid redirect");
                System.exit(1);
            }
        } else {
            redirect = 0;
            updateAsString(method);
        }
        // logger.info("asString = " + asString);

        method.releaseConnection();
        return statuscode;
    }

    private void updateAsString(HttpMethod method) throws IOException {
        Header hce = method.getResponseHeader("Content-Encoding");
        asString = "";
        if (null != hce) {
            if ("gzip".equals(hce.getValue())) {
                logger.info("Extracting GZIP");
                asString = inflate(method.getResponseBodyAsStream());
            } else {
                //better hope this never happens
            }
        } else {
            final InputStream bodyAsStream = method.getResponseBodyAsStream();
            if (bodyAsStream == null)
                this.asString = "";
            else
                this.asString = streamToString(bodyAsStream);
        }
    }

    private String streamToString(final InputStream in) {
        BufferedReader in2 = null;
        StringWriter sw = new StringWriter();
        char[] buffer = new char[4000];
        try {
            in2 = new BufferedReader(new InputStreamReader(in, getContentPageCharset()));
            int x;
            while ((x = in2.read(buffer)) != -1) {
                sw.write(buffer, 0, x);
            }
            return sw.toString();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error during reading content of page", e);
        } finally {
            if (in2 != null) {
                try {
                    in2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }

    /**
     * Checks if the given status code is type redirect
     *
     * @param statuscode http response status code
     * @return true, if status code is one of the redirect code
     */
    protected boolean isRedirect(int statuscode) {
        return (statuscode == HttpStatus.SC_MOVED_TEMPORARILY) ||
                (statuscode == HttpStatus.SC_MOVED_PERMANENTLY) ||
                (statuscode == HttpStatus.SC_SEE_OTHER) ||
                (statuscode == HttpStatus.SC_TEMPORARY_REDIRECT);
    }

    @Override
    public GetMethod getGetMethod(final String uri) {
        GetMethod m;
        try {
            m = new GetMethod(uri);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid URI detected for GetMethod: " + uri + " Trying to reencode ");
            try {
                m = new GetMethod(URIUtil.encodePathQuery(uri));
            } catch (URIException e1) {
                throw e;
            }
        }
        setDefaultsForMethod(m);
        m.setDoAuthentication(hasAuthentification());
        return m;
    }

    /**
     * Help method for to log
     *
     * @param method
     */
    protected void toString(HttpMethod method) {
        logger.info("===============HTTP METHOD===============");
        final String path = method.getPath();
        logger.info("path = " + path);
        final Header[] headers = method.getRequestHeaders();
        StringBuilder builder = new StringBuilder();
        for (Header header : headers) {
            if (header != null)
                builder.append(header.toString());
        }
        logger.info("header = \n" + builder.toString().trim());
        if (method instanceof PostMethod) {
            PostMethod postMethod = (PostMethod) method;
            builder = new StringBuilder();
            final NameValuePair[] parameters = postMethod.getParameters();
            for (NameValuePair pair : parameters) {
                builder.append(pair.getName()).append("=").append(pair.getValue()).append("\n");
            }
        }
        logger.info("post parameters: \n" + builder.toString().trim());
        logger.info("query string = " + method.getQueryString());
        logger.info("=========================================");
    }

    /**
     * Converts given GZIPed input stream into string. <br>
     * UTF-8 encoding is used as default.<br>
     * Shouldn't be called to file input streams.<br>
     *
     * @param in input stream which should be converted
     * @return input stream as string
     * @throws IOException when there was an error during reading from the stream
     */
    protected String inflate(InputStream in) throws IOException {
        byte[] buffer = new byte[4000];
        int b;
        GZIPInputStream gin = new GZIPInputStream(in);
        StringBuilder builder = new StringBuilder();
        while (true) {
            b = gin.read(buffer);
            if (b == -1)
                break;
            builder.append(new String(buffer, 0, b, getContentPageCharset()));
        }
        return builder.toString();
    }

    protected String getContentPageCharset() {
        final Object o = getHTTPClient().getParams().getParameter("pageCharset");
        if (o == null) {
            return "UTF-8";
        }
        return o.toString();
    }

    @Override
    public void setReferer(String referer) {
        if (referer == null)
            throw new NullPointerException("Referer cannot be null");
        logger.info("Setting referer to " + referer);
        this.referer = referer;
    }

    @Override
    public ConnectionSettings getSettings() {
        return settings;
    }

    @Override
    public HttpClient getHTTPClient() {
        return client;
    }

    @Override
    public String getContentAsString() {
        return asString;
    }
}