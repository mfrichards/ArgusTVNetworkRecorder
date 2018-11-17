package com.networkrecorder.rtp;

public class PSITable {

    int tableId = 0;
    int sectionLen = 0;
    boolean syntaxSection = false;

    // Syntax section info (if present).
    int streamId = 0;             // PAT - transport stream id, PMT - program number
    boolean currentFlag = false;  // If true, data is current and in effect. If false, it is for future use.
    int sectionNum = 0;           // Which table in a sequence of tables.
    int lastSection = 0;          // Last table in the sequence.
    int nextTableStart = 0;       // Position in the packet of the next table.

    // Table data (may span multiple TS packets).
    int continuityCount = 0;      // The continuity counter from the last packet processed.
    int tableDataLen = 0;         // Total expected length of the table data.
    int tableDataSize = 0;        // The current size of table data available. May be incomplete if waiting for additional packets.
    byte[] tableData = null;


    public PSITable(int tableId) {
        this.tableId = tableId;
    }

    public void copyData(byte[] packet, int start, int len, int continuity, boolean storeData) {

        tableDataLen = len;
        int toCopy = len;
        if (start + len > packet.length) {
             toCopy = packet.length - start;
        }
        if (storeData) {
            tableData = new byte[len];
            System.arraycopy(packet, start, tableData, 0, toCopy);
        }
        tableDataSize = toCopy;
        continuityCount = continuity;
    }

    public void addData(byte[] packet, int start, int continuity) {

        if (continuityCount + 1 == continuity) {
            int toCopy = tableDataLen - tableDataSize;
            nextTableStart = start + toCopy;
            int available = packet.length - start;
            if (available < toCopy) toCopy = available;
            if (tableData != null) {
                System.arraycopy(packet, start, tableData, tableDataSize, toCopy);
            }
            tableDataSize += toCopy;
            continuityCount = continuity;
        }
    }

    public boolean isComplete() {
        return (tableDataSize == tableDataLen);
    }
}
