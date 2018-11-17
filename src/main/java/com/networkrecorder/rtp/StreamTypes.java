package com.networkrecorder.rtp;

import java.util.HashSet;
import java.util.Hashtable;

public class StreamTypes {

    static HashSet<Integer> audio = new HashSet<>();
    static HashSet<Integer> video = new HashSet<>();
    static Hashtable<Integer, String> desc = new Hashtable();

    static {
        audio.add(0x03);
        audio.add(0x04);
        audio.add(0x0F);
        audio.add(0x11);
        audio.add(0x80);
        audio.add(0x81);
        audio.add(0x83);
        audio.add(0x84);
        audio.add(0x85);
        audio.add(0x87);

        video.add(0x01);
        video.add(0x02);
        video.add(0x10);
        video.add(0x12);
        video.add(0x13);
        video.add(0x1B);
        video.add(0x24);
        video.add(0xEA);

        desc.put(0x03, "01 MPEG-1 Audio");
        desc.put(0x04, "04 MPEG-2 Audio");
        desc.put(0x0F, "0F MPEG-2 Audio");
        desc.put(0x11, "11 MPEG-4 Audio");
        desc.put(0x80, "80 PCM Audio");
        desc.put(0x81, "81 AC3 Audio");
        desc.put(0x83, "83 Dolby TrueHD Audio");
        desc.put(0x84, "84 EAC3 Audio");
        desc.put(0x85, "85 DTS Audio");
        desc.put(0x87, "87 EAC3 Audio");

        desc.put(0x01, "01 MPEG-1 Video");
        desc.put(0x02, "02 MPEG-2 Video");
        desc.put(0x10, "10 H.263 Video");
        desc.put(0x12, "12 MPEG-4 Video");
        desc.put(0x13, "13 MPEG-4 Video");
        desc.put(0x1B, "1B H.264 Video");
        desc.put(0x24, "24 H.265 Video");
        desc.put(0xEA, "EA WMV-9 Video");

        desc.put(0x82, "82 SCTE Subtitle");
        desc.put(0x86, "86 SCTE Program Insert");
        desc.put(0xC0, "C0 DigiCipher II Text");
    }

    public static boolean isAudio(int streamType) {
        return (audio.contains(streamType));
    }

    public static boolean isVideo(int streamType) {
        return (video.contains(streamType));
    }

    public static String getDesc(int streamType) {

        String d = desc.get(streamType);
        return (d == null ? Integer.toHexString(streamType).toUpperCase() : d);
    }

    public static String getMpegDesc(int mpegStreamType) {

        String code = Integer.toHexString(mpegStreamType).toUpperCase();
        if (mpegStreamType == Stream.MPEG_TYPE_AC3) {
            return code + " AC3 Audio";
        } else if (Stream.isMpegAudio(mpegStreamType)) {
            return code + " MPEG Audio";
        } else if (mpegStreamType == Stream.MPEG_TYPE_H264) {
            return code + " H264 Video";
        } else if (Stream.isMpegVideo(mpegStreamType)) {
            return code + " MPEG Video";
        }
        return code;
    }
}
