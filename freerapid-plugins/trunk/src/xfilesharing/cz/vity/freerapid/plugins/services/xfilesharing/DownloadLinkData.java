package cz.vity.freerapid.plugins.services.xfilesharing;

import org.apache.commons.httpclient.Cookie;

/**
 * @author tong2shot
 */
public class DownloadLinkData {
    private String downloadLink;
    private long created;
    private long maxAge;
    private Cookie[] cookies;

    public DownloadLinkData() {
    }

    public DownloadLinkData(String downloadLink, long created, long maxAge, Cookie[] cookies) {
        this.downloadLink = downloadLink;
        this.created = created;
        this.maxAge = maxAge;
        this.cookies = cookies;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public long getCreated() {
        return created;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public Cookie[] getCookies() {
        return cookies;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - created) > (maxAge - 60 * 1000); //maxage - 1min
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public void setCookies(Cookie[] cookies) {
        this.cookies = cookies;
    }
}
