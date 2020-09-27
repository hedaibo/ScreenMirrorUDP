package com.action.screenmirror.bean;

public class DeviceInfo {

	public static final int CONNECT_UNKNOW = 1;
	public static final int CONNECT_START = 2;
	public static final int CONNECT_FAIL = 3;
	public static final int CONNECT_SUCCESS = 4;
	public static final int CONNECT_TIMEOUT = 5;
	
    private String ipAddress;
    private String Name;
    private long time;
    private String ip6Address;
    
    private int connectState = CONNECT_UNKNOW;
    

    public int getConnectState() {
		return connectState;
	}

	public void setConnectState(int connectState) {
		this.connectState = connectState;
	}
	
	public String getConnectStateString(){
		String state = "";
		if (connectState == CONNECT_UNKNOW) {
			state = "";
		}else if (connectState == CONNECT_START) {
			state = "Connecting";
		}else if (connectState == CONNECT_SUCCESS) {
			state = "Connected";
		}else if (connectState == CONNECT_FAIL) {
			state = "";
		}else if (connectState == CONNECT_TIMEOUT) {
			state = "";
		}
		return state;
	}

	public DeviceInfo(String ipAddress, String name) {
        super();
        this.ipAddress = ipAddress;
        this.connectState = CONNECT_UNKNOW;
        Name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public void setIp6Address(String ip6Address) {
        this.ip6Address = ip6Address;
    }

    public String getValidAddress(){
        if (ip6Address != null && ip6Address.length() > 5) return ip6Address;

        return ipAddress;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "ipAddress='" + ipAddress + '\'' +
                ", Name='" + Name + '\'' +
                ", time=" + time +
                ", ip6Address='" + ip6Address + '\'' +
                '}';
    }
}
