package cz.vity.freerapid.plugins.services.nhk_vod;

/**
 * @author tong2shot
 */
enum VideoQuality {
    _720(720, 1500),
    _360(360, 800),
    _240_1600(240, 400);

    private int quality;
    private int bitrate; //Kbps
    private String name;

    VideoQuality(int quality, int bitrate) {
        this.quality = quality;
        this.bitrate = bitrate;
        this.name = quality + "p (" + bitrate + " kbps)";
    }

    public int getQuality() {
        return quality;
    }

    public int getBitrate() {
        return bitrate;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
