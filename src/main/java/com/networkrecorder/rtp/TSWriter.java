package com.networkrecorder.rtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

public class TSWriter extends OutputStream {

    private byte[] packet = new byte[TSUtil.TS_PACKET_SIZE];
    private int packetSize = 0;
    private OutputStream output = null;
    private AtomicLong outputSize = new AtomicLong(0);
    private HashMap<Integer, PSITable> tempTables = new HashMap();

    // Make this info publicly accessible for testing.
    public long packetCount = 0;
    public long patCount = 0;    // Number of times we've encounter a PAT with start flag.
    public int programCount = 0;
    public HashMap<Integer, Stream> pidMap = new HashMap();
    public HashMap<Integer, Integer> pmtMap = new HashMap();
    public HashMap<Integer, Program> programMap = new HashMap();

    // Pids to include in the output.
    private HashSet<Integer> pidList = new HashSet();
    private HashSet<Integer> programList = new HashSet();

    // Filter settings.
    public boolean analyzeStream = true;        // Set false to skip stream analysis.
    public int maxAnalyzePackets = 3000;        // Maximum number of packets to scan for PSI tables and PID info.
    public int maxPatAnalyzePackets = 4;        // Maximum number of PAT packets to scan for PSI info.
    public boolean writeAllPids = false;
    public boolean includePAT = true;
    public boolean includePMT = true;
    public boolean rewritePAT = false;
    public boolean rewritePMT = false;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public TSWriter(OutputStream output) throws Exception {
        this.output = output;
    }

    public void addPid(int pid) {
        pidList.add(pid);
    }

    public void addProgram(int prog) {
        programList.add(prog);
    }

    @Override
    public void write(int b) throws IOException {
        processByte((byte) b);
    }

    @Override
    public void write(byte buf[], int start, int len) throws IOException {

        int pos = start;
        int size = start + len;

        while (pos < size) {

            // Scan for sync at the start of a new packet, if necessary.
            if (packetSize == 0 && buf[pos] != TSUtil.TS_SYNC_BYTE) {
                pos++;
                continue;
            }

            // Copy as many bytes as we can to complete the packet.
            int toCopy = TSUtil.TS_PACKET_SIZE - packetSize;
            int available = size - pos;
            if (available < toCopy) toCopy = available;
            System.arraycopy(buf, pos, packet, packetSize, toCopy);
            packetSize += toCopy;
            pos += toCopy;

            // If the packet is complete, process it and reset for the next one.
            if (packetSize == TSUtil.TS_PACKET_SIZE) {
                processPacket();
                packetSize = 0;
            }
        }

        // Alternate method - copy byte by byte (slightly slower).
        //for (; pos < size; pos++) {
        //    processByte(buf[pos]);
        //}
    }

    public void printStats() {

        log.info("---- Transport Stream Stats ----");
        log.info("" + programMap.size() + " Programs, " + pidMap.size() + " Pids:");
        for (int progNum : programMap.keySet()) {
            Program prog = programMap.get(progNum);
            log.info("  Program " + progNum +
                    ", PMT Pid: " + Integer.toHexString(prog.pmtPid).toUpperCase() +
                    ", PCR Pid: " + Integer.toHexString(prog.pcrPid).toUpperCase());
            for (Stream s : prog.streams) {
                String streamInfo = s.toString();
                if (pidMap.containsKey(s.pid)) {
                    streamInfo += " (Available)";
                }
                log.info(streamInfo);
            }
        }
    }

    public long getOutputSize() {
        return outputSize.get();
    }

    @Override
    public void close() {
        log.info("TSWriter processed: " + packetCount + " packets.");
        try {
            output.close();
        } catch (IOException ex) {
        }
    }

    private void processByte(byte b) throws IOException {

        // Reject the byte if this is the start of a new packet, and it is not
        // a sync byte.
        if (packetSize > 0 || b == TSUtil.TS_SYNC_BYTE) {
            packet[packetSize++] = (byte) b;

            // If the packet is complete, process it and reset for the next one.
            if (packetSize == TSUtil.TS_PACKET_SIZE) {
                processPacket();
                packetSize = 0;
            }
        }
    }

