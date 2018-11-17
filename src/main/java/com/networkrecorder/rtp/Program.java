package com.networkrecorder.rtp;

import java.util.ArrayList;

public class Program {

    int progNum = 0;
    int pcrPid = -1;
    int pmtPid = -1;
    ArrayList<Stream> streams = new ArrayList();

    public Program(int progNum, int pmtPid) {
        this.progNum = progNum;
        this.pmtPid = pmtPid;
    }

    boolean hasStream(int pid) {

        if (pcrPid == pid) return true;
        for (Stream s : streams) {
            if (s.pid == pid) return true;
        }
        return false;
    }

    boolean hasStream(int pid, int streamType) {
        return hasStream(pid, streamType, null);
    }

    boolean hasStream(int pid, int streamType, String language) {

        for (Stream s : streams) {
            if (s.pid == pid) {
                if (s.streamType == streamType) {
                    if (language == null || s.language.equalsIgnoreCase(language)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
