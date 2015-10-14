/* 
 * Copyright (C) 2012 OpenIntents.org
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
package org.openintents.safe.wrappers.icecreamsandwich;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

/**
 * Notification was introduced with Honeycomb, however this wrapper is targeted
 * at Ice Cream Sandwich to handle setProgress(). The setProgress is included in
 * the NotificationCompat.Builder, however it doesn't do anything. So if you use
 * it, nothing shows even on ICS. Thus use a wrapper for the real API here and
 * over in ServiceNotification, if less than ICS, use
 * NotificationCompat.Builder.
 */
@SuppressLint("NewApi")
public class WrapNotificationBuilder {

    private Builder mInstance;

    static {
        try {
            Class.forName("android.app.Notification$Builder");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /* calling here forces class initialization */
    public static void checkAvailable() {
    }

    public WrapNotificationBuilder(Context context) {
        mInstance = new Notification.Builder(context);
    }

    public Builder GetBuilder() {
        return mInstance;
    }

    public void setContentTitle(CharSequence title) {
        mInstance.setContentTitle(title);
    }

    public void setContentText(CharSequence text) {
        mInstance.setContentText(text);
    }

    public void setSmallIcon(int icon) {
        mInstance.setSmallIcon(icon);
    }

    public void setOngoing(boolean ongoing) {
        mInstance.setOngoing(ongoing);
    }

    public void setContentIntent(PendingIntent intent) {
        mInstance.setContentIntent(intent);
    }

    public void setProgress(int max, int progress, boolean indeterminate) {
        mInstance.setProgress(max, progress, indeterminate);
    }

    @SuppressWarnings("deprecation")
    public void notifyManager(NotificationManager nm, int id) {
        nm.notify(id, mInstance.getNotification());
    }
}
