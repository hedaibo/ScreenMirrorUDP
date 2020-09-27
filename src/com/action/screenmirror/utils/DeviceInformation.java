package com.action.screenmirror.utils;

import java.io.File;
import java.io.FileInputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.util.Log;

public class DeviceInformation {

	// public static String getSerialNumber() {
	// FileInputStream is;
	// String serial = "";
	// byte[] buffer = new byte[16];
	// try {
	// is = new FileInputStream(new File(
	// "/sys/devices/platform/cpu-id/chip_id"));
	// is.read(buffer);
	// is.close();
	// serial = new String(buffer);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// if (serial.length() > 11) {
	// String substring = serial.substring(4, 11);
	// return "RSE_" + substring;
	// }
	//
	// return serial;
	// }

	public static String getSerialNumber(Context context) {
		/*
		StringBuilder builder = new StringBuilder("RSE_");
		builder.append(android.os.Build.SERIAL);
		return builder.toString();*/
		String android_id = null; 
		ContentResolver contentResolver = context.getContentResolver();
		Uri uri = Uri.parse("content://com.action.sbmsetting/setting");
		Cursor query = contentResolver.query(uri , null, null, null, null);
		if (query != null && query.moveToFirst()) {
		    android_id = query.getString(query.getColumnIndex("android_id"));
		}else {
			Log.e("DeviceUtils", "--getAndroidId--fail");
		}
		if (query != null) {
			query.close();
		}
		return android_id ;
	}
	

}
