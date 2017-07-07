/*
 * Copyright (C) 2012 OpenIntents
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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.openintents.intents.CryptoIntents;

import org.openintents.safe.model.CategoryEntry;
import org.openintents.safe.model.PassEntry;
import org.openintents.safe.model.Passwords;
import org.openintents.safe.model.SearchEntry;

public class SearchFragment extends ListFragment {

    private static final String TAG = "Search";
    private static final boolean debug = false;

    public static final int REQUEST_VIEW_PASSWORD = 1;

    public static final String KEY_RESULTS = "results";

    // Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();

    // Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateResultsInUi();
        }
    };
    private Thread searchThread = null;

    private ProgressDialog progressDialog = null;

    private EditText etSearchCriteria;
    private String searchCriteria = "";
    private List<SearchEntry> results = null;
    private SearchListItemAdapter searchAdapter = null;

    Intent frontdoor;
    private Intent restartTimerIntent = null;

    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    CryptoIntents.ACTION_CRYPTO_LOGGED_OUT
            )) {
                if (debug) {
                    Log.d(TAG, "caught ACTION_CRYPTO_LOGGED_OUT");
                }
                startActivity(frontdoor);
            }
        }
    };

    private void updateResultsInUi() {
        // Back in the UI thread -- update our UI elements based on the data in
        // mResults
        searchAdapter = new SearchListItemAdapter(
                getActivity(),
                R.layout.search_row, results
        );
        setListAdapter(searchAdapter);
        if ((searchAdapter != null) && (searchAdapter.isEmpty())) {
            Toast.makeText(
                    getActivity(), R.string.search_nothing_found,
                    Toast.LENGTH_LONG
            ).show();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ViewGroup root = (ViewGroup) inflater.inflate(
                R.layout.search_fragment,
                null
        );

        frontdoor = new Intent(getActivity(), Safe.class);
        frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
        restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

        etSearchCriteria = (EditText) root.findViewById(R.id.search_criteria);
        results = new ArrayList<SearchEntry>();

        Button goButton = (Button) root.findViewById(R.id.go_button);
        goButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        searchCriteria = etSearchCriteria.getText().toString().trim()
                                .toLowerCase();
                        searchThreadStart();
                    }
                }
        );

        etSearchCriteria
                .setOnEditorActionListener(
                        new TextView.OnEditorActionListener() {
                            public boolean onEditorAction(TextView v, int actionId,
                                                          KeyEvent event) {
                                // Sadly with Jelly Bean actionId can equal 0 instead of IME_ACTION_SEARCH,
                                // so the hack event==null is used
                                if (actionId == EditorInfo.IME_ACTION_SEARCH
                                        || event == null) {
                                    // hide the soft keyboard
                                    InputMethodManager imm = (InputMethodManager) getActivity()
                                            .getSystemService(
                                                    Context.INPUT_METHOD_SERVICE
                                            );
                                    imm.toggleSoftInput(0, 0);
                                    searchCriteria = etSearchCriteria.getText()
                                            .toString().trim().toLowerCase();
                                    searchThreadStart();
                                    return true;
                                }
                                return false;
                            }
                        }
                );

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (debug) {
            Log.d(TAG, "onPause()");
        }

        if ((searchThread != null) && (searchThread.isAlive())) {
            if (debug) {
                Log.d(TAG, "wait for search thread");
            }
            int maxWaitToDie = 500000;
            try {
                searchThread.join(maxWaitToDie);
            } catch (InterruptedException e) {
            } // ignore
        }
        try {
            getActivity().unregisterReceiver(mIntentReceiver);
        } catch (IllegalArgumentException e) {
            // if (debug) Log.d(TAG,"IllegalArgumentException");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (debug) {
            Log.d(TAG, "onResume()");
        }

        if (!CategoryList.isSignedIn()) {
            startActivity(frontdoor);
            return;
        }
        IntentFilter filter = new IntentFilter(
                CryptoIntents.ACTION_CRYPTO_LOGGED_OUT
        );
        getActivity().registerReceiver(mIntentReceiver, filter);

        Passwords.Initialize(getActivity());
    }

    public static long[] getRowsIds(List<SearchEntry> rows) {
        if (debug) {
            Log.d(TAG, "getRowsIds() rows=" + rows);
        }
        if (rows != null) {
            long[] ids = new long[rows.size()];
            Iterator<SearchEntry> searchIter = rows.iterator();
            int i = 0;
            while (searchIter.hasNext()) {
                ids[i] = searchIter.next().id;
                i++;
            }
            return ids;
        } else {
            return null;
        }
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (debug) {
            Log.d(
                    TAG, "onListItemClick: position=" + position + " results="
                            + results
            );
        }
        if ((results == null) || (results.size() == 0)) {
            return;
        }
        Intent passView = new Intent(getActivity(), PassView.class);
        passView.putExtra(PassList.KEY_ID, results.get(position).id);
        if (debug) {
            Log.d(
                    TAG, "onListItemClick: category="
                            + results.get(position).category
            );
        }
        passView.putExtra(
                PassList.KEY_CATEGORY_ID,
                results.get(position).categoryId
        );
        passView.putExtra(PassList.KEY_ROWIDS, getRowsIds(results));
        passView.putExtra(PassList.KEY_LIST_POSITION, position);
        startActivityForResult(passView, REQUEST_VIEW_PASSWORD);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent i) {
        super.onActivityResult(requestCode, resultCode, i);

        if (((requestCode == REQUEST_VIEW_PASSWORD) && (PassView.entryEdited))
                || (resultCode == Activity.RESULT_OK)) {
            searchCriteria = etSearchCriteria.getText().toString().trim()
                    .toLowerCase();
            searchThreadStart();
        }
    }

    /**
     * Start a separate thread to search the database. By running the search in
     * a thread it allows the main UI thread to return and permit the updating
     * of the progress dialog.
     */
    private void searchThreadStart() {
        if (searchThread != null) {
            if (searchThread.isAlive()) {
                // it's already searching
            } else {
                // just rerun
                progressDialog.show();
                searchThread.run();
            }
            return;
        }

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.search_progress));
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(true);
        progressDialog.show();

        searchThread = new Thread(
                new Runnable() {
                    public void run() {
                        doSearch();
                        progressDialog.dismiss();
                        getActivity().sendBroadcast(restartTimerIntent);

                        mHandler.post(mUpdateResults);

                        if (debug) {
                            Log.d(TAG, "thread end");
                        }
                    }
                }, "Search"
        );
        searchThread.start();
    }

    private void doSearch() {
        if (debug) {
            Log.d(TAG, "doSearch: searchCriteria=" + searchCriteria);
        }
        results.clear();
        if (searchCriteria.length() == 0) {
            // don't bother searching for nothing
            return;
        }

        List<CategoryEntry> categories = Passwords.getCategoryEntries();
        for (CategoryEntry catRow : categories) {
            if (debug) {
                Log.d(TAG, "doSearch: category=" + catRow.plainName);
            }
            List<PassEntry> passwords = Passwords.getPassEntries(
                    catRow.id,
                    true, false
            );
            for (PassEntry passRow : passwords) {
                if (searchThread.isInterrupted()) {
                    return;
                }

                String description = passRow.plainDescription.toLowerCase();
                String website = passRow.plainWebsite.toLowerCase();
                String username = passRow.plainUsername.toLowerCase();
                String password = passRow.plainPassword.toLowerCase();
                String note = passRow.plainNote.toLowerCase();
                if (description.contains(searchCriteria)
                        || website.contains(searchCriteria)
                        || username.contains(searchCriteria)
                        || password.contains(searchCriteria)
                        || note.contains(searchCriteria)) {
                    if (debug) {
                        Log.d(TAG, "matches: " + passRow.plainDescription);
                    }
                    SearchEntry searchRow = new SearchEntry();
                    searchRow.name = passRow.plainDescription;
                    searchRow.id = passRow.id;
                    searchRow.category = catRow.plainName;
                    searchRow.categoryId = catRow.id;
                    results.add(searchRow);
                    continue;
                }
            }
        }

        Collections.sort(
                results, new Comparator<SearchEntry>() {
                    public int compare(SearchEntry o1, SearchEntry o2) {
                        return o1.name.compareToIgnoreCase(o2.name);
                    }
                }
        );

        updateListFromResults();
    }

    private void updateListFromResults() {
        if (results == null) {
            return;
        }
        searchAdapter = new SearchListItemAdapter(
                getActivity(),
                R.layout.search_row, results
        );
    }

    /**
     * If we have results, save them for use with onActivityCreated()
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (results != null) {
            SearchEntry[] aResults = new SearchEntry[results.size()];
            results.toArray(aResults);
            outState.putParcelableArray(KEY_RESULTS, aResults);
        }
    }

    /**
     * Restore results from onSaveInstanceState()
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (debug) {
            Log.d(TAG, "onActivityCreated(" + savedInstanceState + ")");
        }

        if ((savedInstanceState != null)
                && (savedInstanceState.containsKey(KEY_RESULTS))
                && (results != null)) {
            Parcelable[] parcels = savedInstanceState
                    .getParcelableArray(KEY_RESULTS);
            for (Parcelable par : parcels) {
                results.add((SearchEntry) par);
            }
            if (results.size() > 0) {
                updateResultsInUi();
            }
        }
    }

}
