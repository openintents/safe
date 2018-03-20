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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.openintents.distribution.DownloadOIAppDialog;
import org.openintents.intents.CryptoIntents;
import org.openintents.safe.fingerprint.AskFingerprint;

import java.util.List;

import static org.openintents.safe.fingerprint.AskFingerprintKt.EXTRA_SETUP_FINGERPRINT;

public class PreferenceActivity extends AppCompatActivity {

    public static final String IO_METHOD_DOCUMENT_PROVIDER = "document_provider";
    public static final String IO_METHOD_FILE = "file";
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
    public static final String OISAFE_XML = "oisafe.xml";
    public static final String PREFERENCE_BACKUP_PATH_DEFAULT_VALUE =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + OISAFE_XML;
    public static final String PREFERENCE_EXPORT_PATH = "export_path";
    public static final String PREFERENCE_EXPORT_DOCUMENT = "export_document";
    public static final String PREFERENCE_EXPORT_METHOD = "export_method";
    public static final String PREFERENCE_USE_FINGERPRINT = "use_fingerprint";
    public static final String PREFKEY_FINGERPRINT_ENCRYPTED_MASTER = "fingerprint_encrypted_master";
    public static final String OISAFE_CSV = "oisafe.csv";
    public static final String PREFERENCE_EXPORT_PATH_DEFAULT_VALUE =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + OISAFE_CSV;
    public static final int DIALOG_DOWNLOAD_OI_FILEMANAGER = 0;
    private static final String PREFERENCE_FIRST_TIME_FINGERPRINT = "first_time_fingerprint";
    private static final int REQUEST_BACKUP_FILENAME = 0;
    private static final int REQUEST_BACKUP_DOCUMENT = 1;
    private static final int REQUEST_EXPORT_FILENAME = 2;
    private static final int REQUEST_EXPORT_DOCUMENT = 3;
    private static final int REQUEST_FINGERPRINT = 4;

    private static String TAG = "PreferenceActivity";
    Intent frontdoor;
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
    private Intent restartTimerIntent = null;

    static String getBackupPath(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFERENCE_BACKUP_PATH, PREFERENCE_BACKUP_PATH_DEFAULT_VALUE);
    }

    static void setBackupPathAndMethod(Context context, String path) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFERENCE_BACKUP_PATH, path);
        editor.putString(PREFERENCE_BACKUP_METHOD, IO_METHOD_FILE);
        editor.apply();
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
        editor.apply();
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
        editor.apply();
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
        editor.apply();
    }


    static boolean canUseFingerprint(Context context) {
        String encryptedKey = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREFKEY_FINGERPRINT_ENCRYPTED_MASTER, null);
        return encryptedKey != null && !encryptedKey.isEmpty();
    }

    public static void setUseFingerprint(@NotNull Context context, boolean useFingerprint) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(PREFERENCE_USE_FINGERPRINT, useFingerprint).apply();
    }

    public static boolean isFirstTimeFingerprint(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFERENCE_FIRST_TIME_FINGERPRINT, true);
    }

    public static void setFirstTimeFingerprint(Context context, boolean firstTime) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(PREFERENCE_FIRST_TIME_FINGERPRINT, firstTime).apply();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.preferences);
        frontdoor = new Intent(this, Safe.class);
        frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
        restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

        if (!CategoryList.isSignedIn()) {
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

    public static class PreferenceFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);

            Preference backupPathPref = findPreference(PREFERENCE_BACKUP_PATH);
            backupPathPref.setOnPreferenceClickListener(
                    pref -> {
                        Intent intent;
                        int requestId;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            intent = Intents.createCreateDocumentIntent(CategoryList.MIME_TYPE_BACKUP, OISAFE_XML);
                            requestId = REQUEST_BACKUP_FILENAME;
                        } else {
                            intent = Intents.createPickFileIntent(PreferenceActivity.getBackupPath(getActivity()), R.string.backup_select_file);
                            requestId = REQUEST_BACKUP_FILENAME;
                        }
                        if (intentCallable(intent)) {
                            startActivityForResult(intent, requestId);
                        } else {
                            askForFileManager();
                        }
                        return false;
                    }
            );

            Preference exportPathPref = findPreference(PREFERENCE_EXPORT_PATH);
            exportPathPref.setOnPreferenceClickListener(
                    pref -> {
                        Intent intent;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            intent = Intents.createCreateDocumentIntent(CategoryList.MIME_TYPE_EXPORT, OISAFE_CSV);
                        } else {
                            intent = Intents.createPickFileIntent(getExportPath(getActivity()), R.string.export_file_select);
                        }
                        if (intentCallable(intent)) {
                            startActivityForResult(intent, REQUEST_EXPORT_FILENAME);
                        } else {
                            askForFileManager();
                        }
                        return false;
                    }
            );

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            changePreferenceSummaryToCurrentValue(backupPathPref, getBackupPath(getActivity()));
            changePreferenceSummaryToCurrentValue(exportPathPref, getExportPath(getActivity()));

            findPreference(PREFERENCE_USE_FINGERPRINT)
                    .setVisible(FingerprintManagerCompat.from(getActivity()).isHardwareDetected());
        }


        private boolean intentCallable(Intent intent) {
            List<ResolveInfo> list = getActivity().getPackageManager().queryIntentActivities(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
            );
            return list.size() > 0;
        }

        private void askForFileManager() {
            Toast.makeText(getActivity(), R.string.download_oi_filemanager, Toast.LENGTH_LONG).show();
            getActivity().showDialog(DIALOG_DOWNLOAD_OI_FILEMANAGER);
        }


        private void changePreferenceSummaryToCurrentValue(Preference pref, String value) {
            pref.setSummary(value);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
            if (key.equals(PREFERENCE_BACKUP_PATH)) {
                changePreferenceSummaryToCurrentValue(
                        findPreference(PREFERENCE_BACKUP_PATH),
                        getBackupPath(getActivity())
                );
            } else if (key.equals(PREFERENCE_EXPORT_PATH)) {
                changePreferenceSummaryToCurrentValue(
                        findPreference(PREFERENCE_EXPORT_PATH),
                        getExportPath(getActivity())
                );
            } else if (key.equals(PREFERENCE_USE_FINGERPRINT)) {
                CheckBoxPreference p = (CheckBoxPreference) findPreference(PREFERENCE_USE_FINGERPRINT);
                if (p.isChecked()) {
                    startActivityForResult(new Intent(getContext(), AskFingerprint.class).putExtra(EXTRA_SETUP_FINGERPRINT, true), REQUEST_FINGERPRINT);
                } else {
                    sharedPreferences.edit().remove(PREFKEY_FINGERPRINT_ENCRYPTED_MASTER).apply();
                }
            }
        }

    }
}
