//
// Created by nisha_chen on 2020/3/13.
//

#include "FFAudioFifo.h"
#include "SLLog.h"

extern "C" {
#include <libavutil/audio_fifo.h>
}

FFAudioFifo::FFAudioFifo() : (mAVAudioFifo = nullptr)
{

}

bool FFAudioFifo::initAudioFifo(enum AVSampleFormat sampleFormat, int channels, int nb_samples)
{
    mAVAudioFifo = av_audio_fifo_alloc(sampleFormat, channels, 1);
    if (!mAVAudioFifo) {
        XLOGE("can not alloc audio fifo");
        return false;
    }
    XLOGI("FFAudioFifo init success");
    return true;
}

FFAudioFifo::~FFAudioFifo()
{
    if (mAVAudioFifo) {
        av_audio_fifo_free(mAVAudioFifo);
        mAVAudioFifo = nullptr;
    }
}



