package org.openintents.safe.service;

import org.openintents.safe.LogOffActivity;
import org.openintents.safe.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;


public class ServiceNotification {

	private static final int NOTIFICATION_ID = 1;
	
	/**
	 * @param context
	 * @param force overwrites preferences, if true updates notification regardless the preferences.
	 */
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
	
	public static void setNotification(Context context) {

		// look up the notification manager service
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
	
		String text = "Master Key Logged in";

		Notification notification = new Notification(
				R.drawable.passicon, null, System
						.currentTimeMillis());
		notification.flags = Notification.FLAG_ONGOING_EVENT;

		Intent intent = new Intent(context, LogOffActivity.class);
		PendingIntent pi = PendingIntent.getActivity(context, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);				
		// Set the info for the views that show in the notification
		// panel.
		notification.setLatestEventInfo(context, context
				.getString(R.string.app_name), text, pi);
		
		nm.notify(NOTIFICATION_ID, notification);
	}
	
	public static void clearNotification(Context context) {

		// look up the notification manager service
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION_ID);
	}
}
