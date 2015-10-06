package com.example.kittytest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Properties;

import com.example.kittytest.MainActivityTest;
import com.example.kittytest.utils.Constants;
import com.example.kittytest.utils.CpuInfo;
import com.example.kittytest.utils.CurrentInfo;
import com.example.kittytest.utils.EncryptData;
import com.example.kittytest.utils.MemoryInfo;
import com.example.kittytest.utils.MyApplication;
import com.example.kittytest.utils.ProcessInfo;
import com.example.kittytest.utils.Programe;
import com.example.kittytest.utils.Settings;


import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Service running in background
 * 
 */
public class KittyService extends Service {

		private final static String LOG_TAG = "WestSea-"
				+ KittyService.class.getSimpleName();

		private WindowManager windowManager = null;
		private WindowManager.LayoutParams wmParams = null;
		private View viFloatingWindow;
		private float mTouchStartX;
		private float mTouchStartY;
		private float startX;
		private float startY;
		private float x;
		private float y;
		private TextView txtTotalMem;
		private TextView txtUnusedMem;
		private TextView txtTraffic;
		private ImageView imgViIcon;
		
		private Button btnWifi;
		private Button btnStop;
		
		
		private int delaytime;
		private DecimalFormat fomart;
		private MemoryInfo memoryInfo;
		private WifiManager wifiManager;
		private Handler handler = new Handler();
		private CpuInfo cpuInfo;
		private String time;
		private boolean isFloating;
		private String processName, packageName, settingTempFile;
		private int pid, uid;
		private boolean isServiceStop = false;
		private String sender, password, recipients, smtp;
		private String[] receivers;
		private EncryptData des;

		public static BufferedWriter bw;
		public static FileOutputStream out;
		public static OutputStreamWriter osw;
		public static String resultFilePath;
		public static boolean isStop = false;
		
		//get the battery info
		private String totalBatt;
		private String temperature;
		private String voltage;
		private String currentBatt;
		private CurrentInfo currentInfo;
		
		private BatteryInfoBroadcastReceiver batteryBroadcast = null;
		private static final String BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED";
		public static final String SERVICE_ACTION = "com.example.action.KittyService";
		

		@Override
		public void onCreate() {
			Log.i(LOG_TAG, "onCreate");
			super.onCreate();
			isServiceStop = false;
			isStop = false;
			memoryInfo = new MemoryInfo();
			fomart = new DecimalFormat();
			fomart.setMaximumFractionDigits(2);
			fomart.setMinimumFractionDigits(0);
			des = new EncryptData("WestSea");
			batteryBroadcast = new BatteryInfoBroadcastReceiver();
			registerReceiver(batteryBroadcast, new IntentFilter(BATTERY_CHANGED));
		}

