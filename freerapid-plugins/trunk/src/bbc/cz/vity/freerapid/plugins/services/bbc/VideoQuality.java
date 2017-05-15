package cz.vity.freerapid.plugins.services.bbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author tong2shot
 */
enum VideoQuality {
    Lowest(0, 0, StreamType.HDS, 25, "Lowest available"),
    _176(176, 276, StreamType.RTMP, 25),
    _216(216, 377, StreamType.HDS, 25),
    _224(224, 396, StreamType.RTMP, 25),
    _288(288, 533, StreamType.HDS, 25),
    _360_480(360, 480, StreamType.RTMP, 25),
    _360_796(360, 796, StreamType.RTMP, 25),
    _396_923(396, 923, StreamType.HDS, 25),
    _396_1700(396, 1700, StreamType.HDS, 50),
    _468(468, 1400, StreamType.HDS, 25),
    _480(480, 1500, StreamType.RTMP, 25),
    _540_1700(540, 1700, StreamType.HDS, 25),
    _540_3908(540, 3908, StreamType.HDS, 50),
    _720_2800(720, 2800, StreamType.RTMP, 25),
    _720_5100(720, 5100, StreamType.HDS, 50),
    Highest(10000, 10000, StreamType.HDS, 50, "Highest available");

    private final int quality;
    private final int bitrate;
    private final StreamType streamType;
    private final int fps;
    private final String name;

    VideoQuality(int quality, int bitrate, StreamType streamType, int fps) {
        this.quality = quality;
        this.bitrate = bitrate;
        this.streamType = streamType;
        this.fps = fps;
        this.name = quality + "p (" + bitrate + " kbps, " + fps + " fps)";
    }

    VideoQuality(int quality, int bitrate, StreamType streamType, int fps, String name) {
        this.quality = quality;
        this.bitrate = bitrate;
        this.streamType = streamType;
        this.fps = fps;
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

    public int getFps() {
        return fps;
    }

    @Override
    public String toString() {
        return name;
    }

    public static VideoQuality[] getItemsFor(StreamType streamType) {
        final VideoQuality[] temp = values();
        final List<VideoQuality> items = new ArrayList<VideoQuality>();
        Arrays.sort(temp, Collections.reverseOrder());
        for (VideoQuality videoQuality : temp) {
            if (videoQuality.streamType == streamType || videoQuality.quality == 0 || videoQuality.quality == 10000) {
                items.add(videoQuality);
            }
        }
        return items.toArray(new VideoQuality[items.size()]);
    }

}
