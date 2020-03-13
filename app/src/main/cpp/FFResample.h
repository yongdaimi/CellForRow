//
// Created by nisha_chen on 2020/3/13.
//

#ifndef DONGLEAPPDEMO_FFRESAMPLE_H
#define DONGLEAPPDEMO_FFRESAMPLE_H



#include "stdint.h"

struct SwrContext;

class FFResample
{

public:
    FFResample();

    bool initResample(int outChannelLayout,
                      int outSampleFmt,
                      int outSampleRate,
                      int inChannelLayout,
                      int inSampleFmt,
                      int inSampleRate);

    ~FFResample();

private:

    int mOutChannelLayout;
    int mOutSampleFmt;
    int mOutSampleRate;
    int mInChannelLayout;
    int mInSampleFmt;
    int mInSampleRate;

    SwrContext *mSwrCtx;

};


#endif //DONGLEAPPDEMO_FFRESAMPLE_H
