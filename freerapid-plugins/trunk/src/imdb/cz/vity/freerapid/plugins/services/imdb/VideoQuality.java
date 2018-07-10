package cz.vity.freerapid.plugins.services.imdb;

/**
 * @author birchie
 */

enum VideoQuality {
    _480(480),
    _720(720),
    _1080(1080);

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
