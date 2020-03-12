//
// Created by nisha_chen on 2020/1/20.
//

#ifndef DONGLEAPPDEMO_SLLOG_H
#define DONGLEAPPDEMO_SLLOG_H


class SLLog {

};


#ifdef ANDROID

#include <android/log.h>

#define LOG_TAG "xp.chen"

#define XLOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define XLOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define XLOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define XLOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define XLOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define XLOGD(...) printf(LOG_TAG,__VA_ARGS__)
#define XLOGI(...) printf(LOG_TAG,__VA_ARGS__)
#define XLOGE(...) printf(LOG_TAG,__VA_ARGS__)

#endif


#endif //DONGLEAPPDEMO_SLLOG_H
