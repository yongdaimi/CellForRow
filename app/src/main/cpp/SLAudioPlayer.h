//
// Created by nisha_chen on 2020/1/20.
//

#ifndef DONGLEAPPDEMO_SLAUDIOPLAYER_H
#define DONGLEAPPDEMO_SLAUDIOPLAYER_H


#include <SLES/OpenSLES.h>


class SLAudioPlayer
{

public:

    SLresult initAudioPlayer();

    SLresult startPlay();

    SLresult startRecord();

private:
    SLEngineItf mEngineItf = NULL;
};


#endif //DONGLEAPPDEMO_SLAUDIOPLAYER_H
