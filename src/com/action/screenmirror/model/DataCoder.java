package com.action.screenmirror.model;

import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;
import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.KEY_I_FRAME_INTERVAL;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import com.action.screenmirror.bean.MediaData;
import com.action.screenmirror.utils.ThreadPoolManager;
import com.android.internal.content.NativeLibraryHelper.Handle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageParser.NewPermissionInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaCodec;
import android.media.MediaCodec.Callback;
import android.media.ImageReader;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodec.CodecException;
import android.media.MediaPlayer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

public class DataCoder {
	
	private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video

	// hdb
	private final static int VIDEO_WIDTH = 1024;
	private final static int VIDEO_HEIGHT = 600;

	private final static int FRAME_RATE = 20;
	private final static int FRAME_INTERVAL = 5;
//	private final static int FRAME_BIT_RATE = 1600000;
	private SurfaceView mSurfaceView;
	private MediaCodec mCodec;
	
	public static final int NAL_SLICE = 1;
    public static final int NAL_SLICE_DPA = 2;
    public static final int NAL_SLICE_DPB = 3;
    public static final int NAL_SLICE_DPC = 4;
    public static final int NAL_SLICE_IDR = 5;
    public static final int NAL_SEI = 6;
    public static final int NAL_SPS = 7;
    public static final int NAL_PPS = 8;
    public static final int NAL_AUD = 9;
    public static final int NAL_FILLER = 12;
    

	private final static int FRAME_BIT_RATE = 1000*1024;


	private static final String MIMETYPE_VIDEO_AVC = "video/avc";

	private static final String TAG = "DataCoder";

//	protected static final int INFO_TRY_AGAIN_LATER = 0;
	

	private static final int COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface; //COLOR_FormatSurface

    private MediaCodec.BufferInfo vBufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec vEncoder;
    private Thread videoEncoderThread;
    private boolean videoEncoderLoop;

//    private DisplayManager mDisplayManager;
    private MediaProjection mMediaProjection;
//    private VirtualDisplay mVirtualDisplay;
	private int mCount = 0;
	private ArrayList<MediaData> bufferList = new ArrayList<MediaData>();
	
	private Handler mHandle;
	public DataCoder(Handler handler) {
		mHandle = handler;
	}
	
	@SuppressLint("InlinedApi")
	public void initDecoder(SurfaceView surfaceView) {
		mSurfaceView = surfaceView;
		try {
			mCodec = MediaCodec.createDecoderByType(MIME_TYPE);

			final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE,
					VIDEO_WIDTH, VIDEO_HEIGHT);
			format.setInteger(MediaFormat.KEY_BIT_RATE, FRAME_BIT_RATE);
			format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
			format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FRAME_INTERVAL);
			
