package com.networkrecorder.recorder.encoders


import com.networkrecorder.recorder.util.Encoder
import com.networkrecorder.recorder.util.Settings
import com.networkrecorder.recorder.util.Util
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicLong

abstract class RecordingThread extends Thread {

    final DATE_FORMAT = 'MM/dd/yyyy HH:mm:ss'

    protected String schedulerBaseUrl
    protected long startTime
    protected AtomicLong stopTime = new AtomicLong()
    protected long actualStartTime = 0
    protected Encoder encoder
    protected String physicalChannel
    protected String baseFileName
    protected def channelAllocation
    protected def recordingProgram
    protected File recordingFile
    protected com.networkrecorder.recorder.services.RecorderService recorderService
    protected com.networkrecorder.recorder.services.RecorderCallbackService recorderCallbackService

    protected boolean aborted = false
    protected boolean active = false
    protected Object waitLock =  new Object()
    protected boolean baseFileNameUsed = true

    protected long fileCheckerInterval = 30000      // default 30 seconds
    protected long minSpaceAvailable = 524288000    // default 500 MB
    protected long lastFileSize = 0
    protected long lastFileSizeTime = 0

    private static Random random = new Random()

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    static def instanceOf(String schedulerBaseUrl, long startTime, long stopTime, Encoder encoder,
            String physicalChannel, String baseFileName, channelAllocation, recordingProgram,
            recorderService, recorderCallbackService) {

        def recorderThread
        if (encoder.type == 'NetworkEncoder') {
            recorderThread = new NetworkEncoderRecordingThread()
        } else if (encoder.type == 'CetonTuner') {
            recorderThread = new CetonRecordingThread()
        } else {
            throw new Exception("Unsupported encoder type.")
        }
        recorderThread.schedulerBaseUrl = schedulerBaseUrl
        recorderThread.startTime = startTime
        recorderThread.stopTime.set(stopTime)
        recorderThread.encoder = encoder
        recorderThread.physicalChannel = physicalChannel
        recorderThread.baseFileName = baseFileName
        recorderThread.channelAllocation = channelAllocation
        recorderThread.recordingProgram = recordingProgram
        recorderThread.recorderService = recorderService
        recorderThread.recorderCallbackService = recorderCallbackService

        return recorderThread
    }

    ///////////////////////////////////////
    // Recorder event methods
    // (Override in descendant classes)
    ///////////////////////////////////////

    boolean onPrepareRecording() {
        return true
    }

    boolean onStartRecording() {
        return true
    }

    boolean onStopRecording() {
        return true
    }

    boolean onCheckRecordingActive() {
        return active
    }

    boolean onCheckFileSize() {
        return true
    }

    def onError(Exception ex) {
    }

    def onThreadEnding() {
    }

    ///////////////////////////////////////
    // External signal methods
    ///////////////////////////////////////

    boolean updateRecording(long stopTime) {
        this.stopTime.set(stopTime)
        notifyThread()
        return active
    }

    def abortRecording() {
        aborted = true
        notifyThread()
    }

    def getInfo() {

        def dateFormat = new SimpleDateFormat(DATE_FORMAT)
        long stopTime = this.stopTime.get()
        def result = [:]

        result['status'] = (active ? 'active' : (aborted ? 'aborted' : 'waiting'))
        result['program'] = Util.getRecordingTitle(recordingProgram)
        result['channel_name'] = channelAllocation.ChannelName
        result['physical_channel'] = physicalChannel
        result['start_time'] = (startTime > 0 ? dateFormat.format(new Date(startTime)) : null)
        result['stop_time'] = (stopTime > 0 ? dateFormat.format(new Date(stopTime)) : null)
        result['actual_start_time'] = (actualStartTime > 0 ? dateFormat.format(new Date(actualStartTime)) : null)
        result['file'] = recordingFile.getPath()
        result['size'] = lastFileSize

        return result
    }


    ///////////////////////////////////////
    // Main thread methods
    ///////////////////////////////////////

    void run() {

        aborted = false
        active = false
        processRecordingStart()
        if (!aborted) {
            processRecordingInProgress()
        }

        try {
            onThreadEnding()
        } catch (ex) {
            log.error("ThreadEnding error.", ex)
        }

        recorderService.removeActiveRecording(recordingProgram.UpcomingProgramId)
    }

