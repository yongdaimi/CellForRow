//
// Created by nisha_chen on 2020/1/20.
//

#include <jni.h>
#include <string>
#include "SLAudioPlayer.h"

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include "SLLog.h"


static void writePcmDataCallback(SLAndroidSimpleBufferQueueItf bufferQueue, void *pContext) {
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

static void readPcmDataCallback(SLAndroidSimpleBufferQueueItf queue, void *pContext) {
    // read pcm data from queue.

}


int SLAudioPlayer::initAudioPlayer() {

    if (mEngineInterface == NULL) {
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

        ret = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &mEngineInterface);
        if (ret != SL_RESULT_SUCCESS) {
            XLOGE("engineObject GetInterface SL_IID_ENGINE failed");
            return ret;
        }
    }
    return SL_RESULT_SUCCESS;
}


int SLAudioPlayer::startPlay() {

    SLresult ret;

    // 1. Init the engine interface
    if (mEngineInterface == NULL) {
        ret = (SLresult) initAudioPlayer();
        if (ret != SL_RESULT_SUCCESS) return ret;
    }

    // 2. Create output mix
    SLObjectItf outputMix;
    ret = (*mEngineInterface)->CreateOutputMix(mEngineInterface, &outputMix, 0, NULL, NULL);
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

    ret = (*mEngineInterface)->CreateAudioPlayer(mEngineInterface, &audioPlayer, &audioDataSource,
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


int SLAudioPlayer::startRecord() {

    SLresult ret;
    if (mEngineInterface == NULL) {
        ret = (SLresult) initAudioPlayer();
        if (ret != SL_RESULT_SUCCESS) return ret;
    }

    // Create a new audio record object
    // Audio data source
    SLDataLocator_IODevice device = {
            SL_DATALOCATOR_OUTPUTMIX,
            SL_IODEVICE_AUDIOINPUT,
            SL_DEFAULTDEVICEID_AUDIOINPUT,
            NULL, // Must be NULL if the deviceID parameter is to be used.
    };

    SLDataSource recorderDataSource = {
            &device,
            NULL // This parameter is ignored if pLocator is SLDataLocator_IODevice.
    };

    // Audio data sink
    SLDataLocator_AndroidSimpleBufferQueue queue = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            10
    };

    SLDataFormat_PCM pcm = {
            SL_DATAFORMAT_PCM,
            2,
            SL_SAMPLINGRATE_44_1,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
            SL_BYTEORDER_LITTLEENDIAN
    };

    SLDataSink recorderDataSink = {
            &queue,
            &pcm
    };

    SLuint32 requiredInterfaceNum = 1;
    SLInterfaceID requiredInterfaceArr[] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    SLboolean required[] = {SL_BOOLEAN_TRUE};

    SLObjectItf audioRecorder;

    ret = (*mEngineInterface)->CreateAudioRecorder(mEngineInterface,
                                                   &audioRecorder, &recorderDataSource,
                                                   &recorderDataSink,
                                                   requiredInterfaceNum, requiredInterfaceArr,
                                                   required);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("create audio recorder failed");
        return ret;
    }

    ret = (*audioRecorder)->Realize(audioRecorder, SL_BOOLEAN_FALSE);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audioRecorder realize failed");
        return ret;
    }

    SLAndroidSimpleBufferQueueItf queueInterface;
    ret = (*audioRecorder)->GetInterface(audioRecorder, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                                         &queueInterface);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("audioRecorder get SL_IID_ANDROIDSIMPLEBUFFERQUEUE interface failed");
        return ret;
    }

    (*queueInterface)->RegisterCallback(queueInterface, readPcmDataCallback, NULL);

    // Start recording
    SLRecordItf recordInterface;
    (*audioRecorder)->GetInterface(audioRecorder, SL_IID_RECORD, &recordInterface);

    (*recordInterface)->SetRecordState(recordInterface, SL_RECORDSTATE_RECORDING);

    return ret;
}


int SLAudioPlayer::stopPlay() {
    return 0;
}


int SLAudioPlayer::pauseRecord() {
    return 0;
}


int SLAudioPlayer::stopRecord() {
    return 0;
}






