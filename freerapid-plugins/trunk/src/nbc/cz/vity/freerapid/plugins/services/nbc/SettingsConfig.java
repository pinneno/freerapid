package cz.vity.freerapid.plugins.services.nbc;

/**
 * @author tong2shot
 */
public class SettingsConfig {
    private VideoQuality videoQuality = VideoQuality.Highest;
    private boolean downloadSubtitles = false;
    private boolean onlySubtitles = false;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }
    public boolean isDownloadSubtitles() {  return downloadSubtitles;  }
    public boolean isOnlySubtitles() {  return onlySubtitles;  }

    public void setVideoQuality(VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }
    public void setDownloadSubtitles(boolean downloadSubtitles) {
        this.downloadSubtitles = downloadSubtitles;
    }
    public void setOnlySubtitles(boolean onlySubtitles) {
        this.onlySubtitles = onlySubtitles;
    }

    @Override
    public String toString() {
        String subs = "No subtitles";
        if (downloadSubtitles) {
            subs = "With subtitles";
            if (onlySubtitles)
                subs = "Only subtitles";
        }
        return "SettingsConfig{" +
                "videoQuality=" + videoQuality + " + " + subs +
                '}';
    }
}
