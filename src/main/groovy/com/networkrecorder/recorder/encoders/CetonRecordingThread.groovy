package com.networkrecorder.recorder.encoders

import com.networkrecorder.recorder.util.Util
import com.networkrecorder.rtp.NetworkPacketSource
import com.networkrecorder.rtp.RTPParser
import com.networkrecorder.rtp.TSWriter
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.springframework.http.MediaType

class CetonRecordingThread extends RecordingThread {

    final PROPERTY_HTTP_HOST = 'httpip'
    final PROPERTY_HTTP_PORT = 'httpport'
    final PROPERTY_STREAM_HOST = 'streamip'
    final PROPERTY_STREAM_PORT = 'streamport'
    final PROPERTY_TUNER_NUMBER = 'tunernumber'

    // If the file size has not increased by this amount of time (in ms), consider the recorder
    // stalled and abort the recording.
    long encoderStalledTime = 70000

    String httpBaseUrl
    String streamHost
    int streamPort
    int instanceId

    NetworkPacketSource input
    TSWriter output
    RTPParser parser

    def CetonRecordingThread() {
    }

    @Override
    boolean onPrepareRecording() {

        def httpHost = (encoder.properties[PROPERTY_HTTP_HOST] ?: '192.168.200.1')
        def httpPort = (encoder.properties[PROPERTY_HTTP_PORT] ?: '80')
        httpBaseUrl = "http://${httpHost}:${httpPort}"
        // The "instance_id" paramter in HTTP requests is the zero-based tuner number (i.e. tunerNumber - 1).
        instanceId = (encoder.properties[PROPERTY_TUNER_NUMBER] ?: '1').toString().toInteger() - 1
        streamHost = (encoder.properties[PROPERTY_STREAM_HOST] ?: '192.168.200.2').toString()
        streamPort = (encoder.properties[PROPERTY_STREAM_PORT] ?: 8000 + instanceId).toString().toInteger()

        setChannel()

        return true
    }

    @Override
    boolean onStartRecording() {

        // Wait for channel change to complete, if necessary.
        waitForChannel()

        // Start streaming.
        startStream()

        // Start RTP capture.
        log.info("Starting stream...")
        input = new NetworkPacketSource(streamHost, streamPort)
        output = new TSWriter(new BufferedOutputStream(new FileOutputStream(recordingFile)))
        int programNumber = getProgramNumber()
        if (programNumber > 0) {
            output.addProgram(programNumber)
        }
        RTPParser parser = new RTPParser(input, output, NetworkPacketSource.MAX_PACKET_SIZE)
        input.start()    // Start data listener thread.
        parser.start()   // Start RTP parser thread.
        log.info("RTP processing threads started.")

        lastFileSize = 0
        lastFileSizeTime = Util.now()
        return true
    }

    @Override
    boolean onStopRecording() {

        try {
            input.stop()
            stopStream()
        } catch (ex) {
            log.error("Error stopping stream.")
        } finally {
            if (parser != null) {
                parser.close()   // Closes input and output streams.
            }
        }

        return true
    }

    @Override
    boolean onCheckFileSize() {

        long size = output.getOutputSize()
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

    def setChannel() {
        postRequest('channel_request.cgi', [instance_id: instanceId, channel: physicalChannel])
    }

    def waitForChannel() {

        int retries = 5
        String activeChannel = getActiveChannelNumber()
        while (activeChannel != physicalChannel && retries > 0) {
            retries--
            sleep(1000)
            activeChannel = getActiveChannelNumber()
        }
        log.info("Active Channel: ${activeChannel}")
    }

    def startStream() {

        streamRequest(1)
        sleep(200)

        // Wat for streaming to start.
        int retries = 10
        String state = getTransportState()
        while (state != 'PLAYING' && retries > 0) {
            retries--
            sleep(1000)
            state = getTransportState()
        }
        log.info("Transport State: ${state}")
    }

    def stopStream() {
        streamRequest(0)
    }

    def getActiveChannelNumber() {
        try {
            return getVar("cas", "VirtualChannelNumber")
        } catch (ex) {
            return 'UNKNOWN'
        }
    }

    def getTransportState() {
        try {
            return getVar("av", "TransportState")
        } catch (ex) {
            return 'UNKNOWN'
        }
    }

    def getProgramNumber() {
        try {
            return getVar("mux", "ProgramNumber").toInteger()
        } catch (ex) {
            return 0
        }
    }

    def streamRequest(int start) {
        postRequest('stream_request.cgi', [instance_id: instanceId, dest_ip: streamHost, dest_port: streamPort, protocol: 0, start: start])
    }

    def getVar(service, cmd) {

        def path = "get_var?i=${instanceId}&s=${service}&v=${cmd}".toString();

        def responseBody
        def http = new HTTPBuilder(httpBaseUrl)
        http.request(Method.GET) {
            uri.path = path
            headers = standardHeaders()

            response.success = { resp, html ->
                log.debug("GetVar ${path} success: ${resp.statusLine}")
                responseBody = html.BODY.text()
                log.debug("Response: ${responseBody}")

            }
            response.failure = { resp ->
                log.debug("GetVar ${path} failed: ${resp.statusLine}")
                throw new Exception("Error Response: ${httpBaseUrl}/${path}: ${resp.statusLine}")
            }
        }

        return responseBody.toString().trim()
    }

    def postRequest(String path, Map requestBody) {

        def http = new HTTPBuilder(httpBaseUrl)
        http.request(Method.POST) {
            uri.path = path
            headers = standardHeaders()
            requestContentType = MediaType.APPLICATION_FORM_URLENCODED
            body = requestBody

            response.success = { resp, reader ->
                log.debug("Callback ${path} success: ${resp.statusLine}")
                log.debug("Response: ${reader.text()}")
            }
            response.failure = { resp ->
                log.debug("Callback ${path} failed: ${resp.statusLine}")
                throw new Exception("Error on callback ${httpBaseUrl}/${path}: ${resp.statusLine}")
            }
        }
    }

    def standardHeaders() {

        def headers = [:]
        headers['User-Agent'] = 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.9) Gecko/20071025 Firefox/2.0.0.9 (.NET CLR 3.5.30729)'
        //headers['User-Agent'] = 'ArgusTVNetworkRecorder/0.1'
        headers['Accept'] = 'text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5'
        headers['Accept-Language'] = 'en-us,en;q=0.5'
        headers['Accept-Charset'] = 'ISO-8859-1,utf-8;q=0.7,*;q=0.7'
        headers['Keep-Alive'] = '300'
        headers['Connection'] = 'keep-alive'
        return headers
    }

}
