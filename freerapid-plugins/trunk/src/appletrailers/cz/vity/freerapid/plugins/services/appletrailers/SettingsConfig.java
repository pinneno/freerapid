package cz.vity.freerapid.plugins.services.appletrailers;

/**
 * @author tong2shot
 */
public class SettingsConfig {
    private VideoQuality videoQuality = VideoQuality.Highest;
    private VideoFormat videoFormat = VideoFormat.MOV;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    public VideoFormat getVideoFormat() {
        return videoFormat;
    }

    public void setVideoFormat(VideoFormat videoFormat) {
        this.videoFormat = videoFormat;
    }

    @Override
    public String toString() {
        return "SettingsConfig{" +
                "videoQuality=" + videoQuality +
                ", videoFormat=" + videoFormat +
                '}';
    }
}
