/** log */
#define LOG_TAG "NATVIE-AudioRecord"
#define DGB 1

#include <jni.h>
#include "com_action_screenmirror_audio_AudioRecord.h"

#include "log.h"
#include <stdio.h>
#include <assert.h>

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

/* Size of the recording buffer queue */
#define NB_BUFFERS_IN_QUEUE 1

/* Explicitly requesting SL_IID_ANDROIDSIMPLEBUFFERQUEUE and SL_IID_ANDROIDCONFIGURATION
 * on the AudioRecorder object */
#define NUM_EXPLICIT_INTERFACES_FOR_RECORDER 2

/* Size of the recording buffer queue */
#define NB_BUFFERS_IN_QUEUE 1
/* Size of each buffer in the queue */
#define BUFFER_SIZE_IN_SAMPLES 8192
#define BUFFER_SIZE_IN_BYTES   (2 * BUFFER_SIZE_IN_SAMPLES)

/* Local storage for Audio data */
int8_t pcmData[NB_BUFFERS_IN_QUEUE * BUFFER_SIZE_IN_BYTES];

// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine = NULL;

//audio record interfaces
static SLObjectItf recorderObject = NULL;
static SLRecordItf recordItf = NULL;
static SLAndroidSimpleBufferQueueItf recBuffQueueItf = NULL;
static SLAndroidConfigurationItf configItf = NULL;

static FILE * gFile = NULL;

jobject callback;
JavaVM *g_VM;
jobject g_obj;

//-----------------------------------------------------------------
/* Structure for passing information to callback function */
typedef struct CallbackCntxt_ {
	SLPlayItf playItf;
	SLuint32 size;
	SLint8* pDataBase; // Base address of local audio data storage
	SLint8* pData; // Current address of local audio data storage
} CallbackCntxt;

static CallbackCntxt cntxt;

/* Callback for recording buffer queue events */
void recCallback(SLRecordItf caller, void *pContext, SLuint32 event) {
	if (SL_RECORDEVENT_HEADATNEWPOS & event) {
		SLmillisecond pMsec = 0;
		(*caller)->GetPosition(caller, &pMsec);
		ALOGD("SL_RECORDEVENT_HEADATNEWPOS current position=%ums\n", pMsec);
	}

	if (SL_RECORDEVENT_HEADATMARKER & event) {
		SLmillisecond pMsec = 0;
		(*caller)->GetPosition(caller, &pMsec);
		ALOGD("SL_RECORDEVENT_HEADATMARKER current position=%ums\n", pMsec);
	}
}

/* Callback for recording buffer queue events */
void recBufferQueueCallback(SLAndroidSimpleBufferQueueItf queueItf,
		void *pContext) {


	JNIEnv *env;
	int mNeedDetach = JNI_FALSE;
	CallbackCntxt *pCntxt = (CallbackCntxt*) pContext;

	/* Save the recorded data  */
	//fwrite(pCntxt->pDataBase, BUFFER_SIZE_IN_BYTES, 1, gFile);
	/*-------------------------------------------------------------*/
	//ALOGD("hdb----recBufferQueueCallback----pCntxt->pDataBase");
	int getEnvStat = (*g_VM)->GetEnv(g_VM, (void **)   &env,JNI_VERSION_1_6);
	    if (getEnvStat == JNI_EDETACHED) {
	        //如果没有， 主动附加到jvm环境中，获取到env
	        if ((*g_VM)->AttachCurrentThread(g_VM, &env, NULL) != 0) {
	            return;
	        }
	        mNeedDetach = JNI_TRUE;
	    }
	    //强转回来
	    jobject jcallback = (jobject)callback;

	    //通过强转后的jcallback 获取到要回调的类
	    jclass javaClass = (*env)->GetObjectClass(env, jcallback);

	    if (javaClass == 0) {
	    	ALOGD("hdb---Unable to find class--");
	        (*g_VM)->DetachCurrentThread(g_VM);
	        return;
	    }


	   //获取要回调的方法ID
	    jmethodID javaCallbackId = (*env)->GetMethodID(env, javaClass,
	                                                 "onCallback", "([B)V");//"(Ljava/nio/ByteBuffer;)V");
	    if (javaCallbackId == NULL) {
	    	ALOGD("hdb---Unable to find method:onCallback");
	        return;
	    }

	    //int8_t aaa[10] = {1,2,3,4,5,6};
	    //int8_t pcmData[10] ={1,2};//pCntxt->pDataBase[BUFFER_SIZE_IN_BYTES];
	    //pcmData = aaa;
	    //strcpy(pcmData ,aaa);
	    //int8_t pbuff[BUFFER_SIZE_IN_BYTES];
	    //memcpy(pbuff, pCntxt->pDataBase, BUFFER_SIZE_IN_BYTES);
	    jbyteArray jarrRV =(*env)->NewByteArray(env,BUFFER_SIZE_IN_BYTES);
	    (*env)->SetByteArrayRegion(env,jarrRV, 0,BUFFER_SIZE_IN_BYTES,pCntxt->pDataBase);
	   // ALOGD("hdb---call---method:onCallback---BUFFER_SIZE_IN_BYTES:%d,pbuff=%d",BUFFER_SIZE_IN_BYTES,sizeof(jarrRV));


	   //执行回调
	    (*env)->CallIntMethod(env, jcallback, javaCallbackId,jarrRV);

	    //释放当前线程
	   if(mNeedDetach) {
	        (*g_VM)->DetachCurrentThread(g_VM);
	    }
	    env = NULL;

	    //释放你的全局引用的接口，生命周期自己把控
//	     (*env)->DeleteGlobalRef(env, jcallback);
//	    jcallback = NULL;

	/*-------------------------------------------------------------*/
	/* Increase data pointer by buffer size */
	pCntxt->pData += BUFFER_SIZE_IN_BYTES;

	if (pCntxt->pData
			>= pCntxt->pDataBase
					+ (NB_BUFFERS_IN_QUEUE * BUFFER_SIZE_IN_BYTES)) {
		pCntxt->pData = pCntxt->pDataBase;
	}

	(*queueItf)->Enqueue(queueItf, pCntxt->pDataBase, BUFFER_SIZE_IN_BYTES);

	SLAndroidSimpleBufferQueueState recQueueState;
	(*queueItf)->GetState(queueItf, &recQueueState);

}

