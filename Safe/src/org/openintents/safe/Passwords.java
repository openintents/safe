/* 
 * Copyright (C) 2009 OpenIntents.org
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Abstraction layer for storing encrypted and decrypted versions
 * of all PassEntry and CategoryEntry data.
 * Handles fetching and storing with DBHelper
 * and encrypt/decrypt of data.
 * 
 * @author Randy McEoin
 *
 */
public class Passwords {

	private static final boolean debug = false;
	private static final String TAG = "Passwords";

	private static HashMap<Long, PassEntry> passEntries=null;
	
	private static HashMap<Long, CategoryEntry> categoryEntries=null;
	
	private static HashMap<Long, ArrayList<PackageAccessEntry>> packageAccessEntries=null;
	
	private static CryptoHelper ch=null;
	private static boolean cryptoInitialized=false;
	
	private static DBHelper dbHelper=null;
	
	public static boolean Initialize(Context ctx) {
		if (debug) Log.d(TAG,"Initialize()");

		if (ch==null) {
			ch = new CryptoHelper();
		}
		if ((cryptoInitialized==false) &&
				(CategoryList.getSalt()!=null) &&
				(CategoryList.getMasterKey()!=null))
		{
			try {
				Passwords.InitCrypto(CryptoHelper.EncryptionMedium,
						CategoryList.getSalt(), CategoryList.getMasterKey());
				cryptoInitialized=true;
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(ctx, "CategoryList: " + ctx.getString(R.string.crypto_error),
						Toast.LENGTH_SHORT).show();
			}
		}
				
		if (dbHelper==null) {
			dbHelper = new DBHelper(ctx);
			if (dbHelper.isDatabaseOpen()==false) {
				return false;
			}
		}
		if (passEntries==null) {
			passEntries = new HashMap<Long, PassEntry>();
			InitPassEntries();
		}
		if (categoryEntries==null) {
			categoryEntries = new HashMap<Long, CategoryEntry>();
			InitCategoryEntries();
		}
		if (packageAccessEntries==null) {
			packageAccessEntries = new HashMap<Long, ArrayList<PackageAccessEntry>>();
			InitPackageAccess();
		}
		return true;
	}

	/**
	 * Force a fresh load from the database.
	 */
	public static void Reset() {
		categoryEntries.clear();
		InitCategoryEntries();
		passEntries.clear();
		InitPassEntries();
		packageAccessEntries.clear();
		InitPackageAccess();
	}
	
	public static void InitCrypto(int strength, String salt, String masterKey) 
		throws Exception {
		if (debug) Log.d(TAG,"InitCrypto("+strength+","+salt+","+masterKey);
		try {
			ch.init(strength,salt);
			ch.setPassword(masterKey);
		} catch (CryptoHelperException e1) {
			e1.printStackTrace();
			throw new Exception("Error with Passwords.InitCrypto: "+
					e1.getLocalizedMessage());
		}
	}

	public static boolean isCryptoInitialized() {
		return cryptoInitialized;
	}
	public static void deleteAll() {
		dbHelper.deleteDatabase();
		Reset();
	}
	
	public static boolean getPrePopulate() {
		return dbHelper.getPrePopulate();
	}
	
	public static void clearPrePopulate() {
		dbHelper.clearPrePopulate();
	}
	
	public static String fetchSalt() {
		return dbHelper.fetchSalt();
	}
	
	public static String fetchMasterKeyEncrypted() {
		return dbHelper.fetchMasterKey();
	}
	///////////////////////////////////////////////////
	///////////// Category Functions //////////////////
	///////////////////////////////////////////////////

	private static void InitCategoryEntries() {
		List<CategoryEntry> catRows;
		catRows = dbHelper.fetchAllCategoryRows();
		for (CategoryEntry catRow : catRows) {
			catRow.nameNeedsDecrypt=true;
			catRow.plainNameNeedsEncrypt=false;
			categoryEntries.put(catRow.id, catRow);
		}
	}

	public static List<CategoryEntry> getCategoryEntries() {
		Collection<CategoryEntry> categories=categoryEntries.values();
		Iterator<CategoryEntry> catIter=categories.iterator();
		while (catIter.hasNext()) {
			CategoryEntry catEntry=catIter.next();
			// run through and ensure all entries are decrypted
			getCategoryEntry(catEntry.id);
		}
		List<CategoryEntry> catList=new ArrayList<CategoryEntry>(categories);
		Collections.sort(catList, new Comparator<CategoryEntry>() {
			public int compare(CategoryEntry o1, CategoryEntry o2) {
				return o1.plainName.compareToIgnoreCase(o2.plainName);
			}});
		return catList;
	}
	
