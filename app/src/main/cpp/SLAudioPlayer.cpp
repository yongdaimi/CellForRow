//
// Created by nisha_chen on 2020/1/20.
//

#include <jni.h>
#include <stdio.h>
#include "SLAudioPlayer.h"

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include "SLLog.h"


static void writePcmDataCallback(SLAndroidSimpleBufferQueueItf bufferQueue, void *pContext)
{
    static char *buf = NULL;
    static FILE *fp = NULL;

    if (!buf) {
        buf = new char[1024 * 1024];
    }

    if (!fp) {
        fp = fopen("/sdcard/test.pcm", "rb");
    }

    if (!fp) return;

    if (feof(fp) == 0) {
        int len = fread(buf, 1, 1024, fp);
        if (len > 0) {
            (*bufferQueue)->Enqueue(bufferQueue, buf, (SLuint32) len);
        }
    }
}

/*
static void readPcmDataCallback(SLAndroidSimpleBufferQueueItf queue, void *pContext) {
    // read pcm data from queue.

}
*/


SLresult SLAudioPlayer::initAudioPlayer()
{

    if (mEngineItf == NULL) {
        SLresult ret;
        SLObjectItf engineObject;

        ret = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
        if (ret != SL_RESULT_SUCCESS) {
            XLOGE("slCreateEngine() failed");
            return ret;
        }

        ret = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
        if (ret != SL_RESULT_SUCCESS) {
            XLOGE("engineObject Realize failed");
            return ret;
        }

        ret = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &mEngineItf);
        if (ret != SL_RESULT_SUCCESS) {
            XLOGE("engineObject GetInterface SL_IID_ENGINE failed");
            return ret;
        }

        XLOGI("slCreateEngine() success");
    }
    return SL_RESULT_SUCCESS;
}


SLresult SLAudioPlayer::startPlay()
{

    SLresult ret = 0;

    // 1. Init the engine interface
    if (mEngineItf == NULL) {
        ret = initAudioPlayer();
    }

    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("slCreateEngine() failed");
        return ret;
    }

    // 2. Create output mix
    SLObjectItf outputMix;
    ret = (*mEngineItf)->CreateOutputMix(mEngineItf, &outputMix, 0, NULL, NULL);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("create output mix failed");
        return ret;
    }

    ret = (*outputMix)->Realize(outputMix, SL_BOOLEAN_FALSE);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("output mix Realize failed");
        return ret;
    }

    // 3. Create audio player
    // Audio src
    SLDataLocator_AndroidSimpleBufferQueue inputBuffQueueLocator = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 10};
    SLDataFormat_PCM input_format_pcm = {
            SL_DATAFORMAT_PCM,
            2,
            SL_SAMPLINGRATE_44_1,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
            SL_BYTEORDER_LITTLEENDIAN
    };
    SLDataSource audioDataSource = {&inputBuffQueueLocator, &input_format_pcm};
    // Audio sink
    SLDataLocator_OutputMix outputMixLocator = {SL_DATALOCATOR_OUTPUTMIX, outputMix};
    SLDataSink audioSink = {&outputMixLocator, 0};

    SLObjectItf audioPlayer;
    SLuint32 numInterfaces = 1;
    SLInterfaceID audioPlayerIIDs[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    SLboolean audioPlayerIIDsRequired[] = {SL_BOOLEAN_TRUE};

    ret = (*mEngineItf)->CreateAudioPlayer(mEngineItf, &audioPlayer,
                                           &audioDataSource,
                                           &audioSink, numInterfaces, audioPlayerIIDs,
                                           audioPlayerIIDsRequired);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("create audio player failed");
        return ret;
    }

    ret = (*audioPlayer)->Realize(audioPlayer, SL_BOOLEAN_FALSE);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audio player realize failed");
        return ret;
    }


    SLAndroidSimpleBufferQueueItf bufferQueueInterface;
    ret = (*audioPlayer)->GetInterface(audioPlayer, SL_IID_BUFFERQUEUE, &bufferQueueInterface);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audio player get buffer queue failed");
        return ret;
    }

    (*bufferQueueInterface)->RegisterCallback(bufferQueueInterface, writePcmDataCallback, NULL);


    // 4. Start playing
    SLPlayItf playInterface;
    ret = (*audioPlayer)->GetInterface(audioPlayer, SL_IID_PLAY, &playInterface);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audio player get play interface failed");
        return ret;
    }
    (*playInterface)->SetPlayState(playInterface, SL_PLAYSTATE_PLAYING);

    (*bufferQueueInterface)->Enqueue(bufferQueueInterface, "", 1);
    return ret;

}


SLresult SLAudioPlayer::startRecord()
{
    SLresult ret = 0;

    // 1. Init the engine interface.
    if (mEngineItf == NULL) {
        ret = initAudioPlayer();
    }

    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("init audioPlayer failed");
        return ret;
    }

    SLObjectItf audioRecorder;

    // Configuration the recorder's audio data source
    SLDataLocator_IODevice device;
    device.locatorType = SL_DATALOCATOR_IODEVICE;
    device.deviceType = SL_IODEVICE_AUDIOINPUT;
    device.deviceID = SL_DEFAULTDEVICEID_AUDIOINPUT;
    device.device = NULL; // Must be Null if deviceID parameter is to be used.

    SLDataSource dataSource;
    dataSource.pLocator = &device;
    dataSource.pFormat = NULL; // This parameter is ignored if pLocator is SLDataLocator_IODevice.

    // Configuration the recorder's audio data save way.
    SLDataLocator_AndroidSimpleBufferQueue queue;
    queue.locatorType = SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE;
    queue.numBuffers = 10;

    SLDataFormat_PCM pcmFormat;
    pcmFormat.formatType = SL_DATAFORMAT_PCM;
    pcmFormat.numChannels = 2;
    pcmFormat.samplesPerSec = SL_SAMPLINGRATE_44_1;
    pcmFormat.bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
    pcmFormat.containerSize = SL_PCMSAMPLEFORMAT_FIXED_16;
    pcmFormat.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    pcmFormat.endianness = SL_BYTEORDER_LITTLEENDIAN;

    SLDataSink dataSink;
    dataSink.pLocator = &queue;
    dataSink.pFormat = &pcmFormat;

    // Configure the interface that the recorder needs to support.
    SLInterfaceID ids[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    SLboolean ids_required[] = {SL_BOOLEAN_TRUE};
    SLuint32 numInterfaces = sizeof(ids) / sizeof(SLInterfaceID);

    // Create the audio recorder.
    ret = (*mEngineItf)->CreateAudioRecorder(mEngineItf, &audioRecorder,
                                             &dataSource,
                                             &dataSink,
                                             numInterfaces,
                                             ids,
                                             ids_required);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("CreateAudioRecorder() failed");
        return ret;
    }

    ret = (*audioRecorder)->Realize(audioRecorder, SL_BOOLEAN_FALSE);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audioRecorder realize failed");
        return ret;
    }

    SLAndroidSimpleBufferQueueItf queueItf;
    ret = (*audioRecorder)->GetInterface(audioRecorder, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &queueItf);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audioRecorder get simpleBufferQueue interface failed");
        return ret;
    }

    return ret;
}



