package com.networkrecorder.rtp;

public class TSUtil {

    public static final int TS_PACKET_SIZE = 188;
    public static final byte TS_SYNC_BYTE = 0x47;

    public static final int PID_PAT = 0x00;

    public static final int TABLE_PAT = 0x00;
    public static final int TABLE_PMT = 0x02;

    public static TSPacketInfo analyzePacket(byte[] packet) {

        int byte1 = ByteUtil.unsignedByteToInt(packet[1]);
        int byte2 = ByteUtil.unsignedByteToInt(packet[2]);
        int byte3 = ByteUtil.unsignedByteToInt(packet[3]);
        int byte4 = ByteUtil.unsignedByteToInt(packet[4]);

        TSPacketInfo info = new TSPacketInfo();
        info.pid = ((((byte1 & 0x1F) << 8) + byte2) & 0x1FFF);
        info.errorFlag = ((byte1 & 0x80) != 0);
        info.startFlag = ((byte1 & 0x40) != 0);
        info.scrambled = ((byte3 & 0xC0) != 0);
        info.adaptFlag = ((byte3 & 0x20) != 0);
        info.payloadFlag = ((byte3 & 0x10) != 0);
        info.continuity = (int) (byte3 & 0x0F);

        // Validate the packet.
        if (!(info.adaptFlag || info.payloadFlag)) {
            info.errorFlag = true;
        }

        // Get adaptation field info.
        if (info.adaptFlag) {
            info.adaptLen = byte4 + 1;
            info.adaptStart = 4;

            // Get the PCR.
            //MpegUtil::ReadPCR(packet + 5, &(info->PCR), &(info->PCRext));
        }

        // Fill in payload info.
        if (info.payloadFlag) {
            info.payloadLen = TS_PACKET_SIZE - 4 - info.adaptLen;
            info.payloadStart = 4 + info.adaptLen;
        }

        // Look for Mpeg header to get stream type.
        if (info.startFlag && !info.errorFlag && info.payloadLen > 3) {
            int i = info.payloadStart;
            if (packet[i] == 0x00 && packet[i+1] == 0x00 && packet[i+2] == 0x01) {
                info.mpegStreamType = ByteUtil.unsignedByteToInt(packet[i+3]);
            }
        }

        info.valid = true;
        return info;
    }

    static PSITable readPSITable(TSPacketInfo info, byte[] packet, int expectedID, int tableStart) {

        PSITable table = null;
        int i = tableStart;
        if (tableStart == info.payloadStart) {
            int ptr = ByteUtil.unsignedByteToInt(packet[i]);
            i += ptr + 1;
        }

        if (i < info.payloadLen - 8) {
            int tableId = ByteUtil.unsignedByteToInt(packet[i]);
            if (tableId != 0xFF) {
                table = new PSITable(tableId);
                int byte1 = ByteUtil.unsignedByteToInt(packet[i + 1]);
                int byte2 = ByteUtil.unsignedByteToInt(packet[i + 2]);
                table.syntaxSection = (byte1 & 0x80) > 0;
                table.sectionLen = ((byte1 & 0x03) << 8) + byte2;
                table.nextTableStart = i + table.sectionLen + 3;
                int tableDataStart = i + 3;
                int tableDataLen = table.sectionLen;
                if (table.syntaxSection) {
                    int byte3 = ByteUtil.unsignedByteToInt(packet[i + 3]);
                    int byte4 = ByteUtil.unsignedByteToInt(packet[i + 4]);
                    int byte5 = ByteUtil.unsignedByteToInt(packet[i + 5]);
                    table.streamId = (byte3 << 8) + byte4;
                    table.currentFlag = (byte5 & 0x01) > 0;
                    table.sectionNum = ByteUtil.unsignedByteToInt(packet[i + 6]);
                    table.lastSection = ByteUtil.unsignedByteToInt(packet[i + 7]);
                    tableDataLen -= 5;
                    tableDataStart += 5;
                }
                boolean storeData = (expectedID < 0 || expectedID == table.tableId);
                table.copyData(packet, tableDataStart, tableDataLen, info.continuity, storeData);
            }
        }

        return table;
    }

}
