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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import android.util.Log;

/**
 * Encapsulates storage of and reasoning about a list of available resolutions.
 */
public class ResolutionOptions
{
	// Too see all logging use "adb shell setprop log.tag.ResolutionOptions DEBUG" - default is WARN and above
	private static final String TAG = "ResolutionOptions";
	
	private static final Pattern COMMA = Pattern.compile(",");

	private Option[] options;
	
	public ResolutionOptions(String resolutionList)
	{
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Initialised with: "+resolutionList);
		
		if (resolutionList==null) {
			options = new Option[0];
		} else {
			List<Option> temp = new LinkedList<Option>(); 
			
		    for (String size : COMMA.split(resolutionList))
		    {
		        size = size.trim();
		        
		        int xPos = size.indexOf('x');
		        int width = Integer.parseInt(size.substring(0, xPos));
		        int height = Integer.parseInt(size.substring(xPos+1));
		        	        
		        temp.add(new Option(width,height));
		    }
		    
		    if (temp.size()>=2 && temp.get(0).getWidth()<temp.get(temp.size()-1).getWidth()) {
		    	Collections.reverse(temp);
		    }
		    
		    options = temp.toArray(new Option[temp.size()]);
		}
	}

	public int getSize()
	{
		return options.length;
	}

	public Option biggestWithin(int width, int height)
	{
		Option result = null;
		
		for (int i=0; i<options.length && result==null; i+=1)
		{
			if (options[i].getWidth()<=width && options[i].getHeight()<=height) {
				result = options[i];
			}
		}
		
		return result;
	}
	
	public Option leastDifference(int width, int height)
	{
		Option result = null;
		int bestDiff = Integer.MAX_VALUE;
		
		for (int i=0; i<options.length; i+=1) {
			int newDiff = Math.abs(options[i].getWidth()-width)+Math.abs(options[i].getHeight()-height);
			if (newDiff<bestDiff) {
				bestDiff=newDiff;
				result = options[i];
			}
		}
		
		return result;
	}
	
	public static class Option
	{
		private int width;
		private int height;
		
		public Option(int width, int height)
		{
			this.width = width;
			this.height = height;
		}

		public int getWidth()
		{
			return width;
		}

		public int getHeight()
		{
			return height;
		}
	}
}
