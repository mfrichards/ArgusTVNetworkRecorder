package com.networkrecorder.rtp;

public interface IPacketSource {
    int nextPacket(byte[] outBuf) throws Exception;
    void close() throws Exception;
}