/*
 * Class:     com_action_screenmirror_audio_AudioRecord
 * Method:    createEngine
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_action_screenmirror_audio_AudioRecord_createEngine(JNIEnv * env, jclass class) {
	SLEngineOption EngineOption[] = {
	            {(SLuint32) SL_ENGINEOPTION_THREADSAFE, (SLuint32) SL_BOOLEAN_TRUE}
	    };
	SLresult result;
	result = slCreateEngine(&engineObject, 1, EngineOption, 0, NULL, NULL);
	assert(SL_RESULT_SUCCESS == result);

	 /* Realizing the SL Engine in synchronous mode. */
	result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
	assert(SL_RESULT_SUCCESS == result);

	// get the engine interface, which is needed in order to create other objects
	result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
	assert(SL_RESULT_SUCCESS == result);
}

/*
 * Class:     com_action_screenmirror_audio_AudioRecord
 * Method:    createAudioRecord
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_action_screenmirror_audio_AudioRecord_createAudioRecord(JNIEnv * env, jobject class, jstring uri,jobject jcallback) {

	(*env)->GetJavaVM(env, &g_VM);

	    //生成一个全局引用，回调的时候findclass才不会为null
	callback = (*env)->NewGlobalRef(env, jcallback);

	if (recorderObject != NULL) {

		ALOGD("hdb---already create auido record");
		return ;
	}

	const char* utf8 = (*env)->GetStringUTFChars(env,uri, NULL);

	/*gFile = fopen(utf8, "w");
	(*env)->ReleaseStringUTFChars(env,uri, utf8);

	if (gFile == NULL) {
		ALOGD(" open file fail ");
		return ;
	}*/

	SLresult result;

	/* setup the data source*/
	SLDataLocator_IODevice ioDevice = {
			SL_DATALOCATOR_IODEVICE,
			SL_IODEVICE_AUDIOINPUT,
			SL_DEFAULTDEVICEID_AUDIOINPUT,
			NULL
	};

	SLDataSource recSource = {&ioDevice, NULL};

	SLDataLocator_AndroidSimpleBufferQueue recBufferQueue = {
			SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
			NB_BUFFERS_IN_QUEUE
	};

	SLDataFormat_PCM pcm = {
			SL_DATAFORMAT_PCM,
			2,
			SL_SAMPLINGRATE_44_1,
			SL_PCMSAMPLEFORMAT_FIXED_16,
			16,
			SL_SPEAKER_FRONT_LEFT| SL_SPEAKER_FRONT_RIGHT,
			SL_BYTEORDER_LITTLEENDIAN
	};

	SLDataSink dataSink = { &recBufferQueue, &pcm };
	SLInterfaceID iids[NUM_EXPLICIT_INTERFACES_FOR_RECORDER] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_ANDROIDCONFIGURATION};
	SLboolean required[NUM_EXPLICIT_INTERFACES_FOR_RECORDER] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

	/* Create the audio recorder */
	result = (*engineEngine)->CreateAudioRecorder(engineEngine, &recorderObject , &recSource, &dataSink,
			NUM_EXPLICIT_INTERFACES_FOR_RECORDER, iids, required);
	assert(SL_RESULT_SUCCESS == result);


	/* get the android configuration interface*/
	result = (*recorderObject)->GetInterface(recorderObject, SL_IID_ANDROIDCONFIGURATION, &configItf);
	assert(SL_RESULT_SUCCESS == result);

	/* Realize the recorder in synchronous mode. */
	result = (*recorderObject)->Realize(recorderObject, SL_BOOLEAN_FALSE);
	assert(SL_RESULT_SUCCESS == result);

	/* Get the buffer queue interface which was explicitly requested */
	result = (*recorderObject)->GetInterface(recorderObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, (void*) &recBuffQueueItf);
	assert(SL_RESULT_SUCCESS == result);


	/* get the record interface */
	result = (*recorderObject)->GetInterface(recorderObject, SL_IID_RECORD, &recordItf);
	assert(SL_RESULT_SUCCESS == result);


	/* Set up the recorder callback to get events during the recording */
	result = (*recordItf)->SetMarkerPosition(recordItf, 2000);
	assert(SL_RESULT_SUCCESS == result);

	result = (*recordItf)->SetPositionUpdatePeriod(recordItf, 500);
	assert(SL_RESULT_SUCCESS == result);

	result = (*recordItf)->SetCallbackEventsMask(recordItf,
	            SL_RECORDEVENT_HEADATMARKER | SL_RECORDEVENT_HEADATNEWPOS);
	assert(SL_RESULT_SUCCESS == result);

	result = (*recordItf)->RegisterCallback(recordItf, recCallback, NULL);
	assert(SL_RESULT_SUCCESS == result);

	/* Initialize the callback and its context for the recording buffer queue */

	cntxt.pDataBase = (int8_t*) &pcmData;
	cntxt.pData = cntxt.pDataBase;
	cntxt.size = sizeof(pcmData);
	result = (*recBuffQueueItf)->RegisterCallback(recBuffQueueItf, recBufferQueueCallback, &cntxt);
	assert(SL_RESULT_SUCCESS == result);

	/* Enqueue buffers to map the region of memory allocated to store the recorded data */
	ALOGD("Enqueueing buffer ");
