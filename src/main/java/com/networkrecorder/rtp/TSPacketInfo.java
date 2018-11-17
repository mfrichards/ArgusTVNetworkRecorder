package com.networkrecorder.rtp;

public class TSPacketInfo {

    int pid = 0;
    boolean valid = false;
    boolean startFlag = false;
    boolean errorFlag = false;
    boolean scrambled = false;
    boolean adaptFlag = false;
    boolean payloadFlag = false;
    int continuity = 0;
    int adaptLen = 0;
    int adaptStart = 0;
    int payloadLen = 0;
    int payloadStart = 0;
    int mpegStreamType = 0;

    // For MPEG streams.
    boolean pesFlag = false;
    int streamType = 0;
    int pesLen = 0;

    // PSI Info.
    /*
    int tableId = 0;
    boolean currentFlag = false;  // If true, data is current and in effect. If false, it is for future use.
    int sectionLen = 0;
    int streamId = 0;        // PAT - transport stream id, PMT - program number
    int sectionNum = 0;      // Which table in a sequence of tables.
    int lastSection = 0;     // Last table in the sequence.
    int tableDataStart = 0;  // Start pos of table data.
    int nextTableStart = 0;  // Start pos of the next table.
    */
}
