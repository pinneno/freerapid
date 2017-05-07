package cz.vity.freerapid.plugins.services.nhk_vod;

/**
 * @author tong2shot
 */
public class NHK_vodSettingsConfig {

    private VideoQuality videoQuality = VideoQuality._720;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(final VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    @Override
    public String toString() {
        return "SettingsConfig{" +
                "videoQuality=" + videoQuality +
                '}';
    }
}