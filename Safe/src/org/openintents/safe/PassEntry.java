/* $Id$
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

import java.util.ArrayList;

/**
 * 
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class PassEntry extends Object {
	public long id=-1;
	public boolean needsDecryptDescription;
	public boolean needsDecrypt;
	public boolean needsEncrypt=true;
	public String password;
	public long category;
	public String categoryName;
	public String description;
	public String username;
	public String website;
	public String uniqueName;
	public ArrayList<String> packageAccess;
	public String note;
	public String plainPassword;
	public String plainDescription;
	public String plainUsername;
	public String plainWebsite;
	public String plainNote;
	public String plainUniqueName;
	public String lastEdited;

	public static boolean checkPackageAccess (ArrayList<String> packageAccess, String packageName) {
		return (packageAccess.contains(packageName));
	}
}