#ifndef LOG_H_
#define LOG_H_

#include <android/log.h>

#ifndef DGB
#define DGB 1
#endif

#ifndef LOG_TAG
#define LOG_TAG __FILE__
#endif

#ifndef ALOGD
#if DGB
#define ALOGD(...) \
		__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) \
		__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#else
#define ALOGD(...)   ((void)0)
#define LOGE(...)   ((void)0)
#endif
#endif

#endif /* LOG_H_ */
