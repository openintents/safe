/* $Id$
 * 
 * Copyright (C) 2011 OpenIntents.org
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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * @author Randy McEoin
 *         <p/>
 *         Many thanks to Professional Android Application Development by Reto Meier
 *         from which this adapter is based upon.
 */
public class SearchListItemAdapter extends ArrayAdapter<SearchEntry> {

    private static boolean debug = false;
    private static final String TAG = "SearchListItemAdapter";

    int resource;

    public SearchListItemAdapter(Context _context, int _resource,
                                 List<SearchEntry> _items) {
        super(_context, _resource, _items);
        resource = _resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout searchListView;

        SearchEntry item = getItem(position);

        String name = item.name;
        String category = item.category;

        if (convertView == null) {
            searchListView = new LinearLayout(getContext());
            String inflater = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater vi;
            vi = (LayoutInflater) getContext().getSystemService(inflater);
            vi.inflate(resource, searchListView, true);
        } else {
            searchListView = (LinearLayout) convertView;
        }

        TextView nameView = (TextView) searchListView.findViewById(R.id.rowName);
        TextView categoryView = (TextView) searchListView.findViewById(R.id.rowCategory);

        if (debug) {
            Log.d(TAG, "category=" + category);
        }
        nameView.setText(name);
        categoryView.setText(category);

        return searchListView;
    }
}

