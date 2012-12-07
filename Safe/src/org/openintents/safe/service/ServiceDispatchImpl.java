/* $Id$
 * 
 * Copyright 2008 Isaac Potoczny-Jones
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

package org.openintents.safe.service;

// TODO: Currently the timer MIGHT not actually de-activate the service
// if there are still clients attached.  Should be fixed.

import org.openintents.intents.CryptoIntents;
import org.openintents.safe.CategoryList;
import org.openintents.safe.CryptoHelper;
import org.openintents.safe.CryptoHelperException;
import org.openintents.safe.password.Master;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.os.CountDownTimer;

public class ServiceDispatchImpl extends Service {
	private static boolean debug = false;
	private static String TAG = "ServiceDispatchIMPL";
	public static CryptoHelper ch;  // TODO Peli: Could clean this up by moving it into a singleton? Or at least a separate static class?
	private CountDownTimer t;
	private int timeoutMinutes = 5;
	private long timeoutUntilStop = timeoutMinutes * 60000;
	private BroadcastReceiver mIntentReceiver;
	private boolean lockOnScreenLock=true;
	public static long timeRemaining=0;

	@Override
	public IBinder onBind(Intent intent) {
		// Select the interface to return.  If your service only implements
		// a single interface, you can just return it here without checking
		// the Intent.
		return (mBinder);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mIntentReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
					if (debug) Log.d(TAG,"caught ACTION_SCREEN_OFF");
					if (lockOnScreenLock) {
						stopSelf();
					}
				} else if (intent.getAction().equals(CryptoIntents.ACTION_RESTART_TIMER)) {
					restartTimer();
				}
			}
		};

		IntentFilter filter = new IntentFilter();
		filter.addAction (CryptoIntents.ACTION_RESTART_TIMER);
		filter.addAction (Intent.ACTION_SCREEN_OFF);
		registerReceiver(mIntentReceiver, filter);

		if (debug) Log.d( TAG,"onCreate" );
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (debug) Log.d( TAG,"onDestroy" );
		unregisterReceiver(mIntentReceiver);
		if (Master.getMasterKey()!=null) {
			lockOut();
		}
		ServiceNotification.clearNotification(ServiceDispatchImpl.this);
	}

	private void lockOut() {
		Master.setMasterKey(null);
		ch = null;
		ServiceNotification.clearNotification(ServiceDispatchImpl.this);

		CategoryList.setSignedOut();
		Intent intent = new Intent(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
		sendBroadcast(intent);
	}

	private void startTimer () {
		if (debug) Log.d(TAG,"startTimer with timeoutUntilStop="+timeoutUntilStop);
		t = new CountDownTimer(timeoutUntilStop, 1000) {
			public void onTick(long millisUntilFinished) {
				//doing nothing.
				if (debug) Log.d(TAG, "tick: " + millisUntilFinished );
				timeRemaining=millisUntilFinished;
			}

			public void onFinish() {
				if (debug) Log.d(TAG,"onFinish()");
				lockOut();
				stopSelf(); // countdown is over, stop the service.
				timeRemaining=0;
			}
		};
		t.start();
		timeRemaining=timeoutUntilStop;
		if (debug) Log.d(TAG, "Timer started with: " + timeoutUntilStop );
	}

	private void restartTimer () {
		// must be started with startTimer first.
		if (debug) Log.d(TAG,"timer restarted");
		if (t != null) {
			t.cancel();
			t.start();
		}
	}

	/**
	 * The ServiceDispatch is defined through IDL
	 */
	private final ServiceDispatch.Stub mBinder = new ServiceDispatch.Stub() {
		private String TAG = "SERVICEDISPATCH";

		public String encrypt (String clearText)  {
			restartTimer();
			String cryptoText = null;
			try {
				cryptoText = ch.encrypt (clearText); 
			} catch (CryptoHelperException e) {
				Log.e(TAG, e.toString());
			}  
			return (cryptoText);
		}

		public String decrypt (String cryptoText)  {
			restartTimer();
			String clearText = null;
			try {
				clearText = ch.decrypt (cryptoText); 
			} catch (CryptoHelperException e) {
				Log.e(TAG, e.toString());
			}  
			return (clearText);
		}

		public void setPassword (String masterKeyIn){
			startTimer(); //should be initial timer start
			ch = new CryptoHelper();
			try {
				ch.init(CryptoHelper.EncryptionMedium, Master.getSalt());
				ch.setPassword(Master.getMasterKey());
			} catch (CryptoHelperException e) {
				e.printStackTrace();
				return;
			}
			
			ServiceNotification.setNotification(ServiceDispatchImpl.this);
		}

		public String getPassword() {
			restartTimer();
			return Master.getMasterKey();
		}

		public void setTimeoutMinutes (int timeoutMinutesIn){
			timeoutMinutes = timeoutMinutesIn;
			timeoutUntilStop = timeoutMinutes * 60000;
			Log.d(TAG,"set timeout to "+timeoutMinutes);
		}

		public void setLockOnScreenLock (boolean lock){
			lockOnScreenLock = lock;
		}


	};

}
