//
// Created by nisha_chen on 2020/1/20.
//

#include <jni.h>

#include "SLAudioPlayer.h"

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "SLLog.h"


SLAudioPlayer::SLAudioPlayer() :
        mCurPlayState(SL_PLAYSTATE_STOPPED),
        mEngineObj(nullptr),
        mEngineInterface(nullptr),
        mPlayerObj(nullptr),
        mPlayInterface(nullptr),
        mOutputMixObj(nullptr),
        mBufferQueue(nullptr),
        mMediaFile(nullptr)
{

}

SLAudioPlayer::SLAudioPlayer(const char *mediaFilePath) :
        mCurPlayState(SL_PLAYSTATE_STOPPED),
        mEngineObj(nullptr),
        mEngineInterface(nullptr),
        mPlayerObj(nullptr),
        mPlayInterface(nullptr),
        mOutputMixObj(nullptr),
        mBufferQueue(nullptr)
{
    if (!mediaFilePath) {
        XLOGE("input media file path can not be null!");
    } else {
        mMediaFile = fopen(mediaFilePath, "rb");
        if (!mMediaFile) {
            XLOGE("media file open failed");
        }
    }
}


pthread_mutex_t SLAudioPlayer::mLock         = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t  SLAudioPlayer::mPutAudioData = PTHREAD_COND_INITIALIZER;

bool SLAudioPlayer::mAllowPutAudioData = false;


SLAudioPlayer::~SLAudioPlayer()
{
    release();
}


bool SLAudioPlayer::startPlay()
{
    if (!mPlayInterface) {
        if (!initAudioPlayer()) {
            XLOGE("init audio player failed");
            return false;
        }
    }

    if (mMediaFile == nullptr) {
        XLOGE("audio player start failed, media file open failed");
        return false;
    }

    if (mCurPlayState == SL_PLAYSTATE_PLAYING) {
        return true;
    }

    SLresult ret = (*mPlayInterface)->SetPlayState(mPlayInterface, SL_PLAYSTATE_PLAYING);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audio player start failed, set play state SL_PLAYSTATE_PLAYING failed");
        return false;
    }

    if (mCurPlayState == SL_PLAYSTATE_STOPPED) {
        pthread_t playThread;
        XLOGI("prepare send audio data...");
        pthread_create(&playThread, nullptr, playMediaFileHandle, this);
    }

    mCurPlayState = SL_PLAYSTATE_PLAYING;
    XLOGW("audio player is playing...");
    return true;
}


void *SLAudioPlayer::playMediaFileHandle(void *arg)
{
    SLAudioPlayer *audioPlayer = (SLAudioPlayer *) arg;
    static const int     buffSize     = 2048;
    static short         buffer[buffSize];

    XLOGW("send audio data to decode....");
    while (audioPlayer->mCurPlayState == SL_PLAYSTATE_PLAYING &&
           feof(audioPlayer->mMediaFile) == 0) {
        pthread_mutex_lock(&mLock);
        size_t len = fread(buffer, 1, buffSize, audioPlayer->mMediaFile);
        (*(audioPlayer->mBufferQueue))->Enqueue(audioPlayer->mBufferQueue, buffer, len);
        XLOGV("send audio data, size is: %u", len);
        while (!mAllowPutAudioData) {
            pthread_cond_wait(&audioPlayer->mPutAudioData, &audioPlayer->mLock);
        }
        mAllowPutAudioData = false;
        pthread_mutex_unlock(&mLock);
    }

    // Once the file stream is read to the end or the file is read out, the reading position of
    // the file stream is moved to the beginning, so that the next time you play, you can start
    // playing from the beginning of the file.
    fseek(audioPlayer->mMediaFile, 0, SEEK_SET);
    XLOGE("media file playing finished, play thread will exit");
    return 0;
}


void SLAudioPlayer::pausePlay()
{
    if (!mPlayInterface) {
        XLOGE("pause play failed, the player has not been initialized");
        return;
    }

    if (mCurPlayState == SL_PLAYSTATE_PAUSED) {
        return;
    }

    SLresult ret = (*mPlayInterface)->SetPlayState(mPlayInterface, SL_PLAYSTATE_PAUSED);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("SLAudioPlayer stop play failed");
    }

    mCurPlayState = SL_PLAYSTATE_PAUSED;
    XLOGW("audio player is paused");
}

void SLAudioPlayer::stopPlay()
{
    if (!mPlayInterface) {
        XLOGE("stop play failed, the player has not been initialized");
        return;
    }

    SLresult ret = (*mPlayInterface)->SetPlayState(mPlayInterface, SL_PLAYSTATE_STOPPED);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("SLAudioPlayer stop play failed");
    }

    mCurPlayState = SL_PLAYSTATE_STOPPED;

    pthread_mutex_lock(&mLock);
    mAllowPutAudioData = true;
    pthread_cond_signal(&mPutAudioData);
    pthread_mutex_unlock(&mLock);

    XLOGW("audio player has stopped");
}


