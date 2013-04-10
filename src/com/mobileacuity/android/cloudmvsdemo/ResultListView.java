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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TwoLineListItem;

/**
 * Very thin and dumb wrapper around TwoLineListItem that stores its position in the list
 */
public class ResultListView extends TwoLineListItem
{
	private int listPosition = -1;
	
	public ResultListView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public ResultListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public ResultListView(Context context)
	{
		super(context);
	}

	public int getListPosition()
	{
		return listPosition;
	}

	public void setListPosition(int listPosition)
	{
		this.listPosition = listPosition;
	}

	public void updateText(Result result)
	{
		getText1().setText(result.getResult());
		getText2().setText(""+result.getScore());				
	}
}
