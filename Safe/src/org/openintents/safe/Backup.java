/* $Id$
 * 
 * Copyright 2008 Randy McEoin
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.util.Xml;

public class Backup {

	private static boolean debug = false;
	private static final String TAG = "Backup";
	
	public static int CURRENT_VERSION = 1;
	
	private String result="";
	
	Context myCtx=null;

	public Backup(Context ctx) {
		myCtx=ctx;
	}

	public boolean write(String filename, OutputStream str) {
		if (debug) Log.d(TAG,"write("+filename+",)");

		try {
			org.xmlpull.v1.XmlSerializer serializer = Xml.newSerializer();
			serializer.setOutput(str, "utf-8");
			serializer.startDocument(null, Boolean.valueOf(true));
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			serializer.startTag(null, "OISafe");
			
			serializer.attribute(null, "version", Integer.toString(CURRENT_VERSION));
			
			Date today;
			String dateOut;
			DateFormat dateFormatter;

			dateFormatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
					DateFormat.FULL);
			today = new Date();
			dateOut = dateFormatter.format(today);

			serializer.attribute(null, "date", dateOut);

			String masterKeyEncrypted = Passwords.fetchMasterKeyEncrypted();
			serializer.startTag(null, "MasterKey");
			serializer.text(masterKeyEncrypted);
			serializer.endTag(null, "MasterKey");

			String salt = Passwords.fetchSalt();
			serializer.startTag(null, "Salt");
			serializer.text(salt);
			serializer.endTag(null, "Salt");

			List<CategoryEntry> crows;
			crows = Passwords.getCategoryEntries();
			
			int totalPasswords=0;

			for (CategoryEntry crow : crows) {

				serializer.startTag(null, "Category");
				serializer.attribute(null, "name", crow.name);

				List<PassEntry> rows;
				rows = Passwords.getPassEntries(crow.id, false, false);
	
				for (PassEntry row : rows) {
					totalPasswords++;
					
					serializer.startTag(null, "Entry");

					serializer.startTag(null, "RowID");
					serializer.text(Long.toString(row.id));
					serializer.endTag(null, "RowID");

					serializer.startTag(null, "Description");
					serializer.text(row.description);
					serializer.endTag(null, "Description");
					
					serializer.startTag(null, "Website");
					serializer.text(row.website);
					serializer.endTag(null, "Website");
					
					serializer.startTag(null, "Username");
					serializer.text(row.username);
					serializer.endTag(null, "Username");
					
					serializer.startTag(null, "Password");
					serializer.text(row.password);
					serializer.endTag(null, "Password");

					serializer.startTag(null, "Note");
					serializer.text(row.note);
					serializer.endTag(null, "Note");

					if (row.uniqueName!=null) {
						serializer.startTag(null, "UniqueName");
						serializer.text(row.uniqueName);
						serializer.endTag(null, "UniqueName");
					}
					
					ArrayList<PackageAccessEntry> packageAccess=Passwords.getPackageAccessEntries(row.id);
					if(packageAccess!=null) {
						serializer.startTag(null, "PackageAccess");
						String entry="";
						Iterator<PackageAccessEntry> packIter=packageAccess.iterator();
						while (packIter.hasNext()) {
							if (entry.length()!=0) {
								entry += ",";
							}
							entry += packIter.next().packageAccess;
						}
						entry = "[" + entry + "]";
						serializer.text(entry);
						serializer.endTag(null, "PackageAccess");
					}

					serializer.endTag(null, "Entry");
				}
				serializer.endTag(null, "Category");
			}

			serializer.endTag(null, "OISafe");
			serializer.endDocument();

            str.close();

			TimeZone tz = TimeZone.getDefault(); 
			int julianDay = Time.getJulianDay((new Date()).getTime(), tz.getRawOffset());
			if (debug) Log.d(TAG,"julianDay="+julianDay);

			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(myCtx);
			SharedPreferences.Editor editor = sp.edit();
			editor.putInt(Preferences.PREFERENCE_LAST_BACKUP_JULIAN, julianDay);
			editor.commit();

			result=myCtx.getString(R.string.backup_complete)+" "+
				Integer.toString(totalPasswords);
		} catch (IOException e) {
			e.printStackTrace();
			result=myCtx.getString(R.string.backup_failed)+" "+
				e.getLocalizedMessage();
			return false;
		}
		return true;
	}
	public String getResult() {
		return result;
	}
}
