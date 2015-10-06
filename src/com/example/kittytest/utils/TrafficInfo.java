package com.example.kittytest.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.net.TrafficStats;
import android.util.Log;

/**
 * information of network traffic
 * 
 * 获取网络上传／下载情况，只能获取上传和下载的总速度，并不能够获取网络的具体状态
 * 
 */
public class TrafficInfo {

	private static final String LOG_TAG = "Kitty-" + TrafficInfo.class.getSimpleName();
	private static final int UNSUPPORTED = -1;

	private String uid;

	public TrafficInfo(String uid) {
		this.uid = uid;
	}

	/**
	 * get total network traffic, which is the sum of upload and download
	 * traffic.
	 * 
	 * 
	 * 
	 * @return total traffic include received and send traffic
	 */
	public long getTrafficInfo() {

		Log.i(LOG_TAG, "get traffic information");
		Log.d(LOG_TAG, "uid===" + uid);

		//receive traffic
		long rcvTraffic = UNSUPPORTED;
		//send traffic
		long sndTraffic = UNSUPPORTED;

		// Use getUidRxBytes and getUidTxBytes to get network traffic,these API
		// return both tcp and udp usage
		rcvTraffic = TrafficStats.getUidRxBytes(Integer.parseInt(uid));
		sndTraffic = TrafficStats.getUidTxBytes(Integer.parseInt(uid));

		if (rcvTraffic == UNSUPPORTED || sndTraffic == UNSUPPORTED) {
			return UNSUPPORTED;
		}

		RandomAccessFile rafRcv = null, rafSnd = null;
		String rcvPath = "/proc/uid_stat/" + uid + "/tcp_rcv";
		String sndPath = "/proc/uid_stat/" + uid + "/tcp_snd";

		try {
			rafRcv = new RandomAccessFile(rcvPath, "r");
			rafSnd = new RandomAccessFile(sndPath, "r");
			
			rcvTraffic = Long.parseLong(rafRcv.readLine());
			sndTraffic = Long.parseLong(rafSnd.readLine());
		} catch (FileNotFoundException e) {
			rcvTraffic = UNSUPPORTED;
			sndTraffic = UNSUPPORTED;
		} catch (NumberFormatException e) {
			Log.e(LOG_TAG, "NumberFormatException: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(LOG_TAG, "IOException: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (rafRcv != null) {
					rafRcv.close();
				}
				if (rafSnd != null)
					rafSnd.close();
			} catch (IOException e) {
				Log.w(LOG_TAG, "Close randomAccessFile exception: " + e.getMessage());
			}
		}

		if (rcvTraffic == UNSUPPORTED || sndTraffic == UNSUPPORTED) {
			return UNSUPPORTED;
		}else{
			return rcvTraffic + sndTraffic;
		}
	}
	
	private int getNetworkState(){
		return 0;
	}
	
	
	
	
	private void getDownloadSpeed(){
		
	}
	
	
}
