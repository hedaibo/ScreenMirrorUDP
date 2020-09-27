package com.action.screenmirror.utils;

import android.util.Log;

public class LogUtils {

	private static final String TAG = LogUtils.class.getSimpleName();

	private LogUtils() {
		/* cannot be instantiated */
		throw new UnsupportedOperationException("cannot be instantiated");
	}

	private static final boolean isDebug = false;// 鏄惁闇�瑕佹墦鍗癰ug锛屽彲浠ュ湪application鐨刼nCreate鍑芥暟閲岄潰鍒濆鍖�

	// 涓嬮潰鍥涗釜鏄粯璁ag鐨勫嚱鏁�
	public static void i(String msg) {
		if (isDebug)
			Log.i(TAG, msg);
	}

	public static void d(String msg) {
		if (isDebug)
			Log.d(TAG, msg);
	}

	public static void e(String msg) {
		if (true)
			Log.e(TAG, msg);
	}

	public static void v(String msg) {
		if (isDebug)
			Log.v(TAG, msg);
	}

	// 涓嬮潰鏄紶鍏ヨ嚜瀹氫箟tag鐨勫嚱鏁�
	public static void i(String tag, String msg) {
		if (isDebug)
			Log.i(tag, msg);
	}

	public static void d(String tag, String msg) {
		if (isDebug)
			Log.i(tag, msg);
	}

	public static void e(String tag, String msg) {
		if (true)
			Log.i(tag, msg);
	}

	public static void v(String tag, String msg) {
		if (isDebug)
			Log.i(tag, msg);
	}

}
