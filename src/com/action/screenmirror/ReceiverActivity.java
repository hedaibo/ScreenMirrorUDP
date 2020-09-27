package com.action.screenmirror;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.action.screenmirror.bean.DeviceAdapter;
import com.action.screenmirror.bean.DeviceInfo;
import com.action.screenmirror.interf.IConnectCallback;
import com.action.screenmirror.interf.IConnectRequest;
import com.action.screenmirror.interf.IReceiverData;
import com.action.screenmirror.interf.IWifiState;
import com.action.screenmirror.model.TcpSocketReceiver;
import com.action.screenmirror.model.UdpSocketReceiver;
import com.action.screenmirror.model.UdpSocketSearcher;
import com.action.screenmirror.utils.Config;
import com.action.screenmirror.utils.DeviceInformation;
import com.action.screenmirror.utils.IpUtils;
import com.android.internal.graphics.palette.Palette.Swatch;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

public class ReceiverActivity extends Activity{
	
	private static final String TAG = "ReceiverActivity";

	private UdpSocketSearcher mSearcher;
	private SurfaceView mSurfaceView;
	private TcpSocketReceiver tReceiver;

	private TextView tvLocalName;

	private TextView tvLocalIp;

	private NetChangeReceiver mNetChangeReceiver;

	private UdpSocketReceiver uReceiver;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.activity_receiver);
		if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		initView();
		
	}

	private void initView() {

		tvLocalName = findViewById(R.id.tv_local_name);
		tvLocalIp = findViewById(R.id.tv_local_ip);
		mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
		if (Config.useUdp) {
			uReceiver = new UdpSocketReceiver(mSurfaceView,new IConnectCallback() {
				
				@Override
				public void onDisconnect() {
					Log.i(TAG, "hdb---onDisconnect--");
					setSurfaceViewInvisibility();
					showTopStateBar(true);
					
				}
				
				@Override
				public void onConnect() {
					setSurfaceViewVisibility();
					showTopStateBar(false);
					
				}
			});
			uReceiver.receiverUdpPckg();
		}else {
			tReceiver = new TcpSocketReceiver(mSurfaceView,new IConnectCallback() {
				
				@Override
				public void onDisconnect() {
					Log.i(TAG, "hdb---onDisconnect--");
					setSurfaceViewInvisibility();
					showTopStateBar(true);
				}
				
				@Override
				public void onConnect() {
					setSurfaceViewVisibility();
					showTopStateBar(false);
				}
			});
		}
		

	}
	
	private void setSurfaceViewVisibility(){
		if (mSurfaceView.getVisibility() != View.VISIBLE) {
			mSurfaceView.setVisibility(View.VISIBLE);
		}
	}
	
	private void setSurfaceViewInvisibility(){
		if (mSurfaceView.getVisibility() != View.GONE) {
			mSurfaceView.setVisibility(View.GONE);
		}
	}
	
	
	@Override
	protected void onStart() {
		super.onStart();
		registNetChangeReceiver();
	}
	
	private void getWifiState(){
		ConnectivityManager mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = mConnectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifiInfo.isConnected()) {
//			tvLocalIp.setText("Ip address:"+hostName);
			tvLocalName.setText("Name:"+DeviceInformation.getSerialNumber(this));
		} else {
			tvLocalIp.setText(getResources().getString(R.string.connect_wifi_info));
			tvLocalName.setText("Name:"+DeviceInformation.getSerialNumber(this));
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		unregistNetChangeReceiver();
		pause();
	}
	
	private void onWifiConnect(){
		mSearcher = new UdpSocketSearcher(this, true);
		mSearcher.startReceiverUdpBrodcast();
		mSearcher.setOnReceiverDataListener(new IConnectRequest() {
			
			@Override
			public void onReveiver(final ArrayList<DeviceInfo> deviceInfos) {
			}

			@Override
			public void onRequestConnect(final String ip) {
				runOnUiThread(new Runnable() {
					public void run() {
//						showTopStateBar(false);
						if (!Config.useUdp) {
							tReceiver.startDisPlayRomoteDesk(ip);
						}
						
					}
				});
			}
			
		});
		
		InetAddress localIp = IpUtils.getLocalIp();
		if (null != localIp) {
			String hostName = localIp.getHostAddress();
			if (hostName != null) {
				tvLocalIp.setText("Ip address:"+hostName);
				tvLocalName.setText("Name:"+DeviceInformation.getSerialNumber(this));
			}
		}
	}
	
	private void pause(){
		if (!Config.useUdp) {
			tReceiver.releseServer();
		}else {
			uReceiver.close();
		}
		
		if (mSearcher != null) {
			mSearcher.setOnReceiverDataListener(null);
			mSearcher.stopReceiverUdpBrodcast();
			mSearcher = null;
		}
	}
	
	
	private void registNetChangeReceiver(){
		if (mNetChangeReceiver == null) {
			mNetChangeReceiver = new NetChangeReceiver(new IWifiState() {
				
				@Override
				public void disconnect() {
					pause();
					tvLocalIp.setText(getResources().getString(R.string.connect_wifi_info));
				}
				
				@Override
				public void connect() {
					onWifiConnect();
				}
			});
			
			IntentFilter filter = new IntentFilter();
			filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
			registerReceiver(mNetChangeReceiver, filter);
		}
	}
	
	private void unregistNetChangeReceiver(){
		if (mNetChangeReceiver != null) {
			unregisterReceiver(mNetChangeReceiver);
			mNetChangeReceiver = null;
		}
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		if (tReceiver != null && !Config.useUdp) {
			tReceiver.sendTouchData(event.getAction(), (int)event.getX(), (int)event.getY());
		}
		return true;
	}
	
	
	private void showTopStateBar(boolean show) {
		Window window = getWindow();
		LayoutParams attributes = window.getAttributes();
		if (!show) {
			attributes.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
			window.setAttributes(attributes);
			window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		} else {
			attributes.flags &= ~(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			window.setAttributes(attributes);
			window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);  
            window.setStatusBarColor(getResources().getColor(android.R.color.black));
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		}
	}
	
	
}
