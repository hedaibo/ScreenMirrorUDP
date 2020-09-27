package com.action.screenmirror.bean;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.pm.PackageParser.NewPermissionInfo;

import com.action.screenmirror.model.ReceiverTouchDataRunnable;
import com.action.screenmirror.utils.ThreadPoolManager;


public class SocketMole {
	
	/**video*/
//	private ServerSocket videoSocketsService;
	private Socket videoSocket;
	private DataOutputStream videoDos;
	private DataInputStream videoDisAck;

	/**touch*/
//	private ServerSocket touchSocketsService;
	private Socket touchSocket;
	private DataInputStream touchDis;
	private DataOutputStream touchDos;
	
	/**audio*/
//	private ServerSocket audioSocketsService;
	private Socket audioSocket;
	private DataOutputStream audioDos;
	
	
	//touch receiver flag
	private boolean isTouchRun = false;
	private ReceiverTouchDataRunnable runnable = null;
	
	private Object mLock = new Object();
	

	public ReceiverTouchDataRunnable getRunnable() {
		return runnable;
	}

	public void setRunnable(ReceiverTouchDataRunnable runnable) {
		this.runnable = runnable;
	}

	public boolean isTouchRun() {
		return isTouchRun;
	}

	public void setTouchRun(boolean isTouchRun) {
		this.isTouchRun = isTouchRun;
	}

	public boolean isConnect(){
		synchronized (mLock) {
			if (null != videoDos && null != touchDis && null != audioDos) {
				return true;
			}
			return false;
		}
		
	}
	
	public void close(){
		synchronized (mLock) {
			isTouchRun = false;
			if (runnable != null) {
				ThreadPoolManager.getInstance().remove(runnable);
				runnable = null;
			}
			closeVideo();
			closeAudio();
			closeTouch();
		}
		
	}
	
	private void closeVideo(){
		if(null != videoDisAck){
			try {
				videoDisAck.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(null != videoDos){
			try {
				videoDos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (null != videoSocket) {
			try {
				videoSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		videoDisAck = null;
		videoDos = null;
		videoSocket = null;
	}
	
	private void closeAudio(){
		if(null != audioDos){
			try {
				audioDos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (null != audioSocket) {
			try {
				audioSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		audioDos = null;
		audioSocket = null;
	}
	
	private void closeTouch(){
		if(null != touchDis){
			try {
				touchDis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(null != touchDos){
			try {
				touchDos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (null != touchSocket) {
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
	
	
	public void setVideoSockets(Socket videoSocket,DataOutputStream videoDos,DataInputStream videoDisAck) {
		this.videoSocket = videoSocket;
		this.videoDos = videoDos;
		this.videoDisAck = videoDisAck;
	}
	public Socket getVideoSocket() {
		return videoSocket;
	}
	public void setVideoSocket(Socket videoSocket) {
		this.videoSocket = videoSocket;
	}
	public DataOutputStream getVideoDos() {
		return videoDos;
	}
	public void setVideoDos(DataOutputStream videoDos) {
		this.videoDos = videoDos;
	}
	public DataInputStream getVideoDisAck() {
		return videoDisAck;
	}
	public void setVideoDisAck(DataInputStream videoDisAck) {
		this.videoDisAck = videoDisAck;
	}
	
	public void setTouchSockets(Socket touchSocket,DataInputStream touchDis,DataOutputStream touchDos) {
		this.touchSocket = touchSocket;
		this.touchDis = touchDis;
		this.touchDos = touchDos;
	}
	public Socket getTouchSocket() {
		return touchSocket;
	}
	public void setTouchSocket(Socket touchSocket) {
		this.touchSocket = touchSocket;
	}
	public DataInputStream getTouchDis() {
		return touchDis;
	}
	public void setTouchDis(DataInputStream touchDis) {
		this.touchDis = touchDis;
	}
	public DataOutputStream getTouchDos() {
		return touchDos;
	}
	public void setTouchDos(DataOutputStream touchDos) {
		this.touchDos = touchDos;
	}
	
	public void setAudioSockets(Socket audioSocket,DataOutputStream audioDos) {
		this.audioSocket = audioSocket;
		this.audioDos = audioDos;
	}
	public Socket getAudioSocket() {
		return audioSocket;
	}
	public void setAudioSocket(Socket audioSocket) {
		this.audioSocket = audioSocket;
	}
	public DataOutputStream getAudioDos() {
		return audioDos;
	}
	public void setAudioDos(DataOutputStream audioDos) {
		this.audioDos = audioDos;
	}
	
	
}