for (int i = 0; i < NB_BUFFERS_IN_QUEUE; i++) {
		ALOGD("%d ", i);
result = (*recBuffQueueItf)->Enqueue(recBuffQueueItf, cntxt.pData, BUFFER_SIZE_IN_BYTES);
		assert(SL_RESULT_SUCCESS == result);
		cntxt.pData += BUFFER_SIZE_IN_BYTES;
	}
 	cntxt.pData = cntxt.pDataBase;

	/* Start recording */
	result = (*recordItf)->SetRecordState(recordItf, SL_RECORDSTATE_RECORDING);
	assert(SL_RESULT_SUCCESS == result);
	ALOGD("Starting to record");

}

/*
 * Class:     com_action_screenmirror_audio_AudioRecord
 * Method:    stop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_action_screenmirror_audio_AudioRecord_stop(JNIEnv * env, jclass class) {
	if(callback != NULL){
		(*env)->DeleteGlobalRef(env, callback);
		callback = NULL;
	}

if (recordItf != NULL) {
	SLresult result = (*recordItf)->SetRecordState(recordItf, SL_RECORDSTATE_STOPPED);
	assert(SL_RESULT_SUCCESS == result);
}
}

/*
 * Class:     com_action_screenmirror_audio_AudioRecord
 * Method:    shutdown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_action_screenmirror_audio_AudioRecord_shutdown(JNIEnv * env, jclass class) {

//destroy recorder object , and invlidate all associated interfaces
if (recorderObject != NULL) {
	(*recorderObject)->Destroy(recorderObject);
	recorderObject = NULL;
	recordItf = NULL;
	recBuffQueueItf = NULL;
	configItf = NULL;
}

// destroy engine object, and invalidate all associated interfaces
if (engineObject != NULL) {
	(*engineObject)->Destroy(engineObject);
	engineObject = NULL;
	engineEngine = NULL;
}

//colse the file
/*if (gFile != NULL) {
 fclose(gFile);
 gFile == NULL;
 }*/
}
