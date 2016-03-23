package org.openintents.safe;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

import org.openintents.distribution.DownloadOIAppDialog;
import org.openintents.intents.CryptoIntents;

import org.openintents.safe.wrappers.CheckWrappers;
import org.openintents.safe.wrappers.honeycomb.WrapActionBar;

public class PreferenceActivity extends android.preference.PreferenceActivity
        implements OnSharedPreferenceChangeListener {

    public static final String IO_METHOD_DOCUMENT_PROVIDER = "document_provider";
    public static final String IO_METHOD_FILE = "file";

    private static String TAG = "PreferenceActivity";

    public static final String PREFERENCE_ALLOW_EXTERNAL_ACCESS = "external_access";
    public static final String PREFERENCE_LOCK_TIMEOUT = "lock_timeout";
    public static final String PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE = "5";
    public static final String PREFERENCE_LOCK_ON_SCREEN_LOCK = "lock_on_screen_lock";
    public static final String PREFERENCE_FIRST_TIME_WARNING = "first_time_warning";
    public static final String PREFERENCE_KEYPAD = "keypad";
    public static final String PREFERENCE_KEYPAD_MUTE = "keypad_mute";
    public static final String PREFERENCE_LAST_BACKUP_JULIAN = "last_backup_julian";
    public static final String PREFERENCE_LAST_AUTOBACKUP_CHECK = "last_autobackup_check";
    public static final String PREFERENCE_AUTOBACKUP = "autobackup";
    public static final String PREFERENCE_AUTOBACKUP_DAYS = "autobackup_days";
    public static final String PREFERENCE_AUTOBACKUP_DAYS_DEFAULT_VALUE = "7";

    public static final String PREFERENCE_BACKUP_PATH = "backup_path";
    public static final String PREFERENCE_BACKUP_DOCUMENT = "backup_document";
    public static final String PREFERENCE_BACKUP_METHOD = "backup_method";
    public static final String PREFERENCE_BACKUP_PATH_DEFAULT_VALUE =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/oisafe.xml";
    public static final String PREFERENCE_EXPORT_PATH = "export_path";
    public static final String PREFERENCE_EXPORT_DOCUMENT = "export_document";
    public static final String PREFERENCE_EXPORT_METHOD = "export_method";
    public static final String PREFERENCE_EXPORT_PATH_DEFAULT_VALUE =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/oisafe.csv";

    private static final int REQUEST_BACKUP_FILENAME = 0;
    private static final int REQUEST_BACKUP_DOCUMENT = 1;
    private static final int REQUEST_EXPORT_FILENAME = 2;
    private static final int REQUEST_EXPORT_DOCUMENT = 3;


    public static final int DIALOG_DOWNLOAD_OI_FILEMANAGER = 0;

    Intent frontdoor;
    private Intent restartTimerIntent = null;

    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "caught ACTION_CRYPTO_LOGGED_OUT");
                }
                startActivity(frontdoor);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        frontdoor = new Intent(this, Safe.class);
        frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
        restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        Preference backupPathPref = findPreference(PREFERENCE_BACKUP_PATH);
        backupPathPref.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference pref) {
                        Intent intent;
                        int requestId;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            intent = Intents.createCreateDocumentIntent();
                            requestId = REQUEST_BACKUP_FILENAME;
                        } else {
                            intent = Intents.createPickFileIntent(PreferenceActivity.getBackupPath(PreferenceActivity.this), R.string.backup_select_file);
                            requestId = REQUEST_BACKUP_FILENAME;
                        }
                        if (intentCallable(intent)) {
                            startActivityForResult(intent, requestId);
                        } else {
                            askForFileManager();
                        }
                        return false;
                    }
                }
        );

        Preference exportPathPref = findPreference(PREFERENCE_EXPORT_PATH);
        exportPathPref.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference pref) {
                        Intent intent;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            intent = Intents.createCreateDocumentIntent();
                        } else {
                            intent = Intents.createPickFileIntent(getExportPath(PreferenceActivity.this), R.string.export_file_select);
                        }
                        if (intentCallable(intent)) {
                            startActivityForResult(intent, REQUEST_EXPORT_FILENAME);
                        } else {
                            askForFileManager();
                        }
                        return false;
                    }
                }
        );

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        changePreferenceSummaryToCurrentValue(backupPathPref, getBackupPath(this));
        changePreferenceSummaryToCurrentValue(exportPathPref, getExportPath(this));

        if (CheckWrappers.mActionBarAvailable) {
            WrapActionBar bar = new WrapActionBar(this);
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (CategoryList.isSignedIn() == false) {
            startActivity(frontdoor);
            return;
        }
        IntentFilter filter = new IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        registerReceiver(mIntentReceiver, filter);
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
    public void onUserInteraction() {
        super.onUserInteraction();

        if (BuildConfig.DEBUG) {
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

    static String getBackupPath(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERENCE_BACKUP_PATH, PREFERENCE_BACKUP_PATH_DEFAULT_VALUE);
    }

    static void setBackupPathAndMethod(Context context, String path) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFERENCE_BACKUP_PATH, path);
        editor.putString(PREFERENCE_BACKUP_METHOD, IO_METHOD_FILE);
        editor.commit();
    }

    public static String getBackupDocument(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERENCE_BACKUP_DOCUMENT, null);
    }

    static void setBackupDocumentAndMethod(Context context, String uriString) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFERENCE_BACKUP_DOCUMENT, uriString);
        editor.putString(PREFERENCE_BACKUP_METHOD, IO_METHOD_DOCUMENT_PROVIDER);
        editor.commit();
    }

    static String getExportPath(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERENCE_EXPORT_PATH, PREFERENCE_EXPORT_PATH_DEFAULT_VALUE);
    }

    static void setExportPathAndMethod(Context context, String path) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFERENCE_EXPORT_PATH, path);
        editor.putString(PREFERENCE_EXPORT_METHOD, IO_METHOD_FILE);
        editor.commit();
    }

    static String getExportDocument(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERENCE_EXPORT_DOCUMENT, null);
    }

    static void setExportDocumentAndMethod(Context context, String uriString) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFERENCE_EXPORT_DOCUMENT, uriString);
        editor.putString(PREFERENCE_EXPORT_METHOD, IO_METHOD_DOCUMENT_PROVIDER);
        editor.commit();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i) {
        switch (requestCode) {
            case REQUEST_BACKUP_FILENAME:
                if (resultCode == RESULT_OK) {
                    setBackupPathAndMethod(this, i.getData().getPath());
                }
                break;
            case REQUEST_BACKUP_DOCUMENT:
                if (resultCode == RESULT_OK) {
                    setBackupDocumentAndMethod(this, i.getData().getPath());
                }
                break;

            case REQUEST_EXPORT_FILENAME:
                if (resultCode == RESULT_OK) {
                    setExportPathAndMethod(this, i.getData().getPath());
                }
                break;
            case REQUEST_EXPORT_DOCUMENT:
                if (resultCode == RESULT_OK) {
                    setExportDocumentAndMethod(this, i.getData().getPath());
                }
                break;
        }
    }

    private boolean intentCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
        );
        return list.size() > 0;
    }

    private void askForFileManager() {
        Toast.makeText(this, R.string.download_oi_filemanager, Toast.LENGTH_LONG).show();
        showDialog(DIALOG_DOWNLOAD_OI_FILEMANAGER);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog d = super.onCreateDialog(id);
        switch (id) {
            case DIALOG_DOWNLOAD_OI_FILEMANAGER:
                d = new DownloadOIAppDialog(
                        this,
                        DownloadOIAppDialog.OI_FILEMANAGER
                );
                break;
        }
        return d;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
            case DIALOG_DOWNLOAD_OI_FILEMANAGER:
                DownloadOIAppDialog.onPrepareDialog(this, dialog);
                break;
        }
    }

    private void changePreferenceSummaryToCurrentValue(Preference pref, String value) {
        pref.setSummary(value);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(PREFERENCE_BACKUP_PATH)) {
            changePreferenceSummaryToCurrentValue(
                    findPreference(PREFERENCE_BACKUP_PATH),
                    getBackupPath(this)
            );
        } else if (key.equals(PREFERENCE_EXPORT_PATH)) {
            changePreferenceSummaryToCurrentValue(
                    findPreference(PREFERENCE_EXPORT_PATH),
                    getExportPath(this)
            );
        }
    }

}
