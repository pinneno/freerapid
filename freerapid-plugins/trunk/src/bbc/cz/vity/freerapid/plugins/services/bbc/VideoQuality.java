package cz.vity.freerapid.plugins.services.bbc;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
enum VideoQuality {
    Lowest(0, 0, "Lowest available"),
    _240(240, 500),
    _360(360, 900),
    _480(480, 3000),
    _720(720, 5100),
    Highest(10000, 10000, "Highest available");

    private final int quality;
    private final int bitrate; //for HDS, in kbps
    private final String name;

    VideoQuality(int quality, int bitrate) {
        this.quality = quality;
        this.bitrate = bitrate;
        this.name = quality + "p";
    }

    VideoQuality(int quality, int bitrate, String name) {
        this.quality = quality;
        this.bitrate = bitrate;
        this.name = name;
    }

    public int getQuality() {
        return quality;
    }

    public String getName() {
        return name;
    }

    public int getBitrate() {
        return bitrate;
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
