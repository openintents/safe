package org.openintents.safe;

import android.app.Application;

public class SafeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();
    }
}
