package com.action.screenmirror.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.json.JSONObject;

import com.action.screenmirror.audio.AudioRecord;
import com.action.screenmirror.bean.SocketMole;
import com.action.screenmirror.interf.IDisconnectCallback;
import com.action.screenmirror.utils.Config;
import com.action.screenmirror.utils.LogUtils;
import com.action.screenmirror.utils.ThreadPoolManager;
import com.action.screenmirror.utils.EventInputUtils;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

public class TcpSocketSend {

	private static boolean useSles = false;
	private static final String TAG = "TcpSocketSend";

	/** video */
	private ServerSocket videoSocketsService;

	/** audio */
	private ServerSocket audioSocketsService;

	/** touch */
	private ServerSocket touchSocketsService;
	
	private DataCoder mCoder;
	private MediaEncoder mediaEncoder;

	private SocketMole mSocketMole = null;
	private ArrayList<SocketMole> mListSocketMoles = new ArrayList<SocketMole>();

	private DisplayManager mDisplayManager;

	private Context mContext;

	public TcpSocketSend(Context context) {
		mContext = context;
		mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
	}

	public void initServer() {

		isRun = true;
		if (null == videoSocketsService) {
			initVideoServer();
		}
		if (null == audioSocketsService) {
			initAudioServer();
		}
		if (null == touchSocketsService) {
			initTouchServer();
		}
	}
	
	private void startConnect(){
		if (mSocketMole != null) {
			mSocketMole = null;
		}
		mSocketMole = new SocketMole();
	}

