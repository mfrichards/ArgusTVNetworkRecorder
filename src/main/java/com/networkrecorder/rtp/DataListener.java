package com.networkrecorder.rtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class DataListener extends Thread {

    private static final int BUFFER_WAIT_TIME = 1000;
    private static final int SOCKET_TIMEOUT = 10000;

    private String listenHost = "0.0.0.0";
    private int listenPort = 0;
    private boolean complete = false;
    private LinkedBlockingQueue<DataBuffer> fullBuffers;
    private LinkedBlockingQueue<DataBuffer> freeBuffers;
    private DatagramChannel channel = null;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public DataListener(String host, int port, LinkedBlockingQueue<DataBuffer> fullBuffers, LinkedBlockingQueue<DataBuffer> freeBuffers) {
        this.listenPort = port;
        this.listenHost = host;
        this.freeBuffers = freeBuffers;
        this.fullBuffers = fullBuffers;
    }

    public void stopProcessing() {

        complete = true;

        // Interrupt the socket if it is waiting on data.
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception ex) { }
        }
    }

    @Override
    public void run() {

        boolean receivingData = false;

        try {
            channel = DatagramChannel.open();
            //channel.setOption(StandardSocketOptions.SO_RCVBUF, NetworkPacketSource.MAX_PACKET_SIZE);
            channel.socket().setReceiveBufferSize(NetworkPacketSource.MAX_PACKET_SIZE);
            channel.socket().setReuseAddress(true);
            //channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(listenPort));
            ByteBuffer buffer = ByteBuffer.allocate(NetworkPacketSource.MAX_PACKET_SIZE);

            log.info("Socket listening on post: " + listenPort);

            while (!complete) {

                // Read data from the socket.
                SocketAddress result = channel.receive(buffer);
                if (result != null) {
                    buffer.flip();
                    int size = buffer.remaining();
                    if (!receivingData) {
                        receivingData = true;
                        log.info("Receiving data : " + size);
                    }

                    // Copy the data to the buffer pool. If a buffer is not available, drop the packet.
                    //DataBuffer buf = freeBuffers.poll(BUFFER_WAIT_TIME, TimeUnit.MILLISECONDS);
                    DataBuffer buf = freeBuffers.poll();
                    if (buf != null) {
                        //log.info("Writing buffer : " + size);
                        buffer.get(buf.buffer, 0, size);
                        buf.length = size;
                        fullBuffers.put(buf);
                    }
                    buffer.clear();

                } else {
                    //log.info("Receive returned null...");
                    Thread.sleep(100);
                }
            }

        } catch (AsynchronousCloseException ce) {
            log.info("Socket interrupted!");

        } catch (Exception ex) {
            log.info("Error in DataListener: " + ex.toString());
            ex.printStackTrace(System.out);

        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (Exception ex) { }
            }
        }

        log.info("Data listener complete on port: " + listenPort);
    }
}
