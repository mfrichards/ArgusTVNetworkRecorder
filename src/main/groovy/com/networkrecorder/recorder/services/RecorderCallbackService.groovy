package com.networkrecorder.recorder.services

import com.networkrecorder.recorder.RecorderApplication
import com.networkrecorder.recorder.util.Util
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

@Service
class RecorderCallbackService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    def String schedulerBaseUrl = null

    def setBaseUrl(String baseUrl) {
        schedulerBaseUrl = baseUrl
    }

    def callRegisterRecorder(String baseUrl, String recorderId) {

        if (!schedulerBaseUrl) schedulerBaseUrl = baseUrl
        postCallback(baseUrl, "RecorderCallback/RegisterRecorder/${recorderId}",
            [
                Name: RecorderApplication.RECORDER_NAME,
                Version: RecorderApplication.RECORDER_VERSION
            ]
        )
    }

    def callStartRecordingFailed(String baseUrl, channelAllocation, recordingProgram, String reason) {

        if (!schedulerBaseUrl) schedulerBaseUrl = baseUrl
        putCallback(baseUrl, "RecorderCallback/Recording/StartFailed",
            [
                Allocation: channelAllocation,
                RecordingProgram: recordingProgram,
                Reason: reason
            ]
        )
    }

    def callRecordingStarted(String baseUrl, recordingProgram, long startTime, String filename) {

        if (!schedulerBaseUrl) schedulerBaseUrl = baseUrl
        postCallback(baseUrl, "RecorderCallback/Recording/New",
            [
                RecordingProgram: recordingProgram,
                RecordingStartTimeUtc: Util.toUtcDate(startTime),
                RecordingFileName: filename
            ]
        )
    }

    def callEndRecording(String baseUrl, String filename, long stopTime, boolean partial, boolean allowMove) {

        if (!schedulerBaseUrl) schedulerBaseUrl = baseUrl
        putCallback(baseUrl, "RecorderCallback/Recording/End",
            [
                RecordingFileName: filename,
                RecordingStopTimeUtc: Util.toUtcDate(stopTime),
                IsPartialRecording: partial,
                OkToMoveFile: allowMove
            ]
        )
    }


    def putCallback(String baseUrl, String path, Map json) {
        postCallback(baseUrl, path, json, Method.PUT)
    }

    def postCallback(String baseUrl, String path, Map json, method = Method.POST) {

        def http = new HTTPBuilder(baseUrl ?: schedulerBaseUrl)
        http.request(method, ContentType.TEXT) {
            uri.path = path
            headers.Accept = MediaType.APPLICATION_JSON_VALUE
            requestContentType = MediaType.APPLICATION_JSON_VALUE
            body = json

            response.success = { resp, reader ->
                log.debug("Callback ${path} success: ${resp.statusLine}")
                log.debug("Response: ${reader.getText()}")
            }
            response.failure = { resp ->
                log.debug("Callback ${path} failed: ${resp.statusLine}")
                throw new Exception("Error on callback ${baseUrl}/${path}: ${resp.statusLine}")
            }
        }
    }

}
