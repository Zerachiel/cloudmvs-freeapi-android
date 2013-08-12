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
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * Simple display of a single result.
 * If the result contains a URL it is clickable.
 */
public class ResultActivity extends Activity
{
	// Too see all logging use "adb shell setprop log.tag.ResultActivity DEBUG" - default is WARN and above
	private static final String TAG = "ResultActivity";
	
	// Setup by the caller
	static Result result;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Result: "+result);
		
		setContentView(R.layout.result);
		
		final TextView resultText = (TextView)findViewById(R.id.result_result);
		final TextView rawText = (TextView)findViewById(R.id.result_raw);
		final TextView scoreText = (TextView)findViewById(R.id.result_score);
		final TextView centreText = (TextView)findViewById(R.id.result_centre);
		resultText.setText(result.getResult());
		rawText.setText(result.getRaw());
		scoreText.setText(""+result.getScore());
		centreText.setText(result.getCx()+","+result.getCy());
		
		if (result.getUri()!=null) {
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Result is a valid URL so making clickable.");
			resultText.setTextColor(Color.BLUE);
			resultText.setPaintFlags(resultText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			resultText.setOnClickListener(onClickListener);
		}
	}	

	private OnClickListener onClickListener = new OnClickListener() {
		public void onClick(View v) {
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onClick: "+result.getResult());
			Intent intent = new Intent(Intent.ACTION_VIEW, result.getUri());
			startActivity(intent);
		}
	};
}
