package com.networkrecorder.rtp;

public class DataBuffer {

    public byte[] buffer;
    public int length;

    public DataBuffer(int capacity) {
        buffer = new byte[capacity];
        length = 0;
    }
}
