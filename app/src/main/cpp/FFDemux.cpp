//
// Created by nisha_chen on 2020/3/13.
//

#include "FFDemux.h"
#include "SLLog.h"

extern "C" {
#include <libavformat/avformat.h>
}

bool FFDemux::Open(const char *url)
{

    int ret;
    ret = avformat_open_input(&m_input_format_ctx, url, nullptr, nullptr);
    if (ret != 0) {
        XLOGE("FFAudioConvertTool open %s failed, %s", url, av_err2str(ret));
        return false;
    }

    XLOGI("FFAudioConvertTool open %s success", url);

    ret = avformat_find_stream_info(m_input_format_ctx, nullptr);
    if (ret < 0) {
        XLOGE("avformat_find_stream_info failed, %s", av_err2str(ret));
        Close();
        return false;
    }

    // Make sure that there is only one stream in the input file.
    if (m_input_format_ctx->nb_streams != 1) {
        XLOGE("Expected one audio input stream");
        Close();
        return false;
    }

    // Get audio stream index
    ret = av_find_best_stream(m_input_format_ctx, AVMEDIA_TYPE_AUDIO, -1, -1, 0, 0);
    if (ret < 0) {
        XLOGE("av_find_best_stream failed");
        Close();
        return false;
    }

    return true;
}


void FFDemux::Read()
{
    int ret;
    AVPacket *pkt = av_packet_alloc();
    if (!pkt) {
        XLOGE("could not allocate input frame");
        return;
    }

    ret = av_read_frame(m_input_format_ctx, pkt);

}


void FFDemux::Close()
{

}


