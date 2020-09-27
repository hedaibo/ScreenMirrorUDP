package com.action.screenmirror.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Created by Tianluhua on 2018/4/3.
 */

public class VersionNumberUtils {

	public static String getVersion(Context context) {
		try {
			StringBuilder builder = new StringBuilder();
			builder.append("Version: ");
			PackageManager manager = context.getPackageManager();
			PackageInfo info = manager.getPackageInfo(context.getPackageName(),
					0);
			String versionName = info.versionName;
			int versionCode = info.versionCode;
			builder.append(versionName);
			builder.append(".");
			builder.append(versionCode);
			return builder.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "null";
	}

}
