package com.action.screenmirror.bean;

import java.util.ArrayList;

public class ByteList {
	private byte[] buf;
	private int index;
	private int count;
	private int pIndex;
	private int lenght;
	private int pLenght;
	public ByteList(byte[] buf, int index, int count, int pIndex, int lenght, int pLenght) {
		super();
		this.buf = buf;
		this.index = index;
		this.count = count;
		this.pIndex = pIndex;
		this.lenght = lenght;
		this.pLenght = pLenght;
	}
	
	public byte[] getBuf() {
		return buf;
	}
	public void setBuf(byte[] buf) {
		this.buf = buf;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public int getpIndex() {
		return pIndex;
	}
	public void setpIndex(int pIndex) {
		this.pIndex = pIndex;
	}
	public int getLenght() {
		return lenght;
	}
	public void setLenght(int lenght) {
		this.lenght = lenght;
	}
	public int getpLenght() {
		return pLenght;
	}
	public void setpLenght(int pLenght) {
		this.pLenght = pLenght;
	}
	
	
	

}
