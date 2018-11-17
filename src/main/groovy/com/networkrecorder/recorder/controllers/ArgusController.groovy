package com.networkrecorder.recorder.controllers

import com.networkrecorder.recorder.services.RecorderService
import com.networkrecorder.recorder.util.ApiResult
import com.networkrecorder.recorder.util.LiveStreamResult
import com.networkrecorder.recorder.util.Settings
import com.networkrecorder.recorder.util.Util
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import groovy.json.JsonSlurper

@RestController
@RequestMapping('/Network/Recorder')
class ArgusController {

    @Autowired
    RecorderService recorderService

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    ////////////////////////////////////////////////////////
    // API methods
    ////////////////////////////////////////////////////////

    @RequestMapping(value = '/Ping', method = RequestMethod.GET)
    def ping() {
        log.debug("Ping called...")
        return recorderService.ping()
    }

    @RequestMapping(value = '/Status', method = RequestMethod.GET)
    def status() {
        return recorderService.getStatus()
    }

    @RequestMapping(value = '/KeepAlive', method = RequestMethod.PUT)
    def keepAlive() {
        log.debug("KeepAlive called...")
        return recorderService.keepAlive()
    }

    @RequestMapping(value = '/Initialize/{recorderId}', method = RequestMethod.PUT)
    def initialize(@PathVariable String recorderId, @RequestBody String body) {
        log.debug("Initialize called...")
        def req = new JsonSlurper().parseText(body)
        return recorderService.initialize(recorderId, req.schedulerBaseUrl)
    }

    @RequestMapping(value = '/AllocateCard', method = RequestMethod.PUT)
    def allocateCard(@RequestBody String body) {

        log.debug("AllocateCard called...")
        try {
            def req = new JsonSlurper().parseText(body)
            List alreadyAllocated = req.alreadyAllocated
            def useReversePriority = req.useReversePriority?.toString()?.equalsIgnoreCase('true')
            def logicalChannel = req.channel?.LogicalChannelNumber?.toString()
            return recorderService.allocateCard(alreadyAllocated, logicalChannel, useReversePriority)

        } catch (ex) {
            log.error("Error processing /AllocateCard", ex.getMessage())
            log.info(body)
            throw ex
        }
    }

    @RequestMapping(value = '/Recording/Start', method = RequestMethod.POST)
    def recordingStart(@RequestBody String body) {

        log.debug("Recording/Start called...")
        try {
            def req = new JsonSlurper().parseText(body)
            String schedulerBaseUrl = req.schedulerBaseUrl
            long startTime = Util.parseUtcDate(req.startTimeUtc)
            long stopTime = Util.parseUtcDate(req.stopTimeUtc)
            def channelAllocation = req.channelAllocation
            def recordingProgram = req.recordingProgram
            String baseFileName = req.suggestedBaseFileName

            return recorderService.startRecording(schedulerBaseUrl, startTime, stopTime, baseFileName,
                    channelAllocation, recordingProgram)

        } catch (ex) {
            log.error("Error processing /Recording/Start", ex)
            log.info(body)
            throw ex
        }
    }

    @RequestMapping(value = '/Recording/ValidateAndUpdate', method = RequestMethod.PUT)
    def recordingValidateAndUpdate(@RequestBody String body) {

        log.debug("Recording/ValidateAndUpdate called...")
        try {
            def req = new JsonSlurper().parseText(body)
            def channelAllocation = req.channelAllocation
            def recordingProgram = req.recordingProgram
            long stopTime = Util.parseUtcDate(req.stopTimeUtc)

            return recorderService.updateRecording(channelAllocation, recordingProgram, stopTime)

        } catch (ex) {
            log.error("Error processing /Recording/ValidateAndUpdate", ex)
            log.info(body)
            throw ex
        }
    }

    @RequestMapping(value = '/Recording/Abort', method = RequestMethod.PUT)
    def recordingAbort(@RequestBody String body) {

        log.debug("Recording/Abort called...")
        try {
            def req = new JsonSlurper().parseText(body)
            String schedulerBaseUrl = req.schedulerBaseUrl
            def recordingProgram = req.recordingProgram

            return recorderService.abortRecording(schedulerBaseUrl, recordingProgram)

        } catch (ex) {
            log.error("Error processing /Recording/Abort", ex)
            log.info(body)
            throw ex
        }
    }

    @RequestMapping(value = '/RecordingShares', method = RequestMethod.GET)
    def recordingShares() {

        log.debug("RecordingShares called...")
        return [result: Settings.recordingFolders]
    }

