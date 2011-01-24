/* $Id$
 * 
 * Copyright (C) 2009 OpenIntents.org
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openintents.intents.CryptoIntents;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Search extends ListActivity {
	
	private static final String TAG = "Search";
	private static boolean debug = false;

	public static final int REQUEST_VIEW_PASSWORD = 1;

	private static final int SEARCH_PROGRESS_KEY = 0;

	private static final int MSG_SEARCH_COMPLETE = 0;

	private Thread searchThread=null;

	private EditText etSearchCriteria;
	private String searchCriteria="";
	private List<PassEntry> results=null;
	private ArrayAdapter<String> entries=null;
	
	Intent frontdoor;
    private Intent restartTimerIntent=null;
	
    public Handler myViewUpdateHandler = new Handler(){
    	// @Override
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
    		case MSG_SEARCH_COMPLETE:
    			setListAdapter(entries);
    			if ((entries!=null) && (entries.isEmpty())) {
    				Toast.makeText(Search.this, R.string.search_nothing_found,
   						Toast.LENGTH_LONG).show();
    			}
    			break;
    		}
    		super.handleMessage(msg);
    	}
    }; 

    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)) {
            	 if (debug) Log.d(TAG,"caught ACTION_CRYPTO_LOGGED_OUT");
            	 startActivity(frontdoor);
            }
        }
    };

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		if (debug) Log.d(TAG,"onCreate()");

		setContentView(R.layout.search);
		String title = getResources().getString(R.string.app_name) + " - " +
			getResources().getString(R.string.search);
		setTitle(title);

		frontdoor = new Intent(this, Safe.class);
		frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
		restartTimerIntent = new Intent (CryptoIntents.ACTION_RESTART_TIMER);

		etSearchCriteria = (EditText) findViewById(R.id.search_criteria);
		results=new ArrayList<PassEntry>();

		Button goButton = (Button) findViewById(R.id.go_button);
		goButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				searchCriteria = etSearchCriteria.getText().toString().trim().toLowerCase();
				searchThreadStart();
			}
		});

		etSearchCriteria.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
		        	// hide the soft keyboard
		        	InputMethodManager imm =
		        		(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		        	imm.toggleSoftInput(0, 0);
					searchCriteria = etSearchCriteria.getText().toString().trim().toLowerCase();
					searchThreadStart();
		            return true;
		        }
		        return false;
		    }
		});

		restoreMe();
    }

	@Override
	protected void onPause() {
		super.onPause();

		if (debug) Log.d(TAG,"onPause()");
		
		if ((searchThread != null) && (searchThread.isAlive())) {
			if (debug) Log.d(TAG,"wait for search thread");
			int maxWaitToDie=500000;
			try { searchThread.join(maxWaitToDie); } 
			catch(InterruptedException e){} //  ignore 
		}
		try {
			unregisterReceiver(mIntentReceiver);
		} catch (IllegalArgumentException e) {
			//if (debug) Log.d(TAG,"IllegalArgumentException");
		}
	}

	@Override
    protected void onResume() {
		super.onResume();
		
		if (debug) Log.d(TAG,"onResume()");

		if (CategoryList.isSignedIn()==false) {
			startActivity(frontdoor);
			return;
		}
        IntentFilter filter = new IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        registerReceiver(mIntentReceiver, filter);

        Passwords.Initialize(this);
    }

	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		if (debug) Log.d(TAG,"onListItemClick: position="+position);
		if ((results==null) || (results.size()==0)) {
			return;
		}
		Intent passView = new Intent(this, PassView.class);
		passView.putExtra(PassList.KEY_ID, results.get(position).id);
		if (debug) Log.d(TAG,"onListItemClick: category="+results.get(position).category);
		passView.putExtra(PassList.KEY_CATEGORY_ID, results.get(position).category);
		passView.putExtra(PassList.KEY_ROWIDS, PassList.getRowsIds(results));
		passView.putExtra(PassList.KEY_LIST_POSITION, position);
		startActivityForResult(passView,REQUEST_VIEW_PASSWORD);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent i) {
		super.onActivityResult(requestCode, resultCode, i);
		
		if (((requestCode==REQUEST_VIEW_PASSWORD)&&(PassView.entryEdited)) ||
			(resultCode==RESULT_OK)) {
			searchCriteria = etSearchCriteria.getText().toString().trim().toLowerCase();
			searchThreadStart();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case SEARCH_PROGRESS_KEY: {
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage(getString(R.string.search_progress));
			dialog.setIndeterminate(false);
			dialog.setCancelable(true);
			return dialog;
		}
		}
		return null;
	}

	/**
	 * Start a separate thread to search the database.   By running
	 * the search in a thread it allows the main UI thread to return
	 * and permit the updating of the progress dialog.
	 */
	private void searchThreadStart(){
		if (searchThread!=null) {
			if (searchThread.isAlive()) {
				// it's already searching
			} else {
				// just rerun
				showDialog(SEARCH_PROGRESS_KEY);
				searchThread.run();
			}
			return;
		}
		showDialog(SEARCH_PROGRESS_KEY);
		searchThread = new Thread(new Runnable() {
			public void run() {
				doSearch();
				dismissDialog(SEARCH_PROGRESS_KEY);
				sendBroadcast (restartTimerIntent);

				Message m = new Message();
				m.what = MSG_SEARCH_COMPLETE;
				Search.this.myViewUpdateHandler.sendMessage(m); 

				if (debug) Log.d(TAG,"thread end");
				}
			},"Search");
		searchThread.start();
	}

	private void doSearch() {
		if (debug) Log.d(TAG,"doSearch: searchCriteria="+searchCriteria);
		results.clear();
		if (searchCriteria.length()==0) {
			// don't bother searching for nothing
			return;
		}
		
		List<CategoryEntry> categories=Passwords.getCategoryEntries();
		for (CategoryEntry catRow : categories) {
			if (debug) Log.d(TAG,"doSearch: category="+catRow.plainName);
			List<PassEntry> passwords=Passwords.getPassEntries(catRow.id, true, false);
			for (PassEntry passRow : passwords) {
		    	if (searchThread.isInterrupted()) {
		    		return;
		    	}

				String description=passRow.plainDescription.toLowerCase();
				String website=passRow.plainWebsite.toLowerCase();
				String username=passRow.plainUsername.toLowerCase();
				String password=passRow.plainPassword.toLowerCase();
				String note=passRow.plainNote.toLowerCase();
				if (description.contains(searchCriteria) ||
						website.contains(searchCriteria) ||
						username.contains(searchCriteria) ||
						password.contains(searchCriteria) ||
						note.contains(searchCriteria)) {
					if (debug) Log.d(TAG,"matches: "+passRow.plainDescription);
					results.add(passRow);
					continue;
				}
			}
		}

		Collections.sort(results, new Comparator<PassEntry>() {
		    public int compare(PassEntry o1, PassEntry o2) {
		    	return o1.plainDescription.compareToIgnoreCase(o2.plainDescription);
		    }});

		updateListFromResults();
	}

	@Override  
	public Object onRetainNonConfigurationInstance() {  
		return(results);  
	}  

	@SuppressWarnings("unchecked")
	private void restoreMe() {  
		if (getLastNonConfigurationInstance()!=null) {  
			results=(List<PassEntry>)getLastNonConfigurationInstance();
			updateListFromResults();
			setListAdapter(entries);
		}  
	}
	
	private void updateListFromResults() {
		if (results==null) {
//		if ((results==null) || (results.size()==0)) {
			return;
		}

		List<String> passDescriptions=new ArrayList<String>();

		for (PassEntry passRow : results) {
			passDescriptions.add(passRow.plainDescription);
		}
		entries = 
			new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
				passDescriptions);
	}
	
	@Override
	public void onUserInteraction() {
		super.onUserInteraction();

		if (debug) Log.d(TAG,"onUserInteraction()");

		if (CategoryList.isSignedIn()==false) {
//			startActivity(frontdoor);
		}else{
			if (restartTimerIntent!=null) sendBroadcast (restartTimerIntent);
		}
	}
}
