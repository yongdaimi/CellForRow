//
// Created by nisha_chen on 2020/3/12.
//

#include "FFAudioConvertTool.h"
#include "SLLog.h"

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
}

// Number of channels of the output AAC format file
#define OUTPUT_AAC_CHANNELS 2
// Bit rate of the output AAC format file
#define OUTPUT_AAC_BIT_RATE 96000

FFAudioConvertTool::FFAudioConvertTool() :
        mInputFormatCtx(nullptr),
        mInputCodecPara(nullptr),
        mInputCodec(nullptr),
        mInputCodecCtx(nullptr),
        mOutputFormatCtx(nullptr),
        mOutputCodec(nullptr),
        mOutputCodecCtx(nullptr),

        mAudioStreamIndex(-1),
        mAudioChannels(0),
        mAudioSampleRate(0)
{

}


bool FFAudioConvertTool::open(const char *inputAudioFilePath, const char *outputAacFilePath)
{
    if (!openInputFile(inputAudioFilePath)) return false;
    if (!openOutputFile(outputAacFilePath)) return false;
}


bool FFAudioConvertTool::openInputFile(const char *inputAudioFilePath)
{
    XLOGI("open audio file is: %s", inputAudioFilePath);
    close();

    int ret;
    ret = avformat_open_input(&mInputFormatCtx, inputAudioFilePath, nullptr, nullptr);
    if (ret != 0) {
        XLOGE("FFAudioConvertTool open %s failed, %s", inputAudioFilePath, av_err2str(ret));
        return false;
    }

    XLOGI("FFAudioConvertTool open %s success", inputAudioFilePath);

    ret = avformat_find_stream_info(mInputFormatCtx, nullptr);
    if (ret < 0) {
        XLOGE("avformat_find_stream_info failed, %s", av_err2str(ret));
        close();
        return false;
    }

    // Make sure that there is only one stream in the input file.
    if (mInputFormatCtx->nb_streams != 1) {
        XLOGE("Expected one audio input stream");
        close();
        return false;
    }

    // Get audio stream index
    ret = av_find_best_stream(mInputFormatCtx, AVMEDIA_TYPE_AUDIO, -1, -1, 0, 0);
    if (ret < 0) {
        XLOGE("av_find_best_stream failed");
        close();
        return false;
    }

    mAudioStreamIndex = ret;
    XLOGI("current audio stream index is: %d", mAudioStreamIndex);

    mInputCodecPara  = mInputFormatCtx->streams[mAudioStreamIndex]->codecpar;
    mAudioChannels   = mInputCodecPara->channels;
    mAudioSampleRate = mInputCodecPara->sample_rate;

    mInputCodec = avcodec_find_decoder(mInputCodecPara->codec_id);
    if (!mInputCodec) {
        XLOGE("could not found input decoder");
        close();
        return false;
    }

    // Create decoding context and copy parameters
    mInputCodecCtx = avcodec_alloc_context3(mInputCodec);
    if (!mInputCodecCtx) {
        XLOGE("could not allocate a decoding context");
        close();
        return false;
    }

    ret = avcodec_parameters_to_context(mInputCodecCtx, mInputCodecPara);
    if (ret < 0) {
        XLOGE("copy decoder params to codec context failed, %s", av_err2str(ret));
        close();
        return false;
    }

    ret = avcodec_open2(mInputCodecCtx, mInputCodec, nullptr);
    if (ret < 0) {
        XLOGE("open the decoder for the audio stream failed, %s", av_err2str(ret));
        close();
        return false;
    }

    return true;
}