	public static List<String> getCategoryNames() {
		List<String> items = new ArrayList<String>();

		List<CategoryEntry> catList=getCategoryEntries();
		for (CategoryEntry row : catList) {
			items.add(row.plainName);
		}
		return items;
	}
	
	public static HashMap<Long, String> getCategoryIdToName() {
		HashMap<Long, String> categoryMap = new HashMap<Long, String>();
		Collection<CategoryEntry> categories=categoryEntries.values();
		Iterator<CategoryEntry> catIter=categories.iterator();
		while (catIter.hasNext()) {
			CategoryEntry catEntry=catIter.next();
			// run through and ensure all entries are decrypted
			getCategoryEntry(catEntry.id);
			categoryMap.put(catEntry.id, catEntry.plainName);
		}
		return categoryMap;
	}
	
	public static HashMap<String, Long> getCategoryNameToId() {
		HashMap<String, Long> categoryMap = new HashMap<String, Long>();
		Collection<CategoryEntry> categories=categoryEntries.values();
		Iterator<CategoryEntry> catIter=categories.iterator();
		while (catIter.hasNext()) {
			CategoryEntry catEntry=catIter.next();
			// run through and ensure all entries are decrypted
			getCategoryEntry(catEntry.id);
			categoryMap.put(catEntry.plainName, catEntry.id);
			if (debug) Log.d(TAG,"map "+catEntry.plainName+" to "+catEntry.id);
		}
		return categoryMap;
	}
	
	public static CategoryEntry getCategoryEntryByName(String category) {
		Collection<CategoryEntry> categories=categoryEntries.values();
		Iterator<CategoryEntry> catIter=categories.iterator();
		while (catIter.hasNext()) {
			CategoryEntry catEntry=catIter.next();
			if (catEntry.name.compareTo(category)==0) {
				return catEntry;
			}
		}
		return null;
	}
	
	public static CategoryEntry getCategoryEntry(Long id) {
		CategoryEntry catEntry=categoryEntries.get(id);
		if (catEntry==null) {
			return null;
		}
		if (catEntry.nameNeedsDecrypt) {
			if (debug) Log.d(TAG,"decrypt cat");
			try {
				catEntry.plainName = ch.decrypt(catEntry.name);
			} catch (CryptoHelperException e) {
				Log.e(TAG,e.toString());
			}
			catEntry.nameNeedsDecrypt=false;
			categoryEntries.put(id, catEntry);
		}
		catEntry.count=dbHelper.getCategoryCount(id);
		return catEntry;
	}
	
	public static long putCategoryEntry(CategoryEntry catEntry) {
		if (catEntry.plainNameNeedsEncrypt) {
			if (debug) Log.d(TAG,"encrypt cat");
			try {
				catEntry.name = ch.encrypt(catEntry.plainName);
			} catch (CryptoHelperException e) {
				Log.e(TAG,e.toString());
			}
			catEntry.plainNameNeedsEncrypt=false;
		}
		if (catEntry.id==-1) {
			catEntry.id=dbHelper.addCategory(catEntry);
		} else {
			dbHelper.updateCategory(catEntry.id, catEntry);
		}
		categoryEntries.put(catEntry.id, catEntry);
		return catEntry.id;
	}
	
	public static void updateCategoryCount(long id) {
		CategoryEntry catEntry=categoryEntries.get(id);
		if (catEntry==null) {
			return;
		}
		catEntry.count=dbHelper.getCategoryCount(id);
	}

	public static void deleteCategoryEntry(long id) {
		if (debug) Log.d(TAG,"deleteCategoryEntry("+id+")");
		dbHelper.deleteCategory(id);
		categoryEntries.remove(id);
	}

	///////////////////////////////////////////////////
	///////////// Password Functions //////////////////
	///////////////////////////////////////////////////

	private static void InitPassEntries() {
		List<PassEntry> passRows;
		passRows = dbHelper.fetchAllRows(new Long(0));
		for (PassEntry passRow : passRows) {
			passRow.needsDecryptDescription=true;
			passRow.needsDecrypt=true;
			passRow.needsEncrypt=false;
			passEntries.put(passRow.id, passRow);
		}
	}

	public static List<PassEntry> getPassEntries(long categoryId, boolean decrypt, boolean descriptionOnly) {
		if (debug) Log.d(TAG,"getPassEntries("+categoryId+","+decrypt+","+descriptionOnly+")");
		Collection<PassEntry> passwords=passEntries.values();
		List<PassEntry> passList=new ArrayList<PassEntry>();
		Iterator<PassEntry> passIter=passwords.iterator();
		while (passIter.hasNext()) {
			PassEntry passEntry=passIter.next();
			if ((categoryId==0) || (passEntry.category==categoryId)) {
				getPassEntry(passEntry.id, decrypt, descriptionOnly);
				passList.add(passEntry);
			}
		}
		if (decrypt==true) {
			Collections.sort(passList, new Comparator<PassEntry>() {
				public int compare(PassEntry o1, PassEntry o2) {
					return o1.plainDescription.compareToIgnoreCase(o2.plainDescription);
				}});
		}
		return passList;
	}
	
