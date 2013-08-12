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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class ConfigureActivity extends Activity
{
	// Too see all logging use "adb shell setprop log.tag.ConfigureActivity DEBUG" - default is WARN and above
	private static final String TAG = "ConfigureActivity";
	
	private static final String SERVICE_KEY = "dataset";
	private static final String FOLLOW_KEY = "follow";

	private SharedPreferences prefs = null;
	private String service = null;
	private boolean follow = false;
	
	private EditText configureService = null;
	private CheckBox configureFollow = null;
	
	static boolean configure = false;
	

	///////////////////////////////////////////////////////////////////////////////////////////////////
	// Lifecycle
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onCreate");
        
        prefs = getPreferences(MODE_PRIVATE);
        service = prefs.getString(SERVICE_KEY, "");
        follow = prefs.getBoolean(FOLLOW_KEY, false);

        // If we have come from VisualSearchActivity then show the configuration screen once.
        // Otherwise if we have a configuration then fall straight through
        if (!configure && service.length()>1) {
        	startVisualSearch();
        } else {
	        setContentView(R.layout.configure);
	        configureService = (EditText)findViewById(R.id.configure_edit_service);
	        configureService.setText(service);
	        configureService.setFilters(new InputFilter[] { base64Filter });
	        configureFollow = (CheckBox)findViewById(R.id.configure_follow);
	        configureFollow.setChecked(follow);
	        final Button configureButton = (Button)findViewById(R.id.configure_ok);
	        configureButton.setOnClickListener(configureOk);
        }
    }

    private InputFilter base64Filter = new InputFilter() {
		@Override
        public CharSequence filter(CharSequence source, int start, int end,Spanned dest, int dstart, int dend) { 
            for (int i = start; i < end; i++) {
            	char c = source.charAt(i);
            	if (!Character.isLetterOrDigit(c) && c!='+' && c!='/') { 
            		return "";     
            	}     
            }
            return null;   
        }
    };

    
	///////////////////////////////////////////////////////////////////////////////////////////////////
	// Callbacks and Listeners etc    

	private View.OnClickListener configureOk = new View.OnClickListener() {
		@Override
		public void onClick(View v)
		{
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "configureOk");
			String newService = configureService.getText().toString().trim();
			boolean newFollow = configureFollow.isChecked();
			
			if (newService.length()<=1) {
				Toast.makeText(ConfigureActivity.this, R.string.configure_fail, Toast.LENGTH_LONG).show();
			} else {
				boolean serviceChanged = !newService.equals(service);
				boolean followChanged = newFollow!=follow;
				
				if (serviceChanged || followChanged) {
					Editor edit = prefs.edit();
					if (serviceChanged) {
						service = newService;
						edit.putString(SERVICE_KEY, service);
					}
					if (followChanged) {
						follow=newFollow;
						edit.putBoolean(FOLLOW_KEY, follow);
					}
					edit.commit();
					if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Persisted changes: "+service+" "+follow);
				}
				
				startVisualSearch();
			}
		}
	};
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
    // Actions

	public static void doConfigure(Context caller) {
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Switching to configuration screen.");
		configure = true;
		Intent intent = new Intent(caller, ConfigureActivity.class);
		caller.startActivity(intent);
	}
	
	private void startVisualSearch() {
		SearchCloud.mars3Url = getString(R.string.configure_url)+service;
		SearchingActivity.autoFollowLinks = follow;
		
		if (configure) {
			configure = false;
		} else {
			Intent intent = new Intent(ConfigureActivity.this, VisualSearchActivity.class);
			startActivity(intent);
		}
		finish();
	}
}