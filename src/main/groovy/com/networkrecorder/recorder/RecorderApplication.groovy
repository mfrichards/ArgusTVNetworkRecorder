package com.networkrecorder.recorder


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import com.networkrecorder.recorder.util.Settings

@SpringBootApplication
class RecorderApplication {

    static final String RECORDER_NAME = 'Network Recorder'
    static final String RECORDER_VERSION = '1.0'
    static final int REST_API_VERSION = 67
    static final int RECORDER_API_VERSION = 1

    static context

    static void main(String[] args) {

		context = SpringApplication.run(RecorderApplication, args)
        try {
            Properties props
            if (args.length > 0) {
                Settings.load("${args[0].trim()}.properties")
            } else {
                Settings.load("NetworkRecorder.properties")
            }
        } catch (ex) {
            stop(ex.toString())
        }
	}

    static stop(String msg) {
        if (msg) println(msg)
        println('Shutting down...')
        context.close()
    }
}
