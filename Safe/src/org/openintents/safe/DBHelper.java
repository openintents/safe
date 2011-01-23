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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * DBHelper class.  
 * 
 * The overall theme of this class was borrowed from the Notepad
 * example Open Handset Alliance website.  It's essentially a very
 * primitive database layer.
 * 
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class DBHelper {

    private static final String DATABASE_NAME = "safe";
    private static final String TABLE_DBVERSION = "dbversion";
    private static final String TABLE_PASSWORDS = "passwords";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_MASTER_KEY = "master_key";
    private static final String TABLE_SALT = "salt";
    private static final String TABLE_PACKAGE_ACCESS = "package_access";
    private static final String TABLE_CIPHER_ACCESS = "cipher_access";
    private static final int DATABASE_VERSION = 4;
    private static String TAG = "DBHelper";
    Context myCtx;

    private static final String DBVERSION_CREATE = 
    	"create table " + TABLE_DBVERSION + " ("
    		+ "version integer not null);";

    private static final String PASSWORDS_CREATE =
        "create table " + TABLE_PASSWORDS + " ("
    	    + "id integer primary key autoincrement, "
    	    + "category integer not null, "
            + "password text not null, "
            + "description text not null, "
            + "username text, "
            + "website text, "
            + "note text, "
            + "unique_name text, " //might be null
            + "lastdatetimeedit text);";
    
    private static final String PASSWORDS_DROP =
    	"drop table " + TABLE_PASSWORDS + ";";

    private static final String PACKAGE_ACCESS_CREATE =
        "create table " + TABLE_PACKAGE_ACCESS + " ("
    	    + "id integer not null, "
            + "package text not null);";

    private static final String PACKAGE_ACCESS_DROP =
    	"drop table " + TABLE_PACKAGE_ACCESS + ";";

    private static final String CATEGORIES_CREATE =
        "create table " + TABLE_CATEGORIES + " ("
    	    + "id integer primary key autoincrement, "
            + "name text not null, "
            + "lastdatetimeedit text);";

    private static final String CATEGORIES_DROP =
    	"drop table " + TABLE_CATEGORIES + ";";

    private static final String MASTER_KEY_CREATE = 
    	"create table " + TABLE_MASTER_KEY + " ("
    		+ "encryptedkey text not null);";

    private static final String SALT_CREATE = 
    	"create table " + TABLE_SALT + " ("
    		+ "salt text not null);";

    private static final String CIPHER_ACCESS_CREATE =
		"create table " + TABLE_CIPHER_ACCESS + " ("
			+ "id integer primary key autoincrement, "
			+ "packagename text not null, "
			+ "expires integer not null, "
			+ "dateadded text not null);";

//    private static final String CIPHER_ACCESS_DROP =
//    	"drop table " + TABLE_CIPHER_ACCESS + ";";

    private SQLiteDatabase db=null;
    private static boolean needsPrePopulation=false;
    private static boolean needsUpgrade=false;

    /**
     * 
     * @param ctx
     */
    public DBHelper(Context ctx) {
    	myCtx = ctx;
		try {
			db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);

			// Check for the existence of the DBVERSION table
			// If it doesn't exist than create the overall data,
			// otherwise double check the version
			Cursor c =
				db.query("sqlite_master", new String[] { "name" },
						"type='table' and name='"+TABLE_DBVERSION+"'", null, null, null, null);
			int numRows = c.getCount();
			if (numRows < 1) {
				CreateDatabase(db);
			} else {
				int version=0;
				Cursor vc = db.query(true, TABLE_DBVERSION, new String[] {"version"},
						null, null, null, null, null,null);
				if(vc.getCount() > 0) {
				    vc.moveToFirst();
				    version=vc.getInt(0);
				}
				vc.close();
				if (version!=DATABASE_VERSION) {
					needsUpgrade=true;
					Log.e(TAG,"database version mismatch");
				}
			}
			c.close();
			

		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
    }

    private void CreateDatabase(SQLiteDatabase db)
    {
		try {
			db.execSQL(DBVERSION_CREATE);
			ContentValues args = new ContentValues();
			args.put("version", DATABASE_VERSION);
			db.insert(TABLE_DBVERSION, null, args);

			db.execSQL(CATEGORIES_CREATE);
			needsPrePopulation=true;
			
			db.execSQL(PASSWORDS_CREATE);
			db.execSQL(PACKAGE_ACCESS_CREATE);
			db.execSQL(CIPHER_ACCESS_CREATE);
			db.execSQL(MASTER_KEY_CREATE);
			db.execSQL(SALT_CREATE);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} 
    }
    
    public void deleteDatabase()
    {
        try {
			db.execSQL(PASSWORDS_DROP);
			db.execSQL(PASSWORDS_CREATE);

			db.execSQL(CATEGORIES_DROP);
			db.execSQL(CATEGORIES_CREATE);
			
			db.execSQL(PACKAGE_ACCESS_DROP);
			db.execSQL(PACKAGE_ACCESS_CREATE);
        } catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
    }
    
    public boolean needsUpgrade()
    {
    	return needsUpgrade;
    }

    public boolean getPrePopulate()
    {
    	return needsPrePopulation;
    }

    public void clearPrePopulate()
    {
    	needsPrePopulation=false;
    }
    /**
     * Close database connection
     */
    public void close() {
    	try {
    		db.close();
	    } catch (SQLException e)
	    {
	    	Log.d(TAG,"close exception: " + e.getLocalizedMessage());
	    }
    }

    public int fetchVersion() {
    	int version=0;
        try {
			Cursor c = db.query(true, TABLE_DBVERSION,
				new String[] {"version"},
				null, null, null, null, null,null);
			if(c.getCount() > 0) {
			    c.moveToFirst();
			    version=c.getInt(0);
			}
			c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
		return version;
    }
    
////////// Salt Functions ////////////////

    /**
     * Store the salt
     * 
     * @return String version of salt
     */
    public String fetchSalt() {
    	String salt="";
        try {
			Cursor c = db.query(true, TABLE_SALT, new String[] {"salt"},
				null, null, null, null, null,null);
			if(c.getCount() > 0) {
			    c.moveToFirst();
			    salt=c.getString(0);
			}
			c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
		return salt;
    }
    
    /**
     * Store the salt into the database.
     * 
     * @param salt String version of the salt
     */
    public void storeSalt(String salt) {
		ContentValues args = new ContentValues();
        try {
			db.delete(TABLE_SALT, "1=1", null);
			args.put("salt", salt);
			db.insert(TABLE_SALT, null, args);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
    }
    
////////// Master Key Functions ////////////////

    /**
     * 
     * @return
     */
    public String fetchMasterKey() {
    	String key="";
        try {
			Cursor c = db.query(true, TABLE_MASTER_KEY, new String[] {"encryptedkey"},
				null, null, null, null, null,null);
			if(c.getCount() > 0) {
			    c.moveToFirst();
			    key=c.getString(0);
			}
			c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
		return key;
    }
    
    /**
     * 
     * @param PBEKey
     */
    public void storeMasterKey(String MasterKey) {
		ContentValues args = new ContentValues();
        try {
			db.delete(TABLE_MASTER_KEY, "1=1", null);
			args.put("encryptedkey", MasterKey);
			db.insert(TABLE_MASTER_KEY, null, args);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
    }

//////////Category Functions ////////////////

    /**
     * Doesn't add the category if it already exists.
     * @param entry
     * @returns row id
     */
    public long addCategory(CategoryEntry entry) {
        ContentValues initialValues = new ContentValues();

    	long rowID=-1;
        Cursor c =
        	db.query(true, TABLE_CATEGORIES, new String[] {
        			"id", "name"}, "name='" + entry.name + "'" , null, null, null, null, null);
        if (c.getCount() > 0) {
        	c.moveToFirst();
        	rowID = c.getLong(0);
        	
        } else {// there's not already such a category...
        	initialValues.put("name", entry.name);

        	try {
        		rowID=db.insert(TABLE_CATEGORIES, null, initialValues);
        	} catch (SQLException e)
        	{
        		Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
        	}
        } 
		c.close();
        return rowID;
    }

    /**
     * 
     * @param Id
     */
    public void deleteCategory(long Id) {
        try {
			db.delete(TABLE_CATEGORIES, "id=" + Id, null);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
    }

    /**
     * 
     * @return
     */
    public List<CategoryEntry> fetchAllCategoryRows(){
        ArrayList<CategoryEntry> ret = new ArrayList<CategoryEntry>();
        try {
	        Cursor c =
	            db.query(TABLE_CATEGORIES, new String[] {
	                "id", "name"},
	                null, null, null, null, null);
	        int numRows = c.getCount();
	        c.moveToFirst();
	        for (int i = 0; i < numRows; ++i) {
	            CategoryEntry row = new CategoryEntry();
	            row.id = c.getLong(0);
	            row.name = c.getString(1);
	            ret.add(row);
	            c.moveToNext();
	        }
	        c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
        return ret;
    }

    /**
     * 
     * @param Id
     * @return
     */
    public CategoryEntry fetchCategory(long Id) {
        CategoryEntry row = new CategoryEntry();
        try {
	        Cursor c =
	            db.query(true, TABLE_CATEGORIES, new String[] {
	                "id", "name"}, "id=" + Id, null, null, null, null, null);
	        if (c.getCount() > 0) {
	            c.moveToFirst();
	            row.id = c.getLong(0);
	
	            row.name = c.getString(1);
	        } else {
	            row.id = -1;
	            row.name = null;
	        }
	        c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
        return row;
    }

    public int getCategoryCount(long Id) {
        int count = 0;
        try {
	        Cursor c =
	            db.rawQuery("SELECT count(*) FROM "+TABLE_PASSWORDS+" WHERE category=" + Id, null);
	        if (c.getCount() > 0) {
	            c.moveToFirst();
	            count = c.getInt(0);
	        }
	        c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
        return count;
    }

    /**
     * 
     * @param Id
     * @param entry
     */
    public void updateCategory(long Id, CategoryEntry entry) {
        ContentValues args = new ContentValues();
        args.put("name", entry.name);
        
        try {
			db.update(TABLE_CATEGORIES, args, "id=" + Id, null);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
    }


////////// Password Functions ////////////////
	

	/**
	 * 
	 * @param categoryId if -1 then count all passwords
	 */
	public int countPasswords(long categoryId) {
		int count=0;
	    try {
	    	String selection=null;
	    	if (categoryId>0) {
	    		selection="category="+categoryId;
	    	}
	        Cursor c = db.query(TABLE_PASSWORDS, new String[] {
	                "count(*)"},
	                selection, null, null, null, null);
	        c.moveToFirst();
	        count=c.getInt(0);
	        c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
		//Log.i(TAG,"count="+count);
		return count;
	}

	/**
	 * 
	 * @return
	 */
	public List<PassEntry> fetchAllRows(Long CategoryId){
	    ArrayList<PassEntry> ret = new ArrayList<PassEntry>();
	    if (db==null) { return ret; }
	    try {
	        Cursor c;
	        if (CategoryId==0)
	        {
		        c = db.query(TABLE_PASSWORDS, new String[] {
	                "id", "password", "description", "username", "website",
	                "note", "category", "unique_name", "lastdatetimeedit"},
	                null, null, null, null, null);
	        } else {
		        c = db.query(TABLE_PASSWORDS, new String[] {
		                "id", "password", "description", "username", "website",
		                "note", "category", "unique_name", "lastdatetimeedit"},
		                "category="+CategoryId, null, null, null, null);
	        }
	        int numRows = c.getCount();
	        c.moveToFirst();
	        for (int i = 0; i < numRows; ++i) {
	            PassEntry row = new PassEntry();
	            row.id = c.getLong(0);
	            
	            row.password = c.getString(1);
	            row.description = c.getString(2);
	            row.username = c.getString(3);
	            row.website = c.getString(4);
	            row.note = c.getString(5);
	            
	            row.category = c.getLong(6);
	            row.uniqueName = c.getString(7);
	            row.lastEdited = c.getString(8);
	            
	            ret.add(row);
	            c.moveToNext();
	        }
	        c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	    return ret;
	}
	
	/**
	 * 
	 * @param Id
	 * @return
	 */
	public PassEntry fetchPassword(long Id) {
	    PassEntry row = new PassEntry();
	    try {
	        Cursor c =
	            db.query(true, TABLE_PASSWORDS, new String[] {
	                "id", "password", "description", "username", "website",
	                "note", "category, unique_name", "lastdatetimeedit"},
	                "id=" + Id, null, null, null, null, null);
	        if (c.getCount() > 0) {
	            c.moveToFirst();
	            row.id = c.getLong(0);
	
	            row.password = c.getString(1);
	            row.description = c.getString(2);
	            row.username = c.getString(3);
	            row.website = c.getString(4);
	            row.note = c.getString(5);
	            
	            row.category = c.getLong(6);
	            row.uniqueName = c.getString(7);
	            row.lastEdited = c.getString(8);
	        } else {
	            row.id = -1;
	            row.description = row.password = null;
	        }
	        c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	    return row;
	}
	
	public PassEntry fetchPassword(String uniqueName) {
	    PassEntry row = new PassEntry();
		row.id = -1;
		row.description = row.password = null;
		try {
	        Cursor c =
	        	db.query(true, TABLE_PASSWORDS, new String[] {
					"id", "password", "description", "username", "website",
					"note", "category", "unique_name", "lastdatetimeedit"},
					"unique_name='" + uniqueName + "'",
					null, null, null, null, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				row.id = c.getLong(0);

				row.password = c.getString(1);
				row.description = c.getString(2);
				row.username = c.getString(3);
				row.website = c.getString(4);
				row.note = c.getString(5);

				row.category = c.getLong(6);
	            row.uniqueName = c.getString(7);
	            row.lastEdited = c.getString(8);
			}
			c.close();
		} catch (SQLException e)
	    {
	    	Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
	    }
	    return row;
	}
	
	
	public ArrayList<String> fetchPackageAccess (long passwordID) {
		ArrayList <String> pkgs = new ArrayList<String>();
		Cursor c = null;
		try {
			c =
				db.query(true, TABLE_PACKAGE_ACCESS, new String[] {
				"package"}, "id=" + passwordID,
				null, null, null, null, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				while (! c.isAfterLast()) {
					pkgs.add(c.getString(0));
					c.moveToNext();
				}
			}
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally {
			if (c != null) c.close();
		}

		return pkgs;
	}
	
	/**
	 * Fetch all the package access data into one HashMap.
	 * 
	 * @return  HashMap&lt;Long id, ArrayList&lt;String> package>
	 */
	public HashMap<Long, ArrayList<String>> fetchPackageAccessAll() {
		HashMap<Long, ArrayList<String>> pkgsAll=new HashMap<Long, ArrayList<String>>();

		Cursor c = null;
		try {
			c =
				db.query(true, TABLE_PACKAGE_ACCESS, new String[] {
					"id"}, null, null, null, null, null, null);
			if (c.getCount() > 0) {
				c.moveToFirst();
				while (! c.isAfterLast()) {
					Long id=c.getLong(0);
					ArrayList <String> pkgs = fetchPackageAccess(id);
					if (pkgs!=null) {
						pkgsAll.put(id, pkgs);
					}
					c.moveToNext();
				}
			}
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} finally {
			if (c != null) c.close();
		}
		
		return pkgsAll;
	}
	
	public void addPackageAccess (long passwordID, String packageToAdd) {
		ContentValues packageAccessValues = new ContentValues ();
		packageAccessValues.put("id", passwordID);
		packageAccessValues.put("package", packageToAdd);
		try {
			db.insert(TABLE_PACKAGE_ACCESS, null, packageAccessValues);
		} catch (SQLException e) {
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	}
	
	/**
	 * 
	 * @param Id
	 * @param entry
	 * @return Id on success, -1 on failure
	 */
	public long updatePassword(long Id, PassEntry entry) {
	    ContentValues args = new ContentValues();	    
	    args.put("description", entry.description);
	    args.put("username", entry.username);
	    args.put("password", entry.password);
	    args.put("website", entry.website);
	    args.put("note", entry.note);
	    args.put("unique_name", entry.uniqueName);
		DateFormat dateFormatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
				DateFormat.FULL);
		Date today = new Date();
		String dateOut = dateFormatter.format(today);
	    args.put("lastdatetimeedit", dateOut);
	    try {
			db.update(TABLE_PASSWORDS, args, "id=" + Id, null);
		} catch (SQLException e)
		{
			Log.d(TAG,"updatePassword: SQLite exception: " + e.getLocalizedMessage());
			return -1;
		}
		return Id;
	}
	
	/**
	 * Only update the category field of the password entry.
	 * 
	 * @param Id the id of the password entry
	 * @param newCategoryId the updated category id
	 */
	public void updatePasswordCategory(long Id, long newCategoryId) {
		if (Id<0 || newCategoryId<0) {
			//make sure values appear valid
			return;
		}
	    ContentValues args = new ContentValues();
	    
	    args.put("category", newCategoryId);
	    
	    try {
			db.update(TABLE_PASSWORDS, args, "id=" + Id, null);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Add a password entry to the database.
	 * PassEntry.id should be set to 0, unless a specific
	 * row id is desired.
	 * 
	 * @param entry PassEntry
	 * @return long row id of newly added entry, equal to -1 if an error occurred
	 */
	public long addPassword(PassEntry entry) {
		long id = -1;
	    ContentValues initialValues = new ContentValues();
	    if (entry.id!=0) {
			initialValues.put("id", entry.id);
	    }
		initialValues.put("category", entry.category);
		initialValues.put("password", entry.password);
	    initialValues.put("description", entry.description);
	    initialValues.put("username",entry.username);
	    initialValues.put("website", entry.website);
	    initialValues.put("note", entry.note);
	    initialValues.put("unique_name", entry.uniqueName);
		DateFormat dateFormatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
			DateFormat.FULL);
		Date today = new Date();
		String dateOut = dateFormatter.format(today);
	    initialValues.put("lastdatetimeedit", dateOut);

	    try {
	        id = db.insertOrThrow(TABLE_PASSWORDS, null, initialValues);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
			id=-1;
		}
		return (id);
	}
	
	/**
	 * 
	 * @param Id
	 */
	public void deletePassword(long Id) {
	    try {
			db.delete(TABLE_PASSWORDS, "id=" + Id, null);
			db.delete(TABLE_PACKAGE_ACCESS, "id=" + Id, null);
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	}

//////////Cipher Access Functions ////////////////

	/**
	 * Add a package to the list of packages allowed to use the encrypt/decrypt
	 * cipher services.
	 * 
	 * @param packageToAdd
	 * @param expiration set to 0 if no expiration, otherwise epoch time
	 */
	public void addCipherAccess (String packageToAdd, long expiration) {
		ContentValues initialValues = new ContentValues ();
		initialValues.put("packagename", packageToAdd);
		initialValues.put("expires", expiration);
		DateFormat dateFormatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
			DateFormat.FULL);
		Date today = new Date();
		String dateOut = dateFormatter.format(today);
		initialValues.put("dateadded", dateOut);
		try {
			db.insert(TABLE_CIPHER_ACCESS, null, initialValues);
		} catch (SQLException e) {
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Fetch the cipher access for a package.   This determines if the package
	 * is allowed to use encrypt/decrypt services.
	 * 
	 * @param packageName
	 * @return -1 if not found, 0 if no expiration, otherwise epoch date of expiration
	 */
	public long fetchCipherAccess(String packageName) {
		long expires=-1;	// default to not found
		try {
			Cursor c = db.query(true, TABLE_CIPHER_ACCESS, new String[] {"expires"},
			"packagename="+packageName, null, null, null, null,null);
			if(c.getCount() > 0) {
				c.moveToFirst();
				expires=c.getLong(0);
			}
			c.close();
		} catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
		return expires;
	}
	/**
	 * Begin a transaction on an open database.
	 * 
	 * @return true if successful
	 */
	public boolean beginTransaction() {
        try {
			db.execSQL("begin transaction;");
        } catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
			return false;
		}
        return true;
	}

	/**
	 * Commit all changes since the begin transaction on an 
	 * open database.
	 */
	public void commit() {
        try {
			db.execSQL("commit;");
        } catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Rollback all changes since the begin transaction on an 
	 * open database.
	 */
	public void rollback() {
        try {
			db.execSQL("rollback;");
        } catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	}
}

