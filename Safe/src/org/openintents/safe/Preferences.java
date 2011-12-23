package org.openintents.safe;

import java.util.List;

import org.openintents.distribution.DownloadOIAppDialog;
import org.openintents.intents.CryptoIntents;

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
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class Preferences extends PreferenceActivity
						 implements OnSharedPreferenceChangeListener {

	private static boolean debug = false;
	private static String TAG = "Preferences";

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
	public static final String PREFERENCE_BACKUP_PATH_DEFAULT_VALUE = 
			Environment.getExternalStorageDirectory().getAbsolutePath()+"/oisafe.xml";
	public static final String PREFERENCE_EXPORT_PATH = "export_path";
	public static final String PREFERENCE_EXPORT_PATH_DEFAULT_VALUE = 
			Environment.getExternalStorageDirectory().getAbsolutePath()+"/oisafe.csv";
	
	
	public static final int REQUEST_BACKUP_FILENAME = 0;
	public static final int REQUEST_EXPORT_FILENAME = 1;
	
	public static final int DIALOG_DOWNLOAD_OI_FILEMANAGER = 0;

	Intent frontdoor;
    private Intent restartTimerIntent=null;

    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)) {
            	 if (debug) Log.d(TAG,"caught ACTION_CRYPTO_LOGGED_OUT");
            	 startActivity(frontdoor);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		frontdoor = new Intent(this, Safe.class);
		frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
		restartTimerIntent = new Intent (CryptoIntents.ACTION_RESTART_TIMER);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
		
		Preference backupPathPref = findPreference(PREFERENCE_BACKUP_PATH);
		backupPathPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		    public boolean onPreferenceClick(Preference pref){
		    	Intent intent = new Intent("org.openintents.action.PICK_FILE");
		    	intent.setData(Uri.parse("file://"+getBackupPath(Preferences.this)));
		    	intent.putExtra("org.openintents.extra.TITLE", R.string.backup_select_file);
		    	if(intentCallable(intent))
		    		startActivityForResult(intent, REQUEST_BACKUP_FILENAME);
		    	else
		    		askForFileManager();
		        return false;
		    }
		});

		Preference exportPathPref = findPreference(PREFERENCE_EXPORT_PATH);
		exportPathPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		    public boolean onPreferenceClick(Preference pref){
		    	Intent intent = new Intent("org.openintents.action.PICK_FILE");
		    	intent.setData(Uri.parse("file://"+getExportPath(Preferences.this)));
		    	intent.putExtra("org.openintents.extra.TITLE", R.string.export_file_select);
		    	if(intentCallable(intent))
		    		startActivityForResult(intent, REQUEST_EXPORT_FILENAME);
		    	else
		    		askForFileManager();
		        return false;
		    }
		});
		

		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		changePreferenceSummaryToCurrentValue(backupPathPref, getBackupPath(this));
		changePreferenceSummaryToCurrentValue(exportPathPref, getExportPath(this));
    }

    @Override
	protected void onResume() {
		super.onResume();

		if (CategoryList.isSignedIn()==false) {
			startActivity(frontdoor);
			return;
		}
        IntentFilter filter = new IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        registerReceiver(mIntentReceiver, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();

		SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean lockOnScreenLock = mPreferences.getBoolean(Preferences.PREFERENCE_LOCK_ON_SCREEN_LOCK, true);
		IntentHandler.setLockOnScreenLock(lockOnScreenLock);
		
		try {
			unregisterReceiver(mIntentReceiver);
		} catch (IllegalArgumentException e) {
			//if (debug) Log.d(TAG,"IllegalArgumentException");
		}
	}

	@Override
	public void onUserInteraction() {
		super.onUserInteraction();

		if (debug) Log.d(TAG,"onUserInteraction()");

		if (CategoryList.isSignedIn()==false) {
//			startActivity(frontdoor);
		}else{
			if (restartTimerIntent!=null) sendBroadcast (restartTimerIntent);
		}
	}
	
	static String getBackupPath(Context context){
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(PREFERENCE_BACKUP_PATH, PREFERENCE_BACKUP_PATH_DEFAULT_VALUE);
	}
	
	static void setBackupPath(Context context, String path){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREFERENCE_BACKUP_PATH, path);
		editor.commit();
	}
	
	static String getExportPath(Context context){
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(PREFERENCE_EXPORT_PATH, PREFERENCE_EXPORT_PATH_DEFAULT_VALUE);
	}
	
	static void setExportPath(Context context, String path){
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREFERENCE_EXPORT_PATH, path);
		editor.commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent i) {
		switch(requestCode){
		case REQUEST_BACKUP_FILENAME:
			if(resultCode == RESULT_OK)
				setBackupPath(this, i.getData().getPath());
			break;
			

		case REQUEST_EXPORT_FILENAME:
			if(resultCode == RESULT_OK)
				setExportPath(this, i.getData().getPath());
			break;
		}
	}

	private boolean intentCallable(Intent intent){
		List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,   
				PackageManager.MATCH_DEFAULT_ONLY); 
		return list.size() > 0;
	}
	
	private void askForFileManager(){
		Toast.makeText(this, R.string.download_oi_filemanager, Toast.LENGTH_LONG).show();
		showDialog(DIALOG_DOWNLOAD_OI_FILEMANAGER);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog d = super.onCreateDialog(id);
		switch(id) {
		case DIALOG_DOWNLOAD_OI_FILEMANAGER:
			d = new DownloadOIAppDialog(this,
					DownloadOIAppDialog.OI_FILEMANAGER);
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
	
	private void changePreferenceSummaryToCurrentValue(Preference pref, String value){
		pref.setSummary(value);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals(PREFERENCE_BACKUP_PATH))
			changePreferenceSummaryToCurrentValue(findPreference(PREFERENCE_BACKUP_PATH),
					getBackupPath(this));
		else if(key.equals(PREFERENCE_EXPORT_PATH))
			changePreferenceSummaryToCurrentValue(findPreference(PREFERENCE_EXPORT_PATH),
					getExportPath(this));
	}
}
