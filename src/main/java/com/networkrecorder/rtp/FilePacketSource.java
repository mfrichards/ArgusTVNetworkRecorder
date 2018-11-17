package com.networkrecorder.rtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

public class FilePacketSource implements IPacketSource {

    public static final int PACKET_LENGTH_HEADER_SIZE = 4;

    private InputStream stream = null;
    private boolean foundEof = false;
    private byte[] packetSizeBuf = new byte[PACKET_LENGTH_HEADER_SIZE];

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    public FilePacketSource(String fname) throws Exception {
        stream = new BufferedInputStream(new FileInputStream(fname));
    }

    @Override
    public int nextPacket(byte[] outBuf) throws Exception {

        if (foundEof) return 0;

        if (stream.read(packetSizeBuf) != PACKET_LENGTH_HEADER_SIZE) {
            foundEof = true;
            return 0;
        } else {
            int readSize = (int) ByteUtil.unsignedBytesToLong(packetSizeBuf[0], packetSizeBuf[1], packetSizeBuf[2], packetSizeBuf[3]);
            if (readSize > outBuf.length) {
                log.error("Invalid packet size: " + readSize);
                foundEof = true;
                return -1;
            }
            int actual = stream.read(outBuf, 0, readSize);
            if (actual < readSize) {
                foundEof = true;
            }
            return actual;
        }
    }

    @Override
    public void close() throws Exception {
        stream.close();
    }

}