	public static HashMap<Long, String> getPassIdToDescriptionOLD() {
		HashMap<Long, String> passMap = new HashMap<Long, String>();
		Collection<PassEntry> passwords=passEntries.values();
		Iterator<PassEntry> passIter=passwords.iterator();
		while (passIter.hasNext()) {
			PassEntry passEntry=passIter.next();
			// run through and ensure all entries are decrypted
			getPassEntry(passEntry.id, true, true);
			passMap.put(passEntry.id, passEntry.plainDescription);
		}
		return passMap;
	}
	
	public static HashMap<String, Long> getPassDescriptionToIdOLD() {
		HashMap<String, Long> passMap = new HashMap<String, Long>();
		Collection<PassEntry> passwords=passEntries.values();
		Iterator<PassEntry> passIter=passwords.iterator();
		while (passIter.hasNext()) {
			PassEntry passEntry=passIter.next();
			// run through and ensure all entries are decrypted
			getPassEntry(passEntry.id, true, true);
			passMap.put(passEntry.plainDescription, passEntry.id);
		}
		return passMap;
	}
	
	public static PassEntry getPassEntry(Long id, boolean decrypt, boolean descriptionOnly) {
		if (debug) Log.d(TAG,"getPassEntry("+id+")");
		if ((id==null) || (passEntries==null)) {
			return null;
		}
		PassEntry passEntry=passEntries.get(id);
		if (passEntry==null) {
			return null;
		}
		if (decrypt==false) {
			return passEntry;
		}
		if (passEntry.needsDecryptDescription) {
			//if (debug) Log.d(TAG,"decrypt pass description");
			try {
				passEntry.plainDescription = ch.decrypt(passEntry.description);
			} catch (CryptoHelperException e) {
				Log.e(TAG,e.toString());
			}
			passEntry.needsDecryptDescription=false;
			passEntries.put(id, passEntry);
		}
		if (!descriptionOnly && passEntry.needsDecrypt) {
			if (debug) Log.d(TAG,"decrypt pass");
			try {
				passEntry.plainDescription = ch.decrypt(passEntry.description);
				passEntry.plainWebsite=ch.decrypt(passEntry.website);
				passEntry.plainUsername=ch.decrypt(passEntry.username);
				passEntry.plainPassword=ch.decrypt(passEntry.password);
				passEntry.plainNote=ch.decrypt(passEntry.note);
				passEntry.plainUniqueName=ch.decrypt(passEntry.uniqueName);
			} catch (CryptoHelperException e) {
				Log.e(TAG,e.toString());
			}
			passEntry.needsDecrypt=false;
			passEntries.put(id, passEntry);
		}
		return passEntry;
	}
	
	public static PassEntry findPassWithUniqueName(String plainUniqueName) {
		String uniqueName="";
		try {
			uniqueName = ch.encrypt(plainUniqueName);
		} catch (CryptoHelperException e) {
			Log.e(TAG,e.toString());
			return null;
		}
		Collection<PassEntry> passwords=passEntries.values();
		Iterator<PassEntry> passIter=passwords.iterator();
		while (passIter.hasNext()) {
			PassEntry passEntry=passIter.next();
			if (passEntry.uniqueName.compareTo(uniqueName)==0) {
				passEntry=getPassEntry(passEntry.id,true,false);
				return passEntry;
			}
		}
		return null;
	}
	/**
	 * @param passEntry the entry to placed into the password cache.  If id is 0,
	 * then it will be added, otherwise it will update existing entry.
	 * @return long row id of newly added or updated entry,
	 * equal to -1 if a sql error occurred
	 */
	public static long putPassEntry(PassEntry passEntry) {
		if (debug) Log.d(TAG,"putPassEntry("+passEntry.id+")");
		if (passEntry.needsEncrypt) {
			if (debug) Log.d(TAG,"encrypt pass");
			try {
				passEntry.description = ch.encrypt(passEntry.plainDescription);
				passEntry.website = ch.encrypt(passEntry.plainWebsite);
				passEntry.username = ch.encrypt(passEntry.plainUsername);
				passEntry.password = ch.encrypt(passEntry.plainPassword);
				passEntry.note = ch.encrypt(passEntry.plainNote);
				passEntry.uniqueName = ch.encrypt(passEntry.plainUniqueName);
			} catch (CryptoHelperException e) {
				Log.e(TAG,e.toString());
			}
			passEntry.needsEncrypt=false;
		}
		// Format the current time.
		Date date = new Date();
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,DateFormat.LONG);
		passEntry.lastEdited=df.format(date);

