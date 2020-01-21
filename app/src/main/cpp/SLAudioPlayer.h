//
// Created by nisha_chen on 2020/1/20.
//

#ifndef DONGLEAPPDEMO_SLAUDIOPLAYER_H
#define DONGLEAPPDEMO_SLAUDIOPLAYER_H


#include <SLES/OpenSLES.h>


class SLAudioPlayer {

public:

    int initAudioPlayer();

    int startPlay();
    int stopPlay();

    int startRecord();
    int pauseRecord();
    int stopRecord();


private:
    static SLEngineItf mEngineInterface;

};


#endif //DONGLEAPPDEMO_SLAUDIOPLAYER_H
