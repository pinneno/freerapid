package cz.vity.freerapid.plugins.services.pastebin;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.Utils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.URIException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author birchie
 */
public class PasteBinFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PasteBinFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    protected void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<h1>", "</h1>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            checkNameAndSize(getContentAsString());
            stepCaptcha(method);
            List<URI> list = textURIListToFileList(getLinksText());
            if (list.isEmpty()) throw new PluginImplementationException("No links found");
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, list);
            httpFile.setFileName("Link(s) Extracted !");
            httpFile.setState(DownloadState.COMPLETED);
            httpFile.getProperties().put("removeCompleted", true);
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    protected void stepCaptcha(HttpMethod method) throws Exception {
    }

    protected String getLinksText() throws PluginImplementationException {
        Matcher match = getMatcherAgainstContent("paste_code[^>]+>([^<]+)<");
        if (!match.find()) {
            throw new PluginImplementationException("Text area not found");
        }
        return match.group(1);
    }

    protected void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("This page has been removed") || contentAsString.contains("This page is no longer available")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }


    protected final static Pattern REGEXP_URL = Pattern.compile("((http|https)://)?([a-zA-Z0-9\\.\\-]+(:[a-zA-Z0-9\\.:&%\\$\\-]+)*@)?((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])|([a-zA-Z0-9\\-]+\\.)*[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,4})(:[0-9]+)?(/[^/][\\p{Lu}\\p{Ll}0-9\\[\\]\\.:,\\?\\(\\)'\\\\/\\+&%\\$#!\\|=~_\\-@]*)*", Pattern.MULTILINE);

    protected List<URI> textURIListToFileList(String data) {
        final LinkedList<URI> list = new LinkedList<URI>();
        data = data.replaceAll("(\\p{Punct}|[\\t\\n\\x0B\\f\\r])http(s)?(?!%3A%2F%2F)", "  http$2");//2 spaces
        final Matcher match = REGEXP_URL.matcher(data);
        int start = 0;
        final String http = "http://";
        final Pattern dotsEndPattern = Pattern.compile("(.+)\\.{3,}");
        while (match.find(start)) {
            try {
                String spec = match.group();
                if (!spec.startsWith(http) && !spec.startsWith("https://"))
                    spec = http + spec;

                URL url = new URL(updateApostrophs(spec));
                {
                    //support for links like http://egydental.com/vb/redirector.php?url=http%3A%2F%2Frapidshare.com%2Ffiles%2F142677856%2FImplant_volum_1.rar
                    int index = spec.indexOf("http%3A%2F%2F");
                    if (index >= 0) {
                        int endIndex = spec.indexOf('&', index);
                        if (endIndex > 0) {
                            spec = spec.substring(index, endIndex);
                        } else
                            spec = spec.substring(index);
                        spec = Utils.urlDecode(spec);
                        url = new URL(updateApostrophs(spec));
                    }
                }
                {
                    //support for links like
                    //  http://www.agaleradodownload.com/download/d/?0PPJOQ2X=d?/moc.daolpuagem.www//:ptth
                    //  http://www.zunay.com/d/?KKLZZ2OL=d?/moc.daolpuagem.www
                    int index = spec.toLowerCase(java.util.Locale.ENGLISH).indexOf("//:ptth");
                    if (index >= 0) {
                        int startIndex = spec.indexOf("url=", 0);
                        if (startIndex != -1) {
                            if (startIndex < index) {
                                spec = Utils.reverseString(spec.substring(startIndex + 4, index));
                            } else startIndex = -1;
                        } else {
                            //? has to be prioritized to =
                            startIndex = spec.indexOf('?', 0);
                            if (startIndex != -1) {
                                if (startIndex < index) {
                                    spec = Utils.reverseString(spec.substring(startIndex + 1, index));
                                } else startIndex = -1;
                            } else {
                                startIndex = spec.indexOf('=', 0);
                                if (startIndex != -1) {
                                    if (startIndex < index) {
                                        spec = Utils.reverseString(spec.substring(startIndex + 1, index));
                                    } else startIndex = -1;
                                }
                            }
                        }
                        if (startIndex != -1) {
                            spec = Utils.urlDecode("http://" + spec);
                            url = new URL(updateApostrophs(spec));
                        }
                    }
                }
                {
                    final String urlS = url.toExternalForm();
                    final int i = urlS.indexOf("...");
                    Pattern patternMatcher = null;
                    final Matcher dotsMatcher = dotsEndPattern.matcher(urlS);
                    boolean dotsEnd = dotsMatcher.matches();

                    if (i > 0 && !dotsEnd) {
                        String pattern = Pattern.quote(urlS.substring(0, i)) + ".+" + Pattern.quote(urlS.substring(i + 4));
                        patternMatcher = Pattern.compile(pattern);
                    }

                    boolean containable = false;
                    for (URI u : list) {
                        final String previouslyAdded = u.toURL().toExternalForm();
                        if (previouslyAdded.length() > urlS.length()) {
                            if (previouslyAdded.startsWith(urlS) || (patternMatcher != null && patternMatcher.matcher(previouslyAdded).matches() || (dotsEnd && previouslyAdded.startsWith(dotsMatcher.group(1))))) {
                                containable = true;
                                break;
                            }
                        }
                    }
                    if (!containable) {
                        URI uri = Utils.convertToURI(urlS);

                        if (!list.contains(uri)) {
                            list.add(uri);
                        }
                    }
                }
            } catch (MalformedURLException e) {
                //ignore
            } catch (URISyntaxException e) {
                //ignore
            } catch (URIException e) {
                //ignore
            }
            start = match.end();
        }

        return list;
    }

    protected static String updateApostrophs(String spec) {
        if (spec.endsWith("'") && spec.length() > 2) {
            spec = spec.substring(0, spec.length() - 1);
        }
        return spec;
    }

}