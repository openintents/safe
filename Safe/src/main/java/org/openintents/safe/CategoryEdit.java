/* $Id$
 * 
 * Copyright 2008 Randy McEoin
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.openintents.intents.CryptoIntents;

import org.openintents.safe.model.CategoryEntry;
import org.openintents.safe.model.Passwords;

/**
 * CategoryEdit Activity
 *
 * @author Randy McEoin
 */
public class CategoryEdit extends Activity {

    private static final boolean debug = false;
    private static String TAG = "CategoryEdit";

    private EditText nameText;
    private Long RowId;
    boolean populated = false;

    Intent frontdoor;
    private Intent restartTimerIntent = null;

    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)) {
                if (debug) {
                    Log.d(TAG, "caught ACTION_CRYPTO_LOGGED_OUT");
                }
                startActivity(frontdoor);
            }
        }
    };

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (debug) {
            Log.d(TAG, "onCreate(" + icicle + ")");
        }

        frontdoor = new Intent(this, Safe.class);
        frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
        restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

        String title = getResources().getString(R.string.app_name) + " - " +
                getResources().getString(R.string.edit_entry);
        setTitle(title);

        setContentView(R.layout.cat_edit);

        nameText = (EditText) findViewById(R.id.name);

        Button confirmButton = (Button) findViewById(R.id.save_category);

        RowId = icicle != null ? icicle.getLong(CategoryList.KEY_ID) : null;
        if (RowId == null) {
            Bundle extras = getIntent().getExtras();
            RowId = extras != null ? extras.getLong(CategoryList.KEY_ID) : null;
        }

        confirmButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        // Don't allow the user to enter a blank name, we need
                        // something useful to show in the list
                        if (nameText.getText().toString().trim().length() == 0) {
                            Toast.makeText(
                                    CategoryEdit.this, R.string.notify_blank_name,
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }
                        saveState();
                        setResult(RESULT_OK);
                        finish();
                    }
                }
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (RowId != null) {
            outState.putLong(CategoryList.KEY_ID, RowId);
        } else {
            outState.putLong(CategoryList.KEY_ID, -1);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);

        if (debug) {
            Log.d(TAG, "onRestoreInstanceState(" + inState + ")");
        }
        // because the various EditText automatically handle state
        // when we come back there is no need to re-populate
        populated = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (debug) {
            Log.d(TAG, "onPause");
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
        if (debug) {
            Log.d(TAG, "onResume");
        }
        if (!CategoryList.isSignedIn()) {
            startActivity(frontdoor);
            return;
        }
        IntentFilter filter = new IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        registerReceiver(mIntentReceiver, filter);

        Passwords.Initialize(this);

        populateFields();
    }

    private void saveState() {
        if (debug) {
            Log.d(TAG, "saveState");
        }
        CategoryEntry entry = new CategoryEntry();

        String namePlain = nameText.getText().toString();
        if (debug) {
            Log.d(TAG, "name: " + namePlain);
        }
        entry.plainName = namePlain;

        if (RowId == null || RowId == -1) {
            entry.id = -1;
        } else {
            entry.id = RowId;
        }
        if (debug) {
            Log.d(TAG, "addCategory");
        }
        RowId = Passwords.putCategoryEntry(entry);
    }

    /**
     *
     */
    private void populateFields() {
        if (debug) {
            Log.d(TAG, "populateFields");
        }
        if (populated) {
            return;
        }
        if ((RowId != null) && (RowId > 0)) {
            CategoryEntry catEntry = Passwords.getCategoryEntry(RowId);
            if (catEntry == null) {
                return;
            }
            nameText.setText(catEntry.plainName);
        }
        populated = true;
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        if (debug) {
            Log.d(TAG, "onUserInteraction()");
        }

        if (!CategoryList.isSignedIn()) {
//			startActivity(frontdoor);
        } else {
            if (restartTimerIntent != null) {
                sendBroadcast(restartTimerIntent);
            }
        }
    }
}
