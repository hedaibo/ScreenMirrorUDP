package com.action.screenmirror.audio;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.action.screenmirror.bean.SocketMole;

import android.util.Log;

public class AudioRecord {
	
	
	private static final String TAG = "AudioRecord";

	/*static {
		System.loadLibrary("open_sles");
	}*/


	private boolean mRecording;

	private ArrayList<SocketMole> mListSocketMoles;
	
	public void start(ArrayList<SocketMole> listSocketMoles){
		mListSocketMoles = listSocketMoles;
		createEngine();
		File file = new File("/storage/F8A6-E20E", "audio.pcm");
		createAudioRecord(file.getAbsolutePath(),new OnRecordCallback() {
			
			@Override
			public void onCallback(byte[] buf) {
				Log.i(TAG, "hdb---OnRecordCallback---bytes:"+buf.length);
				if (mListSocketMoles.size() > 0 && buf.length > 0) {
//					Log.i(TAG, "hdb---RecordThread-run-2-");
					byte[] bytes = new byte[buf.length + 3];
					byte[] head = intToBuffer(buf.length);
					System.arraycopy(head, 0, bytes, 0, head.length);
					System.arraycopy(buf, 0, bytes, head.length, buf.length);
					for (int i = 0; i < mListSocketMoles.size(); i++) {
//						Log.i(TAG, "hdb---RecordThread-run-3-mListSocketMoles:"+mListSocketMoles.size()+"  i:"+i);
						SocketMole socketMole = mListSocketMoles.get(i);
						if (socketMole != null && socketMole.isConnect()) {
							DataOutputStream audioDos = socketMole.getAudioDos();
							try {
								audioDos.write(bytes);
								audioDos.flush();
							} catch (IOException e) {
								e.printStackTrace();
							}
							
						}
					}
				}
				
			}
		});
		mRecording = true;
	}
		
		public static byte[] intToBuffer(int value) {
			byte[] src = new byte[3];
			src[2] = (byte) ((value >> 16) & 0xFF);
			src[1] = (byte) ((value >> 8) & 0xFF);
			src[0] = (byte) (value & 0xFF);
			return src;
		}
	
	public void _stop(){
		if (mRecording) {
			stop();
		}
		mRecording = false;
		shutdown();
		
	}
	
	public interface OnRecordCallback{
		void onCallback(byte[] byteBuffer);
		
	}
	
	
	/*public int onProgressCallBack(int a, int b) {
		Log.i(TAG, "hdb-111--onProgressCallBack---");
		return 0;
	}*/
	
	
	private int onProgressCallBack(int a, int b) {
		Log.i(TAG, "hdb-111--onProgressCallBack---");
		return 0;
	}
	
	
	public native void createEngine();
	 
	public native void createAudioRecord(String uri,OnRecordCallback i);
 
	public native void stop();
 
	public native void shutdown();
	

}