bool FFAudioConvertTool::openOutputFile(const char *outputAudioFilePath)
{
    int ret;

    AVIOContext *outputIOCtx;
    ret = avio_open(&outputIOCtx, outputAudioFilePath, AVIO_FLAG_WRITE);
    if (ret < 0) {
        XLOGE("could not open output file %s, %s", outputAudioFilePath, av_err2str(ret));
        return false;
    }

    /* Create a new format context for the output container format. */
    mOutputFormatCtx = avformat_alloc_context();
    if (!mOutputFormatCtx) {
        XLOGE("could not alloc output format context");
        return false;
    }

    // Associate the output file (pointer) with the container format context.
    mOutputFormatCtx->pb = outputIOCtx;

    // Guess the desired container format based on the file extension
    AVOutputFormat *outputFormat = av_guess_format(nullptr, outputAudioFilePath, nullptr);
    if (!outputFormat) {
        XLOGE("there is no match format to matches");
        return false;
    }
    mOutputFormatCtx->oformat = outputFormat;

    /**************************************************************************************************/
    char *url = av_strdup(outputAudioFilePath);
    if (!url) {
        XLOGE("could not to alloc url.");
        return false;
    }
    // mOutputFormatCtx->url = url;
    /**************************************************************************************************/
    mOutputCodec = avcodec_find_encoder(AV_CODEC_ID_AAC);
    if (!mOutputCodec) {
        XLOGE("could not found the AAC encoder");
        return false;
    }

    AVStream *outputAudioStream = avformat_new_stream(mOutputFormatCtx, nullptr);
    if (!outputAudioStream) {
        XLOGE("can not create the new output audio stream");
        return false;
    }

    mOutputCodecCtx = avcodec_alloc_context3(mOutputCodec);
    if (!mOutputCodecCtx) {
        XLOGE("could not allocate the encoding context");
        return false;
    }

    mOutputCodecCtx->channels              = OUTPUT_AAC_CHANNELS;
    mOutputCodecCtx->channel_layout        = static_cast<uint64_t>(av_get_default_channel_layout(
            OUTPUT_AAC_CHANNELS));
    mOutputCodecCtx->sample_rate           = mInputCodecCtx->sample_rate;
    mOutputCodecCtx->sample_fmt            = mOutputCodec->sample_fmts[0];
    mOutputCodecCtx->bit_rate              = OUTPUT_AAC_BIT_RATE;
    mOutputCodecCtx->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL; // Allow the use of the experimental AAC encoder

    // Set the sample rate for the container.
    outputAudioStream->time_base.den = mInputCodecCtx->sample_rate;
    outputAudioStream->time_base.num = 1;

    // Some container formats (like MP4) require global headers to be present.
    // Mark the encoder so that it behaves accordingly
    if (mOutputFormatCtx->oformat->flags & AVFMT_GLOBALHEADER) {
        mOutputCodecCtx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
    }

    // Open the encoder for the audio stream to use it later
    ret = avcodec_open2(mOutputCodecCtx, mOutputCodec, nullptr);
    if (ret < 0) {
        XLOGE("could not open output codec, %s", av_err2str(ret));
        return false;
    }

    ret = avcodec_parameters_from_context(outputAudioStream->codecpar, mOutputCodecCtx);
    if (ret < 0) {
        XLOGE("could not initialize stream parameters, %s", av_err2str(ret));
        return false;
    }

    return true;
}


void FFAudioConvertTool::close()
{
    if (mInputFormatCtx) {
        avformat_close_input(&mInputFormatCtx);
        mInputFormatCtx = nullptr;
    }

    if (mInputCodecCtx) {
        avcodec_free_context(&mInputCodecCtx);
        mInputCodecCtx = nullptr;
    }

    if (mOutputCodecCtx) {
        avcodec_free_context(&mOutputCodecCtx);
        mOutputCodecCtx = nullptr;
    }

    if (mOutputFormatCtx) {
        avio_closep(&mOutputFormatCtx->pb);
        avformat_free_context(mOutputFormatCtx);
        mOutputFormatCtx = nullptr;
    }
}


bool FFAudioConvertTool::writeOutputFileHeader(AVFormatContext *outputForCtx)
{
    int ret = avformat_write_header(outputForCtx, nullptr);
    if (ret < 0) {
        XLOGE("write output file header failed, %s", av_err2str(ret));
        return false;
    }
    XLOGI("write output file header success");
    return true;
}


bool FFAudioConvertTool::convert()
{
    

    return true;
}


FFAudioConvertTool::~FFAudioConvertTool()
{

}


