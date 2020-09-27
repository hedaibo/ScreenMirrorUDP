package com.action.screenmirror.utils;

/**
 * Created by Tianluhua on 2018/04/10
 */

public class Config {

	public static final boolean useUdp = true;
	
	public static class MotionEventKey {

		public static final String JACTION = "action";
		public static final String JX = "x";
		public static final String JY = "y";

		public static final int ACTION_DOWN = 0;
		public static final int ACTION_MOVE = 2;
		public static final int ACTION_UP = 1;
	}

	public static class ActionKey {
		public static final String CLIENT_IP_KEY = "phoneip:";
		public static final String SERVICE_START_KEY = "start:";
		
		public static final String SEND_PRE = "send";
		public static final String RECEIVER_PRE = "receiver";

	}

	public static class PortGlob {
		public static final int MULTIPORT = 9797;
		public static final int MULTIPORT_IPV6 = 19797;
		public static final int DATAPORT = 18686;
		public static final int TOUCHPORT = 18181;		
		public static final int AUDIOPORT = 18687;
		public static final int UDPPACKETPORT = 18688;

	}

	public static class HandlerGlod {
		
		public static final int GET_BROADCASTADDRESS = 0;
		public static final int CHECK_IPTYPE = 1;
		
		public static final long GET_BROADCASTADDRESS_DELAY = 1000;
		
		public static final int SEND_BROADCAST = 2;
		public static final long SEND_BROADCAST_DELAY = 500;
		
		public static final int CHECK_DEVICE_LIVE = 3;
		public static final long CHECK_DEVICE_LIVE_DELAY = 2000;
		
		public static final int CONNECT_SUCCESS = 4;
		public static final int TOUCH_CONNECT_SUCCESS = 5;
		public static final int DATA_CONNECT_SUCCESS = 6;
		public static final int CONNECT_FAIL = 7;
		public static final int TIME_OUT = 8;
		
		public static final int AUDIO_CONNECT_SUCCESS = 9;
		
		public static final int REQUEST_CONNECT_TIMEOUT = 10;
		public static final long REQUEST_CONNECT_TIMEOUT_DELAY = 5000;
		
		public static final int CHECK_ALIVE = 11;
		public static final long CHECK_ALIVE_DELAY = 5000;
	}

}
