package cz.vity.freerapid.plugins.services.xhamster;

/**
 * @author birchie
 */
public class SettingsConfig {
    private VideoQuality qualitySettings[] = {VideoQuality._720, VideoQuality._480, VideoQuality._240, VideoQuality._144};
    private boolean customQuality = false;
    private boolean highestQuality = true;

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
        qualitySettings[0] = VideoQuality._720;
        qualitySettings[1] = VideoQuality._480;
        qualitySettings[2] = VideoQuality._240;
        qualitySettings[3] = VideoQuality._144;
        customQuality = false;
        highestQuality = true;
    }
    public void setLowestQuality() {
        qualitySettings[0] = VideoQuality._144;
        qualitySettings[1] = VideoQuality._240;
        qualitySettings[2] = VideoQuality._480;
        qualitySettings[3] = VideoQuality._720;
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
