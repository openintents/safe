/*  
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

import java.util.List;

import org.openintents.intents.CryptoIntents;
import org.openintents.safe.password.Master;

/**
 * Allows user to change the password used to unlock the application
 * as well as encrypt the database.
 *
 * @author Randy McEoin
 */
public class ChangePass extends Activity {

    private static boolean debug = false;
    private static final String TAG = "ChangePass";

    String oldPassword;
    String newPassword;

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

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (debug) {
            Log.d(TAG, "onCreate()");
        }

        frontdoor = new Intent(this, Safe.class);
        frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
        restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

        setContentView(R.layout.chg_pass);
        String title = getResources().getString(R.string.app_name) + " - " +
                getResources().getString(R.string.change_password);
        setTitle(title);

        Button changePasswordButton = (Button) findViewById(R.id.change_password_button);

        changePasswordButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        performChangePass();
                    }
                }
        );

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (debug) {
            Log.d(TAG, "onPause()");
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
            Log.d(TAG, "onResume()");
        }

        if (CategoryList.isSignedIn() == false) {
            startActivity(frontdoor);
            return;
        }
        IntentFilter filter = new IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        registerReceiver(mIntentReceiver, filter);

        Passwords.Initialize(this);
    }

    /**
     * Check the old, new and verify fields then try to re-encrypt
     * the data.
     */
    private void performChangePass() {
        if (debug) {
            Log.d(TAG, "performChangePass()");
        }

        EditText oldPassword = (EditText) findViewById(R.id.old_password);
        EditText newPassword = (EditText) findViewById(R.id.new_password);
        EditText verifyPassword = (EditText) findViewById(R.id.verify_password);

        String oldPlain = oldPassword.getText().toString();
        String newPlain = newPassword.getText().toString();
        String verifyPlain = verifyPassword.getText().toString();

        if (newPlain.compareTo(verifyPlain) != 0) {
            Toast.makeText(
                    ChangePass.this, R.string.new_verify_mismatch,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        if (newPlain.length() < 4) {
            Toast.makeText(
                    ChangePass.this, R.string.notify_blank_pass,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        if (!checkUserPassword(oldPlain)) {
            Toast.makeText(
                    ChangePass.this, R.string.invalid_old_password,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        changeMasterPassword(oldPlain, newPlain);
    }

    private boolean changeMasterPassword(String oldPass, String newPass) {

        DBHelper dbHelper = new DBHelper(this);

        CryptoHelper ch = new CryptoHelper();

        String encryptedMasterKey = dbHelper.fetchMasterKey();
        String decryptedMasterKey = "";
        try {
            ch.init(CryptoHelper.EncryptionStrong, dbHelper.fetchSalt());
            ch.setPassword(oldPass);
            decryptedMasterKey = ch.decrypt(encryptedMasterKey);
            if (ch.getStatus() == true) {    // successful decryption?
                ch.setPassword(newPass);
                encryptedMasterKey = ch.encrypt(decryptedMasterKey);
                if (ch.getStatus() == true) { // successful encryption?
                    dbHelper.storeMasterKey(encryptedMasterKey);
                    Passwords.InitCrypto(CryptoHelper.EncryptionMedium, dbHelper.fetchSalt(), decryptedMasterKey);
                    Passwords.Reset();
                    dbHelper.close();
                    Toast.makeText(
                            ChangePass.this, R.string.password_changed,
                            Toast.LENGTH_LONG
                    ).show();
                    setResult(RESULT_OK);
                    finish();
                    return true;
                }
            }

        } catch (CryptoHelperException e) {
            Log.e(TAG, e.toString());
            Toast.makeText(
                    this, getString(R.string.crypto_error)
                            + e.getMessage(), Toast.LENGTH_SHORT
            ).show();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            Toast.makeText(
                    this, getString(R.string.crypto_error)
                            + e.getMessage(), Toast.LENGTH_SHORT
            ).show();
        }

        dbHelper.close();

        Toast.makeText(
                ChangePass.this, R.string.error_changing_password,
                Toast.LENGTH_LONG
        ).show();
        return false;
    }

    /**
     * This is an older function.   We'll want to re-use this when we
     * allow the user to regenerate the master key.
     *
     * @param oldPass
     * @param newPass
     */
    public void changePassword(String oldPass, String newPass) {
        if (debug) {
            Log.d(TAG, "changePassword(,)");
        }

        DBHelper dbHelper = new DBHelper(this);

        CryptoHelper ch = new CryptoHelper();

        List<CategoryEntry> categoryRows;
        categoryRows = dbHelper.fetchAllCategoryRows();

        List<PassEntry> passRows;
        passRows = dbHelper.fetchAllRows(Long.valueOf(0));

        /**
         * Decrypt everything using the old password.
         */
        if (debug) {
            Log.d(TAG, "decrypting");
        }
        ch.setPassword(oldPass);

        for (CategoryEntry row : categoryRows) {
            row.plainName = "";
            try {
                row.plainName = ch.decrypt(row.name);
            } catch (CryptoHelperException e) {
                if (debug) {
                    Log.e(TAG, e.toString());
                }
                Toast.makeText(
                        this, getString(R.string.crypto_error)
                                + e.getMessage(), Toast.LENGTH_SHORT
                ).show();
                return;
            }
        }

        for (PassEntry row : passRows) {
            try {
                row.plainDescription = ch.decrypt(row.description);
                row.plainWebsite = ch.decrypt(row.website);
                row.plainUsername = ch.decrypt(row.username);
                row.plainPassword = ch.decrypt(row.password);
                row.plainNote = ch.decrypt(row.note);
            } catch (CryptoHelperException e) {
                if (debug) {
                    Log.e(TAG, e.toString());
                }
                Toast.makeText(
                        this, getString(R.string.crypto_error)
                                + e.getMessage(), Toast.LENGTH_SHORT
                ).show();
                return;
            }
        }

        /**
         * Encrypt everything using the new password.
         */
        if (debug) {
            Log.d(TAG, "encrypting");
        }
        ch.setPassword(newPass);

        for (CategoryEntry row : categoryRows) {
            try {
                row.name = ch.encrypt(row.plainName);
            } catch (CryptoHelperException e) {
                if (debug) {
                    Log.e(TAG, e.toString());
                }
                Toast.makeText(
                        this, getString(R.string.crypto_error)
                                + e.getMessage(), Toast.LENGTH_SHORT
                ).show();
                return;
            }
        }

        for (PassEntry row : passRows) {
            try {
                row.description = ch.encrypt(row.plainDescription);
                row.website = ch.encrypt(row.plainWebsite);
                row.username = ch.encrypt(row.plainUsername);
                row.password = ch.encrypt(row.plainPassword);
                row.note = ch.encrypt(row.plainNote);
            } catch (CryptoHelperException e) {
                if (debug) {
                    Log.e(TAG, e.toString());
                }
                Toast.makeText(
                        this, getString(R.string.crypto_error)
                                + e.getMessage(), Toast.LENGTH_SHORT
                ).show();
                return;
            }
        }

        /**
         * Update the database with the newly encrypted data.
         */
        if (debug) {
            Log.d(TAG, "updating database");
        }
        dbHelper.beginTransaction();

        for (CategoryEntry row : categoryRows) {
            dbHelper.updateCategory(row.id, row);
        }

        for (PassEntry row : passRows) {
            dbHelper.updatePassword(row.id, row);
        }

        byte[] md5Key = CryptoHelper.md5String(newPass);
        String hexKey = CryptoHelper.toHexString(md5Key);
        String cryptKey = "";
        Log.i(TAG, "Saving Password: " + hexKey);
        try {
            cryptKey = ch.encrypt(hexKey);
            dbHelper.storeMasterKey(cryptKey);
        } catch (CryptoHelperException e) {
            Log.e(TAG, e.toString());
            Toast.makeText(
                    this, getString(R.string.crypto_error)
                            + e.getMessage(), Toast.LENGTH_SHORT
            ).show();
            dbHelper.rollback();
            dbHelper.close();
            return;
        }

        dbHelper.commit();

        Master.setMasterKey(newPass);

        dbHelper.close();
    }

    /**
     * Check the provided clear text password with the one stored
     * in the database.
     *
     * @param pass = clear text password
     * @return True if password is correct.
     */
    private boolean checkUserPassword(String pass) {
        if (debug) {
            Log.d(TAG, "checkUserPassword()");
        }

        DBHelper dbHelper = new DBHelper(this);
        String confirmKey = dbHelper.fetchMasterKey();

        CryptoHelper ch = new CryptoHelper();

        try {
            ch.init(CryptoHelper.EncryptionStrong, dbHelper.fetchSalt());
            ch.setPassword(pass);
            ch.decrypt(confirmKey);
        } catch (CryptoHelperException e) {
            Log.e(TAG, e.toString());
        }
        dbHelper.close();

        // was decryption of the master key successful?
        if (ch.getStatus() == true) {
            return true;    // then we must have a good master password
        }
        return false;
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        if (debug) {
            Log.d(TAG, "onUserInteraction()");
        }

        if (CategoryList.isSignedIn() == false) {
//			startActivity(frontdoor);
        } else {
            if (restartTimerIntent != null) {
                sendBroadcast(restartTimerIntent);
            }
        }
    }
}