	private boolean isRun = false;
	private void initVideoServer() {
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				try {
					videoSocketsService = new ServerSocket(Config.PortGlob.DATAPORT);
					while(isRun){
						Log.i(TAG, "hdb-initVideoServer-isRun--");
						
						Socket videoSocket = videoSocketsService.accept();
						DataOutputStream videoDos = new DataOutputStream(videoSocket.getOutputStream());
						DataInputStream videoDisAck = new DataInputStream(videoSocket.getInputStream());
						if (mSocketMole == null) {
							mSocketMole = new SocketMole();
						}
						mSocketMole.setVideoSockets(videoSocket, videoDos, videoDisAck);
						
						if (videoDos != null) {
							mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.DATA_CONNECT_SUCCESS, 0);
						}
					}
					
				} catch (Exception ex) {
					ex.printStackTrace();
					mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
				}

			}
		});
	}

	private void initAudioServer() {
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				try {
					audioSocketsService = new ServerSocket(Config.PortGlob.AUDIOPORT);
					while(isRun){
						Log.i(TAG, "hdb-initAudioServer-isRun--");
						Socket audioSocket = audioSocketsService.accept();
						DataOutputStream audioDos = new DataOutputStream(audioSocket.getOutputStream());
						if (mSocketMole == null) {
							mSocketMole = new SocketMole();
						}
						mSocketMole.setAudioSockets(audioSocket, audioDos);
						if (audioDos != null) {
							mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.AUDIO_CONNECT_SUCCESS, 0);
						}
					}	
					
				} catch (Exception ex) {
					ex.printStackTrace();
					mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
				}

			}
		});
	}

	private void initTouchServer() {
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				try {
					touchSocketsService = new ServerSocket(Config.PortGlob.TOUCHPORT);
					while(isRun){
						Log.i(TAG, "hdb-initTouchServer-isRun--");
						Socket touchSocket = touchSocketsService.accept();
						DataInputStream touchDis = new DataInputStream(touchSocket.getInputStream());
						DataOutputStream touchDos = new DataOutputStream(touchSocket.getOutputStream());
						
						InetAddress inetAddress = touchSocket.getInetAddress();
						String hostAddress = inetAddress.getHostAddress();
						Log.i(TAG, "hdb-initTouchServer-isRun--hostAddress:"+hostAddress);
						
						if (mSocketMole == null) {
							mSocketMole = new SocketMole();
						}
						mSocketMole.setTouchSockets(touchSocket, touchDis, touchDos);
						if (touchDis != null) {
							mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.TOUCH_CONNECT_SUCCESS, 0);
						}
					}
					
				} catch (Exception ex) {
					ex.printStackTrace();
					mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
				}

			}
		});
	}


	public void startReceord(MediaProjection mediaProjection) {
		/*
		 * if (null == mCoder && null != dataOutputStream) { mCoder = new
		 * DataCoder(mHandler); mCoder.start(dataOutputStream, mediaProjection);
		 * }
		 */
		if (null == mediaEncoder && mListSocketMoles.size() > 0) {
			mediaEncoder = new MediaEncoder();
			mediaEncoder.start(mListSocketMoles, mediaProjection,new IDisconnectCallback() {
				
				@Override
				public void onDisconnect(SocketMole socketMole) {
					_close(socketMole);
				}
			});
		}

	}

	private ConnectCallBack mConnCallBack;

	public void setConnectCallBack(ConnectCallBack connCallBack) {
		mConnCallBack = connCallBack;
	}

	public interface ConnectCallBack {
		void onConnectSuccess();
		void onConnectFail();
	}

	protected void receiverTouchData() {
		int size = mListSocketMoles.size();
		for (int i = 0; i < mListSocketMoles.size(); i++) {
			SocketMole socketMole = mListSocketMoles.get(i);
			if (!socketMole.isTouchRun()) {
				socketMole.setTouchRun(true);
				ReceiverTouchDataRunnable reRunnable = new ReceiverTouchDataRunnable(socketMole,new IDisconnectCallback() {
					
					@Override
					public void onDisconnect(SocketMole socketMole) {
						_close(socketMole);
						
					}
				});
				ThreadPoolManager.getInstance().execute(reRunnable);
				socketMole.setRunnable(reRunnable);
				
			}
		}
	}


	

	private Handler mHandler = new DataHandler(this);
	public static class DataHandler extends Handler {
		WeakReference<TcpSocketSend> weakReference;

		public DataHandler(TcpSocketSend mTransmitter) {
			weakReference = new WeakReference<TcpSocketSend>(mTransmitter);

		}

		@Override
		public void handleMessage(Message msg) {
			TcpSocketSend mSend = weakReference.get();
			if (mSend == null)
				return;

			switch (msg.what) {
			case Config.HandlerGlod.DATA_CONNECT_SUCCESS:
				Log.i(TAG, "hdb--------DATA_CONNECT_SUCCESS-----------");
				mSend.saveSocketModle();
				break;

			case Config.HandlerGlod.TOUCH_CONNECT_SUCCESS:
				Log.i(TAG, "hdb--------TOUCH_CONNECT_SUCCESS-----------");
				mSend.saveSocketModle();
				break;
			case Config.HandlerGlod.CONNECT_FAIL:
				Log.i(TAG, "hdb--------CONNECT_FAIL-----------");
				mSend.connectFail();
				break;
			case Config.HandlerGlod.AUDIO_CONNECT_SUCCESS:
				Log.i(TAG, "hdb--------AUDIO_CONNECT_SUCCESS-----------");
				mSend.saveSocketModle();
				break;

			default:
				break;
			}

		}

	}
	
	private synchronized void _close(SocketMole socketMole){
		if (socketMole != null) {
			socketMole.close();
			if (mListSocketMoles.contains(socketMole)) {
				mListSocketMoles.remove(socketMole);
				Log.i(TAG, "hdb---_close---mListSocketMoles.size():"+mListSocketMoles.size());
				if (mListSocketMoles.size() == 0) {
					if (mediaEncoder != null) {
						mediaEncoder.stopScreen();
//						mediaEncoder.release();
						mediaEncoder = null;
					}
					if (mAudioCoder != null) {
						mAudioCoder.stopRecord();
						mAudioCoder = null;
					}
					if (mAudioRecord != null) {
						mAudioRecord.stop();
						mAudioRecord = null;
					}
					startEncode = false;
				}
			}
			socketMole = null;
		}
		if (mConnCallBack != null) {
			mConnCallBack.onConnectFail();
		}
	}
	private void connectFail(){
		_close(mSocketMole);
		
	}
	
	private SocketMole tmpModel = new SocketMole();
	private synchronized void saveSocketModle(){
		boolean connectOver = connectOver();
		Log.i(TAG, "hdb----connectOver:"+connectOver);
		if (connectOver) {
			tmpModel = mSocketMole;
			Log.i(TAG, "hdb----connectOver:"+connectOver+"   "+mListSocketMoles.contains(tmpModel));
			if (mListSocketMoles.contains(tmpModel)) {
				return;
			}
			mListSocketMoles.add(tmpModel);
			if (mConnCallBack != null) {
				mConnCallBack.onConnectSuccess();
			}
			receiverTouchData();
			startAudioCoder();
//			if (mListSocketMoles.size() == 1) {
//				checkAlive();
//			}
			mSocketMole = null;
			if (startEncode) {
				return;
			}
			startEncode = true;
			
			
			
		}
	}
	
	private void checkAlive(){
		ThreadPoolManager.getInstance().execute(new Runnable() {
			
			@Override
			public void run() {
				while (mListSocketMoles.size() > 0) {
					for (int i = 0; i < mListSocketMoles.size(); i++) {
						SocketMole socketMole = mListSocketMoles.get(i);
						if (socketMole != null && socketMole.isConnect()) {
							try {
								LogUtils.e(TAG, "hdb-----sendUrgentData---");
								Socket touchT = socketMole.getTouchSocket();
								touchT.sendUrgentData(0xFF);
							} catch (IOException ex) {
								ex.printStackTrace();
								LogUtils.e(TAG, "hdb-----checkAlive--error-");
								_close(socketMole);
							}
						}
					}
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					
				}
			}
		});
	}
	
	private boolean startEncode = false;

	private AudioCoder mAudioCoder;
	private AudioRecord mAudioRecord;
	public boolean getStartEncode(){
		return startEncode;
	}
	private boolean connectOver(){
		if (null != mSocketMole && mSocketMole.isConnect()) {
			return true;
		}
		return false;
	}

	private void startAudioCoder() {
		if (useSles) {
			mAudioRecord = new AudioRecord();
			mAudioRecord.start(mListSocketMoles);
		}else {
			Log.i(TAG, "hdb---startAudioCoder---mAudioCoder:"+mAudioCoder);
			if (mAudioCoder == null) {
				mAudioCoder = new AudioCoder();
				mAudioCoder.startAudioRecord(mListSocketMoles,new IDisconnectCallback() {
					
					@Override
					public void onDisconnect(SocketMole socketMole) {
						Log.i(TAG, "hdb--AudioRecord-onDisconnect---");
						_close(socketMole);
					}
				});
			}else {
				mAudioCoder.setSocketMoles(mListSocketMoles);
			}
			
		}
		
	}

	public void close(String ip) {
		SocketMole moleByIp = findSocketMoleByIp(ip);
		_close(moleByIp);
	}
	
	private SocketMole findSocketMoleByIp(String ip){
		if (mListSocketMoles.size() > 0) {
			for (int i = 0; i < mListSocketMoles.size(); i++) {
				SocketMole socketMole = mListSocketMoles.get(i);
				Socket touchSocket = socketMole.getTouchSocket();
				String hostAddress = touchSocket.getInetAddress().getHostAddress();
				if (ip.equalsIgnoreCase(hostAddress)) {
					return socketMole;
				}
			}
		}
		
		return null;
	}
	
	public boolean findConnectedByIp(String ip){
		if (mListSocketMoles.size() > 0) {
			for (int i = 0; i < mListSocketMoles.size(); i++) {
				SocketMole socketMole = mListSocketMoles.get(i);
				Socket touchSocket = socketMole.getTouchSocket();
				String hostAddress = touchSocket.getInetAddress().getHostAddress();
				if (ip.equalsIgnoreCase(hostAddress)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	
	public boolean hasConnect(){
		
		return (mListSocketMoles.size() > 0) ? true : false;
	}
	
	public void closeAll(){
		if (mListSocketMoles.size() > 0) {
			for (int i = 0; i < mListSocketMoles.size(); i++) {
				SocketMole socketMole = mListSocketMoles.get(i);
				_close(socketMole);
			}
		}
	}
	

}
