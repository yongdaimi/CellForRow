//
// Created by nisha_chen on 2020/3/13.
//

#include "FFResample.h"
#include "SLLog.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libswresample/swresample.h>
#include <libavutil/samplefmt.h>
#include <libavutil/avassert.h>
}


FFResample::FFResample() : mSwrCtx(nullptr)
{

}


bool FFResample::initResample(int outChannelLayout,
                              int outSampleFmt,
                              int outSampleRate,
                              int inChannelLayout,
                              int inSampleFmt,
                              int inSampleRate)
{

    int ret;

    mSwrCtx = swr_alloc_set_opts(nullptr,
                                 av_get_default_channel_layout(outChannelLayout),
                                 static_cast<AVSampleFormat>(outSampleFmt),
                                 outSampleRate,
                                 inChannelLayout,
                                 static_cast<AVSampleFormat>(inSampleFmt),
                                 inSampleRate,
                                 0,
                                 nullptr);

    if (!mSwrCtx) {
        XLOGE("FFResample init error, could not allocate resample context");
        return false;
    }

    av_assert0(outSampleRate == inSampleRate);

    ret = swr_init(mSwrCtx);
    if (ret != 0) {
        XLOGE("FFResample init error, could not open resample context");
        swr_free(&mSwrCtx);
        return false;
    }

    XLOGI("FFResample init success");
    return true;
}


/*FFResample::FFResample() :
        mOutChannelLayout(0),
        mOutSampleFmt(0),
        mOutSampleRate(0),
        mInChannelLayout(0),
        mInSampleFmt(0),
        mInSampleRate(0)
{

}*/



FFResample::~FFResample()
{
    if (mSwrCtx) {
        swr_free(&mSwrCtx);
        mSwrCtx = nullptr;
    }

}

