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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.openintents.distribution.DistributionLibraryActivity;
import org.openintents.safe.password.Master;
import org.openintents.safe.service.AutoLockService;
import org.openintents.util.VersionUtils;

import java.io.File;
import java.security.NoSuchAlgorithmException;

/**
 * AskPassword Activity
 * <p/>
 * This activity just acts as a splash screen and gets the password from the
 * user that will be used to decrypt/encrypt password entries.
 *
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class AskPassword extends DistributionLibraryActivity {

    public static final int REQUEST_RESTORE = 0;
    public static final int REQUEST_RESTORE_FIRST_TIME = 1;
    // Menu Item order
    public static final int SWITCH_MODE_INDEX = Menu.FIRST;
    public static final int MUTE_INDEX = Menu.FIRST + 1;
    public static final int VIEW_NORMAL = 0;
    public static final int VIEW_KEYPAD = 1;
    private final static boolean debug = false;
    private static final int MENU_DISTRIBUTION_START = Menu.FIRST + 100; // MUST BE LAST

    private static final int DIALOG_DISTRIBUTION_START = 100; // MUST BE LAST
    public static String EXTRA_IS_LOCAL = "org.openintents.safe.bundle.EXTRA_IS_REMOTE";
    private static String TAG = "AskPassword";
    private int viewMode = VIEW_NORMAL;

    private EditText pbeKey;
    private DBHelper dbHelper = null;
    private TextView introText;
    //	private TextView confirmText;
    private TextView remoteAsk;
    private EditText confirmPass;
    private String PBEKey;
    private String dbSalt;
    private String dbMasterKey;
    private CryptoHelper ch;
    private boolean firstTime = false;

    // Keypad variables
    private String keypadPassword = "";

    private MediaPlayer mpDigitBeep = null;
    private MediaPlayer mpErrorBeep = null;
    private MediaPlayer mpSuccessBeep = null;
    private boolean mute = false;

    private Toast blankPasswordToast = null;
    private Toast invalidPasswordToast = null;
    private Toast confirmPasswordFailToast = null;

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("ShowToast")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mDistribution.setFirst(MENU_DISTRIBUTION_START, DIALOG_DISTRIBUTION_START);

        // Check whether EULA has been accepted
        // or information about new version can be presented.
        if (mDistribution.showEulaOrNewVersion()) {
            return;
        }

        if (debug) {
            Log.d(TAG, "onCreate(" + icicle + ")");
        }

        dbHelper = new DBHelper(this);
        if (dbHelper.isDatabaseOpen() == false) {
            Dialog dbError = new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.database_error_title)
                    .setPositiveButton(
                            android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finish();
                                }
                            }
                    )
                    .setMessage(R.string.database_error_msg)
                    .create();
            dbError.show();
            return;
        }

        ch = new CryptoHelper();
        if (dbHelper.needsUpgrade()) {
            switch (dbHelper.fetchVersion()) {
                case 2:
                    databaseVersionError();
            }
        }
        dbSalt = dbHelper.fetchSalt();
        dbMasterKey = dbHelper.fetchMasterKey();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean prefKeypad = sp.getBoolean(PreferenceActivity.PREFERENCE_KEYPAD, false);
        boolean prefKeypadMute = sp.getBoolean(PreferenceActivity.PREFERENCE_KEYPAD_MUTE, false);
        mute = prefKeypadMute;

        if (prefKeypad) {
            viewMode = VIEW_KEYPAD;
        }
        if (dbMasterKey.length() == 0) {
            firstTime = true;
        }
        if ((viewMode == VIEW_NORMAL) || (firstTime)) {
            normalInit();
        } else {
            keypadInit();
        }

        blankPasswordToast = Toast.makeText(AskPassword.this, R.string.notify_blank_pass, Toast.LENGTH_SHORT);
        invalidPasswordToast = Toast.makeText(AskPassword.this, R.string.invalid_password, Toast.LENGTH_SHORT);
        confirmPasswordFailToast = Toast.makeText(AskPassword.this, R.string.confirm_pass_fail, Toast.LENGTH_SHORT);
    }

    private void normalInit() {
        // Setup layout
        setContentView(R.layout.front_door);
        TextView header = (TextView) findViewById(R.id.entry_header);
        String version = VersionUtils.getVersionNumber(this);
        String appName = VersionUtils.getApplicationName(this);
        String head = appName + " " + version;
        header.setText(head);

        Intent thisIntent = getIntent();
        boolean isLocal = thisIntent.getBooleanExtra(EXTRA_IS_LOCAL, false);

        pbeKey = (EditText) findViewById(R.id.password);
        pbeKey.requestFocus();
        pbeKey.postDelayed(
                new Runnable() {
                    public void run() {
                        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        keyboard.showSoftInput(pbeKey, 0);
                    }
                }, 200
        );
        introText = findViewById(R.id.first_time);
        remoteAsk = findViewById(R.id.remote);
        confirmPass = findViewById(R.id.pass_confirm);
//		confirmText = (TextView) findViewById(R.id.confirm_lbl);
        if (dbMasterKey.length() == 0) {
            firstTime = true;
            introText.setVisibility(View.VISIBLE);
//			confirmText.setVisibility(View.VISIBLE);
            confirmPass.setVisibility(View.VISIBLE);
            checkForBackup();
        }
        if (!isLocal) {
            if (remoteAsk != null) {
                remoteAsk.setVisibility(View.VISIBLE);
            }
        }
        if (firstTime) {
            confirmPass.setOnEditorActionListener(
                    new TextView.OnEditorActionListener() {
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                handleContinue();
                                return true;
                            }
                            return false;
                        }
                    }
            );
        } else {
            pbeKey.setOnEditorActionListener(
                    new TextView.OnEditorActionListener() {
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                handleContinue();
                                return true;
                            }
                            return false;
                        }
                    }
            );
        }
        Button continueButton = (Button) findViewById(R.id.continue_button);

        continueButton.setOnClickListener(
                new View.OnClickListener() {

                    public void onClick(View arg0) {
                        handleContinue();
                    }
                }
        );
    }

    private void handleContinue() {
        PBEKey = pbeKey.getText().toString();
        // For this version of CryptoHelper, we use the user-entered password.
        // All other versions should be instantiated with the generated master
        // password.

        // Password must be at least 4 characters
        if (PBEKey.length() < 4) {
            pbeKey.setText("");
            confirmPass.setText("");
            pbeKey.requestFocus();
            blankPasswordToast.show();
            Animation shake = AnimationUtils
                    .loadAnimation(AskPassword.this, R.anim.shake);

            findViewById(R.id.password).startAnimation(shake);
            return;
        }

        // If it's the user's first time to enter a password,
        // we have to store it in the database. We are going to
        // store an encrypted hash of the password.
        // Generate a master key, encrypt that with the pbekey
        // and store the encrypted master key in database.
        if (firstTime) {

            // Make sure password and confirm fields match
            if (pbeKey.getText().toString().compareTo(
                    confirmPass.getText().toString()
            ) != 0) {
                confirmPass.setText("");
                confirmPasswordFailToast.show();
                Animation shake = AnimationUtils
                        .loadAnimation(AskPassword.this, R.anim.shake);

                findViewById(R.id.password).startAnimation(shake);
                return;
            }
            try {
                dbSalt = CryptoHelper.generateSalt();
                dbMasterKey = CryptoHelper.generateMasterKey();
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
                Toast.makeText(
                        AskPassword.this, getString(R.string.crypto_error)
                                + e1.getMessage(), Toast.LENGTH_SHORT
                ).show();
                return;
            }
            if (debug) {
                Log.i(TAG, "Saving Password: " + dbMasterKey);
            }
            try {
                ch.init(CryptoHelper.EncryptionStrong, dbSalt);
                ch.setPassword(PBEKey);
                String encryptedMasterKey = ch.encrypt(dbMasterKey);
                dbHelper.storeSalt(dbSalt);
                dbHelper.storeMasterKey(encryptedMasterKey);
            } catch (CryptoHelperException e) {
                Log.e(TAG, e.toString());
                Toast.makeText(
                        AskPassword.this, getString(R.string.crypto_error)
                                + e.getMessage(), Toast.LENGTH_SHORT
                ).show();
                return;
            }
        } else if (!checkUserPassword(PBEKey)) {
            // Check the user's password and display a
            // message if it's wrong
            pbeKey.setText("");
            invalidPasswordToast.show();
            Animation shake = AnimationUtils
                    .loadAnimation(AskPassword.this, R.anim.shake);

            findViewById(R.id.password).startAnimation(shake);
            return;
        }
        gotPassword();
    }

    private void gotPassword() {
        Intent callbackIntent = new Intent();

        // We no longer need the
        // user-entered PBEKey. The master key is used for everything
        // from here on out.
        if (debug) {
            Log.d(TAG, "got password");
        }
        setResult(RESULT_OK, callbackIntent);

        Master.setMasterKey(dbMasterKey);
        Master.setSalt(dbSalt);

        if (debug) {
            Log.d(TAG, "start AutoLockService");
        }
        Intent myIntent = new Intent(getApplicationContext(), AutoLockService.class);
        startService(myIntent);

        CryptoContentProvider.ch = ch;
        finish();
    }

    private void checkForBackup() {
        String backupFullname = PreferenceActivity.getBackupPath(this);
        File restoreFile = new File(backupFullname);
        if (!restoreFile.exists()) {
            return;
        }
        startActivityForResult(new Intent(this, RestoreFirstTimeActivity.class), REQUEST_RESTORE_FIRST_TIME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (debug) {
            Log.d(TAG, "onPause()");
        }

        if (blankPasswordToast != null) {
            blankPasswordToast.cancel();
        }
        if (invalidPasswordToast != null) {
            invalidPasswordToast.cancel();
        }
        if (confirmPasswordFailToast != null) {
            confirmPasswordFailToast.cancel();
        }

        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (debug) {
            Log.d(TAG, "onDestroy()");
        }
        keypadOnDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (debug) {
            Log.d(TAG, "onResume()");
        }
        if (CategoryList.isSignedIn() == true) {
            if (debug) {
                Log.d(TAG, "already signed in");
            }
            Intent callbackIntent = new Intent();
            setResult(RESULT_OK, callbackIntent);
            finish();
            return;
        }

        if (dbHelper == null) {
            dbHelper = new DBHelper(this);
        }
        if (dbHelper.isDatabaseOpen() == false) {
            if (debug) {
                Log.d(TAG, "eek! database is not open");
            }
            return;
        }
        if (viewMode == VIEW_NORMAL) {
            // clear pbeKey in case user had typed it, strayed
            // to something else, then another person opened
            // the app.   Wouldn't want the password already typed
            pbeKey.setText("");
        }

    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            MenuItem miSwitch = menu.findItem(SWITCH_MODE_INDEX);
            if (firstTime) {
                miSwitch.setEnabled(false);
            } else {
                miSwitch.setEnabled(true);
            }
            MenuItem miMute = menu.findItem(MUTE_INDEX);
            if (viewMode == VIEW_KEYPAD) {
                miMute.setVisible(true);
                if (mute) {
                    miMute.setTitle(R.string.sounds);
                    miMute.setIcon(android.R.drawable.ic_lock_silent_mode_off);
                } else {
                    miMute.setTitle(R.string.mute);
                    miMute.setIcon(android.R.drawable.ic_lock_silent_mode);
                }
            } else {
                miMute.setVisible(false);
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem item = menu.add(0, SWITCH_MODE_INDEX, 0, R.string.switch_mode);
        // icon set below in onPrepareOptionsMenu()
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        if (firstTime) {
            item.setEnabled(false);
        } else {
            item.setEnabled(true);
        }

        MenuItem miMute;
        if (mute) {
            miMute = menu.add(0, MUTE_INDEX, 0, R.string.sounds)
                    .setIcon(android.R.drawable.ic_lock_silent_mode_off);
        } else {
            miMute = menu.add(0, MUTE_INDEX, 0, R.string.mute)
                    .setIcon(android.R.drawable.ic_lock_silent_mode);
        }
        miMute.setVisible(viewMode == VIEW_KEYPAD);

        // Add distribution menu items last.
        mDistribution.onCreateOptionsMenu(menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem item = menu.findItem(SWITCH_MODE_INDEX);
        if (viewMode == VIEW_NORMAL) {
            item.setIcon(R.drawable.ic_menu_switch_numeric);
        } else { // viewMode==VIEW_KEYPAD
            item.setIcon(R.drawable.ic_menu_switch_alpha);
        }
        return true;
    }

    private void switchView() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor spe = sp.edit();
        if (viewMode == VIEW_NORMAL) {
            viewMode = VIEW_KEYPAD;
            spe.putBoolean(PreferenceActivity.PREFERENCE_KEYPAD, true);
            keypadInit();
        } else {
            viewMode = VIEW_NORMAL;
            spe.putBoolean(PreferenceActivity.PREFERENCE_KEYPAD, false);
            normalInit();
        }
        if (!spe.commit()) {
            if (debug) {
                Log.d(TAG, "commitment issues");
            }
        }
        invalidateOptionsMenu();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SWITCH_MODE_INDEX:
                switchView();
                break;
            case MUTE_INDEX:
                SharedPreferences msp = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor mspe = msp.edit();
                mspe.putBoolean(PreferenceActivity.PREFERENCE_KEYPAD_MUTE, !mute);
                mute = !mute;
                if (!mspe.commit()) {
                    if (debug) {
                        Log.d(TAG, "mute commitment issues");
                    }
                }
                break;
            default:
                Log.e(TAG, "Unknown itemId");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void databaseVersionError() {
        Dialog about = new AlertDialog.Builder(this)
                .setIcon(R.drawable.passicon)
                .setTitle(R.string.database_version_error_title)
                .setPositiveButton(
                        android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                setResult(RESULT_CANCELED);
                                finish();
                            }
                        }
                )
                .setMessage(R.string.database_version_error_msg)
                .create();
        about.show();

    }

    /**
     * @return
     */
    private boolean checkUserPassword(String password) {
        if (dbHelper == null) {
            // not sure what can cause this condition, but a NPE has been observed
            return false;
        }
        String encryptedMasterKey = dbHelper.fetchMasterKey();
        String decryptedMasterKey = "";
        if (debug) {
            Log.d(TAG, "checkUserPassword: encryptedMasterKey=" + encryptedMasterKey);
        }
        try {
            ch.init(CryptoHelper.EncryptionStrong, dbSalt);
            ch.setPassword(password);
            decryptedMasterKey = ch.decrypt(encryptedMasterKey);
            if (debug) {
                Log.d(TAG, "decryptedMasterKey=" + decryptedMasterKey);
            }
        } catch (CryptoHelperException e) {
            Log.e(TAG, e.toString());
        }
        if (ch.getStatus()) {
            dbMasterKey = decryptedMasterKey;
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i) {
        super.onActivityResult(requestCode, resultCode, i);

        if ((requestCode == REQUEST_RESTORE_FIRST_TIME) && (resultCode == RESULT_OK)) {
            Intent callbackIntent = new Intent();
            setResult(RESULT_OK, callbackIntent);
            finish();
        }
    }

    /////////////// Keypad Functions /////////////////////

    private void keypadInit() {
        if (mpDigitBeep == null) {
            mpDigitBeep = MediaPlayer.create(this, R.raw.dtmf2a);
            mpErrorBeep = MediaPlayer.create(this, R.raw.click6a);
            mpSuccessBeep = MediaPlayer.create(this, R.raw.dooropening1);
        }

        keypadPassword = "";

        setContentView(R.layout.keypad);

        TextView header = (TextView) findViewById(R.id.entry_header);
        String version = VersionUtils.getVersionNumber(this);
        String appName = VersionUtils.getApplicationName(this);
        String head = appName + " " + version;
        header.setText(head);

        Button keypad1 = (Button) findViewById(R.id.keypad1);
        keypad1.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "1";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypad2 = (Button) findViewById(R.id.keypad2);
        keypad2.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "2";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypad3 = (Button) findViewById(R.id.keypad3);
        keypad3.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "3";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypad4 = (Button) findViewById(R.id.keypad4);
        keypad4.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "4";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypad5 = (Button) findViewById(R.id.keypad5);
        keypad5.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "5";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypad6 = (Button) findViewById(R.id.keypad6);
        keypad6.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "6";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypad7 = (Button) findViewById(R.id.keypad7);
        keypad7.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "7";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypad8 = (Button) findViewById(R.id.keypad8);
        keypad8.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "8";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypad9 = (Button) findViewById(R.id.keypad9);
        keypad9.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "9";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypadStar = (Button) findViewById(R.id.keypad_star);
        keypadStar.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "*";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypad0 = (Button) findViewById(R.id.keypad0);
        keypad0.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "0";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypadPound = (Button) findViewById(R.id.keypad_pound);
        keypadPound.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadPassword += "#";
                        if (!mute) {
                            mpDigitBeep.start();
                        }
                    }
                }
        );
        Button keypadContinue = (Button) findViewById(R.id.keypad_continue);
        keypadContinue.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        keypadTryPassword(keypadPassword);
                    }
                }
        );
        ImageView keypadSwitch = (ImageView) findViewById(R.id.switch_button);
        keypadSwitch.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        switchView();
                    }
                }
        );

        keypadSwitch.setVisibility(View.INVISIBLE);
    }

    private void keypadOnDestroy() {
        if (mpDigitBeep != null) {
            mpDigitBeep.release();
            mpErrorBeep.release();
            mpSuccessBeep.release();
            mpDigitBeep = null;
            mpErrorBeep = null;
            mpSuccessBeep = null;
        }
    }

    private void keypadTryPassword(String password) {
        if (checkUserPassword(password)) {
            if (debug) {
                Log.d(TAG, "match!!");
            }
            if (!mute) {
                mpSuccessBeep.start();
            }
            gotPassword();
        } else {
            if (debug) {
                Log.d(TAG, "bad password");
            }
            if (!mute) {
                mpErrorBeep.start();
            }
            Animation shake = AnimationUtils
                    .loadAnimation(AskPassword.this, R.anim.shake);
            findViewById(R.id.keypad_continue).startAnimation(shake);

            keypadPassword = "";
        }
    }
}
