package com.realsil.android.dongle.helper;

/**
 * @author xp.chen
 */
public class UsbAudioHelper {

    static {
        System.loadLibrary("native-lib");
    }

    /**
     * Start audio recording, This will create a new instance of the recorder locally, and
     * the recorded audio data will be stored in the file specified by the 'recordFileSavePath'
     * parameter.
     *
     * @param recordFileSavePath The location where recorded audio data is saved.
     * @return true: start recording successfully. false: Failed to start recording
     */
    public static native boolean native_start_record(String recordFileSavePath);

    /**
     * Stop audio recording. Calling this method will destroy the previously created recorder and
     * close the recording file.
     */
    public static native void native_stop_record();

    public static native boolean native_start_play(String playMediaFilePath);

    public static native void native_pause_play();

    public static native void native_stop_play();


}
