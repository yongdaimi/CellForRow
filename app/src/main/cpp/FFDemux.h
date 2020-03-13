//
// Created by nisha_chen on 2020/3/13.
//

#ifndef DONGLEAPPDEMO_FFDEMUX_H
#define DONGLEAPPDEMO_FFDEMUX_H

struct AVFormatContext
class FFDemux
{


private:
    AVFormatContext *m_input_format_ctx;

public:
    virtual bool Open(const char *url);

    virtual void Close();

    virtual void Read();

};


#endif //DONGLEAPPDEMO_FFDEMUX_H
