/* 
 * Copyright (C) 2011 OpenIntents.org
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

import org.openintents.safe.LogOffActivity;
import org.openintents.safe.R;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;


public class ServiceNotification {

	private static final int NOTIFICATION_ID = 1;
	
	static NotificationManager mNotifyManager;
	static android.app.Notification.Builder notification;
	static Builder notificationCompat;
	/*
	public static void updateNotification(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		//if (prefs.getBoolean(PreferenceActivity.PREFS_SHOW_NOTIFICATION, false)) {
			if () {
				setNotification(context);
			} else {
				clearNotification(context);
			}
		
		//} else {
			
		//}

	}
	*/
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static void setNotification(Context context) {

		// look up the notification manager service
		mNotifyManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
	
		Intent intent = new Intent(context, LogOffActivity.class);
		PendingIntent pi = PendingIntent.getActivity(context, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);				
		// Set the info for the views that show in the notification
		// panel.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			notificationCompat = new NotificationCompat.Builder(context)
					.setContentTitle(context.getString(R.string.app_name))
					.setContentText(context.getString(R.string.notification_msg))
					.setSmallIcon(R.drawable.passicon).setOngoing(true)
					.setContentIntent(pi);

			mNotifyManager.notify(NOTIFICATION_ID, notificationCompat.build());
		} else {
			// The NotificationCompat library doesn't really have a setProgress(), so only do
			// that for Ice Cream Sandwich and above
			notification = new Notification.Builder(context)
					.setContentTitle(context.getString(R.string.app_name))
					.setContentText(context.getString(R.string.notification_msg))
					.setSmallIcon(R.drawable.passicon).setOngoing(true)
					.setContentIntent(pi)
					.setProgress(100, 0, false);

			mNotifyManager.notify(NOTIFICATION_ID, notification.getNotification());
		}
	}
	
	public static void clearNotification(Context context) {

		// look up the notification manager service
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION_ID);
	}
	
	/**
	 * Update the existing notification progress bar.  This should start with
	 * progress == max and progress decreasing over time to depict time running out. 
	 * 
	 * @param context
	 * @param max
	 * @param progress
	 */
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static void updateProgress(Context context, int max, int progress) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			notification.setProgress(max, progress, false);
			mNotifyManager.notify(NOTIFICATION_ID, notification.getNotification());
		}
	
	}
}
