
package com.example.kittytest.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Debug;
import android.util.Log;

/**
 * get information of processes
 * 
 */
public class ProcessInfo {

	private static final String LOG_TAG = "HelloKitty-" + ProcessInfo.class.getSimpleName();

	private static final String PACKAGE_NAME = "com.example.kittytest";

	/**
	 * get information of all running processes,including package name ,process
	 * name ,icon ,pid and uid.
	 * 
	 * @param context
	 *            context of activity
	 * @return running processes list
	 */
	public List<Programe> getRunningProcess(Context context) {
		Log.i(LOG_TAG, "get running processes");

		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> run = am.getRunningAppProcesses();
		PackageManager pm = context.getPackageManager();
		List<Programe> progressList = new ArrayList<Programe>();
		
		//List<ApplicationInfo> packages = getPackagesInfo(context);
		
		for (ApplicationInfo appinfo : getPackagesInfo(context)) {	
			
			Log.i("test-appinfo", appinfo.packageName);
			
			Programe programe = new Programe();
			//如果是系统APP或者本应用时跳过
			if (((appinfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) || ((appinfo.processName != null) && (appinfo.processName.equals(PACKAGE_NAME)))) {
				continue;
			}
			for (RunningAppProcessInfo runningProcess : run) {
				if ((runningProcess.processName != null) && runningProcess.processName.equals(appinfo.processName)) {
					programe.setPid(runningProcess.pid);
					programe.setUid(runningProcess.uid);
					break;
				}
			}
			
			programe.setPackageName(appinfo.processName);
			programe.setProcessName(appinfo.loadLabel(pm).toString());
			programe.setIcon(appinfo.loadIcon(pm));
			progressList.add(programe);
			
			Log.i("test-programe", programe.getPackageName());
		}
		Collections.sort(progressList);
		
		
		return progressList;
	}

	/**
	 * get information of all applications.
	 * 
	 * @param context
	 *            context of activity
	 * @return packages information of all applications
	 */
	private List<ApplicationInfo> getPackagesInfo(Context context) {
		PackageManager pm = context.getApplicationContext().getPackageManager();
		//获取所有安装应用的信息
		List<ApplicationInfo> appList = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		return appList;
	}

	/**
	 * get pid by package name
	 * 
	 * @param context
	 *            context of activity
	 * @param packageName
	 *            package name of monitoring app
	 * @return pid
	 */
	public Programe getProgrameByPackageName(Context context, String packageName) {
		List<Programe> processList = getRunningProcess(context);
		for (Programe programe : processList) {
			if ((programe.getPackageName() != null) && (programe.getPackageName().equals(packageName))) {
				return programe;
			}
		}
		return null;
	}

	/**
	 * get top activity name
	 * 
	 * @param context
	 *            context of activity
	 * @return top activity name
	 */
	public static String getTopActivity(Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
		if (runningTaskInfos != null)
			return (runningTaskInfos.get(0).topActivity).toString();
		else
			return null;
	}
}
