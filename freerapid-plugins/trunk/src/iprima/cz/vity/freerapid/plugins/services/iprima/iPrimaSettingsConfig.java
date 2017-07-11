package cz.vity.freerapid.plugins.services.iprima;

import org.apache.commons.httpclient.Cookie;

/**
 * @author JPEXS
 * @author ntoskrnl
 */
public class iPrimaSettingsConfig {

    private VideoQuality videoQuality = VideoQuality._1080;
    private String username = null;
    private String password = null;
    private Cookie[] sessionCookies = null;
    private String sessionUsername = null;
    private String sessionPassword = null;
    private long sessionCreated = -1;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(final VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Cookie[] getSessionCookies() {
        return sessionCookies;
    }

    public void setSessionCookies(Cookie[] sessionCookies) {
        this.sessionCookies = sessionCookies;
    }

    public String getSessionUsername() {
        return sessionUsername;
    }

    public void setSessionUsername(String sessionUsername) {
        this.sessionUsername = sessionUsername;
    }

    public String getSessionPassword() {
        return sessionPassword;
    }

    public void setSessionPassword(String sessionPassword) {
        this.sessionPassword = sessionPassword;
    }

    public long getSessionCreated() {
        return sessionCreated;
    }

    public void setSessionCreated(long sessionCreated) {
        this.sessionCreated = sessionCreated;
    }

    @Override
    public String toString() {
        return "iPrimaSettingsConfig{" +
                "videoQuality=" + videoQuality +
                '}';
    }
}