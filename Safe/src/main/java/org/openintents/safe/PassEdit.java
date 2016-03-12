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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import org.openintents.intents.CryptoIntents;

import org.openintents.safe.wrappers.CheckWrappers;
import org.openintents.safe.wrappers.honeycomb.WrapActionBar;

public class PassEdit extends FragmentActivity {

    private static final boolean debug = false;
    private static final String TAG = "PassEdit";

    public static final int REQUEST_GEN_PASS = 10;

    public static final int SAVE_PASSWORD_INDEX = Menu.FIRST;
    public static final int DEL_PASSWORD_INDEX = Menu.FIRST + 1;
    public static final int DISCARD_PASSWORD_INDEX = Menu.FIRST + 2;
    public static final int GEN_PASSWORD_INDEX = Menu.FIRST + 3;

    public static final int RESULT_DELETED = RESULT_FIRST_USER;

    private Intent restartTimerIntent = null;

    PassEditFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (debug) {
            Log.d(TAG, "onCreate()");
        }

        // Prevent screen shot shown on recent apps
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }

        setContentView(R.layout.pass_edit);

        restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

        fragment = (PassEditFragment) getSupportFragmentManager()
                .findFragmentById(R.id.pass_edit_fragment);

    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        if (debug) {
            Log.d(TAG, "onUserInteraction()");
        }

        if (CategoryList.isSignedIn() == false) {
            // startActivity(frontdoor);
        } else {
            if (restartTimerIntent != null) {
                sendBroadcast(restartTimerIntent);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem saveItem = menu.add(0, SAVE_PASSWORD_INDEX, 0, R.string.save)
                .setIcon(android.R.drawable.ic_menu_save).setShortcut('1', 's');
        fragment.setSaveItem(saveItem);
        if (CheckWrappers.mActionBarAvailable) {
            WrapActionBar.showIfRoom(saveItem);
            saveItem.setVisible(!fragment.allFieldsEmpty());
        }
        menu.add(0, DEL_PASSWORD_INDEX, 0, R.string.password_delete).setIcon(
                android.R.drawable.ic_menu_delete
        );
        menu.add(0, DISCARD_PASSWORD_INDEX, 0, R.string.discard_changes)
                .setIcon(android.R.drawable.ic_notification_clear_all);
        menu.add(0, GEN_PASSWORD_INDEX, 0, "Generate")
                .setIcon(android.R.drawable.ic_menu_set_as)
                .setShortcut('4', 'g');

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                fragment.setDiscardEntry(true);
                finish();
                break;
            case SAVE_PASSWORD_INDEX:
                fragment.savePassword();
                finish();
                break;
            case DEL_PASSWORD_INDEX:
                fragment.deletePassword();
                break;
            case DISCARD_PASSWORD_INDEX:
                fragment.setDiscardEntry(true);
                finish();
                break;
            case GEN_PASSWORD_INDEX:
                Intent i = new Intent(getApplicationContext(), PassGen.class);
                startActivityForResult(i, REQUEST_GEN_PASS);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (!CheckWrappers.mActionBarAvailable) {
            menu.findItem(SAVE_PASSWORD_INDEX).setEnabled(
                    !fragment.allFieldsEmpty()
            );
        }
        return super.onMenuOpened(featureId, menu);
    }

    /**
     *
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent i) {
        super.onActivityResult(requestCode, resultCode, i);

        if (requestCode == REQUEST_GEN_PASS) {
            if (resultCode == PassGen.CHANGE_ENTRY_RESULT) {
                String new_pass = i.getStringExtra(PassGen.NEW_PASS_KEY);
                if (debug) {
                    Log.d(TAG, new_pass);
                }
                fragment.passwordText.setText(new_pass);
                fragment.pass_gen_ret = true;
            }
        }
    }

}