    private void processPacket() throws IOException {

        TSPacketInfo info = TSUtil.analyzePacket(packet);
        packetCount++;

        if (analyzeStream) {

            // Process PSI table packets.
            if (info.pid == TSUtil.PID_PAT) {
                processPAT(info, packet);
            } else if (pmtMap.containsKey(info.pid)) {
                processPMT(pmtMap.get(info.pid), info, packet);
            }

            // Keep info on all Pids.
            Stream s = pidMap.get(info.pid);
            if (s == null) {
                s = new Stream(info.pid);
                pidMap.put(info.pid, s);
            }
            s.pidCount++;
            if (info.mpegStreamType > 0) {
                s.mpegStreamType = info.mpegStreamType;
            }

            // Stop analyzing stream after a fixed number of packets.
            if (packetCount >= maxAnalyzePackets || patCount >= maxPatAnalyzePackets) {
                log.info("Stopping stream analyzing at: " + packetCount + " packets.");
                analyzeStream = false;
                selectPids();

            // Stop analyzing stream when program table is complete.
            } else if (programCount > 0 && patCount > 1){
                if (programMap.size() >= programCount) {
                    log.info("Stream discovery complete at: " + packetCount + " packets.");
                    analyzeStream = false;
                    selectPids();
                }
            }
        }

        // Write packets to output stream.
        if (writeAllPids || pidList.contains(info.pid)) {
            output.write(packet);
            outputSize.addAndGet(packet.length);
        }
    }

    private void selectPids() {

        if (includePAT) {
            pidList.add(TSUtil.PID_PAT);
        }

        if (programList.size() > 0) {

            // Look through the program map for video and audio pids.
            for (int progNum : programList) {
                Program p = programMap.get(progNum);
                if (p != null) {
                    pidList.add(p.pcrPid);
                    if (includePMT) {
                        pidList.add(p.pmtPid);
                    }
                    for (Stream s : p.streams) {
                        if (s.isAudio()) {
                            pidList.add(s.pid);
                        } else if (s.isVideo()) {
                            pidList.add(s.pid);
                        }
                    }
                }
            }
        }

        // Check if there are video and audio pids selected.
        boolean videoSelected = false;
        boolean audioSelected = false;
        for (int pid : pidList) {
            Stream s = pidMap.get(pid);
            if (s.isVideo()) {
                videoSelected = true;
            } else if (s.isAudio()) {
                audioSelected = true;
            }
        }

        // If a video pids has not been selected, just include the highest quality one
        // that has been found in the stream.
        if (!videoSelected) {
            Stream selection = null;
            long selectedCount = -1;
            for (Stream s : pidMap.values()) {
                if (s.isVideo()) {
                    if (s.pidCount > selectedCount) {
                        selection = s;
                    }
                }
            }
            if (selection != null) {
                pidList.add(selection.pid);
                if (includePMT) {
                    for (Program p : programMap.values()) {
                        if (p.hasStream(selection.pid)) {
                            pidList.add(p.pmtPid);
                            break;
                        }
                    }
                }
            }
        }

        // If no audio pids have been selected, include all of them.
        if (!audioSelected) {
            for (Stream s : pidMap.values()) {
                if (s.isAudio()) {
                    pidList.add(s.pid);
                }
            }
        }
    }

    private void processPAT(TSPacketInfo info, byte[] packet) {

        // Reject error packets.
        if (info.errorFlag) return;

        if (info.startFlag) {
            patCount++;
            PSITable table = TSUtil.readPSITable(info, packet, TSUtil.TABLE_PAT, info.payloadStart);
            while (table != null && table.isComplete()) {
                if (table.tableId == TSUtil.TABLE_PAT) {
                    processPATTable(table);
                }
                table = TSUtil.readPSITable(info, packet, TSUtil.TABLE_PAT, table.nextTableStart);
            }
        }
    }