		@Override
		public void onStart(Intent intent, int startId) {
			Log.i(LOG_TAG, "onStart");
//		setForeground(true);
			super.onStart(intent, startId);

			pid = intent.getExtras().getInt("pid");
			uid = intent.getExtras().getInt("uid");
			processName = intent.getExtras().getString("processName");
			packageName = intent.getExtras().getString("packageName");
			settingTempFile = intent.getExtras().getString("settingTempFile");

			cpuInfo = new CpuInfo(getBaseContext(), pid, Integer.toString(uid));
			readSettingInfo(intent); 
			delaytime = Integer.parseInt(time) * 1000;
			if (isFloating) {
				viFloatingWindow = LayoutInflater.from(this).inflate(
						R.layout.floating, null);
				txtUnusedMem = (TextView) viFloatingWindow
						.findViewById(R.id.memunused);
				txtTotalMem = (TextView) viFloatingWindow
						.findViewById(R.id.memtotal);
				txtTraffic = (TextView) viFloatingWindow.findViewById(R.id.traffic);
				btnWifi = (Button) viFloatingWindow.findViewById(R.id.wifi);

				wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				if (wifiManager.isWifiEnabled()) {
					//btnWifi.setText(R.string.closewifi);
					btnWifi.setText(R.string.close_wifi);
				} else {
					//btnWifi.setText(R.string.openwifi);
					btnWifi.setText(R.string.open_wifi);
				}
				txtUnusedMem.setText("计算中,请稍后...");
				txtUnusedMem.setTextColor(android.graphics.Color.BLUE);
				txtTotalMem.setTextColor(android.graphics.Color.BLUE);
				txtTraffic.setTextColor(android.graphics.Color.BLUE);
				imgViIcon = (ImageView) viFloatingWindow.findViewById(R.id.img1);
				imgViIcon.setVisibility(View.GONE);
				createFloatingWindow();
				
				btnStop = (Button) viFloatingWindow.findViewById(R.id.stop);
				btnStop.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent();
						intent.putExtra("isServiceStop", true);
						intent.setAction(SERVICE_ACTION);
						sendBroadcast(intent);
						stopSelf();
					}
				});
			}
			createResultCsv();
			handler.postDelayed(task, 1000);
		}

		/**
		 * read configuration file.
		 * 
		 * @throws IOException
		 */
		private void readSettingInfo(Intent intent) {
			try {
				Properties properties = new Properties();
				properties.load(new FileInputStream(settingTempFile));
				String interval = properties.getProperty("interval").trim();
				isFloating = "true"
						.equals(properties.getProperty("isfloat").trim()) ? true
						: false;
				sender = properties.getProperty("sender").trim();
				password = properties.getProperty("password").trim();
				recipients = properties.getProperty("recipients").trim();
				time = "".equals(interval) ? "5" : interval;
				recipients = properties.getProperty("recipients");
				receivers = recipients.split("\\s+");
				smtp = properties.getProperty("smtp");
			} catch (IOException e) {
				time = "5";
				isFloating = true;
				Log.e(LOG_TAG, e.getMessage());
			}
		}

		/**
		 * write the test result to csv format report.
		 */
		private void createResultCsv() {
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
			String mDateTime;
			if ((Build.MODEL.equals("sdk")) || (Build.MODEL.equals("google_sdk")))
				mDateTime = formatter.format(cal.getTime().getTime() + 8 * 60 * 60
						* 1000);
			else
				mDateTime = formatter.format(cal.getTime().getTime());

			if (android.os.Environment.getExternalStorageState().equals(
					android.os.Environment.MEDIA_MOUNTED)) {
				resultFilePath = android.os.Environment
						.getExternalStorageDirectory()
						+ File.separator
						+ "WestSea_TestResult_" + mDateTime + ".csv";
			} else {
				resultFilePath = getBaseContext().getFilesDir().getPath()
						+ File.separator + "WestSea_TestResult_" + mDateTime
						+ ".csv";
			}
			try {
				File resultFile = new File(resultFilePath);
				resultFile.createNewFile();
				out = new FileOutputStream(resultFile);
				//编码格式为GBK
				osw = new OutputStreamWriter(out, "GBK");
				
				bw = new BufferedWriter(osw);
				long totalMemorySize = memoryInfo.getTotalMemory();
				String totalMemory = fomart.format((double) totalMemorySize / 1024);
				
				bw.write("指定应用的CPU内存监控情况\r\n" + "应用包名：," + packageName + "\r\n"
						+ "应用名称: ," + processName + "\r\n" + "应用PID: ," + pid
						+ "\r\n" + "机器内存大小(MB)：," + totalMemory + "MB\r\n"
						+ "机器CPU型号：," + cpuInfo.getCpuName() + "\r\n"
						+ "机器android系统版本：," + memoryInfo.getSDKVersion() + "\r\n"
						+ "手机型号：," + memoryInfo.getPhoneType() + "\r\n" + "UID：,"
						+ uid + "\r\n");
				
				bw.write("时间" + "," + "应用占用内存PSS(MB)" + "," + "应用占用内存比(%)" + ","
						+ " 机器剩余内存(MB)" + "," + "应用占用CPU率(%)" + "," + "CPU总使用率(%)"
						+ "," + "流量(KB)：" + "," + "电量(％)："+ "," + "电流(A)：" 
						+ "," + "温度（度）："+ "," + "电压(V)：" + "\r\n");
			} catch (IOException e) {
				Log.e(LOG_TAG, e.getMessage());
			}
		}

		/**
		 * create a floating window to show real-time data.
		 */
		private void createFloatingWindow() {
			SharedPreferences shared = getSharedPreferences("float_flag",
					Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = shared.edit();
			editor.putInt("float", 1);
			editor.commit();
			windowManager = (WindowManager) getApplicationContext()
					.getSystemService("window");
			wmParams = ((MyApplication) getApplication()).getMywmParams();
			wmParams.type = 2002;
			wmParams.flags |= 8;
			wmParams.gravity = Gravity.LEFT | Gravity.TOP;
			wmParams.x = 0;
			wmParams.y = 0;
			wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
			wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
			wmParams.format = 1;
			windowManager.addView(viFloatingWindow, wmParams);
			viFloatingWindow.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					x = event.getRawX();
					y = event.getRawY() - 25;
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						// state = MotionEvent.ACTION_DOWN;
						startX = x;
						startY = y;
						mTouchStartX = event.getX();
						mTouchStartY = event.getY();
						Log.d("startP", "startX" + mTouchStartX + "====startY"
								+ mTouchStartY);
						break;
					case MotionEvent.ACTION_MOVE:
						// state = MotionEvent.ACTION_MOVE;
						updateViewPosition();
						break;

					case MotionEvent.ACTION_UP:
						// state = MotionEvent.ACTION_UP;
						updateViewPosition();
						showImg();
						mTouchStartX = mTouchStartY = 0;
						break;
					}
					return true;
				}
			});

			btnWifi.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						btnWifi = (Button) viFloatingWindow.findViewById(R.id.wifi);
						String buttonText = (String) btnWifi.getText();
						String wifiText = getResources().getString(
								R.string.close_wifi);
						if (buttonText.equals(wifiText)) {
							wifiManager.setWifiEnabled(true);
							btnWifi.setText(R.string.close_wifi);
						} else {
							wifiManager.setWifiEnabled(false);
							btnWifi.setText(R.string.open_wifi);
						}
					} catch (Exception e) {
						Toast.makeText(viFloatingWindow.getContext(), "操作wifi失败",
								Toast.LENGTH_LONG).show();
						Log.e(LOG_TAG, e.toString());
					}
				}
			});
		}

		/**
		 * show the image.
		 */
		private void showImg() {
			if (Math.abs(x - startX) < 1.5 && Math.abs(y - startY) < 1.5
					&& !imgViIcon.isShown()) {
				imgViIcon.setVisibility(View.VISIBLE);
			} else if (imgViIcon.isShown()) {
				imgViIcon.setVisibility(View.GONE);
			}
		}

		private Runnable task = new Runnable() {

			public void run() {
				if (!isServiceStop) {
					dataRefresh();
					handler.postDelayed(this, delaytime);
					if (isFloating)
						windowManager.updateViewLayout(viFloatingWindow, wmParams);
				} else {
					Intent intent = new Intent();
					intent.putExtra("isServiceStop", true);
					intent.setAction(Constants.SERVICE_ACTION);
					sendBroadcast(intent);
					stopSelf();
				}
			}
		};

		/**
		 * refresh the performance data showing in floating window.
		 * 
		 * @throws FileNotFoundException
		 * 
		 * @throws IOException
		 */
		private void dataRefresh() {
			int pidMemory = memoryInfo.getPidMemorySize(pid, getBaseContext());
			
			long freeMemory = memoryInfo.getFreeMemorySize(getBaseContext());
			
			String freeMemoryKb = fomart.format((double) freeMemory / 1024);
			
			String processMemory = fomart.format((double) pidMemory / 1024);
			
			//在获取数据的时候已经进行了更新
			//ArrayList<String> processInfo = cpuInfo.getCpuRatioInfo();
			ArrayList<String> processInfo = cpuInfo.getCpuRatioInfo(totalBatt, currentBatt, temperature, voltage);
			
			if (isFloating) {
				String processCpuRatio = "0";
				String totalCpuRatio = "0";
				String trafficSize = "0";
				int tempTraffic = 0;
				double trafficMb = 0;
				boolean isMb = false;
				if (!processInfo.isEmpty()) {
					processCpuRatio = processInfo.get(0);
					totalCpuRatio = processInfo.get(1);
					trafficSize = processInfo.get(2);
					if ("".equals(trafficSize) && !("-1".equals(trafficSize))) {
						tempTraffic = Integer.parseInt(trafficSize);
						if (tempTraffic > 1024) {
							isMb = true;
							trafficMb = (double) tempTraffic / 1024;
						}
					}
				}
				//如果应用占用的内存为0且CPU使用率为0时说明该应用已退出，不必再进行监控
				if ("0".equals(processMemory) && "0.00".equals(processCpuRatio)) {
					closeOpenedStream();
					isServiceStop = true;
					return;
				}
				
				if (processCpuRatio != null && totalCpuRatio != null) {
					txtUnusedMem.setText("占用内存:" + processMemory + "MB" + ",机器剩余:"
							+ freeMemoryKb + "MB");
					
					txtTotalMem.setText("占用CPU:" + processCpuRatio + "%"
							+ ",总体CPU:" + totalCpuRatio + "%");
					
					if ("-1".equals(trafficSize)) {
						txtTraffic.setText("本程序或本设备不支持流量统计");
					} else if (isMb)
						txtTraffic.setText("消耗流量:" + fomart.format(trafficMb)
								+ "MB");
					else
						txtTraffic.setText("消耗流量:" + trafficSize + "KB");
				}
			}
		}

		/**
		 * update the position of floating window.
		 */
		private void updateViewPosition() {
			wmParams.x = (int) (x - mTouchStartX);
			wmParams.y = (int) (y - mTouchStartY);
			windowManager.updateViewLayout(viFloatingWindow, wmParams);
		}

		/**
		 * close all opened stream.
		 */
		public static void closeOpenedStream() {
			try {
				if (bw != null)
					bw.close();
				if (osw != null)
					osw.close();
				if (out != null)
					out.close();
			} catch (Exception e) {
				Log.d(LOG_TAG, e.getMessage());
			}
		}

		@Override
		public void onDestroy() {
			Log.i(LOG_TAG, "onDestroy");
			if (windowManager != null)
				windowManager.removeView(viFloatingWindow);
			handler.removeCallbacks(task);
			closeOpenedStream();
			isStop = true;
			//取消注册接收器
			unregisterReceiver(batteryBroadcast);
			
//			boolean isSendSuccessfully = false;
//			try {
//				isSendSuccessfully = MailSender.sendTextMail(sender,
//						des.decrypt(password), smtp,
//						"Emmagee Performance Test Report", "see attachment",
//						resultFilePath, receivers);
//			} catch (Exception e) {
//				isSendSuccessfully = false;
//			}
//			if (isSendSuccessfully) {
//				Toast.makeText(this, "测试结果报表已发送至邮箱:" + recipients,
//						Toast.LENGTH_LONG).show();
//			} else {
//				Toast.makeText(this,
//						"测试结果未成功发送至邮箱，结果保存在:" + EmmageeService.resultFilePath,
//						Toast.LENGTH_LONG).show();
//			}

			super.onDestroy();
		}

		/**
		 * 电池信息监控监听器
		 * 
		 * 
		 */
		public class BatteryInfoBroadcastReceiver extends BroadcastReceiver {

			@Override
			public void onReceive(Context context, Intent intent) {

				if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
					//电池当前的电量
					int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
					//电池电量的最大值 
					int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
					
					//充电状态
					int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
				    boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
				                            status == BatteryManager.BATTERY_STATUS_FULL;
					
					//电量信息
					totalBatt = String.valueOf(level * 100 / scale);
					//电压
					voltage = String.valueOf(intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) * 1.0 / 1000);
					//温度
					temperature = String.valueOf(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) * 1.0 / 10);
				}
			}

		}
		


		
		@Override
		public IBinder onBind(Intent intent) {
			return null;
		}
	

	
}