package com.action.screenmirror.utils;

/**
 * Created by Tianluhua on 2018/3/14.
 */

public class ByteUtils {

	public static int bufferToInt(byte[] src) {
		int value;
		value = (int) ((src[0] & 0xFF) | ((src[1] & 0xFF) << 8) | ((src[2] & 0xFF) << 16));
		return value;
	}

	public static int[] convertToColor_4byte(byte[] piex) {
		int[] colors = new int[1024 * 600];
		int len = piex.length;

		for (int i = 0; i < len; i += 4) {
			colors[i / 4] = (piex[i + 2] << 16) + (piex[i + 1] << 8) + piex[i];
		}
		return colors;
	}

	public static byte[] intToBuffer(int value) {
		byte[] src = new byte[3];
		src[2] = (byte) ((value >> 16) & 0xFF);
		src[1] = (byte) ((value >> 8) & 0xFF);
		src[0] = (byte) (value & 0xFF);
		return src;
	}

	public static String intToIp(int ipInt) {
		StringBuilder sb = new StringBuilder();
		sb.append(ipInt & 0xFF).append(".");
		sb.append((ipInt >> 8) & 0xFF).append(".");
		sb.append((ipInt >> 16) & 0xFF).append(".");
		sb.append((ipInt >> 24) & 0xFF);
		return sb.toString();
	}

}
