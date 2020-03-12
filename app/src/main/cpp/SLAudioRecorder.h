//
// Created by nisha_chen on 2020/3/2.
//

#ifndef DONGLEAPPDEMO_SLAUDIORECORDER_H
#define DONGLEAPPDEMO_SLAUDIORECORDER_H

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include <stdio.h>


class SLAudioRecorder
{

private:
    int      mIndex;
    short    *mRecordBuffs[2];
    unsigned mRecordBufferSize;

    bool mIsRecording;

    FILE *mFile;

    SLObjectItf                   mEngineObj;
    SLEngineItf                   mEngineInterface;
    SLObjectItf                   mRecorderObj;
    SLRecordItf                   mRecorderInterface;
    SLAndroidSimpleBufferQueueItf mBufferQueue;

public:
    SLAudioRecorder(const char *fileSavePath);

    /** Call this method to start audio recording */
    bool start();

    /** Call this method to stop audio recording */
    void stop();

    ~SLAudioRecorder();

private:

    bool initEngine();

    /** Call this method to initialize an audio recorder */
    bool initRecorder();

    /** Call this method to release the resources related to recording */
    void release();

    /** This method is called every time an audio frame is recorded*/
    static void recorderCallback(SLAndroidSimpleBufferQueueItf bufferQueue, void *pContext);

};


#endif //DONGLEAPPDEMO_SLAUDIORECORDER_H
