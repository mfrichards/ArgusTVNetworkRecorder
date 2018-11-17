package com.networkrecorder.recorder.controllers

import groovy.json.JsonSlurper
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping('/RecorderCallback')
class CallbackMockController {

    @RequestMapping(value = '/RegisterRecorder/{recorderId}', method = RequestMethod.POST)
    def registerRecorder(@PathVariable String recorderId, @RequestBody String body) {

        def req = new JsonSlurper().parseText(body)
        return [RecorderId: recorderId, Name: req['Name'], Version: req['Version']]
    }

}
