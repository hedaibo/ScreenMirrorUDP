package com.action.screenmirror.model;

import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodec.Callback;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodec.CodecException;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.action.screenmirror.bean.SocketMole;
import com.action.screenmirror.egl.EGLRender;
import com.action.screenmirror.interf.IConnectCallback;
import com.action.screenmirror.interf.IDisconnectCallback;
import com.action.screenmirror.interf.IOnEncodeData;
import com.action.screenmirror.utils.Config;

public class MediaEncoder extends Thread {

	private final String TAG = "MediaEncoder";

	private final String mime_type = MediaFormat.MIMETYPE_VIDEO_AVC;

	private DisplayManager displayManager;
	private MediaProjection projection;
	private MediaCodec mEncoder;
	private VirtualDisplay virtualDisplay;
	private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
	private EGLRender eglRender;
	private Surface surface;

	// 屏幕相关
	private final static int VIDEO_WIDTH = 1024;
	private final static int VIDEO_HEIGHT = 600;
	private int screen_dpi = 1;

	// 编码参数相关
	private int frame_bit = 1200 * 1024;// 1.2MB
	private int frame_rate = 20;// 这里指的是Mediacodec30张图为1组 ，并不是视屏本身的FPS
	private int frame_internal = 1;// 关键帧间隔 一组加一个关键帧
	private final int TIMEOUT_US = 10000;
	private int video_fps = 30;
	private byte[] sps = null;
	private byte[] pps = null;
	protected static final int SEND_TIMEOUT = 0;

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case SEND_TIMEOUT:
				if (mDisconnectCallback != null) {
					Log.i(TAG, "hdb---SEND_TIMEOUT--");
					mDisconnectCallback.onDisconnect(mSocketMole);
				}
				break;

