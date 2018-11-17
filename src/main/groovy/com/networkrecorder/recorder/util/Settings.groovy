package com.networkrecorder.recorder.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Settings {

    static recordingFolders = []
    static timeshiftFolders = []
    static encoderMap = [:]
    static encoderList = []
    static channelMaps = [:]

    static final Logger log = LoggerFactory.getLogger(Settings.class);

    static load(String settingsFile) {

        // Load the properties file.
        Properties properties = new Properties()
        File propertiesFile = new File(settingsFile)
        if (!propertiesFile.exists()) {
            propertiesFile = new File(System.getenv("PROGRAMDATA"), "ARGUS TV/Settings/${settingsFile}")
        }
        if (!propertiesFile.exists()) {
            throw new Exception("Application settings file does not exist: ${propertiesFile.getPath()}")
        }
        propertiesFile.withInputStream {
            properties.load(it)
        }

        properties.each { key, val ->
            try {
                switch (key.toLowerCase()) {

                    case 'folders.recording':
                        val.split(',').each { p ->
                            try {
                                recordingFolders.add(new File(p.trim()))
                            } catch (ex) {
                                log.error("Invalid recording folder: ${p}")
                            }
                        }
                        break

                    case 'folders.timeshift':
                        timeshiftFolders = val.split(',').collect { p -> p.trim() }
                        break

                    case { it.startsWith('encoder.') }:
                        def parts = key.split('\\.')
                        if (parts.size() == 3) {
                            Encoder encoder = getEncoder(parts[1])
                            if (encoder) {
                                encoder.setPropertyValue(parts[2], val)
                            }
                        }
                        break

                    case { it.startsWith('channelmap.') }:
                        def parts = key.split('\\.')
                        if (parts.size() == 3) {
                            ChannelMap map = getChannelMap(parts[1])
                            if (map) {
                                map.setPropertyValue(parts[2], val)
                            }
                        }
                        break
                }
            } catch (ex) {
                // Continue with the next property if an exception occurs.
            }
        }

        encoderList.sort {Encoder a, Encoder b -> a.id <=> b.id}
        log.info('Settings loaded...')

        return properties
    }

    static getEncodersForChannel(String logicalChannel) {

        return encoderList.findAll { Encoder encoder ->
            ChannelMap channelMap = channelMaps[encoder.channelMap]
            if (channelMap && channelMap.getPhysicalChannel(logicalChannel)) {
                return true
            }
            return false
        }
    }

    static findEncoderByCardId(cardId) {

        if (cardId) {
            def cardIdInt = cardId.toString().toInteger()
            for (Encoder encoder : encoderList) {
                if (encoder.cardId == cardIdInt) return encoder
            }
        }
        return null
    }

    static private getEncoder(String id) {

        def encoder = encoderMap[id]
        if (!encoder) {
            encoder = new Encoder(id)
            encoderMap.put(id, encoder)
            encoderList.add(encoder)
        }
        return encoder
    }

    static private getChannelMap(String id) {

        def map = channelMaps[id]
        if (!map) {
            map = new ChannelMap(id)
            channelMaps.put(id, map)
        }
        return map
    }
}
