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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * Screen shown while the search is being made.
 * Displays the captured image.
 * Search POST made from a background thread.
 */
public class SearchingActivity extends Activity
{
	// Too see all logging use "adb shell setprop log.tag.SearchingActivity DEBUG" - default is WARN and above
	private static final String TAG = "SearchingActivity";
		
	static String mars3Url = null;
	static boolean autoFollowLinks = false;
	static Mac sha1 = null;
	
	private CloudSearch background = null;
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

        // Get the upload kicked off before we do *anything* else!
        background = new CloudSearch(VisualSearchActivity.bitmap);

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
				case CloudSearch.MATCH:
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
				case CloudSearch.NOMATCH:
					showAlert(R.string.nomatch_title, R.string.nomatch_text, false);
					break;
				case CloudSearch.REFUSED:
					showAlert(R.string.fail_title, R.string.refused_text, true);
					break;
				case CloudSearch.NON200:
					showAlert(R.string.fail_title, R.string.non200_text, false);
					break;
				case CloudSearch.FAIL:
					showAlert(R.string.fail_title, R.string.fail_text, false);
					break;
				case CloudSearch.TIMEOUT:
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
		
		// Should we suggest the user checks their service config or simply try again?
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
	
	
	////////////////////////////////////////////////////////////////////////////////// 
	// Background thread

	private class CloudSearch extends Thread
	{
		private static final int JPEG_QUALITY = 70;
		private static final int SOCKET_TIMEOUT = 20000;
		private static final String CUSTOMER = "test";
		private static final String SECRET = "testsecret";

		public static final int WORKING = 0;
		public static final int MATCH = 1;
		public static final int NOMATCH = 2;
		public static final int NON200 = 3;
		public static final int FAIL = 4;
		public static final int TIMEOUT = 5;
		public static final int REFUSED = 6;
		
		// Public state used by the UI code above
		public int state = WORKING;
		public String resultStr = null;
		public Result[] results = null;
		
		private Bitmap image = null;
		
		public CloudSearch(Bitmap image)
		{
			super();
			
			this.image = image;

			start();
		}
				
		@Override
		public void run()
		{
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Background thread starting.");
			
        	try {
		        if (sha1==null) sha1 = makeSha1();
	
				ByteArrayOutputStream out = new ByteArrayOutputStream();
		    	image.compress(CompressFormat.JPEG, JPEG_QUALITY, out);
		    	byte[] jpgImage = out.toByteArray();
		    	image=null;
		    	out=null;
	
		    	if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "About to POST "+jpgImage.length+" bytes of JPG image.");
	
		    	HttpClient http = new DefaultHttpClient();
		    	http.getParams().setIntParameter("http.socket.timeout", SOCKET_TIMEOUT);
		    	ResponseHandler<String> responseHandler = new BasicResponseHandler();
				TelephonyManager telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
				
				// Request signature
				String date = DateUtils.formatDate(new Date());
				String identity = telephonyManager.getDeviceId();
				String toSign = CUSTOMER+"POST"+mars3Url+date+"identity"+identity+jpgImage.length;
				byte[] sigBytes = sha1.doFinal(toSign.getBytes("UTF8"));
				String sig = Base64.encodeToString(sigBytes, Base64.NO_WRAP);	// Can't have newlines in header values!
				if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, toSign+" --> "+sig);
				
				// Build the request
	        	HttpPost post = new HttpPost(mars3Url+"?identity="+identity);
	        	ByteArrayEntity entity = new ByteArrayEntity(jpgImage);
	        	entity.setContentType("image/jpeg");
	        	post.setEntity(entity);
	        	post.setHeader("Accept", "application/json");
	        	post.setHeader("Date", date);
	        	post.setHeader("Authorization", "MAAPIv1 test "+sig);

	        	// Run the POST
				String response = http.execute(post, responseHandler);
				jpgImage = null;
				
				// Unpack the response
				results = Result.unpackJsonResultsList(response);
				if (results.length==0) {
					if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Match failed!");
					state = NOMATCH;
				} else {
					if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Number of matches: "+results.length);
					state = MATCH;
				}
			}
        	catch (HttpResponseException e) {
        		int response = e.getStatusCode();
        		if (Log.isLoggable(TAG, Log.ERROR)) Log.e(TAG, "Non 200 HTTP response: "+response, e);
				if (response==400) {
					state = REFUSED;
				} else {
					state = NON200;
					resultStr=""+e.getStatusCode();
				}
        	}
        	catch (SocketTimeoutException e) {
        		if (Log.isLoggable(TAG, Log.ERROR)) Log.e(TAG, "Timeout during search request.");
        		state = TIMEOUT;
        	}
        	catch (Exception e) {
        		if (Log.isLoggable(TAG, Log.ERROR)) Log.e(TAG, "Exception thrown uploading image: "+e.getMessage(), e);
				state = FAIL;
				resultStr = e.getMessage();
			}

        	// Deal with the outcome in the UI
        	runOnUiThread(onBackgroundDone);

			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Background thread ending.");
		}
		
		private Mac makeSha1() throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException
		{
			Mac result = Mac.getInstance("HmacSHA1");
			byte[] keyBytes = SECRET.getBytes("UTF8");
			SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
			result.init(signingKey);
			return result;
		}
	}
}
