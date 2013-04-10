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

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

/**
 * The camera screen - captures preview frames rather than fullsize images
 */
public class VisualSearchActivity extends Activity
{
	// Too see all logging use "adb shell setprop log.tag.VisualSearchActivity DEBUG" - default is WARN and above
	private static final String TAG = "VisualSearchActivity";
	
	// State
	private boolean weHaveBeenResumed = false;
	private boolean existingSurfaceHasSize = false;
	private boolean cameraIsActive = false;	
	private int preWidth;
	private int preHeight;
	
	// Components
	private PowerManager.WakeLock wakelock=null;
	private SurfaceHolder holder;
	private Camera camera;
	private ImageButton shutterButton;
	
	public static Bitmap bitmap = null;
	private int[] argb8888 = null;
	
	
	////////////////////////////////////////////////////////////////////////////////// 
	// Lifecycle

	/**
	 * Called when the activity is first created. 
	 **/
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onCreate()");
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
		
		// Always in landscape!
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        // No screen decor at all! Helps disguise landscape view!
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.capture);
        final SurfaceView surface = (SurfaceView)findViewById(R.id.surface_preview);
        
        // Wire up the surface
        holder = surface.getHolder();
        holder.addCallback(surfaceHolderCallback);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        shutterButton = (ImageButton)findViewById(R.id.btn_camera);
        shutterButton.setOnClickListener(onClickListener);
    }
    
	/**
	 * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(), for your activity to start interacting with the user. This is a good place to begin animations, open exclusive-access devices (such as the camera), etc.
	 */
	@Override
	protected void onResume() {
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onResume()");
		super.onResume();
		
		synchronized (this) {
			weHaveBeenResumed = true;
			
			if (existingSurfaceHasSize && !cameraIsActive) {
				if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Surface already exists so create and start the camera.");
				createCamera();
				startCamera();
			} else {
				if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "No surface yet so don't create the camera.");	
			}			
		}
		
		wakelock.acquire();
	}

	/**
	 * Called as part of the activity lifecycle when an activity is going into the background, but has not (yet) been killed. The counterpart to onResume().
	 */
	@Override
	protected void onPause() {
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onPause()");
		super.onPause();
		
		// Stop camera - synchronized to avoid races
		synchronized (this) {
			weHaveBeenResumed = false;
			stopAndReleaseCamera();
		}

		wakelock.release();
	}

	
	////////////////////////////////////////////////////////////////////////////////// 
	// Scanner startup and shutdown

	private void createCamera()
	{
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "createCamera()");

		camera = Camera.open();
		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			throw new RuntimeException("Failed to set camera preview surface!", e);
		}
	}
	
	private void startCamera()
	{
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "startScanner()");
		
        Camera.Parameters parameters = camera.getParameters();
        
        ResolutionOptions preOptions = new ResolutionOptions(parameters.get("preview-size-values"));
        if (preOptions.getSize()>0) {
        	ResolutionOptions.Option best = preOptions.leastDifference(preWidth, preHeight);
        	preWidth = best.getWidth();
        	preHeight = best.getHeight();
        } else {
        	// Make sure we are multiples of 8
        	preWidth = (preWidth>>3)<<3;
        	preHeight = (preHeight>>3)<<3;
        }
        parameters.setPreviewSize(preWidth, preHeight);
        
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Preview setup as "+preWidth+"x"+preHeight);

        camera.setParameters(parameters);
        camera.startPreview();
        cameraIsActive=true;
	}
	
	private void stopAndReleaseCamera()
	{
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "stopScannerReleaseCamera()");

		cameraIsActive=false;
		if (camera!=null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera=null;
		} else {
			if (Log.isLoggable(TAG, Log.WARN)) Log.w(TAG, "No camera to stop - can't have been built yet.");
		}
	}


	////////////////////////////////////////////////////////////////////////////////// 
	// Callbacks and listeners etc

	private Callback surfaceHolderCallback = new Callback() {
		public void surfaceCreated(SurfaceHolder holder)
		{
			synchronized (VisualSearchActivity.this) {
				if (weHaveBeenResumed) {
					createCamera();
					if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Surface created - creating camera");
				} else {
					if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Surface created - not yet resumed so don't create the camera");
				}
			}
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
		{
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Surface changed: "+format+","+width+","+height);
			
			synchronized (VisualSearchActivity.this)
			{
				if (cameraIsActive) {
					if (Log.isLoggable(TAG, Log.WARN)) Log.w(TAG, "Scanner is already active - ignoring!");
				} else {		        
			        // We always expect the reader screen to run fixed in landscape mode.
			        // In some circs the moto milestone running 2.1 reverses the width/height
			        if (width<height) {
			        	if (Log.isLoggable(TAG, Log.WARN)) Log.w(TAG, "Width and height appear to be swapped - swapping back!");
			        	int tmp = width;
			        	width = height;
			        	height = tmp;
			        }
			        
			        preWidth = width;
			        preHeight = height;
			        
			        if (weHaveBeenResumed) {
			        	startCamera();
			        } else {
			        	if (Log.isLoggable(TAG, Log.DEBUG))Log.d(TAG, "Not yet resumed so don't start the camera yet!");
			        }			        

			        existingSurfaceHasSize = true;				
				}			
			}
		}

		public void surfaceDestroyed(SurfaceHolder holder)
		{
			if (Log.isLoggable(TAG, Log.DEBUG))Log.d(TAG, "Surface destroyed");
			existingSurfaceHasSize = false;
			
			if (cameraIsActive) {
				if (Log.isLoggable(TAG, Log.WARN)) Log.w(TAG, "We should never get here with an active camera!");
			}
		}
	};
	
	private OnClickListener onClickListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onClick!");
			
			synchronized (VisualSearchActivity.this) {
				if (cameraIsActive) {
					camera.autoFocus(autofocusCallback);
				} else {
					if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Camera isn't active so don't initiate capture.");					
				}
			}
			
			shutterButton.setEnabled(false);
		}		
	};
	
	private Camera.AutoFocusCallback autofocusCallback = new Camera.AutoFocusCallback() {
		public void onAutoFocus(boolean success, Camera camera)
		{
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "AF: onAutoFocus: "+success);
			
			synchronized (VisualSearchActivity.this) {
				if (!cameraIsActive) {
					// Most likely backed out of capture screen
					if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Camera has gone so we must have stopped scanning!");
				} else if (success) {
					// Focus achieved (or non-autofocus device!) - capture a frame
					camera.setPreviewCallback(previewCallback);
				} else {
					// Autofocus failed - this can happen on some non-AF devices so capture the frame anyway
					camera.setPreviewCallback(previewCallback);
				}			
			}
		}
	};
	
	private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
		public void onPreviewFrame(byte[] data, Camera camera)
		{
			camera.setPreviewCallback(null);

			int size = preWidth*preHeight;
			if (argb8888==null || argb8888.length!=size) argb8888=new int[size];;						
			decodeYUV(argb8888, data, preWidth, preHeight);
			bitmap = Bitmap.createBitmap(argb8888, preWidth, preHeight, Config.ARGB_8888);

			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Captured picture is "+bitmap.getWidth()+"x"+bitmap.getHeight());
    		
    		shutterButton.setEnabled(true);
    		Intent intent = new Intent(VisualSearchActivity.this, SearchingActivity.class);
			startActivity(intent);
			// No finish - this stays in the stack
		}
	};
	
	// http://stackoverflow.com/questions/1893072/getting-frames-from-video-image-in-android
	// decode Y, U, and V values on the YUV 420 buffer described as YCbCr_422_SP by Android 
	// David Manpearl 081201 
	public void decodeYUV(int[] out, byte[] fg, int width, int height) {
	    int sz = width * height;
	    int i, j;
	    int Y, Cr = 0, Cb = 0;
	    for (j = 0; j < height; j++) {
	        int pixPtr = j * width;
	        final int jDiv2 = j >> 1;
	        for (i = 0; i < width; i++) {
	            Y = fg[pixPtr];
	            if (Y < 0)
	                Y += 255;
	            if ((i & 0x1) != 1) {
	                final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
	                Cb = fg[cOff];
	                if (Cb < 0)
	                    Cb += 127;
	                else
	                    Cb -= 128;
	                Cr = fg[cOff + 1];
	                if (Cr < 0)
	                    Cr += 127;
	                else
	                    Cr -= 128;
	            }
	            int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
	            if (R < 0)
	                R = 0;
	            else if (R > 255)
	                R = 255;
	            int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1)
	                    + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
	            if (G < 0)
	                G = 0;
	            else if (G > 255)
	                G = 255;
	            int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
	            if (B < 0)
	                B = 0;
	            else if (B > 255)
	                B = 255;
	            out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
	        }
	    }
	}
	
	/**
	 * Called when a key was pressed down and not handled by any of the views inside of the activity.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onKeyDown("+keyCode+","+event.getAction()+")");
		boolean result=false;
		
		if (keyCode==KeyEvent.KEYCODE_FOCUS) {
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Focus keydown - suppress");
			// Ignore!
			result=true;
		} else if (keyCode==KeyEvent.KEYCODE_CAMERA) {
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Shutter keydown - suppress");
			// Ignore
			result=true;
		} else {
			result = super.onKeyDown(keyCode, event); 
		}
		
		return result;
	}
	
	
	////////////////////////////////////////////////////////////////////////////////// 
	// Menu to allow access to configuration screen
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.camera_options, menu);
	    return true;
	}	

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		if (item.getItemId()==R.id.menu_configure) {
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Configure menu item");
			ConfigureActivity.doConfigure(VisualSearchActivity.this);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}
