package com.action.screenmirror.audio;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.action.screenmirror.interf.IOndisconnectCallback;
import com.action.screenmirror.utils.ThreadPoolManager;

import android.content.pm.PackageParser.NewPermissionInfo;
import android.media.AudioFormat;
import android.util.Log;


public class AudioTrack {
	
	static {
		System.loadLibrary("open_sles");
	}
	
	
	static final int sampleRate = 48000;//44100;//16000;
	static final int channels = 1;//AudioFormat.CHANNEL_IN_STEREO;
	static final int samleFormat = AudioFormat.ENCODING_PCM_16BIT;
	private PlayThread playThread;
	private DataInputStream mAudioDis;
	private volatile boolean start = false;
	private static final String TAG = "AudioTrack";
	private IOndisconnectCallback mCallback;
	
	private ArrayList<byte[]> mAudioList = new ArrayList<byte[]>();
	
	public void startAudioPlay(DataInputStream audioDis,IOndisconnectCallback ondisconnectCallback) {
		mCallback = ondisconnectCallback;
		Log.i(TAG, "hdb--startAudioPlay-");
		start = true;
		mAudioDis = audioDis;
		_startSL(sampleRate, samleFormat, channels);
		startPlay();
	}
	
	public synchronized void stopAudioPlay() {
		Log.i(TAG, "hdb--stopAudioPlay-mAudioDis:"+mAudioDis);
		start = false;
		if (mAudioDis != null) {
			try {
				mAudioDis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mAudioDis = null;
		}
		_stopSL();
		if (playThread != null) {
			playThread.interrupt();
			playThread = null;
		}
	}
	
	private void startPlay() {
		Log.i(TAG, "hdb--startPlay-playThread:"+playThread);
		if (playThread == null) {
			playThread = new PlayThread();
			playThread.start();
		}
		
		
		ThreadPoolManager.getInstance().execute(new Runnable() {
			
			@Override
			public void run() {
				while (start) {
					byte[] buf = remove();
					if (buf != null) {
						_writeSL(buf, buf.length);
					}
					
				}
				
			}
		});

	}
	
	private class PlayThread extends Thread {

		public void run() {
			try {
				Log.i(TAG, "hdb--run---start:"+start+"  interrupted():"+interrupted()+"  mAudioDis:"+mAudioDis);
				while (mAudioDis != null && start && !interrupted()) {
					byte[] length = new byte[3];
					mAudioDis.read(length);
					int len = bufferToInt(length);
//					Log.i(TAG, "hdb--PlayThread-read len "+len);
					if (len <= 0) {
						Log.e(TAG, "hdb--PlayThread-read len "+len);
						mCallback.onDisconnect();
						break;
					}
					byte[] buf = new byte[len];
					mAudioDis.readFully(buf);
					add(buf);
//					_writeSL(buf, buf.length);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.i(TAG, "hdb--11-end--"+start);
			stopAudioPlay();
		}
	};

	public static byte[] intToBuffer(int value) {
		byte[] src = new byte[3];
		src[2] = (byte) ((value >> 16) & 0xFF);
		src[1] = (byte) ((value >> 8) & 0xFF);
		src[0] = (byte) (value & 0xFF);
		return src;
	}
	
	public static int bufferToInt(byte[] src) {
		int value;
		value = (int) ((src[0] & 0xFF) | ((src[1] & 0xFF) << 8) | ((src[2] & 0xFF) << 16));
		return value;
	}
	
	private int mListSize = 0;
	
	private void add(byte[] data){
		synchronized (mListLock) {
			int size = mAudioList.size();
			if (size >= 2) {
				Log.e(TAG, "hdb----clear--delay--mAudioList:"+mAudioList.size());//for 音视频不同步
				mAudioList.clear();
			}else if(size == 1){
				mListSize ++;
				if (mListSize == 2) {
					mAudioList.clear();
					Log.e(TAG, "hdb----clear-11-delay--mAudioList:"+mAudioList.size());//for 音视频不同步
				}
			}else {
				mListSize = 0;
			}
			mAudioList.add(data);
//			Log.i(TAG, "hdb--------mAudioList:"+mAudioList.size());
		}
	}
	private byte[] remove(){
		synchronized (mListLock) {
			if (mAudioList.size() <= 0) {
				return null;
			}
			return mAudioList.remove(0);
		}
	}
	
	private Object mListLock = new Object();
	private Object mLock = new Object();
	
	private void _writeSL(byte[] data, int length){
		synchronized (mLock) {
			if (start) {
				writeSL(data, length);
			}
			
		}
	}
	
	private void _stopSL(){
		synchronized (mLock) {
			Log.i(TAG, "hdb--_stopSL-start:"+start);
			stopSL();
		}
	}
	
	private void _startSL(int sampleRate, int samleFormat,int channels){
		synchronized (mLock) {
			startSL(sampleRate, samleFormat, channels);
		}
	}
	
	
	
	public native void startSL(int sampleRate, int samleFormat,int channels);
	 
	public native void writeSL(byte[] data, int length);
 
	public native void stopSL();

}
