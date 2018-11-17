package com.networkrecorder.recorder.controllers

import com.networkrecorder.recorder.services.RecorderService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@Controller
@RequestMapping('/UI')
class UiController {

    @Autowired
    RecorderService recorderService

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @GetMapping('/status')
    def status(Model model) {

        def encoders = []

        def result = recorderService.getStatus()
        result.each {key, val ->
            if (key.toString().startsWith('encoder')) {
                def encoder = [:]
                encoder['name'] = val.name
                encoder['status'] = val.status
                if (val.recording) {
                    encoder['program'] = val.recording.program
                    encoder['channel'] = val.recording.channel_name
                    encoder['physical_channel'] = val.recording.physical_channel
                    encoder['start_time'] = val.recording.start_time
                    encoder['stop_time'] = val.recording.stop_time
                    encoder['actual_start_time'] = val.recording.actual_start_time
                    encoder['file'] = val.recording.file
                    encoder['size'] = val.recording.size
                }
                encoders.add(encoder)
            }
        }

        model.addAttribute('encoders', encoders)
        return "status"
    }

}
