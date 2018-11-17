package com.networkrecorder.rtp

import org.junit.Assert
import org.junit.Test

class RTPReaderTest {

    public static final int STREAM_MPEG2_VIDEO = 0x02;
    public static final int STREAM_H264_VIDEO = 0x1B;
    public static final int STREAM_AC3_AUDIO = 0x81;

    @Test
    public void test_1080i_mpeg2() {

        def programNumber = 2
        def expectedProgramCount = 2
        def expectedRtpPackets = 9614
        def expectedTsPackets = 403788

        def input = new FilePacketSource("data/1080i_mpeg2_ac3.rtp")
        def output = new TSWriter(new BufferedOutputStream(new FileOutputStream("out/1080i_mpeg2_ac3.ts")))
        output.addProgram(programNumber)
        def parser = new RTPParser(input, output, NetworkPacketSource.MAX_PACKET_SIZE)
        parser.start()   // Start RTP parser thread.
        Thread.sleep(2000)

        // Validate packet counts.
        output.printStats()
        Assert.assertEquals(expectedRtpPackets, parser.getRtpPacketCount())
        Assert.assertEquals(expectedTsPackets, parser.getTsPacketCount())
        Assert.assertEquals(expectedTsPackets, output.packetCount)
        Assert.assertEquals(expectedProgramCount, output.programCount)

        // Validate the program has all the required streams.
        Program prog = output.programMap.get(programNumber);
        Assert.assertEquals(7, prog.streams.size())
        Assert.assertTrue(prog.hasStream(0x1691, STREAM_MPEG2_VIDEO))
        Assert.assertTrue(prog.hasStream(0x1692, STREAM_AC3_AUDIO, "eng"))
        Assert.assertTrue(prog.hasStream(0x1693, STREAM_AC3_AUDIO, "spa"))
        Assert.assertTrue(output.pidMap.containsKey(0x1691))
        Assert.assertTrue(output.pidMap.containsKey(0x1692))
        Assert.assertTrue(output.pidMap.containsKey(0x1693))
    }

    @Test
    public void test_720p_mpeg4() {

        def programNumber = 5
        def expectedProgramCount = 8
        def expectedRtpPackets = 2677
        def expectedTsPackets = 106218

        def input = new FilePacketSource("data/720p_h264_ac3.rtp")
        def output = new TSWriter(new BufferedOutputStream(new FileOutputStream("out/720p_h264_ac3.ts")))
        output.addProgram(programNumber)
        def parser = new RTPParser(input, output, NetworkPacketSource.MAX_PACKET_SIZE)
        parser.start()   // Start RTP parser thread.
        Thread.sleep(2000)

        // Validate packet counts.
        output.printStats()
        Assert.assertEquals(expectedRtpPackets, parser.getRtpPacketCount())
        Assert.assertEquals(expectedTsPackets, parser.getTsPacketCount())
        Assert.assertEquals(expectedTsPackets, output.packetCount)
        Assert.assertEquals(expectedProgramCount, output.programCount)

        // Validate the program has all the required streams.
        Program prog = output.programMap.get(programNumber);
        Assert.assertEquals(7, prog.streams.size())
        Assert.assertTrue(prog.hasStream(0x16A2, STREAM_H264_VIDEO))
        Assert.assertTrue(prog.hasStream(0x16A3, STREAM_AC3_AUDIO, "eng"))
        Assert.assertTrue(prog.hasStream(0x16A4, STREAM_AC3_AUDIO, "spa"))
        Assert.assertTrue(output.pidMap.containsKey(0x16A2))
        Assert.assertTrue(output.pidMap.containsKey(0x16A3))
        Assert.assertTrue(output.pidMap.containsKey(0x16A4))
    }

    @Test
    public void test_480i_mpeg2() {

        def programNumber = 8
        def expectedProgramCount = 11
        def expectedRtpPackets = 1560
        def expectedTsPackets = 48573

        def input = new FilePacketSource("data/480i_mpeg2_ac3.rtp")
        def output = new TSWriter(new BufferedOutputStream(new FileOutputStream("out/480i_mpeg2_ac3.ts")))
        output.addProgram(programNumber)
        def parser = new RTPParser(input, output, NetworkPacketSource.MAX_PACKET_SIZE)
        parser.start()   // Start RTP parser thread.
        Thread.sleep(2000)

        // Validate packet counts.
        output.printStats()
        Assert.assertEquals(expectedRtpPackets, parser.getRtpPacketCount())
        Assert.assertEquals(expectedTsPackets, parser.getTsPacketCount())
        Assert.assertEquals(expectedTsPackets, output.packetCount)
        Assert.assertEquals(expectedProgramCount, output.programCount)

        // Validate the program has all the required streams.
        Program prog = output.programMap.get(programNumber);
        Assert.assertEquals(2, prog.streams.size())
        Assert.assertTrue(prog.hasStream(0xF40, STREAM_MPEG2_VIDEO))
        Assert.assertTrue(prog.hasStream(0xF41, STREAM_AC3_AUDIO, "eng"))
        Assert.assertTrue(output.pidMap.containsKey(0xF40))
        Assert.assertTrue(output.pidMap.containsKey(0xF41))
    }
}
