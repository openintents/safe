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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationManagerCompat;

import org.openintents.safe.LogOffActivity;
import org.openintents.safe.R;

public class ServiceNotification {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "safe";
    private NotificationManagerCompat mNotifyManager;
    private Builder wrapBuilder;

    public ServiceNotification(Context context) {
        mNotifyManager = NotificationManagerCompat.from(context);
        createChannel(context);
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = context.getString(R.string.notif_channel_title);
            String description = context.getString(R.string.notif_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.enableLights(false);
            mChannel.enableVibration(false);
            mChannel.setDescription(description);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(mChannel);
        }
    }

    @SuppressLint("NewApi")
    public void setNotification(Context context) {

        Intent intent = new Intent(context, LogOffActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        wrapBuilder = new Builder(context, CHANNEL_ID);
        wrapBuilder.setContentTitle(context.getString(R.string.app_name));
        wrapBuilder.setContentText(
                context
                        .getString(R.string.notification_msg)
        );
        wrapBuilder.setSmallIcon(R.drawable.passicon);
        wrapBuilder.setOngoing(true);
        wrapBuilder.setContentIntent(pi);
        wrapBuilder.setCategory(Notification.CATEGORY_SERVICE);
        wrapBuilder.setProgress(100, 0, false);
        mNotifyManager.notify(NOTIFICATION_ID, wrapBuilder.build());

    }

    public void clearNotification() {
        mNotifyManager.cancel(NOTIFICATION_ID);
    }

    /**
     * Update the existing notification progress bar. This should start with
     * progress == max and progress decreasing over time to depict time running
     * out.
     *
     * @param max
     * @param progress
     */
    public void updateProgress(int max, int progress) {
        wrapBuilder.setProgress(max, progress, false);
        mNotifyManager.notify(NOTIFICATION_ID, wrapBuilder.build());
    }
}
