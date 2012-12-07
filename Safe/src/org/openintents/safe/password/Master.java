/* $Id$
 * 
 * Copyright 2012 OpenIntents.org
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
package org.openintents.safe.password;

/**
 * Centrally stores the Master Key and Salt for all other classes to use. 
 */
public class Master {

	private static String salt = null;
	private static String masterKey = null;

	/**
	 * @return the salt
	 */
	public synchronized static String getSalt() {
		return salt;
	}

	/**
	 * @param salt
	 *            the salt to set
	 */
	public synchronized static void setSalt(String saltIn) {
		salt = saltIn;
	}

	/**
	 * @return the masterKey
	 */
	public synchronized static String getMasterKey() {
		return masterKey;
	}

	/**
	 * @param masterKey
	 *            the masterKey to set
	 */
	public synchronized static void setMasterKey(String masterKeyIn) {
		masterKey = masterKeyIn;
	}

}
