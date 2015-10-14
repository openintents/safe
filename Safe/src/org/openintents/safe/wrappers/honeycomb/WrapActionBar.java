package org.openintents.safe.wrappers.honeycomb;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.view.MenuItem;

public class WrapActionBar {
    private ActionBar mInstance;

    static {
        try {
            Class.forName("android.app.ActionBar");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /* calling here forces class initialization */
    public static void checkAvailable() {
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public WrapActionBar(Activity a) {
        mInstance = a.getActionBar();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setDisplayHomeAsUpEnabled(boolean b) {
        if (mInstance != null) {
            mInstance.setDisplayHomeAsUpEnabled(b);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void setHomeButtonEnabled(boolean b) {
        if (mInstance != null) {
            mInstance.setHomeButtonEnabled(b);
        }
    }

    // show an icon in the actionbar if there is room for it.
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void showIfRoom(MenuItem item) {
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void invalidateOptionsMenu(Activity a) {
        a.invalidateOptionsMenu();
    }
}
