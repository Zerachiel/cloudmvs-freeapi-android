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

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.webkit.URLUtil;

/**
 * Unpack and store JSON results from the Mobile Acuity Cloud MVS search API
 */
public class Result
{
	private static final String RAW = "r0";
	private static final String RESULT = "r";
	private static final String SCORE = "s";
	private static final String CENTRE = "c";
	
	private String raw;
	private String result;
	private Uri uri = null;
	private float score;
	private float cx;
	private float cy;
	
	public Result(String raw, String result, float score, float cx, float cy)
	{
		this.raw = raw;
		this.result = result;
		if (URLUtil.isHttpUrl(result) || URLUtil.isHttpsUrl(result)) uri = Uri.parse(result);
		this.score = score;
		this.cx = cx;
		this.cy = cy;
	}

	public String getRaw()
	{
		return raw;
	}

	public String getResult()
	{
		return result;
	}

	public Uri getUri()
	{
		return uri;
	}

	public float getScore()
	{
		return score;
	}

	public float getCx()
	{
		return cx;
	}

	public float getCy()
	{
		return cy;
	}
	
	@Override
	public String toString()
	{
		return "Result["+result+","+raw+","+score+","+cx+","+cy+"]";
	}

	public static Result[] unpackJsonResultsList(String json) throws JSONException
	{
		JSONArray jsonArray = new JSONArray(json);
		List<Result> resultsList = new LinkedList<Result>();

		for (int resultIdx=0; resultIdx<jsonArray.length(); resultIdx+=1) {
			JSONObject jsonObject = jsonArray.getJSONObject(resultIdx);
			
			String raw = jsonObject.getString(RAW);
			String result = jsonObject.getString(RESULT);
			float score = (float)jsonObject.getDouble(SCORE);
			String centre = jsonObject.getString(CENTRE);
			int commaPos = centre.indexOf(',');
			float cx = Float.parseFloat(centre.substring(0, commaPos));
			float cy = Float.parseFloat(centre.substring(commaPos+1));

			resultsList.add(new Result(raw, result, score, cx, cy));
		}
		
		Result[] results = new Result[resultsList.size()];
		resultsList.toArray(results);
		return results;
	}
}
