package com.networkrecorder.rtp;

import java.io.IOException;
import java.io.OutputStream;

public class RTPParser extends RTPReader {

    public RTPParser(IPacketSource input, OutputStream ouput, int maxPacketSize) {
        super(input, ouput, maxPacketSize);
    }

    @Override
    void writePacket(byte[] packetBuf, int dataPos, int packetLen) throws IOException {
        output.write(packetBuf, dataPos, packetLen - dataPos);
    }
}
