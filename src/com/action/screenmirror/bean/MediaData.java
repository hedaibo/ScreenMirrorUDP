package com.action.screenmirror.bean;

import java.nio.ByteBuffer;

import android.media.MediaCodec.BufferInfo;

public class MediaData {
	private BufferInfo bufferInfo;
	private ByteBuffer outputBuffer;
	private int index;
	
	
	public MediaData(BufferInfo bufferInfo, ByteBuffer outputBuffer, int index) {
		super();
		this.bufferInfo = bufferInfo;
		this.outputBuffer = outputBuffer;
		this.index = index;
	}
	public BufferInfo getBufferInfo() {
		return bufferInfo;
	}
	public void setBufferInfo(BufferInfo bufferInfo) {
		this.bufferInfo = bufferInfo;
	}
	public ByteBuffer getOutputBuffer() {
		return outputBuffer;
	}
	public void setOutputBuffer(ByteBuffer outputBuffer) {
		this.outputBuffer = outputBuffer;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	

}
