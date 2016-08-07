package cz.vity.freerapid.plugins.services.appletrailers;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
enum VideoQuality {
    Lowest(1, "Lowest quality", "Lowest quality"),
    _480(480, "sd"),
    _720(720, "hd720"),
    _1080(1080, "hd1080"),
    Highest(10000, "Highest quality", "Highest quality");

    private final int quality;
    private final String name;
    private final String qualityToken;

    VideoQuality(int quality, String qualityToken) {
        this.quality = quality;
        this.name = quality + "p";
        this.qualityToken = qualityToken;
    }

    VideoQuality(int quality, String name, String qualityToken) {
        this.quality = quality;
        this.name = name;
        this.qualityToken = qualityToken;
    }

    public int getQuality() {
        return quality;
    }

    public String getName() {
        return name;
    }

    public String getQualityToken() {
        return qualityToken;
    }

    @Override
    public String toString() {
        return name;
    }

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}
