package com.action.screenmirror.interf;

import java.util.ArrayList;

import com.action.screenmirror.bean.DeviceInfo;

public interface IReceiverData {
	void onReveiver(ArrayList<DeviceInfo> deviceInfos);
}
