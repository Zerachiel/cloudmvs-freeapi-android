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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Display the list of results.
 */
public class ResultListActivity extends ListActivity implements ListAdapter
{
	// Too see all logging use "adb shell setprop log.tag.ResultListActivity DEBUG" - default is WARN and above
	private static final String TAG = "ResultListActivity";

	static Result[] results;
		
	private LayoutInflater inflater;
	
	////////////////////////////////////////////////////////////////////////////////// 
	// Lifecycle & UI
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// Cache the layout inflator
		inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
				
		setContentView(R.layout.resultlist);
		setListAdapter(this);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "onListItemClick for "+position);
		
		ResultListView view = (ResultListView)v;
		ResultActivity.result = results[view.getListPosition()];
		
		Intent intent = new Intent(this, ResultActivity.class);
		startActivity(intent);
	}
	
	
	////////////////////////////////////////////////////////////////////////////////// 
	// ListAdapter API
	
	@Override
	public int getCount()
	{
		return results.length;
	}

	@Override
	public Object getItem(int position)
	{
		return results[position];
	}

	@Override
	public long getItemId(int position)
	{
		return 0;
	}

	@Override
	public int getItemViewType(int position)
	{
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "getView for "+position);
		
		ResultListView view = (ResultListView)convertView;
		
		if (view==null) {
			view = (ResultListView)inflater.inflate(R.layout.listitem, null, false);
		}
		
		view.setListPosition(position);
		view.updateText(results[position]);
		
		return view;
	}

	@Override
	public int getViewTypeCount()
	{
		return 1;
	}

	@Override
	public boolean hasStableIds()
	{
		return false;
	}

	@Override
	public boolean isEmpty()
	{
		return results.length==0;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer)
	{
		// Ignore
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer)
	{
		// // Ignore
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return true;
	}

	@Override
	public boolean isEnabled(int position)
	{
		return true;
	}
}
