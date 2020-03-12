//
// Created by nisha_chen on 2020/3/2.
//


#include "SLAudioRecorder.h"
#include "SLLog.h"


#define RECORD_BUFFER_QUEUE_NUM 2
#define RECORDER_FRAMES 2048


SLAudioRecorder::SLAudioRecorder(const char *fileSavePath) :
        mEngineObj(nullptr),
        mEngineInterface(nullptr),
        mRecorderObj(nullptr),
        mRecorderInterface(nullptr),
        mRecordBufferSize(RECORDER_FRAMES),
        mBufferQueue(nullptr),
        mIndex(0), mIsRecording(false)
{

    this->mFile = fopen(fileSavePath, "w");

    this->mRecordBuffs[0] = new short[mRecordBufferSize]();
    this->mRecordBuffs[1] = new short[mRecordBufferSize]();
}


bool SLAudioRecorder::initEngine()
{

    SLresult ret;

    ret = slCreateEngine(&mEngineObj, 0, nullptr, 0, nullptr, nullptr);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("slCreateEngine obj failed");
        return false;
    }

    ret = (*mEngineObj)->Realize(mEngineObj, SL_BOOLEAN_FALSE);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("sl engineObj realize failed");
        return false;
    }

    ret = (*mEngineObj)->GetInterface(mEngineObj, SL_IID_ENGINE, &mEngineInterface);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("sl get engine interface failed");
        return false;
    }

    XLOGI("init engine success");
    return true;
}


bool SLAudioRecorder::initRecorder()
{

    if (!mEngineInterface) {
        if (!initEngine()) {
            XLOGE("init engine failed");
            return false;
        }
    }

    SLresult ret;

    // Configuration the recorder's audio data source
    SLDataLocator_IODevice device = {
            SL_DATALOCATOR_IODEVICE,
            SL_IODEVICE_AUDIOINPUT,
            SL_DEFAULTDEVICEID_AUDIOINPUT,
            nullptr // Must be Null if deviceID parameter is to be used.
    };

    SLDataSource dataSource = {&device,
                               nullptr}; // This parameter is ignored if pLocator is SLDataLocator_IODevice.

    // Configuration the recorder's audio data save way.
    SLDataLocator_AndroidSimpleBufferQueue queue = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            RECORD_BUFFER_QUEUE_NUM
    };

    // Audio Format: PCM
    // Audio Channels: 2
    // SampleRate: 44100
    // SampleFormat: 16bit
    // Endian: Little Endian
    SLDataFormat_PCM pcmFormat = {
            SL_DATAFORMAT_PCM,
            1,
            SL_SAMPLINGRATE_16,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_CENTER,
            SL_BYTEORDER_LITTLEENDIAN
    };

    SLDataSink dataSink = {&queue, &pcmFormat};

    // Configure the interface that the recorder needs to support.
    SLInterfaceID ids[]          = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    SLboolean     ids_required[] = {SL_BOOLEAN_TRUE};
    SLuint32      numInterfaces  = 1;

    // Create the audio recorder.
    ret = (*mEngineInterface)->CreateAudioRecorder(mEngineInterface, &mRecorderObj,
                                                   &dataSource,
                                                   &dataSink,
                                                   numInterfaces,
                                                   ids,
                                                   ids_required);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("CreateAudioRecorder() failed");
        return false;
    }

    ret = (*mRecorderObj)->Realize(mRecorderObj, SL_BOOLEAN_FALSE);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("mRecorderObj realize failed");
        return false;
    }

    ret = (*mRecorderObj)->GetInterface(mRecorderObj, SL_IID_RECORD, &mRecorderInterface);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("mRecorderObj get SL_IID_RECORD interface failed");
        return false;
    }

    ret = (*mRecorderObj)->GetInterface(mRecorderObj, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                                        &mBufferQueue);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("mRecorderObj get simpleBufferQueue interface failed");
        return false;
    }

    ret = (*mBufferQueue)->RegisterCallback(mBufferQueue, recorderCallback, this);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("register read pcm data callback failed");
        return false;
    }

    XLOGI("init recorder success");
    return true;

}

