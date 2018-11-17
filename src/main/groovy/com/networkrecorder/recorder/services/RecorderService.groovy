package com.networkrecorder.recorder.services

import com.networkrecorder.recorder.RecorderApplication
import com.networkrecorder.recorder.encoders.RecordingThread
import com.networkrecorder.recorder.util.ApiResult
import com.networkrecorder.recorder.util.Encoder
import com.networkrecorder.recorder.util.Settings
import com.networkrecorder.rtp.NetworkPacketSource
import com.networkrecorder.rtp.RTPParser
import com.networkrecorder.rtp.RTPRawWriter
import com.networkrecorder.rtp.RTPReader
import com.networkrecorder.rtp.TSWriter
import com.sun.jna.platform.win32.Kernel32
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RecorderService {

    final int ES_SYSTEM_REQUIRED = 0x00000001

    @Autowired
    RecorderCallbackService recorderCallbackService

    String recorderId
    String schedulerBaseUrl
    def activeRecordings = [:]

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    def ping() {

        Set macs = []
        NetworkInterface.networkInterfaces.each { iface ->
            if (iface && iface.hardwareAddress && !iface.isLoopback() && !iface.isVirtual() && !iface.isPointToPoint()) {
                macs.add(iface.hardwareAddress?.encodeHex().toString().toUpperCase())
            }
        }
        return [
            result: RecorderApplication.RECORDER_API_VERSION,
            macAddresses: macs
        ]
    }

    def keepAlive() {

        def os = System.getProperty("os.name")
        if (os.toLowerCase().contains('win')) {
            Kernel32.INSTANCE.SetThreadExecutionState(ES_SYSTEM_REQUIRED)
            log.info("OS: ${os} - SetThreadExecutionState called")
            return ApiResult.RESULT_SUCCESS
        }
        log.info("OS: ${os} - No action taken")
        return ApiResult.RESULT_FAILURE
    }

    def initialize(String recorderId, String schedulerBaseUrl) {

        if (!schedulerBaseUrl) {
            throw new Exception('Missing schedulerBaseUrl in request body.')
        }

        this.recorderId = recorderId
        this.schedulerBaseUrl = schedulerBaseUrl
        recorderCallbackService.callRegisterRecorder(schedulerBaseUrl, recorderId)
        return ApiResult.RESULT_SUCCESS
    }


    def allocateCard(List alreadyAllocated, String logicalChannel, boolean useReversePriority) {

        if (!logicalChannel) {
            throw new Exception("Missing logical channel in request.")
        }
        if (!alreadyAllocated) alreadyAllocated = []

        //log.debug("AllocateCard searching for logical channel: ${logicalChannel}")
        def encoderList = Settings.getEncodersForChannel(logicalChannel)
        for (Encoder encoder : (useReversePriority ? encoderList : encoderList.reverse())) {
            def cardId = encoder.cardId.toString()
            if (!(alreadyAllocated.find { it.CardId == cardId })) {
                //log.debug("Allocating card: ${cardId}")
                return [result: cardId]
            }
        }

        return ApiResult.RESULT_NULL
    }

    def startRecording(String schedulerBaseUrl, long startTime, long stopTime, String baseFileName,
                       channelAllocation, recordingProgram) {

        try {
            // Validate request.
            String logicalChannel = recordingProgram?.Channel?.LogicalChannelNumber?.toString()
            def cardId = channelAllocation?.CardId
            if (!logicalChannel) {
                throw new Exception("Missing logical channel in request.")
            }
            if (!schedulerBaseUrl) {
                throw new Exception("Missing callback URL in request.")
            }
            def encoder = Settings.findEncoderByCardId(cardId)
            if (!encoder) {
                throw new Exception("Invalid or missing cardId in request.")
            }
            def physicalChannel = encoder.getPhysicalChannel(logicalChannel)
            if (!physicalChannel) {
                throw new Exception("Invalid channel ${logicalChannel} - no physical channel number.")
            }

            // Start the recording thread.
            RecordingThread recordingThread = RecordingThread.instanceOf(schedulerBaseUrl, startTime, stopTime,
                    encoder, physicalChannel, baseFileName, channelAllocation, recordingProgram,
                    this, recorderCallbackService)
            recordingThread.start()
            saveActiveRecording(recordingProgram.UpcomingProgramId, recordingThread)

            return ApiResult.RESULT_SUCCESS

        } catch (ex) {
            log.error(ex.getMessage())
        }

        return ApiResult.RESULT_FAILURE
    }

    def abortRecording(String schedulerBaseUrl, recordingProgram) {

        RecordingThread recording = getActiveRecording(recordingProgram.UpcomingProgramId)
        if (recording){
            recording.abortRecording()
            return ApiResult.RESULT_SUCCESS
        }

        return ApiResult.RESULT_FAILURE
    }

    def updateRecording(channelAllocation, recordingProgram, long stopTime) {

        RecordingThread recording = getActiveRecording(recordingProgram.UpcomingProgramId)
        if (recording){
            if (recording.updateRecording(stopTime)) {
                return ApiResult.RESULT_SUCCESS
            }
        }

        return ApiResult.RESULT_FAILURE
    }

    def getStatus() {

        def result = [:]

        result['recorder_name'] = RecorderApplication.RECORDER_NAME
        result['version'] = RecorderApplication.RECORDER_VERSION

        // Get active recordings.
        def recordings = [:]
        synchronized (activeRecordings) {
            activeRecordings.values().each { RecordingThread t ->
                recordings[t.encoder.id] = t;
            }
        }

        Settings.encoderList.each { encoder ->
            def encoderInfo = [:]
            encoderInfo['name'] = encoder.name
            RecordingThread recording = recordings[encoder.id]
            encoderInfo['status'] = (recording ? 'recording' : 'idle')
            if (recording) {
                encoderInfo['recording'] = recording.getInfo()
            }
            result["encoder-${encoder.id}"] = encoderInfo
        }

        return result
    }

    NetworkPacketSource input;
    OutputStream output;
    RTPReader parser;

    def startRtpCapture(String port, Integer program, String format, String fname) {

        input = new NetworkPacketSource("0.0.0.0", port.toInteger())
        output = new BufferedOutputStream(new FileOutputStream("C:/temp/${fname}"))
        if (format.equalsIgnoreCase("rtp")) {
            parser = new RTPRawWriter(input, output, NetworkPacketSource.MAX_PACKET_SIZE)
        } else {
            if (program == null || program >= 0) {
                output = new TSWriter(output)
                if (program != null && program >= 0) {
                    output.addProgram(program)
                }
            }
            parser = new RTPParser(input, output, NetworkPacketSource.MAX_PACKET_SIZE)
        }
        input.start()    // Start data listener thread.
        parser.start()   // Start RTP parser thread.

        log.info("RTP capture started...")
        return ApiResult.RESULT_SUCCESS
    }

    def stopRtpCapture() {

        try {
            input.stop()
        } catch (ex) { }

        try {
            parser.close()
        } catch (ex) { }
        input = null
        output = null
        parser = null

        return ApiResult.RESULT_SUCCESS
    }

    def saveActiveRecording(String programId, recordingThread) {
        synchronized (activeRecordings) {
            activeRecordings[programId] = recordingThread
        }
    }

    def getActiveRecording(String programId) {
        synchronized (activeRecordings) {
            return activeRecordings[programId]
        }
    }

    def removeActiveRecording(String programId) {
        synchronized (activeRecordings) {
            return activeRecordings.remove(programId)
        }
    }

}
