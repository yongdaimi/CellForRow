//
// Created by nisha_chen on 2020/1/20.
//


#include <jni.h>
#include "SLLog.h"
#include "SLAudioRecorder.h"
#include "SLAudioPlayer.h"

static SLAudioRecorder *audioRecorder = nullptr;
static SLAudioPlayer   *audioPlayer   = nullptr;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_realsil_android_dongle_helper_UsbAudioHelper_native_1start_1record(JNIEnv *env,
                                                                            jclass clazz,
                                                                            jstring record_file_save_path)
{
    const char *recordFileSavePath = env->GetStringUTFChars(record_file_save_path, nullptr);
    if (!audioRecorder) {
        audioRecorder = new SLAudioRecorder(recordFileSavePath);
    }

    env->ReleaseStringUTFChars(record_file_save_path, recordFileSavePath);
    return (jboolean) audioRecorder->start();
}


extern "C"
JNIEXPORT void JNICALL
Java_com_realsil_android_dongle_helper_UsbAudioHelper_native_1stop_1record(JNIEnv *env,
                                                                           jclass clazz)
{
    if (audioRecorder) {
        audioRecorder->stop();
        delete audioRecorder;
        audioRecorder = nullptr;
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_realsil_android_dongle_helper_UsbAudioHelper_native_1start_1play(JNIEnv *env, jclass clazz,
                                                                          jstring play_media_file_path)
{
    if (!audioPlayer) {
        const char *playMediaFilePath = env->GetStringUTFChars(play_media_file_path, nullptr);
        XLOGI("set play media file path is: %s", playMediaFilePath);
        audioPlayer = new SLAudioPlayer(playMediaFilePath);
        env->ReleaseStringUTFChars(play_media_file_path, playMediaFilePath);
    }
    return (jboolean) audioPlayer->startPlay();
}


extern "C"
JNIEXPORT void JNICALL
Java_com_realsil_android_dongle_helper_UsbAudioHelper_native_1pause_1play(JNIEnv *env, jclass clazz)
{
    if (audioPlayer) {
        audioPlayer->pausePlay();
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_com_realsil_android_dongle_helper_UsbAudioHelper_native_1stop_1play(JNIEnv *env, jclass clazz)
{
    if (audioPlayer) {
        audioPlayer->stopPlay();
    }
}
