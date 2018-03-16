/* 
 * Copyright 2012 OpenIntents.org
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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import org.openintents.intents.CryptoIntents;
import org.openintents.safe.PreferenceActivity;
import org.openintents.safe.password.Master;

public class AutoLockService extends Service {

    private static final boolean debug = false;
    private static String TAG = "AutoLockService";

    private CountDownTimer t;
    private BroadcastReceiver mIntentReceiver;
    private static long timeRemaining = 0;

    SharedPreferences mPreferences;
    private ServiceNotification serviceNotification;

    @Override
    public void onCreate() {
        if (debug) {
            Log.d(TAG, "onCreate");
        }
        mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    if (debug) {
                        Log.d(TAG, "caught ACTION_SCREEN_OFF");
                    }
                    boolean lockOnScreenLock = mPreferences.getBoolean(
                            PreferenceActivity.PREFERENCE_LOCK_ON_SCREEN_LOCK, true
                    );
                    if (lockOnScreenLock) {
                        lockOut();
                    }
                } else if (intent.getAction().equals(
                        CryptoIntents.ACTION_RESTART_TIMER
                )) {
                    restartTimer();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(CryptoIntents.ACTION_RESTART_TIMER);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mIntentReceiver, filter);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        serviceNotification = new ServiceNotification(this);
    }

    @Override
    public void onStart(Intent intent, int startid) {
        if (debug) {
            Log.d(TAG, "onStart");
        }
        startTimer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (debug) {
            Log.d(
                    TAG, "Received start id " + startId + ": " + intent + ": "
                            + this
            );
        }
        startTimer();
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (debug) {
            Log.d(TAG, "onDestroy");
        }
        unregisterReceiver(mIntentReceiver);
        if (Master.getMasterKey() != null) {
            lockOut();
        }
        serviceNotification.clearNotification();
        if (t != null) {
            t.cancel();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Clear the masterKey, notification, and broadcast
     * CryptoIntents.ACTION_CRYPTO_LOGGED_OUT
     */
    private void lockOut() {
        Master.setMasterKey(null);
        serviceNotification.clearNotification();
        if (t != null) {
            t.cancel();
        }

        Intent intent = new Intent(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        sendBroadcast(intent);
    }

    /**
     * Start a CountDownTimer() that will cause a lockOut()
     *
     * @see #lockOut()
     */
    private void startTimer() {
        if (Master.getMasterKey() == null) {
            serviceNotification.clearNotification();
            if (t != null) {
                t.cancel();
            }
            return;
        }
        serviceNotification.setNotification(AutoLockService.this);
        String timeout = mPreferences.getString(
                PreferenceActivity.PREFERENCE_LOCK_TIMEOUT,
                PreferenceActivity.PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE
        );
        int timeoutMinutes = 5; // default to 5
        try {
            timeoutMinutes = Integer.valueOf(timeout);
        } catch (NumberFormatException e) {
            Log.d(TAG, "why is lock_timeout busted?");
        }
        final long timeoutUntilStop = timeoutMinutes * 60000;

        if (debug) {
            Log.d(TAG, "startTimer with timeoutUntilStop=" + timeoutUntilStop);
        }

        t = new CountDownTimer(timeoutUntilStop, 1000) {

            public void onTick(long millisUntilFinished) {
                // doing nothing.
                if (debug) {
                    Log.d(TAG, "tick: " + millisUntilFinished + " this=" + this);
                }
                timeRemaining = millisUntilFinished;
                if (Master.getMasterKey() == null) {
                    if (debug) {
                        Log.d(TAG, "detected masterKey=null");
                    }
                    lockOut();
                } else {
                    serviceNotification.updateProgress(
                            (int) timeoutUntilStop,
                            (int) timeRemaining
                    );
                }
            }

            public void onFinish() {
                if (debug) {
                    Log.d(TAG, "onFinish()");
                }
                lockOut();
                timeRemaining = 0;
            }
        };
        t.start();
        timeRemaining = timeoutUntilStop;
        if (debug) {
            Log.d(TAG, "Timer started with: " + timeoutUntilStop);
        }
    }

    /**
     * Restart the CountDownTimer()
     */
    private void restartTimer() {
        // must be started with startTimer first.
        if (debug) {
            Log.d(TAG, "timer restarted");
        }
        if (t != null) {
            t.cancel();
            t.start();
        }
    }

    /**
     * @return time remaining in milliseconds before auto lock
     */
    public static long getTimeRemaining() {
        return timeRemaining;
    }
}
