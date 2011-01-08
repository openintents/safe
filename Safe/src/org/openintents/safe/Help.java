/* $Id$
 * 
 * Copyright 2008 Steven Osborn and Randy McEoin
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

import java.io.IOException;
import java.io.InputStream;

import org.openintents.intents.CryptoIntents;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

/**
 * This activity shows a help dialog to describe the application.
 * 
 * @author Randy McEoin
 */
public class Help extends Activity {

	private static boolean debug = false;
	private static String TAG = "Help";
	
    // Menu Item order
    public static final int CLOSE_HELP_INDEX = Menu.FIRST;

    Intent frontdoor;
    private Intent restartTimerIntent=null;

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

		frontdoor = new Intent(this, Safe.class);
		frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
		restartTimerIntent = new Intent (CryptoIntents.ACTION_RESTART_TIMER);

		//Setup layout
		setContentView(R.layout.help);
		String title = getResources().getString(R.string.app_name) + " - " +
			getResources().getString(R.string.help);
		setTitle(title);

		
        // Programmatically load text from an asset and place it into the
        // text view.  Note that the text we are loading is ASCII, so we
        // need to convert it to UTF-16.
        try {
            InputStream is = getAssets().open("help.html");
            
            // We guarantee that the available method returns the total
            // size of the asset...  of course, this does mean that a single
            // asset can't be more than 2 gigs.
            int size = is.available();
            
            // Read the entire asset into a local byte buffer.
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            
            // Convert the buffer into a Java string.
            String text = new String(buffer);
            
            final String mimeType = "text/html";
            final String encoding = "utf-8";
            
            // Finally stick the string into the text view.
            WebView wv = (WebView)findViewById(R.id.help);
            wv.loadData(text, mimeType, encoding);
        } catch (IOException e) {
            // Should never happen!
            throw new RuntimeException(e);
        }

    }

    @Override
	protected void onPause() {
		super.onPause();
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
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
	
		menu.add(0,CLOSE_HELP_INDEX, 0, R.string.close)
			.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
			.setShortcut('0', 'w');
		
		return super.onCreateOptionsMenu(menu);
    }
    public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case CLOSE_HELP_INDEX:
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
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