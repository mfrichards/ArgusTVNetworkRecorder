package com.networkrecorder.rtp;

public class Stream {

    public static final int MPEG_TYPE_AC3 = 0xBD;
    public static final int MPEG_TYPE_VIDEO = 0xE0;
    public static final int MPEG_TYPE_H264 = 0xFC;   // Needed? Comcast H.264 streams use 0xE0.

    int streamType = 0;
    int mpegStreamType = 0;
    int pid = -1;
    long pidCount = 0;
    String language = null;

    public Stream() {
    }

    public Stream(int pid) {
        this.pid = pid;
    }

    @Override
    public String toString() {

        StringBuilder s = new StringBuilder();
        s.append("    Pid: ")
            .append(Integer.toHexString(pid).toUpperCase());
        if (streamType > 0) {
            s.append(", ").append(StreamTypes.getDesc(streamType));
        }
        if (mpegStreamType > 0) {
            s.append(", ").append(StreamTypes.getMpegDesc(mpegStreamType));
        }
        if (language != null) {
            s.append(", ").append(language);
        }
        return s.toString();
    }

    public boolean isAudio() {

        if (streamType > 0) {
            return StreamTypes.isAudio(streamType);
        } else if (mpegStreamType > 0) {
            return (mpegStreamType == MPEG_TYPE_AC3 || isMpegAudio(mpegStreamType));
        }
        return false;
    }

    public boolean isVideo() {

        if (streamType > 0) {
            return StreamTypes.isVideo(streamType);
        } else if (mpegStreamType > 0) {
            return (mpegStreamType == MPEG_TYPE_H264 || isMpegVideo(mpegStreamType));
        }
        return false;
    }

    public static boolean isMpegAudio(int b) {
        return ((b >>> 5) == 0x06);
    }

    public static boolean isMpegVideo(int b) {
        return ((b >>> 4) == 0x0E);
    }

}
