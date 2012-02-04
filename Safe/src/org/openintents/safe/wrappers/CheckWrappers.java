package org.openintents.safe.wrappers;

import org.openintents.safe.wrappers.honeycomb.WrapActionBar;

public class CheckWrappers {

	public static boolean mActionBarAvailable;
	
	static {
		try {
			WrapActionBar.checkAvailable();
			mActionBarAvailable = true;
		} catch(Throwable t){
			mActionBarAvailable = false;
		}
	}
}
