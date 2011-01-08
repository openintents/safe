package org.openintents.safe;

import org.openintents.intents.CryptoIntents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class Preferences extends PreferenceActivity {

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
}