void SLAudioPlayer::setMediaFilePath(const char *mediaFilePath)
{
    if (!mediaFilePath) {
        XLOGE("audio player set media file path failed, argus can not be null");
        return;
    }

    if (mCurPlayState == SL_PLAYSTATE_PLAYING) {
        XLOGE("audio player set media file path failed, Currently playing, please stop playing first");
        return;
    }

    mMediaFile = fopen(mediaFilePath, "rb");
    if (!mMediaFile) {
        XLOGE("set media file path failed, open media file failed.");
    }
}

bool SLAudioPlayer::initAudioEngine()
{
    SLresult ret;
    ret = slCreateEngine(&mEngineObj, 0, nullptr, 0, nullptr, nullptr);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audio player slCreateEngine() failed");
        return false;
    }

    ret = (*mEngineObj)->Realize(mEngineObj, SL_BOOLEAN_FALSE);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("mEngineObj realize failed");
        return false;
    }

    ret = (*mEngineObj)->GetInterface(mEngineObj, SL_IID_ENGINE, &mEngineInterface);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("mEngineObj GetInterface SL_IID_ENGINE failed");
        return false;
    }

    XLOGI("SLAudioPlayer initAudioEngine success");
    return true;
}


bool SLAudioPlayer::initAudioPlayer()
{
    if (!mEngineInterface) {
        if (!initAudioEngine()) {
            XLOGE("init audio engine failed.");
            return false;
        }
    }

    XLOGI("audio player create engine success");

    SLresult ret;

    // Configure audio source info
    SLDataLocator_AndroidSimpleBufferQueue inputBufferQueueLocator = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            2
    };

    // Audio Format: PCM
    // Audio Channel: 2
    // Audio SampleRate 44100hz
    // Sample Bit: 16bit
    // ByteOrder: little endian
    SLDataFormat_PCM pcmFormat = {
            SL_DATAFORMAT_PCM,
            2,
            SL_SAMPLINGRATE_44_1,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
            SL_BYTEORDER_LITTLEENDIAN
    };

    SLDataSource playerSource = {
            &inputBufferQueueLocator,
            &pcmFormat
    };

    // Configure audio sink info
    ret = (*mEngineInterface)->CreateOutputMix(mEngineInterface, &mOutputMixObj, 0, NULL, NULL);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audioPlayer create output mix failed");
        return false;
    }

    ret = (*mOutputMixObj)->Realize(mOutputMixObj, SL_BOOLEAN_FALSE);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audioPlayer output mix realize failed");
        return false;
    }

    SLDataLocator_OutputMix outputMixLocator = {
            SL_DATALOCATOR_OUTPUTMIX,
            mOutputMixObj
    };

    SLDataSink playerSink = {
            &outputMixLocator,
            nullptr
    };

    SLInterfaceID requiredInterfacesArr[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    SLboolean     interfaceRequiredFlag[] = {SL_BOOLEAN_TRUE};
    SLuint32      requiredInterfaceNum    = 1;

    ret = (*mEngineInterface)->CreateAudioPlayer(mEngineInterface,
                                                 &mPlayerObj, &playerSource, &playerSink,
                                                 requiredInterfaceNum, requiredInterfacesArr,
                                                 interfaceRequiredFlag);

    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audio player create failed");
        return false;
    }

    ret = (*mPlayerObj)->Realize(mPlayerObj, SL_BOOLEAN_FALSE);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audio player realize failed");
        return false;
    }

    ret = (*mPlayerObj)->GetInterface(mPlayerObj, SL_IID_BUFFERQUEUE, &mBufferQueue);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("mAudioPlayerObj get SL_IID_BUFFERQUEUE failed");
        return false;
    }

    ret = (*mBufferQueue)->RegisterCallback(mBufferQueue, playerCallback, this);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("mBufferQueue RegisterCallback failed");
        return false;
    }

    ret = (*mPlayerObj)->GetInterface(mPlayerObj, SL_IID_PLAY, &mPlayInterface);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audio player get SL_IID_PLAY interface failed");
        return false;
    }

    XLOGI("SLAudioPlayer initAudioPlayer success");
    return true;
}


void SLAudioPlayer::release()
{
    if (mPlayerObj) {
        (*mPlayerObj)->Destroy(mPlayerObj);
        mPlayerObj     = nullptr;
        mPlayInterface = nullptr;
    }

    if (mOutputMixObj) {
        (*mOutputMixObj)->Destroy(mOutputMixObj);
        mOutputMixObj = nullptr;
    }

    if (mEngineObj) {
        (*mEngineObj)->Destroy(mEngineObj);
        mEngineObj       = nullptr;
        mEngineInterface = nullptr;
    }

    if (mMediaFile) {
        fclose(mMediaFile);
        mMediaFile = nullptr;
    }

    mCurPlayState = SL_PLAYSTATE_STOPPED;
    XLOGW("audio Player has release");
}


void SLAudioPlayer::playerCallback(SLAndroidSimpleBufferQueueItf bufferQueue, void *context)
{
    pthread_mutex_lock(&mLock);
    mAllowPutAudioData = true;
    pthread_cond_signal(&mPutAudioData);
    pthread_mutex_unlock(&mLock);
}



