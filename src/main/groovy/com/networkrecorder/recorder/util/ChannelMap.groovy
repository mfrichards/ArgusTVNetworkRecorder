package com.networkrecorder.recorder.util

class ChannelMap {

    def id
    def channels = [:]
    def ranges = []

    def ChannelMap(String id) {
        this.id = id
    }

    def setPropertyValue(String key, String val) {

        if (key.contains('~')) {
            ranges.add(new Range(key))
        } else {
            if (!val?.trim()) val = key
            channels[key] = val.trim()
        }
    }

    def getPhysicalChannel(String channel) {

        def physical = channels[channel]
        if (!physical) {
            try {
                int num = channel.toInteger()
                for (Range range : ranges) {
                    if (range.contains(num)) return channel
                }
            } catch (ex) { }
        }
        return physical
    }

    private class Range {

        int first = 0
        int last = 0

        def Range(String range) {
            try {
                def parts = range.split('\\~')
                if (parts.size() > 1) {
                    first = parts[0].toInteger()
                    last = parts[1].toInteger()
                }
            } catch (ex) { }
        }

        def contains(int num) {
            return (num >= first && num <= last)
        }
    }

}
