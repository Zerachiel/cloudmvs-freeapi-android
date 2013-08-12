/*
 * Copyright (c) 2013 Mobile Acuity Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobileacuity.android.cloudmvsdemo;

import javax.crypto.Mac;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * Screen shown while the search performed.
 * Displays the captured image.
 * Search POST made from a background thread.
 */
public class SearchingActivity extends Activity
{
	// Too see all logging use "adb shell setprop log.tag.SearchingActivity DEBUG" - default is WARN and above
	private static final String TAG = "SearchingActivity";

	static boolean autoFollowLinks = false;
	static Mac sha1 = null;
	
	protected static SearchCloud background = null;
	private boolean running = false;
	private ProgressDialog progress = null;


	////////////////////////////////////////////////////////////////////////////////// 
	// Lifecycle

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onCreate()");

        //connect to searching thread
    	background.setSearchingUI(this);
    	
        // No screen decor at all! Helps reduce image letterboxing
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.searching);
        
        // Bitmap was captured in landscape and this screen is portrait so we need to rotate to display
        int width = VisualSearchActivity.bitmap.getWidth();
        int height = VisualSearchActivity.bitmap.getHeight();
        Matrix rot90 = new Matrix();
        rot90.preRotate(90, width/2, height/2);
        Bitmap bitmap = Bitmap.createBitmap(VisualSearchActivity.bitmap, 0, 0, width, height, rot90, false);
        
        final ImageView imageView = (ImageView)findViewById(R.id.img_review);
        imageView.setImageBitmap(bitmap);
        
		progress = ProgressDialog.show(this, getString(R.string.progress_title), getString(R.string.progress_text), true, true, cancelListener);
    }
    
	@Override
	protected void onResume()
	{
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onResume()");
		super.onResume();
		running = true;
	}

	@Override
	protected void onPause()
	{
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onPause()");
		super.onPause();
		running = false;
	}


	////////////////////////////////////////////////////////////////////////////////// 
	// Listeners etc
	
	private Runnable onBackgroundDone = new Runnable()
	{	
		@Override
		public void run() {
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onBackgroundDone: "+background.resultStr);
			
			if (running) {
				switch (background.state) {
				case SearchCloud.MATCH:
					Intent intent = null;
					
					if (autoFollowLinks) {
						if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Trying to automatically follow the first link");
						Uri uri = background.results[0].getUri();
						if (uri!=null) {
							intent = new Intent(Intent.ACTION_VIEW, uri);
						} else {
							if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Result is not an HTTP uri so can't follow it!");
						}
					}
					
					if (intent==null) {
						if (background.results.length==1) {
							ResultActivity.result = background.results[0]; 
							intent = new Intent(SearchingActivity.this, ResultActivity.class);
						} else {
							ResultListActivity.results = background.results; 
							intent = new Intent(SearchingActivity.this, ResultListActivity.class);					
						}
					}
					if (progress!=null) progress.dismiss();
					startActivity(intent);
					finish();
					break;
				case SearchCloud.NOMATCH:
					showAlert(R.string.nomatch_title, R.string.nomatch_text, false);
					break;
				case SearchCloud.REFUSED:
					showAlert(R.string.fail_title, R.string.refused_text, true);
					break;
				case SearchCloud.NON200:
					showAlert(R.string.fail_title, R.string.non200_text, false);
					break;
				case SearchCloud.FAIL:
					showAlert(R.string.fail_title, R.string.fail_text, false);
					break;
				case SearchCloud.TIMEOUT:
					showAlert(R.string.fail_title, R.string.timeout_text, false);
					break;
				}
			}
		}
	};
	
	private void showAlert(int title, int text, boolean configure)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		if (background.resultStr==null || background.resultStr.length()==0) {
			builder.setMessage(text);
		} else {
			builder.setMessage(getString(text)+background.resultStr);
		}
		builder.setOnCancelListener(cancelListener);

		if (configure) {
			builder.setPositiveButton(R.string.configure_btn, configureListener);			
		} else {
			builder.setPositiveButton(R.string.retry_btn, tryAgainListener);
		}
		
		if (progress!=null) progress.dismiss();
		builder.show();		
	}
	
	private DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
		@Override
		public void onCancel(DialogInterface dialog) {
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onCancel");
			finish();
		}
	};

	private DialogInterface.OnClickListener tryAgainListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "tryAgainListener.onClick()");
			finish();
		}
	};
	
	private DialogInterface.OnClickListener configureListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "configureListener.onClick()");
			ConfigureActivity.doConfigure(SearchingActivity.this);
			finish();
		}
	};
	
	protected void runBackgroundDone() {
		runOnUiThread(onBackgroundDone);
	}
}
