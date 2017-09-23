package cz.vity.freerapid.plugins.services.thevideo;

/**
 * @author birchie
 */
public class SettingsConfig {
    private VideoQuality qualitySetting = VideoQuality._360;

    public void setQualitySetting(VideoQuality qualitySetting) {
        this.qualitySetting = qualitySetting;
    }

    public VideoQuality getQualitySetting() {
        return qualitySetting;
    }

    @Override
    public String toString() {
        return qualitySetting.toString();
    }
}