package com.networkrecorder.recorder.util

class Encoder {

    static int nextId = 1

    int cardId
    String id
    String name
    String type = 'NetworkEncoder'
    String fileType = 'ts'
    String channelMap
    int maxStreams = 1
    Map properties = [:]

    def Encoder(String id) {
        this.id = id
        this.cardId = nextId++
    }

    def setPropertyValue(key, val) {

        switch (key.toLowerCase()) {
            case 'name':
                this.name = val.trim()
                break
            case 'type':
                this.type = val.trim()
                break
            case 'filetype':
                this.fileType = val.trim()
                break
            case 'channelmap':
                this.channelMap = val.trim()
                break
            default:
                this.properties[key.toLowerCase()] = val
        }
    }

    def getPhysicalChannel(String channel) {

        ChannelMap map = Settings.channelMaps[channelMap]
        return map.getPhysicalChannel(channel)
    }

}
