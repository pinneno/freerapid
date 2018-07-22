package cz.vity.freerapid.plugins.services.imdb;

/**
 * @author birchie
 */
public class SettingsConfig {
    private VideoQuality qualitySettings[] = {VideoQuality._1080, VideoQuality._720, VideoQuality._480, VideoQuality._360,  VideoQuality._240};
    private boolean highestQuality = true;
    private boolean customQuality = false;

    public boolean isCustomQuality() {
        return customQuality;
    }
    public boolean isHighestQuality() {
        return highestQuality;
    }
    public void setCustomQuality() {
        customQuality = true;
    }
    public void setHighestQuality() {
        qualitySettings[0] = VideoQuality._1080;
        qualitySettings[1] = VideoQuality._720;
        qualitySettings[2] = VideoQuality._480;
        qualitySettings[3] = VideoQuality._360;
        qualitySettings[4] = VideoQuality._240;
        customQuality = false;
        highestQuality = true;
    }
    public void setLowestQuality() {
        qualitySettings[0] = VideoQuality._240;
        qualitySettings[1] = VideoQuality._360;
        qualitySettings[2] = VideoQuality._480;
        qualitySettings[3] = VideoQuality._720;
        qualitySettings[4] = VideoQuality._1080;
        customQuality = false;
        highestQuality = false;
    }

    public void setVideoQuality(VideoQuality qualitySetting[]) {
        this.qualitySettings = qualitySetting;
    }

    public VideoQuality[] getVideoQuality() {
        return qualitySettings;
    }

}
