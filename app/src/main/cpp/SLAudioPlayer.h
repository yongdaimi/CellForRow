//
// Created by nisha_chen on 2020/1/20.
//

#ifndef DONGLEAPPDEMO_SLAUDIOPLAYER_H
#define DONGLEAPPDEMO_SLAUDIOPLAYER_H


#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <stdio.h>
#include <pthread.h>

class SLAudioPlayer
{

private:

    SLuint32 mCurPlayState;

    SLObjectItf mEngineObj;
    SLEngineItf mEngineInterface;
    SLObjectItf mPlayerObj;
    SLPlayItf   mPlayInterface;

    SLObjectItf mOutputMixObj;

    /** A buffer queue object is used to store the audio frame data to be played */
    SLAndroidSimpleBufferQueueItf mBufferQueue;

    FILE *mMediaFile;

    static pthread_mutex_t mLock;
    static pthread_cond_t  mPutAudioData;

public:

    bool startPlay();

    /** Stop the current playback status */
    void stopPlay();

    /** Pause current playback status */
    void pausePlay();

    /** Call this method to pass in a media file path for playback */
    void setMediaFilePath(const char *mediaFilePath);

    SLAudioPlayer();

    SLAudioPlayer(const char *mediaFilePath);

    ~SLAudioPlayer();

    static bool mAllowPutAudioData;

private:

    bool initAudioEngine();

    /** Call this method to initialize the audio player */
    bool initAudioPlayer();

    /** This function will be called back after a frame of audio is played */
    static void playerCallback(SLAndroidSimpleBufferQueueItf caller, void *pContext);

    static void *playMediaFileHandle(void *arg);

    void release();
};


#endif //DONGLEAPPDEMO_SLAUDIOPLAYER_H
