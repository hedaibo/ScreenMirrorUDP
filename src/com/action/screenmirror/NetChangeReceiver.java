package com.action.screenmirror;

import com.action.screenmirror.interf.IWifiState;
import com.action.screenmirror.utils.LogUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.ImageWriter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetChangeReceiver extends BroadcastReceiver{
	
	private static final String TAG = "NetChangeReceiver";
	private IWifiState mWifiState;
	public NetChangeReceiver(IWifiState wifiState) {
		mWifiState = wifiState;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager mConnectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = mConnectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifiInfo.isConnected()) {
			LogUtils.e(TAG, "hdb-----isWifiConnected");
			if (mWifiState != null) {
				mWifiState.connect();
			}
			
		} else {
			LogUtils.e(TAG, "hdb---not--Connect");
			if (mWifiState != null) {
				mWifiState.disconnect();
			}
			
		}
		
	}

}
