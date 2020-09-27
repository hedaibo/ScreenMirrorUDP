package com.action.screenmirror;


import com.action.screenmirror.interf.IWifiState;
import com.action.screenmirror.model.TcpSocketSend;
import com.action.screenmirror.model.TcpSocketSend.ConnectCallBack;
import com.action.screenmirror.model.UdpSocketSend;
import com.action.screenmirror.utils.Config;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjection;
import android.os.IBinder;
import android.util.Log;

public class SendServer extends Service{

	private static final String TAG = "SendServer";

	private TcpSocketSend tSend;
	private NetChangeReceiver mNetChangeReceiver;
	
	private static SendServer instance;

	private UdpSocketSend uSend;
	public static SendServer getInstance(){
		return instance;
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		instance = this;
		Log.i(TAG, "hdb--onStartCommand-tSend:"+tSend);
		if (tSend == null && !Config.useUdp) {
			tSend = new TcpSocketSend(this);
			tSend.initServer();
			tSend.setConnectCallBack(new ConnectCallBack() {
				
				@Override
				public void onConnectSuccess() {
					SendActivity activity = SendActivity.getInstance();
					if (activity != null) {
						activity.onConnectSuccess();
					}
				}
				
				@Override
				public void onConnectFail() {
					SendActivity activity = SendActivity.getInstance();
					if (activity != null) {
						activity.onConnectFail();
					}
				}
			});
		}else {
			uSend = new UdpSocketSend();
		}
		registNetChangeReceiver();
		
		return START_STICKY;
	}
	
	public boolean getStartEncode(){
		if (Config.useUdp) {
			return uSend.hasStart();
		}
		return tSend.getStartEncode();
	}
	
	public void close(String ip){
		tSend.close(ip);
	}
	
	public void stopEncode(){
		uSend.stopEncode();
	}
	
	public void setIpAddr(String ip){
		uSend.setIpAddr(ip);
	}
	
	
	public void startReceord(MediaProjection mediaProjection) {
		if (Config.useUdp) {
			uSend.startReceord(mediaProjection);
		}else {
			tSend.startReceord(mediaProjection);
		}
		
	}
	
	public boolean findConnectedByIp(String ip) {
		return tSend.findConnectedByIp(ip);
	}
	
	public boolean hasConnect(){
		if (tSend == null) {
			return false;
		}
		return tSend.hasConnect();
	}
	
	
	private void registNetChangeReceiver(){
		
		if (mNetChangeReceiver == null) {
			mNetChangeReceiver = new NetChangeReceiver(new IWifiState() {
				
				@Override
				public void disconnect() {
					tSend.closeAll();
					SendActivity send = SendActivity.getInstance();
					if (send != null) {
						send.onWifiDisconnect();
					}
				}
				
				@Override
				public void connect() {
					SendActivity send = SendActivity.getInstance();
					if (send != null) {
						send.onWifiConnect();
					}
				}
			});
			
		}
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		registerReceiver(mNetChangeReceiver, filter);
	}
	
	private void unregistNetChangeReceiver(){
		if (mNetChangeReceiver != null) {
			unregisterReceiver(mNetChangeReceiver);
			mNetChangeReceiver = null;
		}
	}
	
	
	
	@Override
	public void onDestroy() {
		unregistNetChangeReceiver();
		super.onDestroy();
	}

}
