package com.action.screenmirror.model;

import java.io.DataInputStream;

import org.json.JSONObject;

import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.action.screenmirror.bean.SocketMole;
import com.action.screenmirror.interf.IDisconnectCallback;
import com.action.screenmirror.utils.Config;
import com.action.screenmirror.utils.EventInputUtils;
import com.action.screenmirror.utils.LogUtils;

public class ReceiverTouchDataRunnable implements Runnable{
	
	private static final String TAG = "ReceiverTouchDataRunnable";
	private SocketMole socketMole;
	private DataInputStream dis;
	private boolean canMove = false;
	private IDisconnectCallback mCallback;
	
	
	public ReceiverTouchDataRunnable(SocketMole socketMole,IDisconnectCallback callback) {
		this.socketMole = socketMole;
		dis = socketMole.getTouchDis();
		mCallback = callback;
	}

	@Override
	public void run() {
		
		byte[] buffer = new byte[1];
		LogUtils.i(TAG, "hdb-------isTouchRun:" + socketMole.isTouchRun());
		try {
			while (socketMole.isTouchRun()) {
				int readLine = dis.read(buffer);

				// LogUtils.i(TAG, "hdb-------readLine:" + readLine
				// + "   buffer:" + buffer[0]);

				if (readLine > 0) {

					byte[] data = new byte[buffer[0]];
					// LogUtils.i(TAG, "hdb--------data:" + new
					// String(data));
					dis.readFully(data);
					String point = new String(data, 0, data.length);

					if (point != null) {
						 Log.i(TAG, "hdb--------point:" + point);
						JSONObject jObject = new JSONObject(point);
						int action = jObject
								.getInt(Config.MotionEventKey.JACTION);
						int x = jObject
								.getInt(Config.MotionEventKey.JX);
						int y = jObject
								.getInt(Config.MotionEventKey.JY);
						
						if (action == MotionEvent.ACTION_DOWN) {
							canMove = true;
						}else if(action == MotionEvent.ACTION_UP){
							canMove = false;
						}
						
						if ((action == MotionEvent.ACTION_MOVE && canMove) || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
							EventInputUtils.injectMotionEvent(
									InputDevice.SOURCE_TOUCHSCREEN, action,
									SystemClock.uptimeMillis(), x, y, 1.0f);
						}
						
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			mCallback.onDisconnect(socketMole);
		}

	}

}
