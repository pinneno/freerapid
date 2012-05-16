package cz.vity.freerapid.plugins.services.mediafire;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.services.recaptcha.ReCaptcha;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika, ntoskrnl
 */
class MediafireRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MediafireRunner.class.getName());

    /**
     * MediaFire asks for a captcha if it detects a lot of downloads from an IP.
     * The captcha has to be solved only once; after that, several downloads can
     * proceed normally without entering further captchas. These locks are used
     * to ensure that a captcha only has to be solved once per IP, and that
     * downloads without a captcha can still be processed in parallel.
     */
    private final static ConcurrentMap<ConnectionSettings, CaptchaState> STATES = new ConcurrentHashMap<ConnectionSettings, CaptchaState>(1);

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        if (!isFolder()) {
            final Matcher matcher = getMatcherAgainstContent("oFileSharePopup\\.ald\\('[^']+?','([^']+?)','(\\d+?)'");
            if (!matcher.find()) {
                throw new PluginImplementationException("File name/size not found");
            }
            httpFile.setFileName(matcher.group(1));
            httpFile.setFileSize(Long.parseLong(matcher.group(2)));
        }
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("The key you provided for file download")
                || content.contains("How can MediaFire help you?")
                || content.contains("File Removed for Violation")
                || content.contains("File Belongs to Suspended Account")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        while (true) {
            /**
             * Grab the current captcha state for use after the initial request.
             *
             * Getting the state and loading initial page should actually be an
             * atomic operation for strict thread safety, but getting the wrong
             * state only results in one redundant page load in the worst case.
             */
            final CaptchaState captchaState = getCaptchaState();

            final HttpMethod method = getGetMethod(fileURL);
            if (!makeRedirectedRequest(method)) {
                checkProblems();
                throw new ServiceConnectionProblemException();
            }
            checkProblems();
            if (isFolder()) {
                parseFolder();
                return;
            }
            checkNameAndSize();

            //TODO password handling

            if (!isCaptcha()) {
                break;
            }
            final Lock lock = captchaState.getLock();
            if (lock.tryLock()) {
                try {
                    final boolean alreadySolved = captchaState.setSolved();
                    /**
                     * Was the captcha solved while we were in the initial
                     * request? If so, reload the page.
                     */
                    if (!alreadySolved) {
                        /**
                         * We were the first to notice the captcha. Solve it.
                         */
                        stepCaptcha();
                        removeCaptchaState();
                        break;
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                /**
                 * Somebody else is already solving the captcha.
                 * Wait for that and reload the page.
                 */
                lock.lockInterruptibly();
                lock.unlock();
            }
        }

        final HttpMethod method = getMethodBuilder().setActionFromTextBetween("kNO = \"", "\";").toGetMethod();
        setFileStreamContentTypes("text/plain");
        if (!tryDownloadAndSaveFile(method)) {
            checkProblems();
            throw new ServiceConnectionProblemException("Error starting download");
        }
    }

    private CaptchaState getCaptchaState() {
        final CaptchaState newState = new CaptchaState();
        final CaptchaState oldState = STATES.putIfAbsent(client.getSettings(), newState);
        if (oldState == null) {
            return newState;
        } else {
            return oldState;
        }
    }

    private void removeCaptchaState() {
        STATES.remove(client.getSettings());
    }

    private static class CaptchaState {
        private final Lock lock = new ReentrantLock();
        private final AtomicBoolean solved = new AtomicBoolean();

        public Lock getLock() {
            return lock;
        }

        /**
         * Sets the state of this captcha to solved.
         * Further invocations of this method will return false.
         *
         * @return true if this invocation changed the state to solved,
         *         false if the state was already solved
         */
        public boolean setSolved() {
            return !solved.getAndSet(true);
        }
    }

    private boolean isFolder() {
        return getContentAsString().contains("<body class=\"myfiles\">");
    }

    private void parseFolder() throws Exception {
        final String id = fileURL.substring(fileURL.indexOf('?') + 1);
        final List<FolderItem> list = new LinkedList<FolderItem>();
        if (id.contains(",")) {
            for (final String s : id.split(",")) {
                list.add(new FolderItem(s, null));
            }
        } else {
            final HttpMethod method = getMethodBuilder()
                    .setAction("http://www.mediafire.com/api/folder/get_info.php")
                    .setParameter("recursive", "yes")
                    .setParameter("content_filter", "files")
                    .setParameter("folder_key", id)
                    .setParameter("response_format", "json")
                    .setParameter("version", "1")
                    .toGetMethod();
            setFileStreamContentTypes(new String[0], new String[]{"application/json"});
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
            final Matcher matcher = getMatcherAgainstContent("\"quickkey\":\"(.+?)\",\"filename\":\"(.+?)\"");
            while (matcher.find()) {
                list.add(new FolderItem(matcher.group(1), matcher.group(2)));
            }
            Collections.sort(list);
        }
        if (list.isEmpty()) {
            throw new PluginImplementationException("No links found");
        }
        final List<URI> uriList = new LinkedList<URI>();
        for (final FolderItem item : list) {
            try {
                uriList.add(new URI(item.getFileUrl()));
            } catch (final URISyntaxException e) {
                LogUtils.processException(logger, e);
            }
        }
        httpFile.getProperties().put("removeCompleted", true);
        getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
    }

    private static class FolderItem implements Comparable<FolderItem> {
        private final String fileId;
        private final String fileName;

        public FolderItem(final String fileId, final String fileName) {
            this.fileId = fileId;
            this.fileName = fileName;
        }

        public String getFileUrl() {
            return "http://www.mediafire.com/?" + fileId;
        }

        @Override
        public int compareTo(final FolderItem that) {
            return this.fileName.compareTo(that.fileName);
        }
    }

    private boolean isCaptcha() {
        return getContentAsString().contains("\"form_captcha\"");
    }

    private void stepCaptcha() throws Exception {
        while (isCaptcha()) {
            final Matcher matcher = getMatcherAgainstContent("challenge\\?k=([^\"]+)");
            if (!matcher.find()) {
                throw new PluginImplementationException("ReCaptcha key not found");
            }
            final String content = getContentAsString();
            final ReCaptcha r = new ReCaptcha(matcher.group(1), client);
            final String captcha = getCaptchaSupport().getCaptcha(r.getImageURL());
            if (captcha == null) {
                throw new CaptchaEntryInputMismatchException();
            }
            r.setRecognized(captcha);
            final HttpMethod method = r.modifyResponseMethod(getMethodBuilder(content)
                    .setReferer(fileURL)
                    .setActionFromFormByName("form_captcha", true))
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private boolean isPassworded() {
        return getContentAsString().contains("\"form_password\"");
    }

    private void stepPassword() throws Exception {
        while (isPassworded()) {
            final HttpMethod method = getMethodBuilder()
                    .setReferer(fileURL)
                    .setActionFromFormByName("form_password", true)
                    .setParameter("downloadp", getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(method)) {
                throw new ServiceConnectionProblemException();
            }
        }
    }

    private String getPassword() throws Exception {
        final String password = getDialogSupport().askForPassword("MediaFire");
        if (password == null) {
            throw new NotRecoverableDownloadException("This file is secured with a password");
        } else {
            return password;
        }
    }

}