			default:
				break;
			}
		};
	};

	private OnScreenCallBack onScreenCallBack;

	public void setOnScreenCallBack(OnScreenCallBack onScreenCallBack) {
		this.onScreenCallBack = onScreenCallBack;
	}

	public interface OnScreenCallBack {
		void onScreenInfo(byte[] bytes);

		void onCutScreen(Bitmap bitmap);
	}

	/**
	 * 设置视频FPS
	 * 
	 * @param fps
	 */
	public MediaEncoder setVideoFPS(int fps) {
		video_fps = fps;
		return this;
	}

	/**
	 * 设置视屏编码采样率
	 * 
	 * @param bit
	 */
	public MediaEncoder setVideoBit(int bit) {
		frame_bit = bit;
		return this;
	}

	@Override
	public void run() {
		super.run();
		try {
			prepareEncoder();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (projection != null) {
			virtualDisplay = projection.createVirtualDisplay("screen", VIDEO_WIDTH, VIDEO_HEIGHT, screen_dpi,
					DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, eglRender.getDecodeSurface(), null, null);
		} else {
			virtualDisplay = displayManager.createVirtualDisplay("screen", VIDEO_WIDTH, VIDEO_HEIGHT, screen_dpi,
					eglRender.getDecodeSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC);
		}
		startRecordScreen();
		// release();
	}

	/**
	 * 初始化编码器
	 */
	private void prepareEncoder() throws IOException {
		MediaFormat mediaFormat = MediaFormat.createVideoFormat(mime_type, VIDEO_WIDTH, VIDEO_HEIGHT);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, frame_bit);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frame_rate);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, frame_internal);
		mEncoder = MediaCodec.createEncoderByType(mime_type);
		mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		surface = mEncoder.createInputSurface();
		eglRender = new EGLRender(surface, VIDEO_WIDTH, VIDEO_HEIGHT, video_fps);
		eglRender.setCallBack(new EGLRender.onFrameCallBack() {
			@Override
			public void onUpdate() {
				startEncode();
			}

			@Override
			public void onCutScreen(Bitmap bitmap) {
				onScreenCallBack.onCutScreen(bitmap);
			}

			@Override
			public void onstop() {
				release();
			}
		});
		mEncoder.start();

	}

	/**
	 * 开始录屏
	 */
	private void startRecordScreen() {
		eglRender.start();
		// release();
	}
	
	public boolean hasStart(){
		if (eglRender != null) {
			return eglRender.hasStart();
		}
		return false;
    	
    }

	private void startEncode() {
		ByteBuffer[] byteBuffers = null;
		// if (SysValue.api < 21) {
		// byteBuffers = mEncoder.getOutputBuffers();
		// }

		int index = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
		if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
			resetOutputFormat();
		} else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
			// Log.d("---", "retrieving buffers time out!");
			// try {
			// // wait 10ms
			// Thread.sleep(10);
			// } catch (InterruptedException e) {
			// }
		} else if (index >= 0) {
			// if (SysValue.api < 21) {
			// encodeToVideoTrack(byteBuffers[index]);
			// } else {
			encodeToVideoTrack(mEncoder.getOutputBuffer(index));
			// }
			mEncoder.releaseOutputBuffer(index, false);
		}
	}

	private void encodeToVideoTrack(ByteBuffer encodeData) {

		// ByteBuffer encodeData = mEncoder.getOutputBuffer(index);
		if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
			Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
			mBufferInfo.size = 0;
		}
		if (mBufferInfo.size == 0) {
			Log.d(TAG, "info.size == 0, drop it.");
			encodeData = null;
		} else {
			// Log.d(TAG, "got buffer, info: size=" + mBufferInfo.size
			// + ", presentationTimeUs=" + mBufferInfo.presentationTimeUs
			// + ", offset=" + mBufferInfo.offset);
		}
		if (encodeData != null) {
			encodeData.position(mBufferInfo.offset);
			encodeData.limit(mBufferInfo.offset + mBufferInfo.size);
			// muxer.writeSampleData(mVideoTrackIndex, encodeData,
			// mBufferInfo);//写入文件
			byte[] bytes;
			int flag = 0;
			if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
				// todo 关键帧上添加sps,和pps信息
				bytes = new byte[mBufferInfo.size + sps.length + pps.length];
				System.arraycopy(sps, 0, bytes, 0, sps.length);
				System.arraycopy(pps, 0, bytes, sps.length, pps.length);
				encodeData.get(bytes, sps.length + pps.length, mBufferInfo.size);
				flag = 1;
			} else {
				bytes = new byte[mBufferInfo.size];
				encodeData.get(bytes, 0, mBufferInfo.size);
				flag = 0;
			}
			// onScreenCallBack.onScreenInfo(bytes);
			if (Config.useUdp && mIEncodeData != null) {
				mIEncodeData.OnData(bytes);
			}else {
				onImageData(bytes, flag);
			}
			
		}
	}

	// private DataOutputStream mDataOutputStream;
	private ArrayList<SocketMole> mListSocketMoles = new ArrayList<SocketMole>();
	private IDisconnectCallback mDisconnectCallback;

	public void start(ArrayList<SocketMole> listSocketMoles, MediaProjection mediaProjection,
			IDisconnectCallback disconnectCallback) {
		mListSocketMoles = listSocketMoles;
		projection = mediaProjection;
		mDisconnectCallback = disconnectCallback;
		start();
	}
	
	private IOnEncodeData mIEncodeData;
	public void startForUdp( MediaProjection mediaProjection,
			IOnEncodeData iEncodeData) {
		projection = mediaProjection;
		mIEncodeData = iEncodeData;
		start();
	}

	private SocketMole mSocketMole = null;

	private void onImageData(byte[] buf, int flag) {
		// Log.v(TAG, "onImageData  " + buf.length +
		// "  ------  mListSocketMoles.size():"+mListSocketMoles.size());
		if (mListSocketMoles.size() > 0) {

			try {
				byte[] bytes = new byte[buf.length + 4];
				byte[] head = intToBuffer(buf.length);
				bytes[0] = (byte) flag;
				System.arraycopy(head, 0, bytes, 1, head.length);
				System.arraycopy(buf, 0, bytes, 4, buf.length);
				mHandler.removeMessages(SEND_TIMEOUT);
				mHandler.sendEmptyMessageDelayed(SEND_TIMEOUT, 1000);
				for (int i = 0; i < mListSocketMoles.size(); i++) {
//					Log.i(TAG, "onImageData  " + buf.length + "  ------  mListSocketMoles.size():"+mListSocketMoles.size());
					mSocketMole = mListSocketMoles.get(i);
					if (mSocketMole != null && mSocketMole.isConnect()) {
						DataOutputStream videoDos = mSocketMole.getVideoDos();
						videoDos.write(bytes);
						videoDos.flush();
					}
				}

				bytes = null;
				head = null;
			} catch (IOException e) {
				if (mDisconnectCallback != null) {
					mDisconnectCallback.onDisconnect(mSocketMole);
				}

				e.printStackTrace();
			}
		}
	}

	public static byte[] intToBuffer(int value) {
		byte[] src = new byte[3];
		src[2] = (byte) ((value >> 16) & 0xFF);
		src[1] = (byte) ((value >> 8) & 0xFF);
		src[0] = (byte) (value & 0xFF);
		return src;
	}

	private int mVideoTrackIndex;

	private void resetOutputFormat() {
		MediaFormat newFormat = mEncoder.getOutputFormat();
		Log.i(TAG, "output format changed.\n new format: " + newFormat.toString());
		getSpsPpsByteBuffer(newFormat);
		Log.i(TAG, "started media muxer, videoIndex=" + mVideoTrackIndex);
	}

	/**
	 * 获取编码SPS和PPS信息
	 * 
	 * @param newFormat
	 */
	private void getSpsPpsByteBuffer(MediaFormat newFormat) {
		sps = newFormat.getByteBuffer("csd-0").array();
		pps = newFormat.getByteBuffer("csd-1").array();
		// EventBus.getDefault().post(new EventLogBean("编码器初始化完成"));
	}

	public void stopScreen() {
		if (eglRender != null) {
			eglRender.stop();
		}
		mHandler.removeMessages(SEND_TIMEOUT);
	}

	public synchronized void release() {
		Log.i(TAG, "hdb----release------mEncoder:" + mEncoder);
		if (mEncoder != null) {
			mEncoder.stop();
			mEncoder.release();
			mEncoder = null;
		}
		if (virtualDisplay != null) {
			virtualDisplay.release();
		}
	}

	public void cutScreen() {
		eglRender.cutScreen();
	}
}
