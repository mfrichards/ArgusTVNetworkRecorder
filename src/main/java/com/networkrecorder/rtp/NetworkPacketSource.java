package com.networkrecorder.rtp;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NetworkPacketSource implements IPacketSource {

    public static final int MAX_PACKET_SIZE = 8192;
    public static final int BUFFER_COUNT = 30;

    private static final long BUFFER_WAIT_TIME = 15000;

    private String host;
    private int port;
    private LinkedBlockingQueue<DataBuffer> fullBuffers = new LinkedBlockingQueue();
    private LinkedBlockingQueue<DataBuffer> freeBuffers = new LinkedBlockingQueue();
    private DataListener listener = null;
    private boolean stopped = false;

    public NetworkPacketSource(String host, int port) {

        this.host = host;
        this.port = port;
        for (int i = 0; i < BUFFER_COUNT; i++) {
            freeBuffers.add(new DataBuffer(MAX_PACKET_SIZE));
        }
    }

    public void start() {

        listener = new DataListener(host, port, fullBuffers, freeBuffers);
        listener.start();
    }

    public void stop() {
        listener.stopProcessing();
        stopped = true;
    }

    @Override
    public int nextPacket(byte[] outBuf) throws Exception {

        int rc = 0;
        DataBuffer buf = null;
        if (stopped) {
            buf = fullBuffers.poll();
        } else {
            buf = fullBuffers.poll(BUFFER_WAIT_TIME, TimeUnit.MILLISECONDS);
        }
        if (buf != null) {
            System.arraycopy(buf.buffer, 0, outBuf, 0, buf.length);
            rc = buf.length;
            freeBuffers.put(buf);
        }
        return rc;
    }

    @Override
    public void close() throws Exception {

    }

}
