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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.openintents.intents.CryptoIntents;
import org.openintents.safe.wrappers.CheckWrappers;
import org.openintents.safe.wrappers.honeycomb.ClipboardManager;
import org.openintents.safe.wrappers.honeycomb.WrapActionBar;

public class PassEditFragment extends Fragment {

    private static final String TAG = "PassEditFragment";
    private static final boolean debug = false;
    public static final int REQUEST_GEN_PASS = 10;

    public static final int SAVE_PASSWORD_INDEX = Menu.FIRST;
    public static final int DEL_PASSWORD_INDEX = Menu.FIRST + 1;
    public static final int DISCARD_PASSWORD_INDEX = Menu.FIRST + 2;
    public static final int GEN_PASSWORD_INDEX = Menu.FIRST + 3;

    public static final int RESULT_DELETED = Activity.RESULT_FIRST_USER;

    private EditText descriptionText;
    public EditText passwordText;
    private EditText usernameText;
    private EditText websiteText;
    private EditText noteText;
    private Long RowId;
    private Long CategoryId;
    public boolean pass_gen_ret = false;
    private boolean discardEntry = false;
    public static boolean entryEdited = false;
    boolean populated = false;
    private MenuItem saveItem = null;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (debug) {
            Log.d(TAG, "onCreateView()");
        }

        ViewGroup root = (ViewGroup) inflater.inflate(
                R.layout.pass_edit_fragment, null
        );

        RowId = savedInstanceState != null ? savedInstanceState
                .getLong(PassList.KEY_ID) : null;
        if (RowId == null) {
            Bundle extras = getActivity().getIntent().getExtras();
            RowId = extras != null ? extras.getLong(PassList.KEY_ID) : null;
        }
        CategoryId = savedInstanceState != null ? savedInstanceState
                .getLong(PassList.KEY_CATEGORY_ID) : null;
        if (CategoryId == null) {
            Bundle extras = getActivity().getIntent().getExtras();
            CategoryId = extras != null ? extras
                    .getLong(PassList.KEY_CATEGORY_ID) : null;
        }
        if (debug) {
            Log.d(TAG, "RowId=" + RowId);
        }
        if (debug) {
            Log.d(TAG, "CategoryId=" + CategoryId);
        }

        if ((CategoryId == null) || (CategoryId < 1)) {
            // invalid Category
            getActivity().finish();
            return root;
        }

        frontdoor = new Intent(getActivity(), Safe.class);
        frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
        restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

        String title = getResources().getString(R.string.app_name) + " - "
                + getResources().getString(R.string.edit_entry);
        getActivity().setTitle(title);

        descriptionText = (EditText) root.findViewById(R.id.description);
        descriptionText.requestFocus();
        passwordText = (EditText) root.findViewById(R.id.password);
        usernameText = (EditText) root.findViewById(R.id.username);
        noteText = (EditText) root.findViewById(R.id.note);
        websiteText = (EditText) root.findViewById(R.id.website);

        Button goButton = (Button) root.findViewById(R.id.go);

        entryEdited = false;

        goButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {

                        String link = websiteText.getText().toString();
                        if (link == null || link.equals("") || link.equals("http://")) {
                            Toast.makeText(
                                    getActivity(),
                                    getString(R.string.invalid_url), Toast.LENGTH_SHORT
                            )
                                    .show();
                            return;
                        }

                        Toast.makeText(
                                getActivity(),
                                getString(R.string.password) + " "
                                        + getString(R.string.copied_to_clipboard),
                                Toast.LENGTH_SHORT
                        ).show();

                        ClipboardManager cb = ClipboardManager
                                .newInstance(getActivity().getApplication());
                        cb.setText(passwordText.getText().toString());

                        Intent i = new Intent(Intent.ACTION_VIEW);

                        Uri u = Uri.parse(link);
                        i.setData(u);
                        try {
                            startActivity(i);
                        } catch (ActivityNotFoundException e) {
                            // Let's try to catch the most common mistake: omitting
                            // http:
                            u = Uri.parse("http://" + link);
                            i.setData(u);
                            try {
                                startActivity(i);
                            } catch (ActivityNotFoundException e2) {
                                Toast.makeText(
                                        getActivity(), R.string.invalid_website,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    }
                }
        );
        // restoreMe();

        if (CheckWrappers.mActionBarAvailable) {
            WrapActionBar bar = new WrapActionBar(getActivity());
            bar.setDisplayHomeAsUpEnabled(true);

            // if Action Bar is available, save button needs to be updated more
            // often
            TextWatcher textWatcher = new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }

                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {
                    if (saveItem != null) {
                        saveItem.setVisible(!allFieldsEmpty());
                    }
                }
            };
            descriptionText.addTextChangedListener(textWatcher);
            passwordText.addTextChangedListener(textWatcher);
            usernameText.addTextChangedListener(textWatcher);
            noteText.addTextChangedListener(textWatcher);
            websiteText.addTextChangedListener(textWatcher);
        }

        getActivity().sendBroadcast(restartTimerIntent);
        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (debug) {
            Log.d(TAG, "onSaveInstanceState()");
        }
        if (RowId != null) {
            outState.putLong(PassList.KEY_ID, RowId);
        } else {
            outState.putLong(PassList.KEY_ID, -1);
        }
        outState.putLong(PassList.KEY_CATEGORY_ID, CategoryId);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (debug) {
            Log.d(TAG, "onActivityCreated(" + savedInstanceState + ")");
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(PassList.KEY_ID)) {
                RowId = savedInstanceState.getLong(PassList.KEY_ID);
            }
            if (savedInstanceState.containsKey(PassList.KEY_CATEGORY_ID)) {
                CategoryId = savedInstanceState
                        .getLong(PassList.KEY_CATEGORY_ID);
            }
            populated = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (debug) {
            Log.d(TAG, "onPause()");
        }

        if (getActivity().isFinishing() && discardEntry == false && allFieldsEmpty() == false) {
            savePassword();
        }
        try {
            getActivity().unregisterReceiver(mIntentReceiver);
        } catch (IllegalArgumentException e) {
            // if (debug) Log.d(TAG,"IllegalArgumentException");
        }
    }

    public void setDiscardEntry(boolean discard) {
        discardEntry = discard;
    }

    public void setSaveItem(MenuItem save) {
        saveItem = save;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (debug) {
            Log.d(TAG, "onResume()");
        }

        if (CategoryList.isSignedIn() == false) {
            // if (Passwords.isCryptoInitialized()) {
            // saveState();
            // }
            startActivity(frontdoor);
            return;
        }
        IntentFilter filter = new IntentFilter(
                CryptoIntents.ACTION_CRYPTO_LOGGED_OUT
        );
        getActivity().registerReceiver(mIntentReceiver, filter);

        Passwords.Initialize(getActivity());

        populateFields();
    }

    private void saveState() {
        PassEntry entry = new PassEntry();

        entry.category = CategoryId;
        entry.plainDescription = descriptionText.getText().toString();
        entry.plainWebsite = websiteText.getText().toString();
        entry.plainUsername = usernameText.getText().toString();
        entry.plainPassword = passwordText.getText().toString();
        entry.plainNote = noteText.getText().toString();

        entryEdited = true;

        if (RowId == null || RowId == -1) {
            entry.id = 0; // brand new entry
            RowId = Passwords.putPassEntry(entry);
            if (RowId == -1) {
                Toast.makeText(
                        getActivity(), R.string.entry_save_error,
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
        } else {
            entry.id = RowId;
            PassEntry storedEntry = Passwords.getPassEntry(RowId, true, false);
            // update fields that aren't set in the UI:
            entry.uniqueName = storedEntry.uniqueName;
            long success = Passwords.putPassEntry(entry);
            if (success == -1) {
                Toast.makeText(
                        getActivity(), R.string.entry_save_error,
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
        }
        Toast.makeText(getActivity(), R.string.entry_saved, Toast.LENGTH_SHORT)
                .show();
    }

    /**
     * Returns true if all fields are empty.
     */
    public boolean allFieldsEmpty() {
        return (descriptionText.getText().toString().trim().equals("")
                && websiteText.getText().toString().trim().equals("")
                && usernameText.getText().toString().trim().equals("")
                && passwordText.getText().toString().trim().equals("") && noteText
                .getText().toString().trim().equals(""));
    }

    /**
     * Save the password entry and finish the activity.
     */
    public void savePassword() {
        saveState();
        getActivity().setResult(Activity.RESULT_OK);
    }

    /**
     * Prompt the user with a dialog asking them if they really want to delete
     * the password.
     */
    public void deletePassword() {
        Dialog about = new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.passicon)
                .setTitle(R.string.dialog_delete_password_title)
                .setPositiveButton(
                        R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                deletePassword2();
                            }
                        }
                )
                .setNegativeButton(
                        R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // do nothing
                            }
                        }
                ).setMessage(R.string.dialog_delete_password_msg)
                .create();
        about.show();
    }

    /**
     * Follow up for the Delete Password dialog. If we have a RowId then delete
     * the password, otherwise just finish this Activity.
     */
    public void deletePassword2() {
        if ((RowId != null) && (RowId > 0)) {
            delPassword(RowId);
        } else {
            // user specified to delete a new entry
            // so simply exit out
            getActivity().finish();
        }
    }

    /**
     * Delete the password entry from the database given the row id within the
     * database.
     *
     * @param Id
     */
    private void delPassword(long Id) {
        Passwords.deletePassEntry(Id);
        discardEntry = true;
        getActivity().setResult(RESULT_DELETED);
        getActivity().finish();
    }

    /**
     *
     */
    private void populateFields() {
        if (debug) {
            Log.d(TAG, "populateFields()");
        }
        if (pass_gen_ret == true) {
            pass_gen_ret = false;
            return;
        }
        if (debug) {
            Log.d(TAG, "populateFields: populated=" + populated);
        }
        if (populated) {
            return;
        }
        if ((RowId != null) && (RowId != -1)) {
            PassEntry passEntry = Passwords.getPassEntry(RowId, true, false);
            if (passEntry != null) {
                descriptionText.setText(passEntry.plainDescription);
                websiteText.setText(passEntry.plainWebsite);
                usernameText.setText(passEntry.plainUsername);
                passwordText.setText(passEntry.plainPassword);
                noteText.setText(passEntry.plainNote);
            }
        }
        populated = true;
    }

}
