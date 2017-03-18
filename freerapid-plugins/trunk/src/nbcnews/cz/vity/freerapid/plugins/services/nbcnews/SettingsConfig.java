package cz.vity.freerapid.plugins.services.nbcnews;


/**
 * @author birchie
 */
public class SettingsConfig {
    private boolean highVideoQuality = false;
    private boolean downloadSubtitles = false;
    private boolean onlySubtitles = false;

    public boolean isHighVideoQuality() {  return highVideoQuality;  }
    public boolean isDownloadSubtitles() {  return downloadSubtitles;  }
    public boolean isOnlySubtitles() {  return onlySubtitles;  }

    public void setHighVideoQuality(boolean highVideoQuality) {
        this.highVideoQuality = highVideoQuality;
    }
    public void setDownloadSubtitles(boolean downloadSubtitles) {
        this.downloadSubtitles = downloadSubtitles;
    }
    public void setOnlySubtitles(boolean onlySubtitles) {
        this.onlySubtitles = onlySubtitles;
    }

}
