/* 
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
package org.openintents.intents;

/**
 * @version 1.1.1
 * 
 * @author Isaac Potoczny-Jones
 * @author Peli
 *
 */
public class CryptoIntents {

	/**
	 * Activity Action: Encrypt all strings given in the extra(s) EXTRA_TEXT or
	 * EXTRA_TEXT_ARRAY.
	 * 
	 * If a file URI is given, the file is encrypted.
	 * The new file URI is returned.
	 * 
	 * If the extra EXTRA_SESSION_KEY is provided, the session key is returned,
	 * and the content URI is returned in the data field.
	 * 
	 * Returns all encrypted string in the same extra(s).
	 * 
	 * <p>Constant Value: "org.openintents.action.ENCRYPT"</p>
	 */
	public static final String ACTION_ENCRYPT = "org.openintents.action.ENCRYPT";

	/**
	 * Activity Action: Decrypt all strings given in the extra TEXT or
	 * EXTRA_TEXT_ARRAY.
	 * 
	 * If a file URI is given, the file is decrypted.
	 * The new file URI is returned.
	 * 
	 * If the extra EXTRA_SESSION_KEY is provided, the session key is returned,
	 * and the content URI is returned in the data field.
	 * 
	 * Returns all decrypted string in the same extra(s).
	 * 
	 * <p>Constant Value: "org.openintents.action.DECRYPT"</p>
	 */
	public static final String ACTION_DECRYPT = "org.openintents.action.DECRYPT";
	
	/**
	 * Activity Action: Get the password corresponding to the category of the
	 * calling application, and the EXTRA_DESCRIPTION, as provided.
	 * Returns the decrypted username & password in the extras EXTRA_USERNAME and
	 * EXTRA_PASSWORD. CATEGORY is an optional parameter.
	 * 
	 * <p>Constant Value: "org.openintents.action.GET_PASSWORD"</p>
	 */
	public static final String ACTION_GET_PASSWORD = "org.openintents.action.GET_PASSWORD";
	
	/**
	 * Activity Action: Set the password corresponding to the category of the
	 * calling application, and the EXTRA_DESCRIPTION, EXTRA_USERNAME and
	 * EXTRA_PASSWORD as provided. CATEGORY is an optional parameter.
	 * 
	 * If both username and password are the non-null empty string, delete this
	 * password entry.
	 * <p>Constant Value: "org.openintents.action.SET_PASSWORD"</p>
	 */
	public static final String ACTION_SET_PASSWORD = "org.openintents.action.SET_PASSWORD";
	
	/**
	 * Activity Action: Restarts the timer for the Crypto intent service.
	 * The timer gets reset when using GET or set password anyway, but this is
	 * a way to reset the timer for other kinds of actions. Use sparingly since
	 * we do actually want the timer to time out eventually!
	 * 
	 * <p>Constant Value: "org.openintents.action.RESTART_TIMER"</p>
	 */
	public static final String ACTION_RESTART_TIMER = "org.openintents.action.RESTART_TIMER";

	/**
	 * Broadcast Action: Sent when the user got logged out of the
	 * crypto session.
	 * 
	 * This can happen after the user logs out actively, 
	 * or through a time-out.
	 * 
	 * Activities that show decrypted content should hide that content again.
	 * 
	 * <p>Constant Value: "org.openintents.action.CRYPTO_LOGGED_OUT"</p>
	 */
	public static final String ACTION_CRYPTO_LOGGED_OUT = "org.openintents.action.CRYPTO_LOGGED_OUT";
	
	/**
	 * Activity Action: Initiate automatic locking of the safe.
	 * This is used internally from any activity to launch the lock screen.
	 * 
	 * <p>Constant Value: "org.openintents.action.AUTOLOCK"</p>
	 */
	public static final String ACTION_AUTOLOCK = "org.openintents.action.AUTOLOCK";

	/**
	 * The text to encrypt or decrypt, or the location for the return result.
	 * 
	 * <p>Constant Value: "org.openintents.extra.TEXT"</p>
	 */
	public static final String EXTRA_TEXT = "org.openintents.extra.TEXT";
	
	/**
	 * An array of text to encrypt or decrypt, or the location for the return result.
	 * Use this to encrypt several strings at once.
	 * 
	 * Entries of the array that are null will be simply ignored and not
	 * encrypted or decrypted.
	 * 
	 * <p>Constant Value: "org.openintents.extra.TEXT_ARRAY"</p>
	 */
	public static final String EXTRA_TEXT_ARRAY = "org.openintents.extra.TEXT_ARRAY";
	
	/**
	 * A session key for encryption or decryption through a content provider.
	 * 
	 * Include this extra with non-empty value to the ENCRYPT or DECRYPT action,
	 * and the resulting intent will contain the current session key, valid until
	 * OI Safe logs out, and the content URI as data.
	 * 
	 * <p>Constant Value: "org.openintents.extra.SESSION_KEY"</p>
	 */
	public static final String EXTRA_SESSION_KEY = "org.openintents.extra.SESSION_KEY";
	
	/**
	 * Required input parameter to GET_PASSWORD and SET_PASSWORD. Corresponds to the "description"
	 * field in passwordsafe. Should be a unique name for the password you're using,
	 * and will already be specific to your application, ie "org.syntaxpolice.opensocial"
	 * 
	 * <p>Constant Value: "org.openintents.extra.UNIQUE_NAME"</p>
	 */
	public static final String EXTRA_UNIQUE_NAME = "org.openintents.extra.UNIQUE_NAME";

	/**
	 * Output parameter from GET_PASSWORD and optional input parameter to SET_PASSWORD.
	 * Corresponds to the decrypted "username" field in passwordsafe.
	 * 
	 * <p>Constant Value: "org.openintents.extra.USERNAME"</p>
	 */
	public static final String EXTRA_USERNAME = "org.openintents.extra.USERNAME";
	
	/**
	 * Output parameter from GET_PASSWORD and _required_ input parameter to SET_PASSWORD.
	 * Corresponds to the decrypted "password" field in passwordsafe.
	 * 
	 * <p>Constant Value: "org.openintents.extra.PASSWORD"</p>
	 */
	public static final String EXTRA_PASSWORD = "org.openintents.extra.PASSWORD";

	/**
	 * Whether to prompt for the password if the service is not running yet.
	 * 
	 * Default value is 'true'. Set to 'false' if you want to suppress prompting for
	 * a password.
	 * 
	 * <p>Constant Value: "org.openintents.extra.PROMPT"</p>
	 */
	public static final String EXTRA_PROMPT = "org.openintents.extra.PROMPT";
	
	/**
	 * Set if the activity handles encrypted content.
	 * 
	 * <p>Constant Value: "org.openintents.category.SAFE"</p>
	 */
	public static final String CATEGORY_SAFE = "org.openintents.category.SAFE";
}