bool SLAudioRecorder::start()
{
    if (mIsRecording) return true;

    if (!mRecorderInterface) {
        if (!initRecorder()) {
            XLOGE("init recorder failed");
            return false;
        }
    }

    if (!mFile) {
        XLOGE("record file open failed");
        return false;
    }

    SLresult ret;

    ret = (*mRecorderInterface)->SetRecordState(mRecorderInterface, SL_RECORDSTATE_STOPPED);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("mRecorderInterface stop record state failed");
        return false;
    }

    ret = (*mBufferQueue)->Clear(mBufferQueue);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("mBufferQueue clear bufferQueue failed");
        return false;
    }

    // Enqueue an empty buffer to fill data when recording audio to the recorder.
    ret = (*mBufferQueue)->Enqueue(mBufferQueue, mRecordBuffs[mIndex],
                                   mRecordBufferSize * sizeof(short));
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("mBufferQueue enqueue buffer failed");
        return false;
    }

    ret = (*mRecorderInterface)->SetRecordState(mRecorderInterface, SL_RECORDSTATE_RECORDING);
    if (ret != SL_RESULT_SUCCESS) {
        XLOGE("mRecorderInterface start record state failed");
        return false;
    }

    mIsRecording = true;
    XLOGI("audioRecorder start recording...");
    return true;
}

void SLAudioRecorder::stop()
{
    mIsRecording = false;
}

/**
 * Called after each frame of audio is recorded
 * @param bufferQueue
 * @param pContext
 */
void SLAudioRecorder::recorderCallback(SLAndroidSimpleBufferQueueItf bufferQueue, void *pContext)
{
    if (pContext == nullptr) {
        XLOGE("SLAudioRecorder recorderCallback argus is null");
        return;
    }

    SLAudioRecorder *recorder = (SLAudioRecorder *) pContext;

    /*int ret = write(recorder->mRecordedFilePathFd,
                    recorder->mRecordBuffs[recorder->mIndex],
                    recorder->mRecordBufferSize);*/

    int ret = fwrite(recorder->mRecordBuffs[recorder->mIndex], sizeof(short),
                     recorder->mRecordBufferSize, recorder->mFile);

    if (ret < 0) {
        XLOGE("SLAudioRecorder recorderCallback write failed");
        return;
    }

    // If it is currently recording, modify the index and switch another buffer for the next recording
    if (recorder->mIsRecording) {
        recorder->mIndex = 1 - recorder->mIndex;
        (*recorder->mBufferQueue)->Enqueue(recorder->mBufferQueue,
                                           recorder->mRecordBuffs[recorder->mIndex],
                                           recorder->mRecordBufferSize * sizeof(short));
    } else {
        (*recorder->mRecorderInterface)->SetRecordState(recorder->mRecorderInterface,
                                                        SL_RECORDSTATE_STOPPED);
        fclose(recorder->mFile);
    }
}

void SLAudioRecorder::release()
{
    if (mRecorderObj) {
        (*mRecorderInterface)->SetRecordState(mRecorderInterface, SL_RECORDSTATE_STOPPED);
        (*mRecorderObj)->Destroy(mRecorderObj);
        mRecorderObj       = nullptr;
        mRecorderInterface = nullptr;
        mBufferQueue       = nullptr;
    }

    if (mEngineObj) {
        (*mEngineObj)->Destroy(mEngineObj);
        mEngineObj       = nullptr;
        mEngineInterface = nullptr;
    }

    if (mRecordBuffs[0]) {
        delete mRecordBuffs[0];
        mRecordBuffs[0] = nullptr;
    }

    if (mRecordBuffs[1]) {
        delete mRecordBuffs[1];
        mRecordBuffs[1] = nullptr;
    }

    if (mFile) {
        fclose(mFile);
    }

    mIsRecording = false;
    mIndex       = 0;

    XLOGI("audioRecorder stopped");
}


SLAudioRecorder::~SLAudioRecorder()
{
    release();
}

