//
// Created by nisha_chen on 2020/3/13.
//

#ifndef DONGLEAPPDEMO_FFAUDIOFIFO_H
#define DONGLEAPPDEMO_FFAUDIOFIFO_H

extern "C" {
#include <libavutil/samplefmt.h>
};

struct AVAudioFifo;

class FFAudioFifo
{

public:
    FFAudioFifo();

    ~FFAudioFifo();

    bool initAudioFifo(enum AVSampleFormat sampleFormat, int channels, int nb_samples);

private:
    AVAudioFifo *mAVAudioFifo;


};


#endif //DONGLEAPPDEMO_FFAUDIOFIFO_H
