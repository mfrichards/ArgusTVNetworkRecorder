package com.networkrecorder.recorder.util

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.springframework.http.MediaType

class Util {

    static long parseUtcDate(String utc) {

        int start = utc.indexOf('(')
        int end = utc.indexOf('-')
        if (end < start) {
            end = utc.indexOf(')')
        }
        return utc.substring(start + 1, end).toLong()
    }

    static String toUtcDate(long time) {
        return "/Date(${time})/"
    }

    static boolean isWindows() {
        def os = System.getProperty("os.name").toLowerCase()
        return (os.contains('win'))
    }

    static long now() {
        return (new Date()).getTime()
    }

    static String getRecordingTitle(recordingProgram) {

        if (recordingProgram.SubTitle) {
            return "${recordingProgram.Title} (${recordingProgram.SubTitle})"
        } else {
            return recordingProgram.Title
        }
    }

}
