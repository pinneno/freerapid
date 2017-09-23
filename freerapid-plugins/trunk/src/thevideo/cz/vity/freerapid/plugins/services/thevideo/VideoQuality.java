package cz.vity.freerapid.plugins.services.thevideo;

/**
 * @author birchie
 */

enum VideoQuality {
    _240(240),
    _360(360),
    _480(480);

    private final int quality;

    VideoQuality(int quality) {
        this.quality = quality;
    }

    public int getQuality() {
        return quality;
    }

    @Override
    public String toString() {
        return quality + "p";
    }
}
