package com.networkrecorder.rtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public abstract class RTPReader extends Thread {

    protected final IPacketSource input;
    protected final OutputStream output;
    protected byte[] packetBuf;

    protected int rtpPacketCount = 0;
    protected long tsPacketCount = 0;

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    public RTPReader(IPacketSource input, OutputStream ouput, int maxPacketSize) {
        this.input = input;
        this.output = ouput;
        this.packetBuf = new byte[maxPacketSize];
    }

    // Call start() to parse in a background thread.
    @Override
    public void run() {
        try {
            parse();
        } catch (Exception ex) {
            log.error("RTP Parser failed.", ex);
        }
    }

    public void parse() throws Exception {

        int packetLen = input.nextPacket(packetBuf);
        while (packetLen > 12 && rtpPacketCount < 999999999) {

            int byte0 = ByteUtil.unsignedByteToInt(packetBuf[0]);
            int byte1 = ByteUtil.unsignedByteToInt(packetBuf[1]);
            int byte2 = ByteUtil.unsignedByteToInt(packetBuf[2]);
            int byte3 = ByteUtil.unsignedByteToInt(packetBuf[3]);

            int version = (int) ((byte0 >> 6) & 0x03);
            int csrcCount = (int) (byte0 & 0x0F);
            int pType = (int) ((byte1 >> 4) & 0x07);
            int seq = (int) (byte2 << 8) + (int) (byte3);
            int dataPos = 12 + csrcCount * 4;

            // Validate the packet.
            tsPacketCount += packetLen / 188;
            int rem = (packetLen - dataPos) % 188;
            //log.info("Packet Size: " + packetLen + ": " + rem);
            if (rem != 0) {
                log.info("Invalid Packet - size: " + packetLen + " : " + rem);
            }
            for (int i = dataPos; i < packetLen; i += 188) {
                if (packetBuf[i] != 0x47) {
                    log.info("Invalid TS Packet at packet " + rtpPacketCount + "[" + i + "]");
                }
            }

            try {
                writePacket(packetBuf, dataPos, packetLen);
            } catch (Exception ex) {
                // TODO: Log exceptions? Could fill up the log quickly...
            }

            rtpPacketCount++;
            packetLen = input.nextPacket(packetBuf);
        }

        log.info("Total RTP packets read: " + rtpPacketCount);
        log.info("Total TS packets read: " + tsPacketCount);
    }

    abstract void writePacket(byte[] packetBuf, int dataPos, int packetLen) throws IOException;

    public int getRtpPacketCount() {
        return rtpPacketCount;
    }

    public long getTsPacketCount() {
        return tsPacketCount;
    }

    public void close() {
        try {
            input.close();
        } catch (Exception ex) {
        }
        try {
            output.close();
        } catch (Exception ex) {
        }
    }

}
