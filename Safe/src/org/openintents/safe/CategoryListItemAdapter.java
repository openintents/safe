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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Randy McEoin
 *         <p/>
 *         Many thanks to Professional Android Application Development by Reto Meier
 *         from which this adapter is based upon.
 */
public class CategoryListItemAdapter extends ArrayAdapter<CategoryEntry> {

    private static boolean debug = false;
    private static final String TAG = "CategoryListItemAdapter";
    private List<CategoryEntry> filteredItems;
    private List<CategoryEntry> allItems;
    private final Object lock = new Object();
    private Filter filter = null;

    int resource;

    public CategoryListItemAdapter(Context _context, int _resource,
                                   List<CategoryEntry> _items) {
        super(_context, _resource, _items);
        resource = _resource;
        filteredItems = _items;
        allItems = new ArrayList<CategoryEntry>();
        for (CategoryEntry item : _items) {
            allItems.add(item);
        }
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
            vi = (LayoutInflater) getContext().getSystemService(inflater);
            vi.inflate(resource, categoryListView, true);
        } else {
            categoryListView = (LinearLayout) convertView;
        }

        TextView nameView = (TextView) categoryListView.findViewById(R.id.rowName);
        TextView countView = (TextView) categoryListView.findViewById(R.id.rowCount);

        if (debug) {
            Log.d(TAG, "count=" + count);
        }
        nameView.setText(name);
        countView.setText(Integer.toString(count));

        return categoryListView;
    }

    @Override
    public int getCount() {
        synchronized (lock) {
            return filteredItems != null ? filteredItems.size() : 0;
        }
    }

    @Override
    public boolean isEmpty() {
        return allItems.isEmpty();
    }

    @Override
    public CategoryEntry getItem(int item) {
        CategoryEntry gi = null;
        synchronized (lock) {
            gi = filteredItems != null ? filteredItems.get(item) : null;

        }
        return gi;
    }

    public Filter getFilter() {
        if (filter == null) {
            filter = new CategoryEntryFilter();
        }
        return filter;
    }

    private class CategoryEntryFilter extends Filter {
        protected FilterResults performFiltering(CharSequence search) {
            FilterResults results = new FilterResults();

            if (search == null || search.length() == 0) {
                synchronized (lock) {
                    results.values = allItems;
                    results.count = allItems.size();
                }
            } else {
                synchronized (lock) {
                    String q = search.toString().toLowerCase();
                    final ArrayList<CategoryEntry> filtered = new ArrayList<CategoryEntry>();
                    final int count = allItems.size();

                    for (int i = 0; i < count; i++) {
                        CategoryEntry item = allItems.get(i);
                        String itemName = item.plainName.toLowerCase();
                        String[] words = itemName.split(" ");
                        int wordCount = words.length;

                        for (int k = 0; k < wordCount; k++) {
                            if (words[k].startsWith(q)) {
                                filtered.add(item);
                                break;
                            }
                        }
                    }

                    results.values = filtered;
                    results.count = filtered.size();
                }// /synchronized
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence prefix, FilterResults results) {
            synchronized (lock) {
                @SuppressWarnings("unchecked")
                final ArrayList<CategoryEntry> localItems = (ArrayList<CategoryEntry>) results.values;
                clear();
                Iterator<CategoryEntry> iterator = localItems.iterator();
                while (iterator.hasNext()) {
                    CategoryEntry gi = (CategoryEntry) iterator.next();
                    add(gi);
                }
                notifyDataSetChanged();
            }
        }
    }
}

