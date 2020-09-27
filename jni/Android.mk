LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libopen_sles
LOCAL_SRC_FILES:= \
	com_action_screenmirror_audio_AudioTrack.cpp \
	BQAudioPlayer.cpp

#LOCAL_LDLIBS += -llog -lOpenSLES -landroid
LOCAL_SHARED_LIBRARIES := liblog libOpenSLES libandroid
include $(BUILD_SHARED_LIBRARY)