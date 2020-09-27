package com.action.screenmirror.model;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import org.json.JSONException;
import org.json.JSONObject;

import com.action.screenmirror.audio.AudioTrack;
import com.action.screenmirror.interf.IConnectCallback;
import com.action.screenmirror.interf.IOndisconnectCallback;
import com.action.screenmirror.utils.Config;
import com.action.screenmirror.utils.LogUtils;
import com.action.screenmirror.utils.ThreadPoolManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class TcpSocketReceiver {

	public static final String TAG = "TcpSocketReceiver";

	private String serverIp;
	private Socket touchSocket;
	private DataOutputStream touchDos;
	private DataInputStream touchDis;


	private Socket dataSocket;
	private boolean isRun = true;

	private DataInputStream dataDis;
	private DataOutputStream dataDos;
	private SurfaceView mSurfaceView;
	
	private Socket audioSocket;
	private DataInputStream audioDis;
	private IConnectCallback mConnectCallback;
	private static int VIDEO_WIDTH = 1024;//720;//1080;//1024;//
	private static int VIDEO_HEIGHT = 600;//1560;//1920;//600;//

	public TcpSocketReceiver(SurfaceView surfaceView,IConnectCallback connectCallback) {
		mSurfaceView = surfaceView;
		mConnectCallback = connectCallback;
		mSurfaceView.getHolder().addCallback(new Callback() {
			
			@Override
			public void surfaceDestroyed(SurfaceHolder arg0) {
				Log.i(TAG, "hdb---surfaceDestroyed----");
				releseServer();
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder hodler) {
				Log.i(TAG, "hdb---surfaceCreated----");
				ThreadPoolManager.getInstance().execute(new Runnable() {
					
					@Override
					public void run() {
						
						try {
							isRun = true;
							startCoder();
						} catch (IOException e) {
							e.printStackTrace();
							mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
						}
						
					}
				});
				
				/*ThreadPoolManager.getInstance().execute(new Runnable() {
					
					@Override
					public void run() {
						
							isRun = true;
							while (isRun) {
								byte[] buf = removeList();
								if (buf != null) {
									onFrame(buf);
								}
								
							}
					}
				});*/
				
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	/**
	 * 与远程服务端建立连接
	 * 
	 * @param serverIp
	 *            远程服务端设备的
	 */
	public void startDisPlayRomoteDesk(String serverIp) {
		Log.i(TAG,"hdb---startDisPlayRomoteDesk---ip:" + serverIp);
		this.serverIp = serverIp;
		startTouchServer();
		startServer(serverIp);
	}

	/**
	 * 和远程服务器建立TCP连接，用于屏幕事件的交互
	 */
	private void startTouchServer() {

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					touchSocket = new Socket(serverIp, Config.PortGlob.TOUCHPORT);
					touchDos = new DataOutputStream(touchSocket.getOutputStream());
					touchDis = new DataInputStream(touchSocket.getInputStream());
				} catch (Exception e) {
					e.printStackTrace();
					mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
				}
//				receiveAliveData();
			}
		}).start();

	}

	private void releseTouchServer() {
		LogUtils.i("hdb----onDestroy----releseTouchServer:");
		if (touchDos != null) {
			try {
				touchDos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (touchDis != null) {
			try {
				touchDis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (touchSocket != null) {
			try {
				touchSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		touchDis = null;
		touchDos = null;
		touchSocket = null;
	}
	
	private void releseAudioServer() {
		if (audioDis != null) {
			try {
				audioDis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (audioSocket != null) {
			try {
				audioSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		audioDis = null;
		audioSocket = null;
	}

	public void releseServer() {
		Log.i(TAG, "hdb---releseServer--");
		releseTouchServer();
		releseDateServer();
		releseAudioServer();
		closeCoder();
		stopAudioPlay();
	}

	public void reConnect(final String ip) {
		Log.e(TAG, "hdb------reConnect----");
		releseServer();
		startDisPlayRomoteDesk(ip);

	}

	/**
	 * 发送本地屏幕事件到远程服务端
	 * 
	 * @param actionType
	 *            时间类型
	 * @param changeX
	 *            事件对应的 X 值
	 * @param changeY
	 *            事件对应的 值
	 */
	public synchronized void sendTouchData(final int actionType, final int changeX, final int changeY) {
		LogUtils.i(TAG, "sendTouchData---action:" + actionType + "  changeX:" + changeX + "  changeY:" + changeY
				+ "  dos:" + touchDos);

		ThreadPoolManager.getInstance().execute(new Runnable() {
			@Override
			public void run() {
				if (touchDos != null) {
					if (changeX >= 0 && changeY >= 0) {
						JSONObject jObject = new JSONObject();
						try {
							jObject.put(Config.MotionEventKey.JACTION, actionType);
							jObject.put(Config.MotionEventKey.JX, changeX);
							jObject.put(Config.MotionEventKey.JY, changeY);
						} catch (JSONException e1) {
							e1.printStackTrace();
						}
						byte[] jBytes = jObject.toString().getBytes();
						byte[] intToByte = new byte[1];
						intToByte[0] = (byte) jBytes.length;
						byte[] data = new byte[jBytes.length + 1];
						System.arraycopy(intToByte, 0, data, 0, 1);
						System.arraycopy(jBytes, 0, data, 1, jBytes.length);
						LogUtils.i(TAG, "hdb----data:" + new String(data));
						writeTouchData(data);
					}

				}
			}

		});

	}

	private synchronized void writeTouchData(byte[] data) {
		try {
			if (touchDos != null) {
				touchDos.write(data);
				touchDos.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
		}
	}

	long time = 0;

	private void receiveAliveData() {
		try {
			while (true) {
				time = SystemClock.uptimeMillis();
				byte[] aLive = new byte[5];
				if (touchDis == null) {
					return;
				}
				touchDis.read(aLive);
				LogUtils.i("hdb----timeLive:" + (SystemClock.uptimeMillis() - time));
				time = SystemClock.uptimeMillis();
				mHandler.removeMessages(Config.HandlerGlod.TIME_OUT);
				mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.TIME_OUT, 10000);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private Handler mHandler = new TcpHandler(this);

	private DataCoder mCoder;

	public static class TcpHandler extends Handler {
		private WeakReference<TcpSocketReceiver> weakReference;
		public TcpHandler(TcpSocketReceiver tcpSocketReceiver) {
			weakReference = new WeakReference<TcpSocketReceiver>(tcpSocketReceiver);
		}

		@Override
		public void handleMessage(Message msg) {
			TcpSocketReceiver mTcpSocketReceiver = weakReference.get();
			if (mTcpSocketReceiver == null)
				return;
			switch (msg.what) {

			case Config.HandlerGlod.CONNECT_FAIL:
				mTcpSocketReceiver.releseServer();
				if (mTcpSocketReceiver.mConnectCallback != null) {
					mTcpSocketReceiver.mConnectCallback.onDisconnect();
				}
				break;

			case Config.HandlerGlod.CONNECT_SUCCESS:
				if (mTcpSocketReceiver.mConnectCallback != null) {
					mTcpSocketReceiver.mConnectCallback.onConnect();
				}
				break;
			case Config.HandlerGlod.TIME_OUT:
				Log.i(TAG, "hdb--TIME_OUT---mConnectCallback:"+mTcpSocketReceiver.mConnectCallback);
				mTcpSocketReceiver.releseServer();
				if (mTcpSocketReceiver.mConnectCallback != null) {
					mTcpSocketReceiver.mConnectCallback.onDisconnect();
				}
				break;
			case Config.HandlerGlod.CHECK_ALIVE:
				Log.i(TAG, "hdb--CHECK_ALIVE---");
				if (mTcpSocketReceiver.checkCount == mTcpSocketReceiver.receiverCount) {
					sendEmptyMessage(Config.HandlerGlod.TIME_OUT);
					return;
				}
				mTcpSocketReceiver.checkCount = mTcpSocketReceiver.receiverCount;
				removeMessages(Config.HandlerGlod.CHECK_ALIVE);
				sendEmptyMessageDelayed(Config.HandlerGlod.CHECK_ALIVE, Config.HandlerGlod.CHECK_ALIVE_DELAY);
				break;
			case Config.HandlerGlod.AUDIO_CONNECT_SUCCESS:
				mTcpSocketReceiver.startAudioPlay();
				break;

			default:
				break;
			}

		}
	}
	
	private static boolean useSles = true;
	private AudioTrack mAudioTrack;
	private void startAudioPlay(){
		if (useSles) {
			mAudioTrack = new AudioTrack();
			mAudioTrack.startAudioPlay(audioDis,new IOndisconnectCallback() {
				
				@Override
				public void onDisconnect() {
					mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
					
				}
			});
		}else {
			mAudioCoder = new AudioCoder();
			mAudioCoder.startAudioPlay(audioDis);
		}
		
	}
	private void stopAudioPlay(){
		Log.i(TAG, "hdb---stopAudioPlay----");
		if (useSles) {
			if (mAudioTrack != null) {
				mAudioTrack.stopAudioPlay();
				mAudioTrack = null;
			}
		}else {
			if (mAudioCoder != null) {
				mAudioCoder.stopPlay();
				mAudioCoder = null;
			}
		}
		
		
		
	}
	
	/**
	 * 开启远程服务端,通过远程服务器的
	 * 
	 * @param serverIp
	 */
	public void startServer(final String serverIp) {

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					LogUtils.i(TAG, "hdb---data--连接start");
					
					dataSocket = new Socket(serverIp, Config.PortGlob.DATAPORT);// 10.0.0.24
					dataSocket.setKeepAlive(true);
					dataDis = new DataInputStream(dataSocket.getInputStream());
					dataDos = new DataOutputStream(dataSocket.getOutputStream());
					LogUtils.i(TAG, "hdb---data--连接成功");
					mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_SUCCESS);
					
				} catch (Exception ex) {
					ex.printStackTrace();
					mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
				}
			}
		}).start();
		
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					audioSocket = new Socket(serverIp, Config.PortGlob.AUDIOPORT);// 10.0.0.24
					audioSocket.setKeepAlive(true);
					audioDis = new DataInputStream(audioSocket.getInputStream());
					mHandler.sendEmptyMessage(Config.HandlerGlod.AUDIO_CONNECT_SUCCESS);
					
				} catch (Exception ex) {
					ex.printStackTrace();
					mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
				}
			}
		}).start();

	}
	

	private MediaCodec decoder;

	
	private void startCoder() throws IOException{
		/*mCoder = new DataCoder(mHandler);
		mCoder.initDecoder(mSurfaceView);
		while (isRun) {
			byte[] head = new byte[8];
			dataDis.read(head);
			int flag = head[0];
			byte[] timeUs = new byte[4];
			byte[] length = new byte[3];
			System.arraycopy(head, 1, timeUs, 0, timeUs.length);
			System.arraycopy(head, 5, length, 0, length.length);
			int len = bufferToInt(length);
			int time = bufferToInt4(timeUs);
//			Log.v(TAG, "hdb---read len " + len +"  flag:"+flag+"  time:"+time);
			if (len > 0 && len < 1000000) {
				byte[] buf = new byte[len];
				dataDis.readFully(buf);
				if (flag == 0) {
					dataList.add(buf);
				}else {
					dataList.clear();
					dataList.add(buf);
				}
				Log.i(TAG, "hdb---flags:"+flag+"   length:"+buf.length);
				mCoder.onFrame(buf, 0, buf.length);
				buf = null;
			}
			
		}*/
		receiverCount = 0;
		checkCount = -1;
		Log.i(TAG, "hdb---startCoder--");
		mHandler.sendEmptyMessage(Config.HandlerGlod.CHECK_ALIVE);

		initMediaDecoder(mSurfaceView.getHolder().getSurface());
		while (isRun && dataDis != null) {
			
			byte[] head = new byte[4];
			dataDis.read(head);
			int flag = head[0];
			byte[] length = new byte[3];
			System.arraycopy(head, 1, length, 0, length.length);
			int len = bufferToInt(length);
//			Log.v(TAG, "hdb---read len " + len +"  flag:"+flag+"  time:"+time);
			if (len > 0 && len < 1000000) {
				byte[] buf = new byte[len];
				dataDis.readFully(buf);
//				Log.i(TAG, "hdb---flags:"+flag+"   length:"+buf.length);
				onFrame(buf);
				buf = null;
				receiverCount ++;
				if (receiverCount > 1000000) {
					receiverCount = 0;
				}
			}
			
		}
	}
	
	private int receiverCount = 0;
	private int checkCount = -1;
	
	private void initMediaDecoder(Surface surface) {
		synchronized (mCoderLock) {
			Log.i(TAG, "hdb----initMediaDecoder--VIDEO_WIDTH:"+VIDEO_WIDTH+"  VIDEO_HEIGHT:"+VIDEO_HEIGHT);
	        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT);
	        try {
	            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
	            decoder.configure(mediaFormat, surface, null, 0);
	            decoder.start();
	        } catch (IOException e) {
	            e.printStackTrace();
	            mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
	        }
		}
		
    }
	
	private Object mCoderLock = new Object();
	private void closeCoder(){
		synchronized (mCoderLock) {
			isRun = false;
			Log.i(TAG, "hdb----closeCoder---");
			if (null != decoder) {
				decoder.stop();
				decoder.release();
				decoder = null;
			}
		}
		
	}
	
	private int mCount = 0;

	private AudioCoder mAudioCoder;
	public void onFrame(byte[] buf) {
		synchronized (mCoderLock) {
			if(!isRun) return;
			ByteBuffer[] inputBuffers = decoder.getInputBuffers();
	        int inputBufferIndex = decoder.dequeueInputBuffer(0);
	        if (inputBufferIndex >= 0) {
	            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
	            inputBuffer.clear();
	            inputBuffer.put(buf, 0, buf.length);
	            decoder.queueInputBuffer(inputBufferIndex, 0, buf.length, mCount * 1000, 0);
	            mCount++;
	        }

	        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
	        int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo,0);
	        while (outputBufferIndex >= 0) {
	            decoder.releaseOutputBuffer(outputBufferIndex, true);
	            outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
	        }
		}
        
    }
	
	private static String getByteStringHex(byte[] data, int len) {
		char[] DIGITS_UPPER = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		if (data.length < len) {
			return "wrong length!";
		}
		StringBuffer ret = new StringBuffer(512);
		for (int i = 0; i < len; i++) {
			byte hf = (byte) ((data[i] >> 4) & 0x0F);
			byte lf = (byte) (data[i] & 0x0F);
			ret.append(DIGITS_UPPER[hf]);
			ret.append(DIGITS_UPPER[lf]);
			if ((i + 1) < len)
				ret.append(",");
		}
		return ret.toString();
	}
	
	public static int bufferToInt(byte[] src) {
		int value;
		value = (int) ((src[0] & 0xFF) | ((src[1] & 0xFF) << 8) | ((src[2] & 0xFF) << 16));
		return value;
	}
	
	public static int bufferToInt4(byte[] src) {
		int value;
		value = (int) ((src[0] & 0xFF) | ((src[1] & 0xFF) << 8) | ((src[2] & 0xFF) << 16) | ((src[3] & 0xFF) << 24));
		return value;
	}

	private void releseDateServer() {
		LogUtils.i("hdb----onDestroy----releseDateServer:");
		isRun = false;
		if (dataDis != null) {
			try {
				dataDis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (dataSocket != null) {
			try {
				dataSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (dataDos != null) {
			try {
				dataDos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		dataDos = null;
		dataDis = null;
		dataSocket = null;
	}

}
