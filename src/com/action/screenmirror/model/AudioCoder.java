package com.action.screenmirror.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import com.action.screenmirror.bean.SocketMole;
import com.action.screenmirror.interf.IDisconnectCallback;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.os.Handler;
import android.util.Log;

public class AudioCoder {

	private volatile boolean isRecording = false;
	private RecordThread recordThread;
	private PlayThread playThread;
	static final int frequency = 48000;// 44100;//16000;
	static final int channelConfiguration = AudioFormat.CHANNEL_IN_STEREO;
	static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private static final String TAG = "AudioCoder";
	
	// private LinkedList<byte[]> mLinkedList = null;
	// private LinkedList<byte[]> mPlayList = null;
	int recBufSize, playBufSize;
	AudioRecord audioRecord;
	AudioTrack audioTrack;
	private byte[] buffer;

	// private DataOutputStream mAudioOutputStream;
	private ArrayList<SocketMole> mListSocketMoles = new ArrayList<SocketMole>();
	private DataInputStream mAudioDis;
	private IDisconnectCallback mCallback;
	
	

	public void startAudioRecord(ArrayList<SocketMole> listSocketMoles, IDisconnectCallback callback) {
		Log.i(TAG, "hdb--startAudioRecord--" + listSocketMoles.size());
		mCallback = callback;
		mListSocketMoles = listSocketMoles;
		initRecord();
		startRecord();
	}

	public void setSocketMoles(ArrayList<SocketMole> listSocketMoles) {
		Log.i(TAG, "hdb--setSocketMoles--" + listSocketMoles.size());
		mListSocketMoles = listSocketMoles;
	}

	public void startAudioPlay(DataInputStream audioDis) {
		mAudioDis = audioDis;
		initPlay();
		startPlay();
	}

	@SuppressWarnings("deprecation")
	private void initPlay() {
		playBufSize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency, channelConfiguration, audioEncoding,
				playBufSize, AudioTrack.MODE_STREAM);// MODE_STATIC
		Log.i(TAG, "hdb-----playBufSize:" + playBufSize);
		// audioTrack.setStereoVolume(1.0f, 1.0f);
	}

	private void initRecord() {
		recBufSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
		audioRecord = new AudioRecord(AudioSource.REMOTE_SUBMIX, frequency, channelConfiguration, audioEncoding,
				recBufSize);
		Log.i(TAG, "hdb-----recBufSize:" + recBufSize);

		// buffer = new byte[recBufSize];
	}

	private void startRecord() {
		if (!isRecording) {
			isRecording = true;
			if (recordThread == null) {
				recordThread = new RecordThread();
			}
			recordThread.start();
		}
	}

	private void startPlay() {
		// if (!isRecording) {
		isRecording = true;
		if (playThread == null) {
			playThread = new PlayThread();
		}
		playThread.start();
		// }
	}

	public synchronized void stopRecord() {
		Log.i(TAG, "hdb---stopRecord---isRecording:" + isRecording);
		if (isRecording) {
			isRecording = false;
			if (recordThread != null && recordThread.isAlive()) {
				recordThread.interrupt();
				recordThread = null;
			}
			if (audioRecord != null) {
				audioRecord.stop();
				audioRecord = null;
			}
		}
		
	}

	public synchronized void stopPlay() {
		if (isRecording) {
			isRecording = false;
			if (playThread != null && playThread.isAlive()) {
				playThread.interrupt();
				playThread = null;
			}
			if (audioRecord != null) {
				audioTrack.stop();
				audioTrack = null;
			}

		}

	}

	private SocketMole mSocketMole;
	private class RecordThread extends Thread {
		public void run() {
			while (isRecording) {

//				Log.i(TAG, "hdb---RecordThread-run--isRecording:" + isRecording);
				audioRecord.startRecording();

				// Log.i(TAG, "hdb---RecordThread-run-1-");
				buffer = new byte[recBufSize];
				int bufferReadResult = audioRecord.read(buffer, 0, recBufSize);
				byte[] buf = buffer.clone();
				buffer = null;
				if (mListSocketMoles.size() > 0 && buf.length > 0) {
					// Log.i(TAG, "hdb---RecordThread-run-2-");
					byte[] bytes = new byte[buf.length + 3];
					byte[] head = intToBuffer(buf.length);
//					Log.i(TAG,
//							"hdb---RecordThread---buf:" + buf.length + "  mListSocketMoles:" + mListSocketMoles.size());
					System.arraycopy(head, 0, bytes, 0, head.length);
					System.arraycopy(buf, 0, bytes, head.length, buf.length);
					for (int i = 0; i < mListSocketMoles.size(); i++) {
//						 Log.i(TAG,"hdb---RecordThread-run-3-mListSocketMoles:"+mListSocketMoles.size()+"  i:"+i);
						 mSocketMole = mListSocketMoles.get(i);
						 
						if (mSocketMole != null && mSocketMole.isConnect()) {
							try {
								DataOutputStream audioDos = mSocketMole.getAudioDos();
								audioDos.write(bytes);
								audioDos.flush();
							} catch (IOException ex) {
								ex.printStackTrace();
								if (mCallback != null) {
									mCallback.onDisconnect(mSocketMole);
								}
							}
						}

					}

				}

			}
			Log.i(TAG, "hdb---RecordThread-run--end");
			// stopRecord();

		}
	};

	private class PlayThread extends Thread {
		public void run() {
			try {
				// Log.i(TAG, "hdb--PlayThread--run");

				audioTrack.play();
				while (isRecording && mAudioDis != null) {
					byte[] length = new byte[3];
					mAudioDis.read(length);
					int len = bufferToInt(length);
					byte[] buf = new byte[len];
					mAudioDis.readFully(buf);
					// Log.v(TAG,
					// "hdb--PlayThread-read len "+len+buf.toString());
					// audioTrack.write(buf, 0, buf.length);

					/**
					 * ENCODING_PCM_16BIT 转short 消除杂音
					 * */
					short[] sData = new short[len / 2];
					int j = 0;
					for (int i = 0; i < sData.length; i++) {
						j = 2 * i;
						sData[i] = (short) ((buf[j] & 0xff) | ((buf[j + 1] & 0xff) << 8));
					}
					audioTrack.write(sData, 0, sData.length);

				}

			} catch (Exception ex) {
				ex.printStackTrace();

			}
			stopPlay();

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

}
