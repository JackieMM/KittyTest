//package com.example.kittytest;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.text.DecimalFormat;
//import java.text.DecimalFormatSymbols;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Locale;
//
//import com.example.kittytest.utils.CPUInfo;
//import com.example.kittytest.utils.Constants;
//import com.example.kittytest.utils.CurrentInfo;
//import com.example.kittytest.utils.MemoryInfo;
//import com.example.kittytest.utils.ProcessInfo;
//
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Handler;
//import android.os.IBinder;
//import android.support.v4.app.NotificationCompat;
//import android.util.Log;
//import android.widget.Toast;
//
///*
// * 该Service主要完成收集相关性能测试的结果并进行返回；
// * 
// * 
// * 
// * */
//public class KittyService_backup extends Service {
//
//	private final static String LOG_TAG = "Jackie-" + KittyService_backup.class.getSimpleName();
//
//	private static final String BLANK_STRING = "";
//	
//	public static final String SERVICE_ACTION = "com.example.kitty.kittyservice";
//	public static final String BATTERY_MONITOR = "android.intent.action.BATTERY_CHANGED";
//	
//	
//	private DecimalFormat formart;
//
//	
//	public static BufferedWriter bw;
//	public static FileOutputStream out;
//	public static OutputStreamWriter osw;
//	public static String resultFilePath;
//	
//	public static boolean isStop = false;
//	public static boolean isServiceStop = false;
//	
//	private int pid,uid;
//	private MemoryInfo memoryInfo ;
//	private CPUInfo cpuInfo = null;
//	private ProcessInfo procInfo;
//	private CurrentInfo curInfo;
//
//	private String processName;
//
//	private String packageName;
//
//	private String startActivity;
//
//	private Handler handler = new Handler();
//	
//	private long delaytime = 500;
//	
//	// get start time
//	private static final int MAX_START_TIME_COUNT = 5;
//	private static final String START_TIME = "#startTime";
//	private int getStartTimeCount = 0;
//	private boolean isGetStartTime = true;
//	private String startTime = "";
//	
//	//protected static String resultFilePath;
//	
//	@Override
//	public void onCreate() {
//		Log.i(LOG_TAG , "in the onCreate's function");
//		super.onCreate();
//		isStop = false;
//		isServiceStop = false;
//		
//	    memoryInfo = new MemoryInfo();
//		procInfo = new ProcessInfo();
//		
//		//设置数据显示的格式
//	    formart = new DecimalFormat();
//	    formart.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
//	    formart.setGroupingUsed(false);
//	    formart.setMaximumIntegerDigits(2);
//	    formart.setMinimumIntegerDigits(0);
//	    
//	    curInfo = new CurrentInfo();
//	    
//	    //后续可将电池耗电量相关信息，下载速度检测信息放在此处
//	}
//	
//
//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		Log.i(LOG_TAG,"begin to start service~~");
//		PendingIntent contentIntent = PendingIntent.getActivity(getBaseContext(), 0, new Intent(this, MainPageActivity.class), 0);
//		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//		builder.setContentIntent(contentIntent).setSmallIcon(R.drawable .icon).setWhen(System.currentTimeMillis()).
//		                                                      setAutoCancel(false).setContentTitle("KittyTest");
//		
//		startForeground(startId, builder.build());
//		
//		pid = intent.getExtras().getInt("pid");
//		uid = intent.getExtras().getInt("uid");
//		processName = intent.getExtras().getString("processName");
//		packageName = intent.getExtras().getString("packageName");
//		startActivity = intent.getExtras().getString("startActivity");
//		
//		cpuInfo = new CPUInfo(getBaseContext(), pid, Integer.toString(uid));
//		
//		readSettingInfo();
//		
//		createResultCsv();
//		handler.postDelayed(task, 1000);
//		
//		return START_NOT_STICKY;
//	}
//
//	private Runnable task = new Runnable(){
//		@Override
//		public void run() {
//			if(!isServiceStop){
//				dataRefresh();
//				handler.postDelayed(this, delaytime);
//			}else{
//				Intent intent = new Intent();
//				intent.putExtra("isServiceStop", true);
//				intent.setAction(SERVICE_ACTION);
//				sendBroadcast(intent);
//				stopSelf();
//			}
//		}
//	};
//		
//	private void createResultCsv() {
//		Calendar cal = Calendar.getInstance();
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//		String mDateTime;
//		String heapData = "";
//		
//		if((Build.MODEL.equals("sdk")) || Build.MODEL.equals("google_sdk"))
//			mDateTime = formatter.format(cal.getTime().getTime() + 8 * 60 * 60 * 1000);
//		else
//			mDateTime = formatter.format(cal.getTime().getTime());
//		
//		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
//			// 在4.0以下的低版本上/sdcard连接至/mnt/sdcard，而4.0以上版本则连接至/storage/sdcard0，所以有外接sdcard，/sdcard路径一定存在
//			resultFilePath = "/sdcard" + File.separator + "Kitty_TestResult_" + mDateTime + ".csv";
//		} else {
//			resultFilePath = getBaseContext().getFilesDir().getPath() + File.separator + "Kitty_TestResult_" + mDateTime + ".csv";
//		}
//		
//		try {
//			File resultFile = new File(resultFilePath);
//			resultFile.createNewFile();
//			out = new FileOutputStream(resultFile);
//			//OutputStreamWriter is constructed by FileOutputStream
//			osw = new OutputStreamWriter(out);
//			//BufferedWriter is constructed by OutputStreamWriter
//			bw = new BufferedWriter(osw);
//			long totalMemorySize = memoryInfo.getTotalMemory();
//			String totalMemory = formart.format((double) totalMemorySize / 1024);
//			String multiCpuTitle = BLANK_STRING;
//			// titles of multiple cpu cores
//			ArrayList<String> cpuList = cpuInfo.getCpuList();
//			for (int i = 0; i < cpuList.size(); i++) {
//				multiCpuTitle += Constants.COMMA + cpuList.get(i) + getString(R.string.total_usage);
//			}
//			bw.write(getString(R.string.process_package) + Constants.COMMA + packageName + Constants.LINE_END + getString(R.string.process_name)
//					+ Constants.COMMA + processName + Constants.LINE_END + getString(R.string.process_pid) + Constants.COMMA + pid
//					+ Constants.LINE_END + getString(R.string.mem_size) + Constants.COMMA + totalMemory + "MB" + Constants.LINE_END
//					+ getString(R.string.cpu_type) + Constants.COMMA + cpuInfo.getCpuName() + Constants.LINE_END
//					+ getString(R.string.android_system_version) + Constants.COMMA + memoryInfo.getSDKVersion() + Constants.LINE_END
//					+ getString(R.string.mobile_type) + Constants.COMMA + memoryInfo.getPhoneType() + Constants.LINE_END + "UID" + Constants.COMMA
//					+ uid + Constants.LINE_END);
//
//			if (isGrantedReadLogsPermission()) {
//				bw.write(START_TIME);
//			}
////			if (isRoot) {
////				heapData = getString(R.string.native_heap) + Constants.COMMA + getString(R.string.dalvik_heap) + Constants.COMMA;
////			}
//			bw.write(getString(R.string.timestamp) + Constants.COMMA + getString(R.string.top_activity) + Constants.COMMA + heapData
//					+ getString(R.string.used_mem_PSS) + Constants.COMMA + getString(R.string.used_mem_ratio) + Constants.COMMA
//					+ getString(R.string.mobile_free_mem) + Constants.COMMA + getString(R.string.app_used_cpu_ratio) + Constants.COMMA
//					+ getString(R.string.total_used_cpu_ratio) + multiCpuTitle + Constants.COMMA + getString(R.string.traffic) + Constants.COMMA
//					+ getString(R.string.battery) + Constants.COMMA + getString(R.string.current) + Constants.COMMA + getString(R.string.temperature)
//					+ Constants.COMMA + getString(R.string.voltage) + Constants.LINE_END);
//		} catch (IOException e) {
//			Log.e(LOG_TAG, e.getMessage());
//		}
//		
//	}
//	
//	private boolean isGrantedReadLogsPermission() {
//		int permissionState = getPackageManager().checkPermission(android.Manifest.permission.READ_LOGS, getPackageName());
//		return permissionState == PackageManager.PERMISSION_GRANTED;
//	}
//
//	protected void dataRefresh() {
//		int pidMemory = memoryInfo.getPidMamorySize(pid, getBaseContext());
//		long freeMemory = memoryInfo.getFreeMemorySize(getBaseContext());
//		String freeMemoryKb = formart.format((double) freeMemory/1024);
//		String processMemory = formart.format((double) freeMemory/1024);
//		String currentBat = String.valueOf(curInfo.getCurrentValue());
//		
//		try{
//			if(Math.abs(Double.parseDouble(currentBat)) >= 500){
//				currentBat = Constants.NA;
//			}
//		}catch(Exception e){
//			currentBat = Constants.NA;
//		}
//		
//		//ArrayList<String> processInfo = cpuInfo.getCpuRatioInfo(totalBatt, currentBatt, temperature, voltage, isRoot);
//		
//		
//		
//	}
//
//
//	private void readSettingInfo() {
//		// TODO Auto-generated method stub
//		
//	}
//
//
//	/**
//	 * close all opened stream.
//	 */
//	public void closeOpenedStream() {
//		try {
//			if (bw != null) {
//				bw.write(getString(R.string.comment1) + Constants.LINE_END + getString(R.string.comment2) + Constants.LINE_END
//						+ getString(R.string.comment3) + Constants.LINE_END + getString(R.string.comment4) + Constants.LINE_END);
//				bw.close();
//			}
//			if (osw != null)
//				osw.close();
//			if (out != null)
//				out.close();
//		} catch (Exception e) {
//			Log.d(LOG_TAG, e.getMessage());
//		}
//	}
//	
//	/**
//	 * Try to get start time from logcat.
//	 */
//	private void getStartTimeFromLogcat() {
//		if (!isGetStartTime || getStartTimeCount >= MAX_START_TIME_COUNT) {
//			return;
//		}
//		try {
//			// filter logcat by Tag:ActivityManager and Level:Info
//			String logcatCommand = "logcat -v time -d ActivityManager:I *:S";
//			Process process = Runtime.getRuntime().exec(logcatCommand);
//			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//			StringBuilder strBuilder = new StringBuilder();
//			String line = BLANK_STRING;
//
//			while ((line = bufferedReader.readLine()) != null) {
//				strBuilder.append(line);
//				strBuilder.append(Constants.LINE_END);
//				String regex = ".*Displayed.*" + startActivity + ".*\\+(.*)ms.*";
//				if (line.matches(regex)) {
//					Log.w("my logs", line);
//					if (line.contains("total")) {
//						line = line.substring(0, line.indexOf("total"));
//					}
//					startTime = line.substring(line.lastIndexOf("+") + 1, line.lastIndexOf("ms") + 2);
//					Toast.makeText(KittyService_backup.this, getString(R.string.start_time) + startTime, Toast.LENGTH_LONG).show();
//					isGetStartTime = false;
//					break;
//				}
//			}
//			getStartTimeCount++;
//		} catch (IOException e) {
//			Log.d(LOG_TAG, e.getMessage());
//		}
//	}
//	
//	
//	@Override
//	public void onDestroy() {
//		handler.removeCallbacks(task);
//		closeOpenedStream();
//		// replace the start time in file
//		if (!BLANK_STRING.equals(startTime)) {
//			replaceFileString(resultFilePath, START_TIME, getString(R.string.start_time) + startTime + Constants.LINE_END);
//		} else {
//			replaceFileString(resultFilePath, START_TIME, BLANK_STRING);
//		}
//		isStop = true;
//		//unregisterReceiver(batteryBroadcast);
////		boolean isSendSuccessfully = false;
////		try {
////			isSendSuccessfully = MailSender.sendTextMail(sender, des.decrypt(password), smtp, "Emmagee Performance Test Report", "see attachment",
////					resultFilePath, receivers);
////		} catch (Exception e) {
////			isSendSuccessfully = false;
////		}
////		if (isSendSuccessfully) {
////			Toast.makeText(this, getString(R.string.send_success_toast) + recipients, Toast.LENGTH_LONG).show();
////		} else {
////			Toast.makeText(this, getString(R.string.send_fail_toast) + EmmageeService.resultFilePath, Toast.LENGTH_LONG).show();
////		}
//		super.onDestroy();
//		stopForeground(true);
//	}
//
//	/**
//	 * Replaces all matches for replaceType within this replaceString in file on
//	 * the filePath
//	 * 
//	 * @param filePath
//	 * @param replaceType
//	 * @param replaceString
//	 */
//
//	private void replaceFileString(String filePath, String replaceType, String replaceString) {
//		try {
//			File file = new File(filePath);
//			BufferedReader reader = new BufferedReader(new FileReader(file));
//			String line = BLANK_STRING;
//			String oldtext = BLANK_STRING;
//			while ((line = reader.readLine()) != null) {
//				oldtext += line + Constants.LINE_END;
//			}
//			reader.close();
//			// replace a word in a file
//			String newtext = oldtext.replaceAll(replaceType, replaceString);
//			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), getString(R.string.csv_encoding)));
//			writer.write(newtext);
//			writer.close();
//		} catch (IOException e) {
//			Log.d(LOG_TAG, e.getMessage());
//		}
//	}
//
//	
//	@Override
//	public IBinder onBind(Intent intent) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//}
