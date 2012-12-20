/* $Id$
 * 
 * Copyright 2008 Isaac Potoczny-Jones
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

// TODO: Stripped everything down to the ch static.  Need to find a better place to hold it.

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ServiceDispatchImpl extends Service {
	private static boolean debug = false;
	private static String TAG = "ServiceDispatchIMPL";
//	public static CryptoHelper ch=null;  // TODO Peli: Could clean this up by moving it into a singleton? Or at least a separate static class?

	public class LocalBinder extends Binder {
		ServiceDispatchImpl getService() {
			return ServiceDispatchImpl.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// Select the interface to return.  If your service only implements
		// a single interface, you can just return it here without checking
		// the Intent.
		return (mBinder);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (debug) Log.d( TAG,"onCreate" );
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (debug) Log.d( TAG,"onDestroy" );
	}

	/**
	 * The ServiceDispatch is defined through IDL
	 */
	private final ServiceDispatch.Stub mBinder = new ServiceDispatch.Stub() {
	};

}