			format.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAT);

			byte[] header_sps = { 0, 0, 0, 1, 39, 66, -32, 31, -115, 104, 4, 0, 77, -7, 97, 0, 0, 3, 0, 1, 0, 0, 3, 0, 50, 15, 16, 122, -128};
			byte[] header_pps = {0, 0, 0, 1, 40, -50, 50, 72 };

			format.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
			format.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
			mCodec.configure(format, mSurfaceView.getHolder().getSurface(),
					null, 0);
			mCodec.start();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	int generateIndex = 0;
	int TIMEOUT_USEC = 100;
	
	@SuppressWarnings("deprecation")
	/*public synchronized boolean onFrame(byte[] buf, int offset, int length) {
//		long startMs = System.currentTimeMillis();
		// Get input buffer index
		ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
		int inputBufferIndex = mCodec.dequeueInputBuffer(TIMEOUT_USEC);
		
		//if (inputBufferIndex >= 0) {
		while (inputBufferIndex < 0) {
			Log.e(TAG, "hdb---onFrame--w---inputBufferIndex  " + inputBufferIndex);
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			inputBufferIndex = mCodec.dequeueInputBuffer(TIMEOUT_USEC);
		}
		if (inputBufferIndex >= 0) {
			long ptsUsec = computePresentationTime(generateIndex);
			ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
			inputBuffer.clear();
			inputBuffer.put(buf, offset, length);
			mCodec.queueInputBuffer(inputBufferIndex, 0, length,
					ptsUsec, 0);
			generateIndex++;
		} else {
			Log.e(TAG, "hdb---onFrame-----inputBufferIndex  " + inputBufferIndex);
			return false;
		}
		// Get output buffer index
		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

		if (outputBufferIndex >= 0) {
			mCodec.releaseOutputBuffer(outputBufferIndex, true);
		}
//		long value = (bufferInfo.presentationTimeUs / 1000) - (System.currentTimeMillis() - startMs);
//		Log.i(TAG, "hdb---onFrame--outputBufferIndex:"+outputBufferIndex+" buf[0]"+buf[0]+" buf[1]"+buf[1]+" buf[2]"+buf[2]+" buf[3]"+buf[3]);
		
//		while (outputBufferIndex >= 0) {
//			mCodec.releaseOutputBuffer(outputBufferIndex, true);
//			outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
////			Log.e(TAG, "hdb---onFrame---while--outputBufferIndex  " + outputBufferIndex);
//		}
		return true;
	}*/
	
	
	public void onFrame(byte[] buf,int offset, int length) {
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        int inputBufferIndex = mCodec.dequeueInputBuffer(0);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, 0, buf.length);
            mCodec.queueInputBuffer(inputBufferIndex, 0, buf.length, mCount * 1000, 0);
            mCount++;
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo,0);
        while (outputBufferIndex >= 0) {
        	mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
	
	private static long computePresentationTime(int frameIndex) {
        return 132 + frameIndex * 1000000 / FRAME_RATE;
    }
	
	
	private DataOutputStream mDataOutputStream;
	public void start(DataOutputStream dataOutputStream, MediaProjection mediaProjection) {
		mDataOutputStream = dataOutputStream;
		mMediaProjection = mediaProjection;
        try {
            prepareVideoEncoder();
            startVideoEncode();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        videoEncoderLoop = false;

        if (null != vEncoder) {
            vEncoder.stop();
        }
        if(mMediaProjection != null){
        	mMediaProjection = null;
        } 
       // if(mMediaProjection!= null) mMediaProjection.stop();
    }

    private ImageReader mReader;
    
    public void prepareVideoEncoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FORMAT);
        format.setInteger(KEY_BIT_RATE, FRAME_BIT_RATE);
        format.setInteger(KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(KEY_I_FRAME_INTERVAL, FRAME_INTERVAL);
        
        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 200000);
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        //设置复用模式
        format.setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
        
        MediaCodec vencoder = MediaCodec.createEncoderByType(MIMETYPE_VIDEO_AVC);
        vencoder.configure(format, null, null, CONFIGURE_FLAG_ENCODE);
        Surface surface = vencoder.createInputSurface();
        
//        mReader = ImageReader.newInstance(VIDEO_WIDTH, VIDEO_HEIGHT, 1, 6);
        
//        mVirtualDisplay = mDisplayManager.createVirtualDisplay("-display", VIDEO_WIDTH, VIDEO_HEIGHT, 1,
//                surface, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR);//VIRTUAL_DISPLAY_FLAG_PUBLIC
        mMediaProjection.createVirtualDisplay("ScreenRecorder-display0", VIDEO_WIDTH, VIDEO_HEIGHT, 1,
        		DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
        vEncoder = vencoder;
        
        /*mReader.setOnImageAvailableListener(new OnImageAvailableListener() {
			
			@Override
			public void onImageAvailable(ImageReader imageReader) {
				Image mImage = mReader.acquireLatestImage();
				Log.i(TAG, "hdb---onImageAvailable---"+mImage.getWidth()+"  "+mImage.getHeight());
				mImage.close();
			}
		}, null);*/
    }

    public void startVideoEncode() {
        if (vEncoder == null) {
            throw new RuntimeException("请初始化视频编码器");
        }
        if (videoEncoderLoop) {
            throw new RuntimeException("必须先停止");
        }
        videoEncoderThread = new Thread() {
            @Override
            public void run() {
//                presentationTimeUs = System.currentTimeMillis() * 1000;
                vEncoder.start();
                videoEncoderLoop = true;
                while (videoEncoderLoop && !Thread.interrupted()) {
                    /*try {
                        ByteBuffer[] outputBuffers = vEncoder.getOutputBuffers();
                        int outputBufferId = vEncoder.dequeueOutputBuffer(vBufferInfo, 0);
                        if (outputBufferId >= 0) {
                            ByteBuffer bb = outputBuffers[outputBufferId];
                            onEncodedAvcFrame(bb, vBufferInfo);
                            vEncoder.releaseOutputBuffer(outputBufferId, false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }*/
                	
                	if (bufferList.size() > 0) {
//                		Log.i(TAG, "hdb--run--");
                		MediaData mediaData = getData();
                		if (mediaData != null) {
                			onEncodedAvcFrame(mediaData.getOutputBuffer(), mediaData.getBufferInfo(),mediaData.getIndex());
						}
						
					}
                }
            }
        };
        
        videoEncoderThread.start();
        
//        vEncoder.
//        vEncoder.PARAMETER_KEY_REQUEST_SYNC_FRAME
        
        vEncoder.setCallback(new Callback() {
			
			@Override
			public void onOutputFormatChanged(MediaCodec arg0, MediaFormat arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onOutputBufferAvailable(MediaCodec mediaCodec, int index, BufferInfo bufferInfo) {
				ByteBuffer outputBuffer = vEncoder.getOutputBuffer(index);
				Log.i(TAG, "hdb--onOutputBufferAvailable--");
				addData(outputBuffer, bufferInfo,index);
				
				/*ThreadPoolManager.getInstance().execute(new Runnable() {
					
					@Override
					public void run() {
						onEncodedAvcFrame(outputBuffer, bufferInfo);
						vEncoder.releaseOutputBuffer(index, false);
					}
				});*/
				
				
			}
			
			@Override
			public void onInputBufferAvailable(MediaCodec arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onError(MediaCodec arg0, CodecException arg1) {
				Log.i(TAG, "hdb---onError--");
				
			}
		});
    }
    
    private Object mLock = new Object();
    private void addData(ByteBuffer outputBuffer,BufferInfo bufferInfo,int index){
    	synchronized (mLock) {
//    		Log.i(TAG, "hdb--addData--");
    		bufferList.add(new MediaData(bufferInfo, outputBuffer,index));
		}
    }
    
    private MediaData getData(){
    	synchronized (mLock) {
    		if (bufferList.size() >=0) {
//    			Log.i(TAG, "hdb--getData--bufferList:"+bufferList.size());
    			return bufferList.remove(0);
			}
    		return null;
		}
    }

    private synchronized void onEncodedAvcFrame(ByteBuffer bb, MediaCodec.BufferInfo vBufferInfo,int index) {
    	
    //	Log.i(TAG, "hdb----b0:"+bb.get(0)+"b1:"+bb.get(1)+"b2:"+bb.get(2)+"b3:"+bb.get(3)+"b4:"+bb.get(4));
//    	Log.i(TAG, "hdb---onEncodedAvcFrame--size:"+vBufferInfo.size );
        int offset = 4;
        //判断帧的类型
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
        int type = bb.get(offset) & 0x1f;
        if (type == NAL_SPS) {
            //[0, 0, 0, 1, 103, 66, -64, 13, -38, 5, -126, 90, 1, -31, 16, -115, 64, 0, 0, 0, 1, 104, -50, 6, -30]
            //打印发现这里将 SPS帧和 PPS帧合在了一起发送
            // SPS为 [4，len-8]
            // PPS为后4个字节
        	final byte[] bytes1 = new byte[vBufferInfo.size];
            bb.get(bytes1);
            Log.d(TAG, "hdb---sps:" + Arrays.toString(bytes1));
          

        } else if (type == NAL_SLICE || type == NAL_SLICE_IDR) {
            final byte[] bytes = new byte[vBufferInfo.size];
            bb.get(bytes);
            
            byte flag;
            if ((vBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) == 1) {
				flag = 1;
			}else {
				flag = 0;
			}
            long presentationTimeUs = vBufferInfo.presentationTimeUs;
            int flags = vBufferInfo.flags;
            int offset2 = vBufferInfo.offset;
            int size = vBufferInfo.size;
            byte[] timeUs = longToBuffer(presentationTimeUs);
            Log.i(TAG, "hdb---presentationTimeUs:"+presentationTimeUs+"  vBufferInfo:"+flags+"  offset:"+offset2+"  size:"+size);
//            Log.i(TAG, "hdb---flags:"+flags+"   "+getByteStringHex(bytes, 50));
            vEncoder.releaseOutputBuffer(index, false);
            onImageData(bytes,flag,timeUs);
        }
    }
    
    private void onImageData(byte[] buf, byte flag, byte[] timeUs) {
//		Log.v(TAG, "onImageData  " + buf.length + "  ------  " + mDataOutputStream);
		if (null != mDataOutputStream) {
			try {
				byte[] bytes = new byte[buf.length + 3 + 5];
				byte[] head = intToBuffer(buf.length);
				bytes[0] = flag;
				System.arraycopy(timeUs, 0, bytes, 1, timeUs.length);
				System.arraycopy(head, 0, bytes, 5, head.length);
				System.arraycopy(buf, 0, bytes, 8, buf.length);
				mDataOutputStream.write(bytes);
				mDataOutputStream.flush();
				bytes = null;
				head = null;
			} catch (IOException e) {
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
	
	public static byte[] longToBuffer(long value) {
		byte[] src = new byte[4];
		src[3] = (byte) ((value >> 24) & 0xFF);
		src[2] = (byte) ((value >> 16) & 0xFF);
		src[1] = (byte) ((value >> 8) & 0xFF);
		src[0] = (byte) (value & 0xFF);
		return src;
	}
	
	@SuppressLint("InlinedApi") public void requestIFarme(){
		Log.i(TAG, "hdb--requestIFarme--vEncoder:"+vEncoder);
		if (null != vEncoder) {//request-sync  //bool AMessage::findAsInt64(const char *name, int64_t *value)
			Bundle params = new Bundle();
			params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
			Log.i(TAG, "hdb--requestIFarme-");
			vEncoder.setParameters(params);
		}
		
	}
	
//	int64_t timeUs;
//    CHECK(buffer->meta()->findInt64("timeUs", &timeUs));
	
	
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