		if (passEntry.id==0) {
			passEntry.id=dbHelper.addPassword(passEntry);
			if (passEntry.id==-1) {
				// error adding
				return -1;
			}
			updateCategoryCount(passEntry.category);
		} else {
			long success=dbHelper.updatePassword(passEntry.id, passEntry);
			if (success==-1) {
				return -1;
			}
		}
		passEntries.put(passEntry.id, passEntry);
		return passEntry.id;
	}
	
	public static void deletePassEntry(long id) {
		if (debug) Log.d(TAG,"deletePassEntry("+id+")");
		PassEntry passEntry=getPassEntry(id, false, false);
		if (passEntry==null) {
			return;
		}
		long categoryId=passEntry.category;
		dbHelper.deletePassword(id);
		passEntries.remove(id);
		updateCategoryCount(categoryId);
	}

	public static void updatePassCategory(long passId, long categoryId) {
		PassEntry passEntry=passEntries.get(passId);
		passEntry.category=categoryId;
		dbHelper.updatePasswordCategory(passId, categoryId);
	}

	/**
	 * @param categoryId if -1 then count all passwords
	 */
	public static int countPasswords(long categoryId) {
		return dbHelper.countPasswords(categoryId);
	}

	///////////////////////////////////////////////////
	////////// Package Access Functions ///////////////
	///////////////////////////////////////////////////

	private static void InitPackageAccess() {
		HashMap<Long, ArrayList<String>> dbPackageAccess=dbHelper.fetchPackageAccessAll();
		if (!dbPackageAccess.isEmpty()) {
			Set<Long> keys=dbPackageAccess.keySet();
			Iterator<Long> keysIter=keys.iterator();
			while (keysIter.hasNext()) {
				Long key=keysIter.next();
				ArrayList<PackageAccessEntry> packageNames=new ArrayList<PackageAccessEntry>();
				ArrayList<String> dbPackageNames=dbPackageAccess.get(key);
				Iterator<String> packIter=dbPackageNames.iterator();
				while (packIter.hasNext()) {
					String packageName=packIter.next();
					PackageAccessEntry packageAccess = new PackageAccessEntry(); 
					packageAccess.packageAccess=packageName;
					packageAccess.needsDecrypt=true;
					packageAccess.needsEncrypt=false;
					packageNames.add(packageAccess);
				}
				packageAccessEntries.put(key, packageNames);
			}
		}
	}

	public static ArrayList<String> getPackageAccess(Long id) {
		ArrayList<String> packageAccess=null;
		if (packageAccessEntries.containsKey(id)) {
			ArrayList<PackageAccessEntry> packageAccessEntry=packageAccessEntries.get(id);
			Iterator<PackageAccessEntry> packIter=packageAccessEntry.iterator();
			packageAccess=new ArrayList<String>();
			while(packIter.hasNext()) {
				PackageAccessEntry packEntry=packIter.next();
				if (packEntry.needsDecrypt) {
					try {
						packEntry.plainPackageAccess=ch.decrypt(packEntry.packageAccess);
					} catch (CryptoHelperException e) {
						Log.e(TAG,e.toString());
					}
				}
				packageAccess.add(packEntry.plainPackageAccess);
			}
		}
		return packageAccess;
	}
	
	public static ArrayList<PackageAccessEntry> getPackageAccessEntries(Long id) {
		if (packageAccessEntries.containsKey(id)) {
			return packageAccessEntries.get(id);
		}
		return null;
	}
	
	public static void addPackageAccess(Long id, String packageName) {
		String encryptedPackageName="";
		try {
			encryptedPackageName = ch.encrypt(packageName);
			dbHelper.addPackageAccess(id, encryptedPackageName);
		} catch (CryptoHelperException e) {
			Log.e(TAG,e.toString());
			return;
		}

		ArrayList<PackageAccessEntry> packageNames;
		if (packageAccessEntries.containsKey(id)) {
			packageNames=packageAccessEntries.get(id);
		} else {
			packageNames=new ArrayList<PackageAccessEntry>();
		}
		PackageAccessEntry newPackageAccessEntry=new PackageAccessEntry();
		newPackageAccessEntry.plainPackageAccess=packageName;
		newPackageAccessEntry.packageAccess=encryptedPackageName;
		newPackageAccessEntry.needsDecrypt=false;
		newPackageAccessEntry.needsEncrypt=false;
		packageNames.add(newPackageAccessEntry);
		packageAccessEntries.put(id, packageNames);
	}
}
