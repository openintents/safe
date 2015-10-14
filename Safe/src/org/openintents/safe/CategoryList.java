/* 
 * Copyright 2008-2012 OpenIntents.org
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.openintents.distribution.AboutDialog;
import org.openintents.intents.AboutMiniIntents;
import org.openintents.intents.CryptoIntents;
import org.openintents.safe.dialog.DialogHostingActivity;
import org.openintents.safe.password.Master;
import org.openintents.safe.service.AutoLockService;
import org.openintents.safe.wrappers.CheckWrappers;
import org.openintents.safe.wrappers.honeycomb.ClipboardManager;
import org.openintents.safe.wrappers.honeycomb.WrapActionBar;
import org.openintents.util.IntentUtils;
import org.openintents.util.SecureDelete;

/**
 * CategoryList Activity
 *
 * @author Randy McEoin
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class CategoryList extends ListActivity {

    private static boolean debug = false;
    private static final String TAG = "CategoryList";

    // Menu Item order
    public static final int LOCK_CATEGORY_INDEX = Menu.FIRST;
    public static final int OPEN_CATEGORY_INDEX = Menu.FIRST + 1;
    public static final int EDIT_CATEGORY_INDEX = Menu.FIRST + 2;
    public static final int ADD_CATEGORY_INDEX = Menu.FIRST + 3;
    public static final int DEL_CATEGORY_INDEX = Menu.FIRST + 4;
    public static final int HELP_INDEX = Menu.FIRST + 5;
    public static final int SEARCH_INDEX = Menu.FIRST + 6;
    public static final int EXPORT_INDEX = Menu.FIRST + 7;
    public static final int IMPORT_INDEX = Menu.FIRST + 8;
    public static final int CHANGE_PASS_INDEX = Menu.FIRST + 9;
    public static final int BACKUP_INDEX = Menu.FIRST + 10;
    public static final int RESTORE_INDEX = Menu.FIRST + 11;
    public static final int PREFERENCES_INDEX = Menu.FIRST + 12;
    public static final int ABOUT_INDEX = Menu.FIRST + 13;

    public static final int REQUEST_ONCREATE = 0;
    public static final int REQUEST_EDIT_CATEGORY = 1;
    public static final int REQUEST_ADD_CATEGORY = 2;
    public static final int REQUEST_OPEN_CATEGORY = 3;
    public static final int REQUEST_RESTORE = 4;
    public static final int REQUEST_IMPORT_FILENAME = 5;
    public static final int REQUEST_EXPORT_FILENAME = 6;
    public static final int REQUEST_BACKUP_FILENAME = 7;
    public static final int REQUEST_BACKUP_DOCUMENT = 8;

    private static final int ABOUT_KEY = 2;

    public static final int MAX_CATEGORIES = 256;

    private static final String PASSWORDSAFE_IMPORT_FILENAME = "passwordsafe.csv";

    public static final String KEY_ID = "id";  // Intent keys

    private static importTask taskImport = null;
    private ProgressDialog importProgress = null;
    private static boolean importDeletedDatabase = false;

    private static backupTask taskBackup = null;
    private ProgressDialog backupProgress = null;

    private List<CategoryEntry> rows = null;
    private CategoryListItemAdapter catAdapter = null;
    private Intent restartTimerIntent = null;
    private int lastPosition = 0;

    private AlertDialog autobackupDialog;

    private boolean lockOnScreenLock = true;

    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (debug) {
                    Log.d(TAG, "caught ACTION_SCREEN_OFF");
                }
                if (lockOnScreenLock) {
                    Master.setMasterKey(null);
                }
            } else if (intent.getAction().equals(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)) {
                if (debug) {
                    Log.d(TAG, "caught ACTION_CRYPTO_LOGGED_OUT");
                }
                lockAndShutFrontDoor();
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
            Log.d(TAG, "onCreate(" + icicle + ")");
        }

        restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

        Passwords.Initialize(this);

        setContentView(R.layout.cat_list);
        String title = getResources().getString(R.string.app_name) + " - " +
                getResources().getString(R.string.categories);
        setTitle(title);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        registerReceiver(mIntentReceiver, filter);

        final ListView list = getListView();
        list.setFocusable(true);
        list.setOnCreateContextMenuListener(this);
        list.setTextFilterEnabled(true);
        registerForContextMenu(list);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (debug) {
            Log.d(TAG, "onResume()");
        }

        if (isSignedIn() == false) {
            Intent frontdoor = new Intent(this, Safe.class);
            frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
            startActivity(frontdoor);
            return;
        }
        IntentFilter filter = new IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        registerReceiver(mIntentReceiver, filter);

        showFirstTimeWarningDialog();
        if (Passwords.getPrePopulate() == true) {
            prePopulate();
            Passwords.clearPrePopulate();
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        lockOnScreenLock = sp.getBoolean(Preferences.PREFERENCE_LOCK_ON_SCREEN_LOCK, true);

        if (taskImport != null) {
            // taskImport still running
            taskImport.setActivity(this);
            startImportProgressDialog();
            return;
        }
        Passwords.Initialize(this);

        ListAdapter la = getListAdapter();
        if (la != null) {
            if (debug) {
                Log.d(TAG, "onResume: count=" + la.getCount());
            }
        } else {
            if (debug) {
                Log.d(TAG, "onResume: no list");
            }
            fillData();
        }
        checkForAutoBackup();
    }

    private void checkForAutoBackup() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean prefAutobackup = sp.getBoolean(Preferences.PREFERENCE_AUTOBACKUP, true);

        if (prefAutobackup == false) {
            return;
        }
        if (Passwords.countPasswords(-1) < 1) {
            // if no passwords populated yet, then nevermind
            return;
        }
        TimeZone tz = TimeZone.getDefault();
        int julianDay = Time.getJulianDay((new Date()).getTime(), tz.getRawOffset());

        SharedPreferences.Editor editor = sp.edit();

        int lastAutobackupCheck = sp.getInt(Preferences.PREFERENCE_LAST_AUTOBACKUP_CHECK, 0);
        editor.putInt(Preferences.PREFERENCE_LAST_AUTOBACKUP_CHECK, julianDay);
        editor.commit();
        if (lastAutobackupCheck == 0) {
            // first time
            if (debug) {
                Log.d(TAG, "first time autobackup");
            }
            return;
        }
        int lastBackupJulian = sp.getInt(Preferences.PREFERENCE_LAST_BACKUP_JULIAN, 0);
        String maxDaysToAutobackupString = sp.getString(
                Preferences.PREFERENCE_AUTOBACKUP_DAYS,
                Preferences.PREFERENCE_AUTOBACKUP_DAYS_DEFAULT_VALUE
        );
        int maxDaysToAutobackup = 7;
        try {
            maxDaysToAutobackup = Integer.valueOf(maxDaysToAutobackupString);
        } catch (NumberFormatException e) {
            Log.d(TAG, "why is autobackup_days busted?");
        }

        int daysSinceLastBackup = julianDay - lastBackupJulian;
        if (((julianDay - lastAutobackupCheck) > (maxDaysToAutobackup - 1)) &&
                ((daysSinceLastBackup) > (maxDaysToAutobackup - 1))) {
            if (debug) {
                Log.d(TAG, "in need of backup");
            }
            // used a custom layout in order to get the checkbox
            AlertDialog.Builder builder;

            LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.auto_backup, null);

            builder = new AlertDialog.Builder(this);
            builder.setView(layout);
            builder.setTitle(getString(R.string.autobackup));
            builder.setIcon(android.R.drawable.ic_menu_save);
            builder.setNegativeButton(
                    R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            checkAutobackupTurnoff();
                        }
                    }
            );
            builder.setPositiveButton(
                    R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            checkAutobackupTurnoff();
                            backupThreadStart();
                        }
                    }
            );
            if (daysSinceLastBackup == julianDay) {
                builder.setMessage(R.string.backup_never);
            } else {
                String backupInDays = String.format(getString(R.string.backup_in_days), daysSinceLastBackup);
                builder.setMessage(backupInDays);
            }
            autobackupDialog = builder.create();
            autobackupDialog.show();
        }
    }

    private void checkAutobackupTurnoff() {
        CheckBox autobackupTurnoff = (CheckBox) autobackupDialog.findViewById(R.id.autobackup_turnoff);
        if (autobackupTurnoff.isChecked()) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(Preferences.PREFERENCE_AUTOBACKUP, false);
            editor.commit();
        }
    }

    /**
     *
     */
    private void showFirstTimeWarningDialog() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstTimeWarning = sp.getBoolean(Preferences.PREFERENCE_FIRST_TIME_WARNING, false);

        if (!firstTimeWarning) {
            Intent i = new Intent(this, DialogHostingActivity.class);
            i.putExtra(DialogHostingActivity.EXTRA_DIALOG_ID, DialogHostingActivity.DIALOG_ID_FIRST_TIME_WARNING);
            startActivity(i);

            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(Preferences.PREFERENCE_FIRST_TIME_WARNING, true);
            editor.commit();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (debug) {
            Log.d(TAG, "onPause()");
        }

        if (taskImport != null) {
            importProgress.dismiss();
            taskImport.setActivity(null);
        }
        if (taskBackup != null) {
            backupProgress.dismiss();
            taskBackup.setActivity(null);
        }

        try {
            unregisterReceiver(mIntentReceiver);
        } catch (IllegalArgumentException e) {
            //if (debug) Log.d(TAG,"IllegalArgumentException");
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (debug) {
            Log.d(TAG, "onStop()");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (debug) {
            Log.d(TAG, "onDestroy()");
        }
        try {
            unregisterReceiver(mIntentReceiver);
        } catch (IllegalArgumentException e) {
            //if (debug) Log.d(TAG,"IllegalArgumentException");
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenuInfo menuInfo) {

        AdapterView.AdapterContextMenuInfo info;
        info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        menu.setHeaderTitle(rows.get(info.position).plainName);
        menu.add(0, OPEN_CATEGORY_INDEX, 0, R.string.open)
                .setIcon(android.R.drawable.ic_menu_view)
                .setAlphabeticShortcut('o');
        menu.add(0, EDIT_CATEGORY_INDEX, 0, R.string.password_edit)
                .setIcon(android.R.drawable.ic_menu_edit)
                .setAlphabeticShortcut('e');
        menu.add(0, DEL_CATEGORY_INDEX, 0, R.string.password_delete)
                .setIcon(android.R.drawable.ic_menu_delete)
                .setAlphabeticShortcut('d');
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        onOptionsItemSelected(item);
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case ABOUT_KEY:
                return new AboutDialog(this);
        }
        return null;
    }

    /**
     * Returns the current status of signedIn.
     *
     * @return True if signed in
     */
    public static boolean isSignedIn() {
        if ((Master.getSalt() != null) && (Master.getMasterKey() != null)) {
            if (debug) {
                Log.d(TAG, "isSignedIn: true");
            }
            return true;
        }
        if (debug) {
            Log.d(TAG, "isSignedIn: false");
        }
        return false;
    }

    /**
     * Sets signedIn status to false.
     *
     * @see org.openintents.safe.CategoryList#isSignedIn
     */
    public static void setSignedOut() {
        if (debug) {
            Log.d(TAG, "setSignedOut()");
        }
        Master.setMasterKey(null);
    }

    /**
     * Populates the category ListView
     */
    private void fillData() {
        if (debug) {
            Log.d(TAG, "fillData()");
        }

        rows = Passwords.getCategoryEntries();
        if (debug) {
            Log.d(TAG, "fillData: rows=" + rows.size());
        }

        catAdapter =
                new CategoryListItemAdapter(
                        this, R.layout.cat_row,
                        rows
                );
        setListAdapter(catAdapter);

    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu == null) {
            return super.onMenuOpened(featureId, menu);
        }
        MenuItem miDelete = menu.findItem(DEL_CATEGORY_INDEX);
        MenuItem miEdit = menu.findItem(EDIT_CATEGORY_INDEX);
        if (getSelectedItemPosition() > -1) {
            miDelete.setEnabled(true);
            miEdit.setEnabled(true);
        } else {
            miDelete.setEnabled(false);
            miEdit.setEnabled(false);
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem item = menu.add(0, LOCK_CATEGORY_INDEX, 0, R.string.password_lock)
                .setIcon(android.R.drawable.ic_lock_lock)
                .setShortcut('0', 'l');
        if (CheckWrappers.mActionBarAvailable) {
            WrapActionBar.showIfRoom(item);
        }

        menu.add(0, EDIT_CATEGORY_INDEX, 0, R.string.password_edit)
                .setIcon(android.R.drawable.ic_menu_edit)
                .setShortcut('1', 'e');

        menu.add(0, ADD_CATEGORY_INDEX, 0, R.string.password_add)
                .setIcon(android.R.drawable.ic_menu_add)
                .setShortcut('2', 'a');

        item = menu.add(0, SEARCH_INDEX, 0, R.string.search)
                .setIcon(android.R.drawable.ic_menu_search);
        if (CheckWrappers.mActionBarAvailable) {
            WrapActionBar.showIfRoom(item);
        }

        menu.add(0, DEL_CATEGORY_INDEX, 0, R.string.password_delete)
                .setIcon(android.R.drawable.ic_menu_delete)
                .setEnabled(false);

        menu.add(0, HELP_INDEX, 0, R.string.help)
                .setIcon(android.R.drawable.ic_menu_help);

        menu.add(0, EXPORT_INDEX, 0, R.string.export_database)
                .setIcon(android.R.drawable.ic_menu_upload);
        menu.add(0, IMPORT_INDEX, 0, R.string.import_database)
                .setIcon(android.R.drawable.ic_input_get);

        menu.add(0, CHANGE_PASS_INDEX, 0, R.string.change_password)
                .setIcon(android.R.drawable.ic_menu_manage);

        menu.add(0, BACKUP_INDEX, 0, R.string.backup);
        menu.add(0, RESTORE_INDEX, 0, R.string.restore);

        menu.add(0, PREFERENCES_INDEX, 0, R.string.preferences);

        if (IntentUtils.isIntentAvailable(this, new Intent(AboutMiniIntents.ACTION_SHOW_ABOUT_DIALOG))) {
            // Only show "About" dialog, if OI About (or compatible) is installed.
            menu.add(0, ABOUT_INDEX, 0, R.string.oi_distribution_about).setIcon(
                    android.R.drawable.ic_menu_info_details
            );
        }

        return super.onCreateOptionsMenu(menu);
    }

    private void addCategoryActivity() {
        if (debug) {
            Log.d(TAG, "addCategoryActivity()");
        }
        Intent i = new Intent(this, CategoryEdit.class);
        startActivityForResult(i, REQUEST_ADD_CATEGORY);
    }

    private void delCategory(long Id) {
        if (Passwords.countPasswords(Id) > 0) {
            Toast.makeText(
                    CategoryList.this, R.string.category_not_empty,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        Passwords.deleteCategoryEntry(Id);
        fillData();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (restartTimerIntent != null) {
            sendBroadcast(restartTimerIntent);
        }

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = -1;
        if (info == null) {
            position = getSelectedItemPosition();
        } else {
            // used when this is called from a ContextMenu
            position = info.position;
        }
        switch (item.getItemId()) {
            case LOCK_CATEGORY_INDEX:
                lockAndShutFrontDoor();
                break;
            case OPEN_CATEGORY_INDEX:
                launchPassList(rows.get(info.position).id);
                break;
            case EDIT_CATEGORY_INDEX:
                if (position > -1) {
                    Intent i = new Intent(this, CategoryEdit.class);
                    i.putExtra(KEY_ID, rows.get(position).id);
                    startActivityForResult(i, REQUEST_EDIT_CATEGORY);

                    lastPosition = position;
                }
                break;
            case ADD_CATEGORY_INDEX:
                addCategoryActivity();
                break;
            case DEL_CATEGORY_INDEX:
                try {
                    if (position > -1) {
                        delCategory(rows.get(position).id);
                        if (position > 2) {
                            setSelection(position - 1);
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    // This should only happen when there are no
                    // entries to delete.
                    Log.w(TAG, e.toString());
                }
                break;
            case HELP_INDEX:
                Intent help = new Intent(this, Help.class);
                startActivity(help);
                break;
            case SEARCH_INDEX:
                onSearchRequested();
                break;
            case EXPORT_INDEX:
                Dialog exportDialog = new AlertDialog.Builder(CategoryList.this)
                        .setIcon(R.drawable.passicon)
                        .setTitle(R.string.export_database)
                        .setPositiveButton(
                                R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        exportDatabase();
                                    }
                                }
                        )
                        .setNegativeButton(
                                R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                }
                        )
                        .setMessage(R.string.export_msg)
                        .create();
                exportDialog.show();
                break;
            case IMPORT_INDEX:
                importDatabase();
                break;
            case CHANGE_PASS_INDEX:
                Intent changePass = new Intent(this, ChangePass.class);
                startActivity(changePass);
                break;
            case BACKUP_INDEX:
                backupThreadStart();
                break;
            case RESTORE_INDEX:
                restoreDatabase();
                break;
            case PREFERENCES_INDEX:
                Intent preferences = new Intent(this, Preferences.class);
                startActivity(preferences);
                break;
            case ABOUT_INDEX:
                AboutDialog.showDialogOrStartActivity(this, ABOUT_KEY);
                break;
            default:
                Log.e(TAG, "Unknown itemId");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchPassList(long id) {
        Intent passList = new Intent(this, PassList.class);
        passList.putExtra(KEY_ID, id);
        startActivityForResult(passList, REQUEST_OPEN_CATEGORY);
    }

    private String backupDatabase(String filename, OutputStream str) {
        Backup backup = new Backup(this);
        backup.write(filename, str);
        return backup.getResult();
    }

    private void lockAndShutFrontDoor() {

		/* Clear the clipboard, if it contains the last password used */
        ClipboardManager cb = ClipboardManager.newInstance(getApplication());
        if (cb.hasText()) {
            String clipboardText = cb.getText().toString();
            if (clipboardText.equals(Safe.last_used_password)) {
                cb.setText("");
            }
        }

        Master.setMasterKey(null);

        Intent autoLockIntent = new Intent(getApplicationContext(), AutoLockService.class);
        stopService(autoLockIntent);

        Intent frontdoor = new Intent(this, Safe.class);
        frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
        startActivity(frontdoor);
    }

    /**
     * Start a separate thread to backup the database.   By running
     * the backup in a thread it allows the main UI thread to return
     * and permit the updating of the progress dialog.
     */
    private void backupThreadStart() {
        String filename = Preferences.getBackupPath(this);
        Intent intent;
        int requestId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = Intents.createCreateDocumentIntent();
            requestId = REQUEST_BACKUP_DOCUMENT;
        } else {
            intent = Intents.createPickFileIntent(filename, R.string.import_file_select);
            requestId = REQUEST_BACKUP_FILENAME;
        }
        if (intentCallable(intent)) {
            startActivityForResult(intent, requestId);
        } else {
            backupToFile(filename);
        }
    }

    private class backupTask extends AsyncTask<OutputStreamData, Void, String> {
        CategoryList currentActivity = null;

        public void setActivity(CategoryList cat) {
            currentActivity = cat;
        }

        @Override
        protected String doInBackground(OutputStreamData... streams) {

            OutputStreamData streamData = streams[0];
            String response = backupDatabase(streamData.getFilename(), streamData.getStream());
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            showResultToast(result);
            if ((currentActivity != null)
                    && (currentActivity.backupProgress != null)) {
                currentActivity.backupProgress.dismiss();
            }
            taskBackup = null;
        }
    }

    private void showResultToast(String result) {
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

    private void backupToFile(String filename) {
        OutputStreamData streamData = null;
        try {
            streamData = new OutputStreamData(filename);
            backupToFile(streamData);
        } catch (FileNotFoundException e) {
            showResultToast(getString(R.string.backup_failed) + " " + e.getLocalizedMessage());
        }

    }

    private void backupToDocument(Uri documentUri) {
        try {
            OutputStreamData streamData = new OutputStreamData(documentUri, this);
            backupToFile(streamData);
        } catch (FileNotFoundException e) {
            showResultToast(getString(R.string.backup_failed) + " " + e.getLocalizedMessage());
        }

    }

    public void backupToFile(OutputStreamData streamData) {
        if (taskBackup != null) {
            // there's already a running backup
            return;
        }
        startBackupProgressDialog();
        taskBackup = new backupTask();
        taskBackup.setActivity(this);
        taskBackup.execute(streamData);
    }

    private void startBackupProgressDialog() {
        if (backupProgress == null) {
            backupProgress = new ProgressDialog(
                    CategoryList.this
            );
            backupProgress.setMessage(
                    getString(R.string.backup_progress) + " "
                            + Preferences.getBackupPath(CategoryList.this)
            );
            backupProgress.setIndeterminate(false);
            backupProgress.setCancelable(false);
        }
        backupProgress.show();
    }

    private void restoreDatabase() {
        Intent i = new Intent(this, Restore.class);
        startActivityForResult(i, REQUEST_RESTORE);
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (restartTimerIntent != null) {
            sendBroadcast(restartTimerIntent);
        }
        launchPassList(rows.get(position).id);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i) {
        super.onActivityResult(requestCode, resultCode, i);

        String path;
		
		/* Don't know what it is good for, just necessary
		 * to get the same behavior as the one before this patch*/
        if (resultCode == RESULT_OK
                && requestCode == REQUEST_ONCREATE
                || requestCode == REQUEST_EDIT_CATEGORY
                || requestCode == REQUEST_ADD_CATEGORY
                || requestCode == REQUEST_OPEN_CATEGORY
                || requestCode == REQUEST_RESTORE) {
            fillData();
        }

        switch (requestCode) {
            case REQUEST_EDIT_CATEGORY:
                if (resultCode == RESULT_OK) {
                    setSelection(lastPosition);
                }
                break;

            case REQUEST_OPEN_CATEGORY:
                // update in case passwords were added/deleted and caused the counts to update
                if (catAdapter != null) {
                    catAdapter.notifyDataSetChanged();
                }
                break;

            case REQUEST_BACKUP_FILENAME:
                if (resultCode == RESULT_OK) {
                    path = i.getData().getPath();
                    backupToFile(path);
                    Preferences.setBackupPathAndMethod(this, path);
                }
                break;
            case REQUEST_BACKUP_DOCUMENT:
                if (resultCode == RESULT_OK) {
                    backupToDocument(i.getData());
                    Preferences.setBackupDocumentAndMethod(this, i.getDataString());
                }
                break;

            case REQUEST_EXPORT_FILENAME:
                if (resultCode == RESULT_OK) {
                    path = i.getData().getPath();
                    exportDatabaseToFile(path);
                    Preferences.setExportPath(this, path);
                }
                break;

            case REQUEST_IMPORT_FILENAME:
                if (resultCode == RESULT_OK) {
                    path = i.getData().getPath();
                    importFile(path);
                    Preferences.setExportPath(this, path);
                }
                break;
        }
    }

    private void prePopulate() {
        addCategory(getString(R.string.category_business));
        addCategory(getString(R.string.category_personal));
    }

    private long addCategory(String name) {
        if (debug) {
            Log.d(TAG, "addCategory(" + name + ")");
        }
        if ((name == null) || (name == "")) {
            return -1;
        }
        CategoryEntry entry = new CategoryEntry();
        entry.plainName = name;

        return Passwords.putCategoryEntry(entry);
    }

    public void exportDatabase() {
        if (restartTimerIntent != null) {
            sendBroadcast(restartTimerIntent);
        }
        String filename = Preferences.getExportPath(this);

        Intent intent = new Intent("org.openintents.action.PICK_FILE");
        intent.setData(Uri.parse("file://" + filename));
        intent.putExtra("org.openintents.extra.TITLE", R.string.export_file_select);
        if (intentCallable(intent)) {
            startActivityForResult(intent, REQUEST_EXPORT_FILENAME);
        } else {
            exportDatabaseToFile(filename);
        }
    }

    private void exportDatabaseToFile(final String filename) {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(filename), ',');

            String[] header = {getString(R.string.category),
                    getString(R.string.description),
                    getString(R.string.website),
                    getString(R.string.username),
                    getString(R.string.password),
                    getString(R.string.notes),
                    getString(R.string.last_edited)
            };
            writer.writeNext(header);

            HashMap<Long, String> categories = Passwords.getCategoryIdToName();

            List<PassEntry> rows;
            rows = Passwords.getPassEntries(Long.valueOf(0), true, false);

            for (PassEntry row : rows) {
                String[] rowEntries = {categories.get(row.category),
                        row.plainDescription,
                        row.plainWebsite,
                        row.plainUsername,
                        row.plainPassword,
                        row.plainNote,
                        row.lastEdited
                };
                writer.writeNext(rowEntries);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(
                    CategoryList.this, R.string.export_file_error,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        String msg = getString(R.string.export_success, filename);
        showResultToast(msg);
    }

    private void deleteDatabaseNow() {
        Passwords.deleteAll();
    }

    public void deleteDatabase4Import(final String filename) {
//		Log.i(TAG,"deleteDatabase4Import");
        Dialog about = new AlertDialog.Builder(this)
                .setIcon(R.drawable.passicon)
                .setTitle(R.string.dialog_delete_database_title)
                .setPositiveButton(
                        R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                deleteDatabaseNow();
                                importDeletedDatabase = true;
                                performImportFile(filename);
                            }
                        }
                )
                .setNegativeButton(
                        R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }
                )
                .setMessage(R.string.dialog_delete_database_msg)
                .create();
        about.show();
    }

    public void importDatabase() {
        String defaultExportFilename = Preferences.getExportPath(this);
        final String filename;
        File oiImport = new File(defaultExportFilename);
        String pwsFilename = Environment.getExternalStorageDirectory()
                .getPath() + PASSWORDSAFE_IMPORT_FILENAME;
        File pwsImport = new File(pwsFilename);
        if (oiImport.exists() || !pwsImport.exists()) {
            filename = defaultExportFilename;
        } else {
            filename = pwsFilename;
        }
        Intent intent = new Intent("org.openintents.action.PICK_FILE");
        intent.setData(Uri.parse("file://" + filename));
        intent.putExtra("org.openintents.extra.TITLE", R.string.import_file_select);
        if (intentCallable(intent)) {
            startActivityForResult(intent, REQUEST_IMPORT_FILENAME);
        } else {
            importFile(filename);
        }
    }

    private class importTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... filenames) {
            String response = "";
            importDatabaseFromCSV(filenames[0]);
            return response;
        }

        private String importedFilename = "";
        private int importedEntries = 0;
        private String importMessage = "";

        CategoryList currentActivity = null;

        /**
         * Set the current activity to be used on PostExecute
         */
        public void setActivity(CategoryList categoryList) {
            currentActivity = categoryList;
        }

        /**
         * While running inside a thread, read from a CSV and import into the
         * database.
         */
        private void importDatabaseFromCSV(String filename) {
            if (restartTimerIntent != null) {
                sendBroadcast(restartTimerIntent);
            }
            try {
                importMessage = "";
                importedEntries = 0;

                final int recordLength = 6;
                CSVReader reader = new CSVReader(new FileReader(filename));
                String[] nextLine;
                nextLine = reader.readNext();
                if (nextLine == null) {
                    importMessage = getString(R.string.import_error_first_line);
                    return;
                }
                if (nextLine.length < recordLength) {
                    importMessage = getString(R.string.import_error_first_line);
                    return;
                }
                if ((nextLine[0]
                        .compareToIgnoreCase(getString(R.string.category)) != 0)
                        || (nextLine[1]
                        .compareToIgnoreCase(getString(R.string.description)) != 0)
                        || (nextLine[2]
                        .compareToIgnoreCase(getString(R.string.website)) != 0)
                        || (nextLine[3]
                        .compareToIgnoreCase(getString(R.string.username)) != 0)
                        || (nextLine[4]
                        .compareToIgnoreCase(getString(R.string.password)) != 0)
                        || (nextLine[5]
                        .compareToIgnoreCase(getString(R.string.notes)) != 0)) {
                    importMessage = getString(R.string.import_error_first_line);
                    return;
                }
                // Log.i(TAG,"first line is valid");

                HashMap<String, Long> categoryToId = Passwords
                        .getCategoryNameToId();
                //
                // take a pass through the CSV and collect any new Categories
                //
                HashMap<String, Long> categoriesFound = new HashMap<String, Long>();
                int categoryCount = 0;
                // int line=0;
                while ((nextLine = reader.readNext()) != null) {
                    // line++;
                    if (isCancelled()) {
                        if (debug) {
                            Log.d(TAG, "cancelled");
                        }
                        return;
                    }
                    // nextLine[] is an array of values from the line
                    if ((nextLine == null) || (nextLine[0] == "")) {
                        continue; // skip blank categories
                    }
                    if (categoryToId.containsKey(nextLine[0])) {
                        continue; // don't recreate existing categories
                    }
                    // if (debug)
                    // Log.d(TAG,"line["+line+"] found category ("+nextLine[0]+")");
                    Long passwordsInCategory = Long.valueOf(1);
                    if (categoriesFound.containsKey(nextLine[0])) {
                        // we've seen this category before, bump its count
                        passwordsInCategory += categoriesFound.get(nextLine[0]);
                    } else {
                        // newly discovered category
                        categoryCount++;
                    }
                    categoriesFound.put(nextLine[0], passwordsInCategory);
                    if (categoryCount > MAX_CATEGORIES) {
                        importMessage = getString(R.string.import_too_many_categories);
                        return;
                    }
                }
                if (debug) {
                    Log.d(TAG, "found " + categoryCount + " categories");
                }
                if (categoryCount != 0) {
                    Set<String> categorySet = categoriesFound.keySet();
                    Iterator<String> i = categorySet.iterator();
                    while (i.hasNext()) {
                        addCategory(i.next());
                    }
                }
                reader.close();

                // re-read the categories to get id's of new categories
                categoryToId = Passwords.getCategoryNameToId();
                //
                // read the whole file again to import the actual fields
                //
                reader = new CSVReader(new FileReader(filename));
                nextLine = reader.readNext();
                int newEntries = 0;
                int lineNumber = 0;
                String lineErrors = "";
                int lineErrorsCount = 0;
                final int maxLineErrors = 10;
                while ((nextLine = reader.readNext()) != null) {
                    lineNumber++;
                    // Log.d(TAG,"lineNumber="+lineNumber);

                    if (isCancelled()) {
                        if (debug) {
                            Log.d(TAG, "cancelled");
                        }
                        return;
                    }

                    // nextLine[] is an array of values from the line
                    if (nextLine.length < 2) {
                        if (lineErrorsCount < maxLineErrors) {
                            lineErrors += "line "
                                    + lineNumber
                                    + ": "
                                    + getString(R.string.import_not_enough_fields)
                                    + "\n";
                            lineErrorsCount++;
                        }
                        continue; // skip if not enough fields
                    }
                    if (nextLine.length < recordLength) {
                        // if the fields after category and description are
                        // missing,
                        // just fill them in
                        String[] replacement = new String[recordLength];
                        for (int i = 0; i < nextLine.length; i++) {
                            // copy over the fields we did get
                            replacement[i] = nextLine[i];
                        }
                        for (int i = nextLine.length; i < recordLength; i++) {
                            // flesh out the rest of the fields
                            replacement[i] = "";
                        }
                        nextLine = replacement;
                    }
                    if ((nextLine == null) || (nextLine[0] == "")) {
                        if (lineErrorsCount < maxLineErrors) {
                            lineErrors += "line " + lineNumber + ": "
                                    + getString(R.string.import_blank_category)
                                    + "\n";
                            lineErrorsCount++;
                        }
                        continue; // skip blank categories
                    }
                    String description = nextLine[1];
                    if ((description == null) || (description == "")) {
                        if (lineErrorsCount < maxLineErrors) {
                            lineErrors += "line "
                                    + lineNumber
                                    + ": "
                                    + getString(R.string.import_blank_description)
                                    + "\n";
                            lineErrorsCount++;
                        }
                        continue;
                    }

                    PassEntry entry = new PassEntry();
                    entry.category = categoryToId.get(nextLine[0]);
                    entry.plainDescription = description;
                    entry.plainWebsite = nextLine[2];
                    entry.plainUsername = nextLine[3];
                    entry.plainPassword = nextLine[4];
                    entry.plainNote = nextLine[5];
                    entry.id = 0;
                    Passwords.putPassEntry(entry);
                    newEntries++;
                }
                reader.close();
                if (lineErrors != "") {
                    if (debug) {
                        Log.d(TAG, lineErrors);
                    }
                }

                importedEntries = newEntries;
                if (newEntries == 0) {
                    importMessage = getString(R.string.import_no_entries);
                    return;
                } else {
                    importMessage = getString(R.string.added) + " "
                            + newEntries + " " + getString(R.string.entries);
                    importedFilename = filename;
                }
            } catch (IOException e) {
                e.printStackTrace();
                importMessage = getString(R.string.import_file_error);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (currentActivity != null) {
                currentActivity.importProgress.dismiss();
            }
            if (importMessage != "") {
                showResultToast(importMessage);
            }
            if (importedFilename != "") {
                String deleteMsg = getString(R.string.import_delete_csv) + " "
                        + importedFilename + "?";
                Dialog about = new AlertDialog.Builder(CategoryList.this)
                        .setIcon(R.drawable.passicon)
                        .setTitle(R.string.import_complete)
                        .setPositiveButton(
                                R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        File csvFile = new File(
                                                importedFilename
                                        );
                                        // csvFile.delete();
                                        SecureDelete.delete(csvFile);
                                        importedFilename = "";
                                    }
                                }
                        )
                        .setNegativeButton(
                                R.string.no,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                    }
                                }
                        ).setMessage(deleteMsg).create();
                about.show();
            }

            if ((importedEntries != 0) || (importDeletedDatabase)) {
                if (currentActivity != null) {
                    currentActivity.fillData();
                    if (currentActivity.importProgress != null) {
                        currentActivity.importProgress.dismiss();
                    }
                }
            }
            taskImport = null;
        }
    }

    public void performImportFile(String importFilename) {
        startImportProgressDialog();
        taskImport = new importTask();
        taskImport.setActivity(this);
        taskImport.execute(new String[]{importFilename});
    }

    private void startImportProgressDialog() {
        if (importProgress == null) {
            importProgress = new ProgressDialog(this);
            importProgress.setMessage(getString(R.string.import_progress));
            importProgress.setIndeterminate(false);
            importProgress.setCancelable(false);
        }
        importProgress.show();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        if (debug) {
            Log.d(TAG, "onUserInteraction()");
        }

        if (CategoryList.isSignedIn() == false) {
//			Intent frontdoor = new Intent(this, FrontDoor.class);
//			frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
//			startActivity(frontdoor);
        } else {
            if (restartTimerIntent != null) {
                sendBroadcast(restartTimerIntent);
            }
        }
    }

    public boolean onSearchRequested() {
        Intent search = new Intent(this, Search.class);
        startActivity(search);
        return true;
    }

    private boolean intentCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
        );
        return list.size() > 0;
    }

    private void importFile(final String filename) {
        File csvFile = new File(filename);
        if (!csvFile.exists()) {
            String msg = getString(R.string.import_file_missing) + " " +
                    filename;
            showResultToast(msg);
            return;
        }
        Dialog about = new AlertDialog.Builder(this)
                .setIcon(R.drawable.passicon)
                .setTitle(R.string.dialog_import_title)
                .setPositiveButton(
                        R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                deleteDatabase4Import(filename);
                            }
                        }
                )
                .setNegativeButton(
                        R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                importDeletedDatabase = false;
                                performImportFile(filename);
                            }
                        }
                )
                .setMessage(getString(R.string.dialog_import_msg, filename))
                .create();
        about.show();
    }
}