    private void processPATTable(PSITable table) {

        if (table.currentFlag) {
            int count = 0;
            byte[] data = table.tableData;
            for (int i = 0; i < table.tableDataLen - 4; i += 4) {
                int byte0 = ByteUtil.unsignedByteToInt(data[i]);
                int byte1 = ByteUtil.unsignedByteToInt(data[i + 1]);
                int byte2 = ByteUtil.unsignedByteToInt(data[i + 2]);
                int byte3 = ByteUtil.unsignedByteToInt(data[i + 3]);
                int progNum = (byte0 << 8) + byte1;
                int pmtPid = ((byte2 & 0x1F) << 8) + byte3;
                pmtMap.put(pmtPid, progNum);
                count++;
            }
            if (count > 0) programCount = count;
        }
    }

    private void processPMT(int progNum, TSPacketInfo info, byte[] packet) {

        // Reject error packets.
        if (info.errorFlag) return;

        PSITable table = null;
        if (info.startFlag) {
            table = TSUtil.readPSITable(info, packet, TSUtil.TABLE_PMT, info.payloadStart);
        } else {
            table = tempTables.remove(info.pid);
            if (table != null) {
                if (table.continuityCount + 1 == info.continuity) {
                    table.addData(packet, info.payloadStart, info.continuity);
                } else {
                    table = null;
                }
            }
        }

        // Loop through all "tables" found in the PMT packet. Note there may be additional ones
        // besides the PMT table (i.e. 0xC0). However, we only process the PMT table.
        while (table != null && table.isComplete()) {
            if (table.tableId == TSUtil.TABLE_PMT) {
                processPMTTable(info.pid, progNum, table);
            }
            table = TSUtil.readPSITable(info, packet, TSUtil.TABLE_PMT, table.nextTableStart);
        }

        // If there is an incomplete table, store it and wait for the next packet on this pid.
        if (table != null) {
            tempTables.put(info.pid, table);
        }
    }

    private void processPMTTable(int pmtPid, int progNum, PSITable table) {

        if (table.tableId == TSUtil.TABLE_PMT && table.currentFlag) {
            byte[] data = table.tableData;
            if (data != null && data.length >= table.tableDataLen) {
                Program prog = new Program(progNum, pmtPid);
                int byte0, byte1, byte2, byte3, byte4;
                byte0 = ByteUtil.unsignedByteToInt(data[0]);
                byte1 = ByteUtil.unsignedByteToInt(data[1]);
                byte2 = ByteUtil.unsignedByteToInt(data[2]);
                byte3 = ByteUtil.unsignedByteToInt(data[3]);
                prog.pcrPid = ((byte0 & 0x1F) << 8) + byte1;
                int descriptorLen = ((byte2 & 0x03) << 8) + byte3;

                // Skip descriptors, and process each elementary stream.
                for (int i = 4 + descriptorLen; i < table.tableDataLen - 4; ) {
                    Stream stream = new Stream();
                    stream.streamType = ByteUtil.unsignedByteToInt(data[i]);
                    byte1 = ByteUtil.unsignedByteToInt(data[i + 1]);
                    byte2 = ByteUtil.unsignedByteToInt(data[i + 2]);
                    byte3 = ByteUtil.unsignedByteToInt(data[i + 3]);
                    byte4 = ByteUtil.unsignedByteToInt(data[i + 4]);
                    stream.pid = ((byte1 & 0x1F) << 8) + byte2;
                    descriptorLen = ((byte3 & 0x03) << 8) + byte4;

                    // Process descriptors for language code (0x0A).
                    for (int k = i + 5; k < i + 5 + descriptorLen; ) {
                        int tag = ByteUtil.unsignedByteToInt(data[k]);
                        int len = ByteUtil.unsignedByteToInt(data[k + 1]);
                        try {
                            if (tag == 0x0A) {
                                stream.language = new String(data, k + 2, len, "UTF-8").trim();
                            }
                        } catch (Exception ex) { }
                        k = k + len + 2;
                    }

                    prog.streams.add(stream);
                    i = i + 5 + descriptorLen;
                }
                programMap.put(progNum, prog);
            }
        }
    }
}
