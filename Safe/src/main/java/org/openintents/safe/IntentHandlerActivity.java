/*  
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import org.openintents.intents.CryptoIntents;

import org.openintents.safe.dialog.DialogHostingActivity;
import org.openintents.safe.model.CategoryEntry;
import org.openintents.safe.model.PassEntry;
import org.openintents.safe.model.Passwords;
import org.openintents.safe.password.Master;

/**
 * FrontDoor Activity
 * <p/>
 * This activity just acts as a splash screen and gets the password from the
 * user that will be used to decrypt/encrypt password entries.
 *
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class IntentHandlerActivity extends Activity {

    private static final boolean debug = true;
    private static String TAG = "IntentHandlerActivity";

    private static final int REQUEST_CODE_ASK_PASSWORD = 1;
    private static final int REQUEST_CODE_ALLOW_EXTERNAL_ACCESS = 2;

    private CryptoHelper ch;

    SharedPreferences mPreferences;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (debug) {
            Log.d(TAG, "onCreate(" + icicle + ")");
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (Passwords.Initialize(this) == false) {
            finish();
        }
    }

    //currently only handles result from askPassword function.
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (debug) {
            Log.d(TAG, "onActivityResult: requestCode == " + requestCode + ", resultCode == " + resultCode);
        }

        switch (requestCode) {
            case REQUEST_CODE_ASK_PASSWORD:
                if (resultCode == RESULT_OK) {
                    if (debug) {
                        Log.d(TAG, "RESULT_OK");
                    }
                    actionDispatch();

                } else { // resultCode == RESULT_CANCELED, which means the user hit Back at AskPassword
                    if (debug) {
                        Log.d(TAG, "RESULT_CANCELED");
                    }
                    moveTaskToBack(true);
                    setResult(RESULT_CANCELED);
                    finish();
                }
                break;
            case REQUEST_CODE_ALLOW_EXTERNAL_ACCESS:
                // Check again, regardless whether user pressed "OK" or "Cancel".
                // Also, DialogHostingActivity never returns a resultCode different than
                // RESULT_CANCELED.
                if (debug) {
                    Log.i(TAG, "actionDispatch called right now");
                }
                actionDispatch();
                break;
        }

    }

    /**
     *
     */
    private void showDialogAllowExternalAccess() {
        Intent i = new Intent(this, DialogHostingActivity.class);
        i.putExtra(DialogHostingActivity.EXTRA_DIALOG_ID, DialogHostingActivity.DIALOG_ID_ALLOW_EXTERNAL_ACCESS);
        this.startActivityForResult(i, REQUEST_CODE_ALLOW_EXTERNAL_ACCESS);
    }

    protected void actionDispatch() {
        final Intent thisIntent = getIntent();
        final String action = thisIntent.getAction();
        Intent callbackIntent = getIntent();
        int callbackResult = RESULT_CANCELED;

        if (debug) {
            Log.d(TAG, "actionDispatch()");
        }
        if ((Master.getSalt() == null) || (Master.getSalt() == "")) {
            return;
        }
        if (ch == null) {
            ch = new CryptoHelper();
        }
        try {
            ch.init(CryptoHelper.EncryptionMedium, Master.getSalt());
            ch.setPassword(Master.getMasterKey());
        } catch (CryptoHelperException e1) {
            e1.printStackTrace();
            Toast.makeText(
                    this, getString(R.string.crypto_error)
                            + e1.getMessage(), Toast.LENGTH_SHORT
            ).show();
            return;
        }

        boolean externalAccess = mPreferences.getBoolean(PreferenceActivity.PREFERENCE_ALLOW_EXTERNAL_ACCESS, false);

        if (action == null || action.equals(Intent.ACTION_MAIN)) {
            //TODO: When launched from debugger, action is null. Other such cases?
            Intent i = new Intent(
                    getApplicationContext(),
                    CategoryList.class
            );
            startActivity(i);
        } else if (action.equals(CryptoIntents.ACTION_AUTOLOCK)) {
            if (debug) {
                Log.d(TAG, "autolock");
            }
            finish();
        } else if (externalAccess) {

            // which action?
            if (action.equals(CryptoIntents.ACTION_ENCRYPT)) {
                callbackResult = encryptIntent(thisIntent, callbackIntent);
            } else if (action.equals(CryptoIntents.ACTION_DECRYPT)) {
                callbackResult = decryptIntent(thisIntent, callbackIntent);
            } else if (action.equals(CryptoIntents.ACTION_GET_PASSWORD)
                    || action.equals(CryptoIntents.ACTION_SET_PASSWORD)) {
                try {
                    callbackIntent = getSetPassword(thisIntent, callbackIntent);
                    callbackResult = RESULT_OK;
                } catch (CryptoHelperException e) {
                    Log.e(TAG, e.toString(), e);
                    Toast.makeText(
                            IntentHandlerActivity.this,
                            "There was a crypto error while retreiving the requested password: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                } catch (Exception e) {
                    Log.e(TAG, e.toString(), e);
                    //TODO: Turn this into a proper error dialog.
                    Toast.makeText(
                            IntentHandlerActivity.this,
                            "There was an error in retreiving the requested password: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
            setResult(callbackResult, callbackIntent);
        }
        finish();
    }

    /**
     * Encrypt all supported fields in the intent and return the result in callbackIntent.
     * <p/>
     * This is supposed to be called by outside functions, so we encrypt using a random session key.
     *
     * @param thisIntent
     * @param callbackIntent
     * @return callbackResult
     */
    private int encryptIntent(final Intent thisIntent, Intent callbackIntent) {
        if (debug) {
            Log.d(TAG, "encryptIntent()");
        }

        int callbackResult = RESULT_CANCELED;
        try {
            if (thisIntent.hasExtra(CryptoIntents.EXTRA_TEXT)) {
                // get the body text out of the extras.
                String inputBody = thisIntent.getStringExtra(CryptoIntents.EXTRA_TEXT);
                if (debug) {
                    Log.d(TAG, "inputBody=" + inputBody);
                }
                String outputBody = "";
                outputBody = ch.encryptWithSessionKey(inputBody);
                // stash the encrypted text in the extra
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT, outputBody);
            }

            if (thisIntent.hasExtra(CryptoIntents.EXTRA_TEXT_ARRAY)) {
                String[] in = thisIntent.getStringArrayExtra(CryptoIntents.EXTRA_TEXT_ARRAY);
                if (debug) {
                    Log.d(TAG, "in=" + Arrays.toString(in));
                }
                String[] out = new String[in.length];
                for (int i = 0; i < in.length; i++) {
                    if (in[i] != null) {
                        out[i] = ch.encryptWithSessionKey(in[i]);
                    }
                }
                if (debug) {
                    Log.d(TAG, "out=" + Arrays.toString(out));
                }
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT_ARRAY, out);
            }

            if (thisIntent.getData() != null) {
                // Encrypt file from file URI
                Uri fileUri = thisIntent.getData();

                Uri newFileUri = ch.encryptFileWithSessionKey(getContentResolver(), fileUri);

                callbackIntent.setData(newFileUri);
            }

            if (thisIntent.hasExtra(CryptoIntents.EXTRA_SESSION_KEY)) {
                String sessionkey = ch.getCurrentSessionKey();
                if (sessionkey == null) {
                    return RESULT_CANCELED;
                }
                callbackIntent.putExtra(CryptoIntents.EXTRA_SESSION_KEY, sessionkey);

                // Warning! This overwrites any data intent set previously.
                callbackIntent.setData(CryptoContentProvider.CONTENT_URI);
            }

            callbackResult = RESULT_OK;
        } catch (CryptoHelperException e) {
            Log.e(TAG, e.toString());
        }
        return callbackResult;
    }

    /**
     * Decrypt all supported fields in the intent and return the result in callbackIntent.
     *
     * @param thisIntent
     * @param callbackIntent
     * @return callbackResult
     */
    private int decryptIntent(final Intent thisIntent, Intent callbackIntent) {
        int callbackResult = RESULT_CANCELED;
        try {

            if (thisIntent.hasExtra(CryptoIntents.EXTRA_TEXT)) {
                // get the body text out of the extras.
                String inputBody = thisIntent.getStringExtra(CryptoIntents.EXTRA_TEXT);
                String outputBody = "";
                outputBody = ch.decryptWithSessionKey(inputBody);
                // stash the encrypted text in the extra
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT, outputBody);
            }

            if (thisIntent.hasExtra(CryptoIntents.EXTRA_TEXT_ARRAY)) {
                String[] in = thisIntent.getStringArrayExtra(CryptoIntents.EXTRA_TEXT_ARRAY);
                String[] out = new String[in.length];
                for (int i = 0; i < in.length; i++) {
                    if (in[i] != null) {
                        out[i] = ch.decryptWithSessionKey(in[i]);
                    }
                }
                callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT_ARRAY, out);
            }

            if (thisIntent.getData() != null) {
                // Decrypt file from file URI
                Uri fileUri = thisIntent.getData();

                Uri newFileUri = ch.decryptFileWithSessionKey(this, fileUri);

                callbackIntent.setData(newFileUri);
            }

            if (thisIntent.hasExtra(CryptoIntents.EXTRA_SESSION_KEY)) {
                String sessionkey = ch.getCurrentSessionKey();
                callbackIntent.putExtra(CryptoIntents.EXTRA_SESSION_KEY, sessionkey);

                // Warning! This overwrites any data intent set previously.
                callbackIntent.setData(CryptoContentProvider.CONTENT_URI);
            }

            callbackResult = RESULT_OK;
        } catch (CryptoHelperException e) {
            Log.e(TAG, e.toString());
        }
        return callbackResult;
    }

    private Intent getSetPassword(Intent thisIntent, Intent callbackIntent) throws CryptoHelperException, Exception {
        String action = thisIntent.getAction();
        //TODO: Consider moving this elsewhere. Maybe DBHelper? Also move strings to resource.
        //DBHelper dbHelper = new DBHelper(this);
        if (debug) {
            Log.d(TAG, "GET_or_SET_PASSWORD");
        }
        String username = null;
        String password = null;

        String clearUniqueName = thisIntent.getStringExtra(CryptoIntents.EXTRA_UNIQUE_NAME);

        if (clearUniqueName == null) {
            throw new Exception("EXTRA_UNIQUE_NAME not set.");
        }

        PassEntry row = Passwords.findPassWithUniqueName(clearUniqueName);
        boolean passExists = (row != null);

        String callingPackage = getCallingPackage();
        if (passExists) { // check for permission to access this password.
            ArrayList<String> packageAccess = Passwords.getPackageAccess(row.id);
            if ((packageAccess == null) ||
                    (!PassEntry.checkPackageAccess(packageAccess, callingPackage))) {
                throw new Exception("It is currently not permissible for this application to request this password.");
            }
            /*TODO: check if this package is in the package_access table corresponding to this password:
             * "Application 'org.syntaxpolice.ServiceTest' wants to access the
					password for 'opensocial'.
					[ ] Grant access this time.
					[ ] Always grant access.
					[ ] Always grant access to all passwords in org.syntaxpolice.ServiceTest category?
					[ ] Don't grant access"
			 */
        } else {
            row = new PassEntry();
        }

        if (action.equals(CryptoIntents.ACTION_GET_PASSWORD)) {
            if (passExists) {
                username = row.plainUsername;
                password = row.plainPassword;
            } else {
                throw new Exception("Could not find password with the unique name: " + clearUniqueName);
            }

            // stashing the return values:
            callbackIntent.putExtra(CryptoIntents.EXTRA_USERNAME, username);
            callbackIntent.putExtra(CryptoIntents.EXTRA_PASSWORD, password);
        } else if (action.equals(CryptoIntents.ACTION_SET_PASSWORD)) {
            String clearUsername = thisIntent.getStringExtra(CryptoIntents.EXTRA_USERNAME);
            String clearPassword = thisIntent.getStringExtra(CryptoIntents.EXTRA_PASSWORD);
            if (clearPassword == null) {
                throw new Exception("PASSWORD extra must be set.");
            }
            row.plainUsername = clearUsername == null ? "" : clearUsername;
            row.plainPassword = clearPassword;
            // since this package is setting the password, it automatically gets access to it:
            if (passExists) { //exists already
                if (clearUsername.equals("") && clearPassword.equals("")) {
                    Passwords.deletePassEntry(row.id);
                } else {
                    Passwords.putPassEntry(row);
                }
            } else {// add a new one
                row.plainUniqueName = clearUniqueName;
                row.plainDescription = clearUniqueName; //for display purposes
                // TODO: Should we send these fields in extras also?  If so, probably not using
                // the org.openintents namespace?  If another application were to implement a keystore
                // they might not want to use these.
                row.plainWebsite = "";
                row.plainNote = "";

                String category = "Application Data";
                CategoryEntry c = Passwords.getCategoryEntryByName(category);
                if (c == null) {
                    c = new CategoryEntry();
                    c.plainName = "Application Data";
                    c.id = Passwords.putCategoryEntry(c); //doesn't add category if it already exists
                }
                row.category = c.id;
                row.id = 0;    // entry is truly new
                row.id = Passwords.putPassEntry(row);
            }
            ArrayList<String> packageAccess = Passwords.getPackageAccess(row.id);
            if ((packageAccess == null) ||
                    (!PassEntry.checkPackageAccess(packageAccess, callingPackage))) {
                Passwords.addPackageAccess(row.id, callingPackage);
            }

        }
        return (callbackIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (debug) {
            Log.d(TAG, "onPause()");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (debug) {
            Log.d(TAG, "onResume()");
        }
        startUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * @return
     */
    private boolean isIntentLocal() {
        String action = getIntent().getAction();
        boolean isLocal = action == null || action.equals(Intent.ACTION_MAIN) ||
                action.equals(CryptoIntents.ACTION_AUTOLOCK);
        if (debug) {
            Log.d(TAG, "isLocal=" + isLocal + ", action=" + action);
        }
        return isLocal;
    }

    public void startUp() {
        boolean askPassIsLocal = isIntentLocal();

        if (Master.getMasterKey() == null) {
            boolean promptforpassword = getIntent().getBooleanExtra(CryptoIntents.EXTRA_PROMPT, true);
            if (debug) {
                Log.d(TAG, "Prompt for password: " + promptforpassword);
            }
            if (promptforpassword) {
                if (debug) {
                    Log.d(TAG, "ask for password");
                }
                Intent askPass = new Intent(
                        getApplicationContext(),
                        AskPassword.class
                );

                String inputBody = getIntent().getStringExtra(CryptoIntents.EXTRA_TEXT);

                askPass.putExtra(CryptoIntents.EXTRA_TEXT, inputBody);
                askPass.putExtra(AskPassword.EXTRA_IS_LOCAL, askPassIsLocal);
                //TODO: Is there a way to make sure all the extras are set?
                startActivityForResult(askPass, REQUEST_CODE_ASK_PASSWORD);
            } else {
                if (debug) {
                    Log.d(TAG, "ask for password");
                }
                // Don't prompt but cancel
                setResult(RESULT_CANCELED);
                finish();
            }
        } else {
            boolean externalAccess = mPreferences.getBoolean(PreferenceActivity.PREFERENCE_ALLOW_EXTERNAL_ACCESS, false);

            if (askPassIsLocal || externalAccess) {
                if (debug) {
                    Log.d(TAG, "starting actiondispatch");
                }

                actionDispatch();
            } else {
                if (debug) {
                    Log.d(TAG, "start showDialogAllowExternalAccess()");
                }
                showDialogAllowExternalAccess();
            }
        }
    }
}
