/* $Id$
 * 
 * Copyright 2007-2008 Steven Osborn
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.openintents.intents.CryptoIntents;

import org.openintents.safe.wrappers.CheckWrappers;
import org.openintents.safe.wrappers.honeycomb.ClipboardManager;
import org.openintents.safe.wrappers.honeycomb.WrapActionBar;

/**
 * PassGen Activity
 *
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class PassGen extends Activity {

    private static boolean debug = false;
    private static String TAG = "PassGen";

    public static final int CHANGE_ENTRY_RESULT = 2;
    public static final String NEW_PASS_KEY = "new_pass";

    EditText pass_view;
    EditText pass_len;
    CheckBox pass_upper;
    CheckBox pass_lower;
    CheckBox pass_num;
    CheckBox pass_symbol;
    String charset = "";

    Button copy_clip;
    Button copy_entry;
    Button cancel;

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

    private final OnClickListener update_click = new OnClickListener() {
        public void onClick(View v) {
            genPassword();
        }
    };
    private final OnCheckedChangeListener update_checked = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
            genPassword();
        }
    };
    private final OnKeyListener update_key = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            genPassword();
            return false;
        }

    };
    private final OnFocusChangeListener update_focus = new OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            genPassword();
        }
    };

    private final OnClickListener cancel_listener = new OnClickListener() {
        public void onClick(View v) {
            finish();
        }
    };

    private final OnClickListener copy_clip_listener = new OnClickListener() {
        public void onClick(View v) {
            ClipboardManager cb = ClipboardManager.newInstance(getApplication());
            cb.setText(pass_view.getText().toString());
            finish();
        }
    };

    private final OnClickListener copy_entry_listener = new OnClickListener() {
        public void onClick(View v) {
            getIntent().putExtra(PassGen.NEW_PASS_KEY, pass_view.getText().toString());
            setResult(CHANGE_ENTRY_RESULT, getIntent());
            finish();
        }
    };

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        frontdoor = new Intent(this, Safe.class);
        frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
        restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

        setContentView(R.layout.pass_gen);

        String title = getString(R.string.app_name) + " - " +
                getString(R.string.generate_password);
        setTitle(title);

        pass_view = (EditText) findViewById(R.id.pass_view);
        pass_len = (EditText) findViewById(R.id.pass_length);
        pass_upper = (CheckBox) findViewById(R.id.pass_upper);
        pass_lower = (CheckBox) findViewById(R.id.pass_lower);
        pass_num = (CheckBox) findViewById(R.id.pass_num);
        pass_symbol = (CheckBox) findViewById(R.id.pass_symbol);

        pass_view.setOnClickListener(update_click);
        pass_len.setOnKeyListener(update_key);
        pass_len.setOnFocusChangeListener(update_focus);
        pass_upper.setOnCheckedChangeListener(update_checked);
        pass_lower.setOnCheckedChangeListener(update_checked);
        pass_num.setOnCheckedChangeListener(update_checked);
        pass_symbol.setOnCheckedChangeListener(update_checked);

        copy_clip = (Button) findViewById(R.id.copy_clip);
        copy_entry = (Button) findViewById(R.id.copy_entry);
        cancel = (Button) findViewById(R.id.cancel);

        copy_clip.setOnClickListener(copy_clip_listener);
        copy_entry.setOnClickListener(copy_entry_listener);
        cancel.setOnClickListener(cancel_listener);

        if (CheckWrappers.mActionBarAvailable) {
            WrapActionBar bar = new WrapActionBar(this);
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     *
     */
    protected void genPassword() {
        charset = "";
        StringBuilder pass = new StringBuilder();
        if (pass_upper.isChecked()) {
            charset += "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        }
        if (pass_lower.isChecked()) {
            charset += "abcdefghijklmnopqrstuvwxyz";
        }
        if (pass_num.isChecked()) {
            charset += "0123456789";
        }
        if (pass_symbol.isChecked()) {
            charset += "!@#$%^&*";
        }

        if (charset.length() == 0) {
            return;
        }
        int len = 0;
        try {
            len = Integer.parseInt(pass_len.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        SecureRandom generator = null;
        try {
            generator = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < len; i++) {
            int pos = generator.nextInt(charset.length());
            pass.append(charset.charAt(pos));
        }

        pass_view.setText(pass.toString());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            unregisterReceiver(mIntentReceiver);
        } catch (IllegalArgumentException e) {
            if (debug) {
                Log.d(TAG, "IllegalArgumentException");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!CategoryList.isSignedIn()) {
            startActivity(frontdoor);
            return;
        }
        IntentFilter filter = new IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        registerReceiver(mIntentReceiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return false;
    }

    /**
     * Handler for when a MenuItem is selected from the Activity.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
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
