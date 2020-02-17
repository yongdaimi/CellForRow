//
// Created by nisha_chen on 2020/1/20.
//


#include "jni.h"
#include "SLAudioPlayer.h"


extern "C" JNIEXPORT void
Java_com_realsil_android_dongle_fragment_UsbAudioFragment_native_1start_1play(JNIEnv *env,
                                                                              jobject jobject)
{
    SLAudioPlayer *audioPlayer = new SLAudioPlayer;
    // audioPlayer->initAudioPlayer();
    audioPlayer->startPlay();

    // audioPlayer->startPlay();
}

