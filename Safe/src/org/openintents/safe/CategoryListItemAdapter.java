/* $Id$
 * 
 * Copyright 2009 OpenIntents.org
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
package org.openintents.safe;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author Randy McEoin
 *
 * Many thanks to Professional Android Application Development by Reto Meier
 * from which this adapter is based upon.
 */
public class CategoryListItemAdapter extends ArrayAdapter<CategoryEntry> {

	private static boolean debug = false;
	private static final String TAG = "CategoryListItemAdapter";
	
	int resource;
	
	public CategoryListItemAdapter(Context _context, int _resource,
			List<CategoryEntry> _items) {
		super(_context, _resource, _items);
		resource = _resource;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout categoryListView;
		
		CategoryEntry item = getItem(position);
		
		String name = item.plainName;
		int count = item.count;
		
		if (convertView == null) {
			categoryListView = new LinearLayout(getContext());
			String inflater = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater vi;
			vi = (LayoutInflater)getContext().getSystemService(inflater);
			vi.inflate(resource, categoryListView, true);
		} else {
			categoryListView = (LinearLayout) convertView;
		}
		
		TextView nameView = (TextView)categoryListView.findViewById(R.id.rowName);
		TextView countView = (TextView)categoryListView.findViewById(R.id.rowCount);
		
		if (debug) Log.d(TAG, "count="+count);
		nameView.setText(name);
		countView.setText(Integer.toString(count));
		
		return categoryListView;
	}
}

