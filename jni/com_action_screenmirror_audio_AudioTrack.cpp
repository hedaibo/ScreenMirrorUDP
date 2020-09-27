//
// Created by hdb on 2018/7/27 0002.
//
#define LOG_TAG "NATVIE-AudioTrack"
#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <pthread.h>
#include "log.h"
#include "BQAudioPlayer.h"

BQAudioPlayer *bqAudioPlayer = nullptr;

extern "C"
JNIEXPORT void JNICALL
Java_com_action_screenmirror_audio_AudioTrack_startSL(JNIEnv *env, jclass type,
                                                                jint sampleRate, jint samleFormat,
                                                                jint channels) {
	LOGE("hdb---startSL--------");
    if (bqAudioPlayer) {
        bqAudioPlayer->release();
        delete bqAudioPlayer;
    }
    bqAudioPlayer = new BQAudioPlayer(sampleRate, samleFormat, channels);
    if (!bqAudioPlayer->init()) {
        bqAudioPlayer->release();
        delete bqAudioPlayer;
        bqAudioPlayer = nullptr;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_action_screenmirror_audio_AudioTrack_writeSL(JNIEnv *env, jclass type,jbyteArray data_, jint length) {
	//LOGE("hdb---writeSL--------bqAudioPlayer=%d",bqAudioPlayer);
    if (!bqAudioPlayer) {
        return;
    }
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    bqAudioPlayer->enqueueSample(data, (size_t) length);
    env->ReleaseByteArrayElements(data_, data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_action_screenmirror_audio_AudioTrack_stopSL(JNIEnv *env, jclass type) {
	LOGE("hdb---stopSL--------");
    if (bqAudioPlayer) {
        bqAudioPlayer->release();
        delete bqAudioPlayer;
        bqAudioPlayer = nullptr;
    }
}
