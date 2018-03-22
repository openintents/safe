/* $Id: FrontDoor.java 1805 2009-01-20 04:05:01Z rmceoin $
 * 
 * Copyright 2007-2008 Steven Osborn
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
package org.openintents.safe;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.openintents.intents.CryptoIntents;

/**
 * Safe Activity
 * <p/>
 * This activity just makes sure we're entering the front door, not calling encrypt/decrypt
 * or other intents.
 *
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class Safe extends AppCompatActivity {
    public static String last_used_password = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Intent thisIntent = getIntent();
        final String action = thisIntent.getAction();
        if (action == null || action.equals(Intent.ACTION_MAIN) || action.equals(CryptoIntents.ACTION_AUTOLOCK)) {
            //TODO: When launched from debugger, action is null. Other such cases?
            Intent i = new Intent(
                    getApplicationContext(),
                    IntentHandlerActivity.class
            );
            i.setAction(action);
            startActivity(i);
        }  // otherwise, do not start intents, those must be protected by permissions
        finish();
    }
}
