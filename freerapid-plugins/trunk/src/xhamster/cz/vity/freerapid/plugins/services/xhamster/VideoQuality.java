package cz.vity.freerapid.plugins.services.xhamster;

/**
 * @author birchie
 */

enum VideoQuality {
    _144(144),
    _240(240),
    _480(480),
    _720(720);

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
