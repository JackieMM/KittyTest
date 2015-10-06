
package com.example.kittytest.utils;

import android.app.Application;
import android.view.WindowManager;

/**
 * my application class
 * 
 */
public class MyApplication extends Application {

	private WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();

	public WindowManager.LayoutParams getMywmParams() {
		return wmParams;
	}
}
