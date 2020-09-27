package com.action.screenmirror.bean;

import java.util.ArrayList;


public class BufferList<MediaData> extends ArrayList<MediaData>{

	private Object mLock = new Object();
	
	private void addData(MediaData data){
		synchronized (mLock) {
			add(data);
		}
	}
	
	private void removeData(int index){
		synchronized (mLock) {
			remove(index);
		}
	}
}
