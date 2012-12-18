package org.openintents.safe.wrappers;

import org.openintents.safe.wrappers.honeycomb.WrapActionBar;
import org.openintents.safe.wrappers.icecreamsandwich.WrapNotificationBuilder;

public class CheckWrappers {

	public static boolean mActionBarAvailable;
	public static boolean mNotificationBuilderAvailable;
	
	static {
		try {
			WrapActionBar.checkAvailable();
			mActionBarAvailable = true;
		} catch(Throwable t){
			mActionBarAvailable = false;
		}
		try {
			WrapNotificationBuilder.checkAvailable();
			mNotificationBuilderAvailable = true;
		} catch(Throwable t){
			mNotificationBuilderAvailable = false;
		}
	}
}
