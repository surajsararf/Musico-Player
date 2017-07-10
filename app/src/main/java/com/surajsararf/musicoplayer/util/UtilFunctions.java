package com.surajsararf.musicoplayer.util;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.surajsararf.musicoplayer.service.SongPlayback;

public class UtilFunctions extends Application {
	static String LOG_CLASS = "UtilFunctions";
	public final static String FileKey ="com.surajsararf.musicoplayer";
	public final static String LastDuration="lastduration";
	public final static String LastSongNumber="lastsongnumber";

	private SongPlayback mService;

	/**
	 * Check if service is running or not
	 * @param serviceName
	 * @param context
	 * @return
	 */
	public static boolean isServiceRunning(String serviceName, Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for(RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if(serviceName.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param context
	 * @return
	 */

	/**
	 * Convert milliseconds into time hh:mm:ss
	 * @param milliseconds
	 * @return time in String
	 */
	public static String getDuration(long milliseconds) {
		long sec = (milliseconds / 1000) % 60;
		long min = (milliseconds / (60 * 1000))%60;
		long hour = milliseconds / (60 * 60 * 1000);

		String s = (sec < 10) ? "0" + sec : "" + sec;
		String m = (min < 10) ? "0" + min : "" + min;
		String h = "" + hour;
		
		String time = "";
		if(hour > 0) {
			time = h + ":" + m + ":" + s;
		} else {
			time = m + ":" + s;
		}
		return time;
	}
	
	public static boolean currentVersionSupportBigNotification() {
		int sdkVersion = android.os.Build.VERSION.SDK_INT;
		if(sdkVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN){
			return true;
		}
		return false;
	}
	
	public static boolean currentVersionSupportLockScreenControls() {
		int sdkVersion = android.os.Build.VERSION.SDK_INT;
		if(sdkVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			return true;
		}
		return false;
	}

	public void setService(SongPlayback songPlayback){
		mService=songPlayback;
	}

	public SongPlayback getService() {
		return mService;
	}

	public static void saveSharedPreferenceint(Context context,String key,int value){
		SharedPreferences sharedPreferences=context.getSharedPreferences(UtilFunctions.FileKey,MODE_PRIVATE);
		SharedPreferences.Editor editor=sharedPreferences.edit();
		editor.putInt(key,value);
		editor.commit();
	}

	public static void saveSharedPreferencestring(Context context,String key,String value){
		SharedPreferences sharedPreferences=context.getSharedPreferences(UtilFunctions.FileKey,MODE_PRIVATE);
		SharedPreferences.Editor editor=sharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}

	public static int getSharedPreferenceint(Context context,String key,int defvalue){
		SharedPreferences sharedPreferences=context.getSharedPreferences(UtilFunctions.FileKey,MODE_PRIVATE);
		int value=sharedPreferences.getInt(key, defvalue);
		return value;
	}

	public static String getSharedPreferencestring(Context context,String key,String defvalue){
		SharedPreferences sharedPreferences=context.getSharedPreferences(UtilFunctions.FileKey,MODE_PRIVATE);
		String value=sharedPreferences.getString(key, defvalue);
		return value;
	}
}
