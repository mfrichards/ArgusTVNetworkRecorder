package com.networkrecorder.rtp;

import java.io.IOException;
import java.io.OutputStream;

public class RTPRawWriter extends RTPReader {

    private byte[] packetLenBuf = new byte[4];

    public RTPRawWriter(IPacketSource input, OutputStream ouput, int maxPacketSize) {
        super(input, ouput, maxPacketSize);
    }

    @Override
    void writePacket(byte[] packetBuf, int dataPos, int packetLen) throws IOException {
        ByteUtil.intToBytes(packetLen, packetLenBuf, 0);
        output.write(packetLenBuf, 0, 4);
        output.write(packetBuf, 0, packetLen);
    }
}