    @RequestMapping(value = '/TimeshiftShares', method = RequestMethod.GET)
    def timeshiftShares() {

        log.debug("TimeshiftShares called...")
        return [result: Settings.timeshiftFolders]
    }

    @RequestMapping(value = '/Live/Tune', method = RequestMethod.POST)
    def liveTune(@RequestBody String body) {

        log.debug("Live/Tune called...")
        log.debug(body)
        return [
            result: LiveStreamResult.NOT_SUPPORTED,
            stream: null
        ]
    }

    @RequestMapping(value = '/Live/KeepAlive', method = RequestMethod.PUT)
    def liveKeepAlive(@RequestBody String body) {

        log.debug("Live/KeepAlive called...")
        log.debug(body)
        return ApiResult.RESULT_FAILURE
    }

    @RequestMapping(value = '/Live/Stop', method = RequestMethod.PUT)
    def liveStop(@RequestBody String body) {

        log.debug("Live/Stop called...")
        log.debug(body)
        return ApiResult.RESULT_SUCCESS

    }

    @RequestMapping(value = '/LiveStreams', method = RequestMethod.GET)
    def liveStreams() {

        log.debug("LiveStreams called...")
        return [result: []]
    }

    @RequestMapping(value = '/ChannelsLiveState', method = RequestMethod.PUT)
    def channelsLiveState(@RequestBody String body) {

        log.debug("ChannelsLiveState called...")
        log.debug(body)
        try {
            def result = []
            def req = new JsonSlurper().parseText(body)
            req.channels.each { channel ->
                result.add(LiveStreamResult.NOT_SUPPORTED)
            }
            return [result: result]

        } catch (ex) {
            log.error("Error processing /ChannelsLiveState", ex)
            log.info(body)
            throw ex
        }
    }

    @RequestMapping(value = '/Live/TuningDetails', method = RequestMethod.PUT)
    def liveTuningDetails(@RequestBody String body) {

        log.debug("Live/TuningDetails called...")
        log.debug(body)
        return [result: null]
    }

    @RequestMapping(value = '/Live/HasTeletext', method = RequestMethod.PUT)
    def liveHasTeletext(@RequestBody String body) {

        log.debug("Live/HasTeletext called...")
        log.debug(body)
        return [result: null]
    }

    @RequestMapping(value = '/Live/Teletext/StartGrabbing', method = RequestMethod.PUT)
    def liveTeletextStartGrabbing(@RequestBody String body) {

        log.debug("Live/Teletext/StartGrabbing called...")
        log.debug(body)
        return [result: null]
    }

    @RequestMapping(value = '/Live/Teletext/StopGrabbing', method = RequestMethod.PUT)
    def liveTeletextStopGrabbing(@RequestBody String body) {

        log.debug("Live/Teletext/StopGrabbing called...")
        log.debug(body)
        return [result: null]
    }

    @RequestMapping(value = '/Live/Teletext/IsGrabbing', method = RequestMethod.PUT)
    def liveTeletextIsGrabbing(@RequestBody String body) {

        log.debug("Live/Teletext/IsGrabbing called...")
        log.debug(body)
        return [result: null]
    }

    @RequestMapping(value = '/Live/Teletext/GetPage/{pageNumber}/{subPageNumber}', method = RequestMethod.PUT)
    def liveTeletextGetPage(@PathVariable int pageNumber, @PathVariable int subPageNumber, @RequestBody String body) {

        log.debug("Live/Teletext/GetPage called...")
        log.debug(body)
        return [result: null]
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Additional API methods - mainly used for testing, not part of the standard ArgusTV API.
    //////////////////////////////////////////////////////////////////////////////////////////////

    // Use this API to capture an RTP sream for debugging. Set the Ceton tuner to a specific channel,
    // then call this API with the tuner's port number. The RTP stream wiil be captured to C:\temp\capture.ts.
    @RequestMapping(value = '/StartCapture/{port}', method = RequestMethod.GET)
    def startCapture(@PathVariable String port,
                     @RequestParam(value = "program", required = false) Integer program,
                     @RequestParam(value = "format", defaultValue = "ts") String format,
                     @RequestParam(value = "file", defaultValue = "capture.ts") String fname) {
        return recorderService.startRtpCapture(port, program, format, fname)
    }

    // Call this API to stop a capture previously started by calling /StartCapture.
    @RequestMapping(value = '/StopCapture', method = RequestMethod.GET)
    def stopCapture() {
        return recorderService.stopRtpCapture()
    }

    // Call this API to stop a capture previously started by calling /StartCapture.
    @RequestMapping(value = '/index', method = RequestMethod.GET)
    def statusPage() {
        return "main"
    }

}
