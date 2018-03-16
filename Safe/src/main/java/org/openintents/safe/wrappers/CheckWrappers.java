package org.openintents.safe.wrappers;

import org.openintents.safe.wrappers.icecreamsandwich.WrapNotificationBuilder;

public class CheckWrappers {

    public static boolean mNotificationBuilderAvailable;

    static {
        try {
            WrapNotificationBuilder.checkAvailable();
            mNotificationBuilderAvailable = true;
        } catch (Throwable t) {
            mNotificationBuilderAvailable = false;
        }
    }
}