    void processRecordingStart() {

        try {
            // Validate start and end times.
            if (stopTime.get() <= Util.now() || startTime >= stopTime.get()) {
                throw new Exception("Recording of ${recordingProgram.Title} on ${channelAllocation.ChannelName} has invalid timing parameters.")
            }

            // Build recording file and make sure the parent dir exists.
            def share = selectRecordingShare()
            recordingFile = new File(share, "${baseFileName}.${encoder.fileType}")
            File parentDir = recordingFile.getParentFile()
            parentDir.mkdirs()

            // Append a number to the file name if it already exists.
            int segment = 0
            while (recordingFile.exists() && segment < 10) {
                segment++
                recordingFile = new File(share, "${baseFileName}-${segment}.${encoder.fileType}")
                baseFileNameUsed = false
            }

            if (!onPrepareRecording()) {
                throw new Exception("PrepareRecording failed.")
            }

            // Wait for actual start time (minus 1 second)
            long delay = (startTime - Util.now()) - 1000
            while (!aborted && delay > 100) {
                if (waitWithInterrupt(delay)) {
                    log.info("Wait for start time was interrupted.")
                }
                delay = (startTime - Util.now()) - 1000
            }
            if (aborted) {
                throw new Exception("Recording aborted while waiting for start time.")
            }

            // Start the recording.
            if (!onStartRecording()) {
                throw new Exception("StartRecording failed.")
            } else {
                active = true
                actualStartTime = Util.now()
                recorderCallbackService.callRecordingStarted(schedulerBaseUrl, recordingProgram, actualStartTime, recordingFile.getPath())
            }

        } catch (ex) {
            log.error("Recording failed.", ex)
            onError(ex)
            recorderCallbackService.callStartRecordingFailed(schedulerBaseUrl, channelAllocation, recordingProgram, ex.getMessage())
            aborted = true
        }
    }

    void processRecordingInProgress() {

        try {

            // Wait for recording end time. Check file size at regular intervals.
            long delay = getFileCheckerDelay()
            while (!aborted && delay > 0) {
                waitWithInterrupt(delay)
                long remaining = (stopTime.get() - Util.now())
                if (remaining > 0) {
                    if (!onCheckFileSize()) aborted = true
                }
                delay = getFileCheckerDelay()
            }

        } catch (ex) {
            log.error("Recording failed.", ex)
            onError(ex)
            aborted = true

        } finally {
            try {
                if (!onStopRecording()) {
                    log.error("StopRecording failed.")
                }
            } catch (re) {
                log.error("StopRecording failed with exception.", re)
            }
            active = false
            recorderCallbackService.callEndRecording(schedulerBaseUrl, recordingFile.getPath(), Util.now(), !aborted, false)
        }

        if (aborted) {
            log.info("Recording of ${recordingProgram.Title} on ${channelAllocation.ChannelName} aborted before stop time.")
        }
    }

    long getFileCheckerDelay() {
        long delay = (stopTime.get() - Util.now())
        return (delay > fileCheckerInterval ? fileCheckerInterval : delay)
    }

    ///////////////////////////////////////
    // Util methods
    ///////////////////////////////////////

    // Waits for the specified interval.
    // Returns true if the wait was interrupted.
    boolean waitWithInterrupt(long delay) {

        synchronized (waitLock) {
            try {
                waitLock.wait(delay)
            } catch (InterruptedException ie) {
                return true
            }
        }
        return false
    }

    // Notifies the thread to continue if it is waiting.
    // Recording parameters may have been changed, or the recording may have been aborted.
    boolean notifyThread() {

        synchronized (waitLock) {
            try {
                waitLock.notifyAll()
            } catch (ex) {
                log.info("Error on notify.", ex)
            }
        }
    }

    // TODO: more intelligently select recording share. Currently just selects share with the most free space.
    def selectRecordingShare() {

        // Select folder with most space available
        long freeSpace = minSpaceAvailable
        File selectedFolder = null
        Settings.recordingFolders.each { File folder ->
            def space = folder.getUsableSpace()
            log.info("Free Space on ${folder.getPath()}: ${space}")
            if (space > freeSpace) {
                freeSpace = space
                selectedFolder = folder
            }
        }
        log.info("Selected by Space: ${selectedFolder.getPath()}")

        // Select folder by random.
        int pos = 0
        synchronized (random) {
            pos = random.nextInt(Settings.recordingFolders.size())
        }
        return Settings.recordingFolders.get(pos)
    }
}
