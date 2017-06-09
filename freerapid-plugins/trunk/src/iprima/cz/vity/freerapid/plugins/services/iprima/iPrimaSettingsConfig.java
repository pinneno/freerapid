package cz.vity.freerapid.plugins.services.iprima;

/**
 * @author JPEXS
 * @author ntoskrnl
 */
public class iPrimaSettingsConfig {

    private VideoQuality videoQuality = VideoQuality._1080;
    private String username = null;
    private String password = null;

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

    @Override
    public String toString() {
        return "iPrimaSettingsConfig{" +
                "videoQuality=" + videoQuality +
                '}';
    }
}