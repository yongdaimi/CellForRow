//
// Created by nisha_chen on 2020/3/12.
//

#ifndef DONGLEAPPDEMO_FFAUDIOCONVERTTOOL_H
#define DONGLEAPPDEMO_FFAUDIOCONVERTTOOL_H

struct AVFormatContext;
struct AVCodecParameters;
struct AVCodec;
struct AVCodecContext;

class FFAudioConvertTool
{

private:
    AVFormatContext   *mInputFormatCtx;
    AVCodecParameters *mInputCodecPara;

    AVCodec        *mInputCodec;
    AVCodecContext *mInputCodecCtx;


    AVFormatContext *mOutputFormatCtx;
    AVCodec         *mOutputCodec;
    AVCodecContext  *mOutputCodecCtx;


    int mAudioChannels;

    int mAudioSampleRate;

    /** Audio stream index in media file */
    int mAudioStreamIndex;


public:

    FFAudioConvertTool();

    ~FFAudioConvertTool();

    bool open(const char *inputAudioFilePath, const char *outputAacFilePath);

    bool convert();

    void close();

private:

    bool openInputFile(const char *inputAudioFilePath);

    bool openOutputFile(const char *outputAudioFilePath);

    static bool writeOutputFileHeader(AVFormatContext *outputForCtx);

};


#endif //DONGLEAPPDEMO_FFAUDIOCONVERTTOOL_H
