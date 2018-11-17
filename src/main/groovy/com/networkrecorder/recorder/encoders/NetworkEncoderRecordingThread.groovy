package com.networkrecorder.recorder.encoders

import com.networkrecorder.recorder.util.Util

class NetworkEncoderRecordingThread extends RecordingThread {

    final PROPERTY_HOST = 'host'
    final PROPERTY_PORT = 'port'

    // If the file size has not increased by this amount of time, consider the recorder
    // stalled and abort the recording.
    long encoderStalledTime = 70000

    def NetworkEncoderRecordingThread() {
    }

    @Override
    boolean onPrepareRecording() {

        // Check that the recorder is alive and responding.
        String cmd = "NOOP"
        String response = sendToEncoder(cmd)
        return (response.startsWith('OK'))
    }

    @Override
    boolean onStartRecording() {

        // START sourceName|channel|durationMilliseconds|filename|encodingQuality\r\n
        String cmd = "START ${encoder.id}|${physicalChannel}|0|${recordingFile.getPath()}|Normal"
        String response = sendToEncoder(cmd)
        boolean result = (response.startsWith('OK'))
        lastFileSize = 0
        lastFileSizeTime = Util.now()
        return result
    }

    @Override
    boolean onStopRecording() {

        // Stop the recording.
        String cmd = "STOP"
        String response = sendToEncoder(cmd)
        return (response.startsWith('OK'))
    }

    @Override
    boolean onCheckFileSize() {

        String cmd = "GET_FILE_SIZE ${recordingFile.getPath()}"
        String response = sendToEncoder(cmd)
        long size = (response?.isNumber() ? response.toLong() : 0)
        log.info("File Size: ${size}")
        if (size - lastFileSize > 0) {
            lastFileSize = size
            lastFileSizeTime = Util.now()
        } else {
            // Return false if the recorder is stalled.
            if (Util.now() - lastFileSizeTime > encoderStalledTime) {
                return false
            }
        }

        // Return true to continue recording.
        return true
    }

    def sendToEncoder(String cmd) {
        send(encoder.properties[PROPERTY_HOST], encoder.properties[PROPERTY_PORT].toInteger(), cmd)
    }

    def send(String host, int port, String msg) {

        def response

        // Synchronize on the encoder object so only one command at a time will be sent
        // to each encoder.
        synchronized (encoder) {
            def socket
            try {
                log.info("Sending to encoder: ${msg}")
                socket = new Socket(host, port)
                socket.withStreams { input, output ->
                    output << msg
                    output.flush()
                    response = input.newReader().readLine()
                }
                log.info("Received response: ${response}")
            } catch (ex) {
                log.error("Error sending request.", ex)

            } finally {
                if (socket) {
                    socket.close()
                }
            }
        }

        return response
    }
}
