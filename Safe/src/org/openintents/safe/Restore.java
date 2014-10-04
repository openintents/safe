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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.openintents.intents.CryptoIntents;
import org.openintents.safe.password.Master;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class Restore extends Activity {

	private static final String TAG = "Restore";

    private DBHelper dbHelper=null;
	private String masterKey="";
	private RestoreDataSet restoreDataSet=null;
	private boolean firstTime=false;

	public static final String KEY_FIRST_TIME = "first_time";  // Intent keys
	public static final String KEY_FILE_PATH = "backup_file_path";
	
	public static final int REQUEST_RESTORE_FILENAME = 0;
    public static final int REQUEST_RESTORE_DOCUMENT = 1;

	Intent frontdoor;
	private Intent restartTimerIntent=null;

	BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)) {
				if (BuildConfig.DEBUG) Log.d(TAG,"caught ACTION_CRYPTO_LOGGED_OUT");
				startActivity(frontdoor);
			}
		}
	};
    @InjectView(R.id.restore_filename)
    TextView filenameText;
    @InjectView(R.id.restore_info)
    TextView restoreInfoText;
    @InjectView(R.id.restore_password)
    EditText passwordText;
    @InjectView(R.id.restore_button)
    Button restoreButton;

    @Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		if (BuildConfig.DEBUG) Log.d(TAG,"onCreate()");

		firstTime = icicle != null ? icicle.getBoolean(Restore.KEY_FIRST_TIME) : false;
		if (firstTime == false) {
			Bundle extras = getIntent().getExtras();
			firstTime = extras != null ? extras.getBoolean(Restore.KEY_FIRST_TIME) : false;
		}

		frontdoor = new Intent(this, Safe.class);
		frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
		restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

		Passwords.Initialize(this);

		setContentView(R.layout.restore);
        ButterKnife.inject(this);

		String title = getResources().getString(R.string.app_name) + " - " +
			getResources().getString(R.string.restore);
		setTitle(title);
		
		String backupPath = getIntent().getStringExtra(KEY_FILE_PATH);
		if (backupPath != null) {
            restoreFromFile(backupPath);
		} else {
			backupPath = Preferences.getBackupPath(this);
            Intent intent;
            int requestId;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                intent = Intents.createOpenDocumentIntents(Preferences.getBackupDocument(this));
                requestId = REQUEST_RESTORE_DOCUMENT;
            } else {
                intent = Intents.createPickFileIntent(backupPath, R.string.restore_select_file);
                requestId = REQUEST_RESTORE_FILENAME;
            }
			if (intentCallable(intent))
				startActivityForResult(intent, requestId);
			else {
                restoreFromFile(backupPath);
            }
		}
	}

    private void restoreFromFile(String backupPath) {
        filenameText.setText(backupPath);
        try {
            InputStreamData streamData = new InputStreamData(backupPath);
            restore(streamData);
            Preferences.setBackupPathAndMethod(this, backupPath);
        } catch (FileNotFoundException e) {
            updateNoRestoreFileUI();
        }
    }

    @Override
	protected void onPause() {
		super.onPause();
		
		try {
			unregisterReceiver(mIntentReceiver);
		} catch (IllegalArgumentException e) {
			//if (BuildConfig.DEBUG) Log.d(TAG,"IllegalArgumentException");
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if (BuildConfig.DEBUG) Log.d(TAG,"onResume()");
		
		if ((!firstTime) && (CategoryList.isSignedIn()==false)) {
			startActivity(frontdoor);		
			return;
		}
		IntentFilter filter = new IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
		registerReceiver(mIntentReceiver, filter);
	}


	public boolean read(InputStreamData streamData, String masterPassword) {
		if (BuildConfig.DEBUG) Log.d(TAG,"read("+streamData.getFilename()+",)");

		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			SAXParser sp = spf.newSAXParser();

			XMLReader xr = sp.getXMLReader(); 

			RestoreHandler myRestoreHandler = new RestoreHandler();
			xr.setContentHandler(myRestoreHandler); 

			xr.parse(new InputSource(streamData.getStream()));

			restoreDataSet = myRestoreHandler.getParsedData();

		} catch (ParserConfigurationException e) {
			//e.printStackTrace();
			Toast.makeText(Restore.this, getString(R.string.restore_unable_to_open,
				e.getLocalizedMessage()),
				Toast.LENGTH_LONG).show();
			return false;
		} catch (SAXException e) {
			//e.printStackTrace();
			Toast.makeText(Restore.this, getString(R.string.restore_unable_to_open,
				e.getLocalizedMessage()),
				Toast.LENGTH_LONG).show();
			return false;
		} catch (IOException e) {
			//e.printStackTrace();
			Toast.makeText(Restore.this, getString(R.string.restore_unable_to_open,
				e.getLocalizedMessage()),
				Toast.LENGTH_LONG).show();
			return false;
		} 

		if (restoreDataSet.getVersion() != Backup.CURRENT_VERSION) {
			Toast.makeText(Restore.this, getString(R.string.restore_bad_version,
				Integer.toString(restoreDataSet.getVersion())),
				Toast.LENGTH_LONG).show();
			return false;
		}
		CategoryEntry firstCatEntry= null;
		if (restoreDataSet.getCategories().size() > 0) {
			firstCatEntry = restoreDataSet.getCategories().get(0);
			if (firstCatEntry==null) {
				Toast.makeText(Restore.this, getString(R.string.restore_error),
					Toast.LENGTH_LONG).show();
				return false;
			}
		}
		CryptoHelper ch=new CryptoHelper();
		
		String salt=restoreDataSet.getSalt();
		String masterKeyEncrypted=restoreDataSet.getMasterKeyEncrypted();
		masterKey="";
		try {
			ch.init(CryptoHelper.EncryptionStrong, salt);
			ch.setPassword(masterPassword);
			masterKey = ch.decrypt(masterKeyEncrypted);
		} catch (CryptoHelperException e) {
			Log.e(TAG,e.toString());
			Toast.makeText(this, getString(R.string.crypto_error)
				+ e.getMessage(), Toast.LENGTH_SHORT).show();
			return false;
		}
		if (ch.getStatus()==false) {
			Toast.makeText(Restore.this, getString(R.string.restore_decrypt_error),
					Toast.LENGTH_LONG).show();
			Animation shake = AnimationUtils
				.loadAnimation(Restore.this, R.anim.shake);
			findViewById(R.id.restore_password).startAnimation(shake);

			return false;
		}
		ch=new CryptoHelper();
		try {
			ch.init(CryptoHelper.EncryptionMedium, salt);
			ch.setPassword(masterKey);
		} catch (CryptoHelperException e1) {
			e1.printStackTrace();
			Toast.makeText(this, getString(R.string.crypto_error)
				+ e1.getMessage(), Toast.LENGTH_SHORT).show();
			return false;
		}
		
		if (firstCatEntry != null) {
			String firstCategory="";
			try {
				firstCategory = ch.decrypt(firstCatEntry.name);
			} catch (CryptoHelperException e) {
				Log.e(TAG,e.toString());
			}
			if (ch.getStatus() == false) {
				Toast.makeText(Restore.this, getString(R.string.restore_decrypt_error),
					Toast.LENGTH_LONG).show();
				return false;
			}
			if (BuildConfig.DEBUG) Log.d(TAG,"firstCategory="+firstCategory);
		}
		
		dbHelper=new DBHelper(Restore.this);

		String msg=getString(R.string.restore_found, 
				Integer.toString(restoreDataSet.getTotalEntries()),
				restoreDataSet.getDate())
				+"\n"+
				getString(R.string.dialog_restore_database_msg);
		Dialog confirm = new AlertDialog.Builder(Restore.this)
		.setIcon(android.R.drawable.ic_menu_manage)
		.setTitle(R.string.dialog_restore_database_title)
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				restoreDatabase();
			}
		})
		.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dbHelper.close();
			}
		}) 
		.setMessage(msg)
		.create();
		confirm.show();

		return true;
	}

	private void restoreDatabase() {
		dbHelper.beginTransaction();
		dbHelper.deleteDatabase();

		dbHelper.storeSalt(restoreDataSet.getSalt());
		dbHelper.storeMasterKey(restoreDataSet.getMasterKeyEncrypted());
		Master.setSalt(restoreDataSet.getSalt());
		Master.setMasterKey(masterKey);
		for (CategoryEntry category : restoreDataSet.getCategories()) {
			if (BuildConfig.DEBUG) Log.d(TAG,"category="+category.name);
			dbHelper.addCategory(category);
		}
		int totalPasswords=0;
		for (PassEntry password : restoreDataSet.getPass()) {
			totalPasswords++;
			long rowid=dbHelper.addPassword(password);
			if (password.packageAccess!=null) {
				for (String packageName : password.packageAccess) {
					if (BuildConfig.DEBUG) Log.d(TAG,"packageName="+packageName);
					dbHelper.addPackageAccess(rowid, packageName);
				}
			}
		}
		dbHelper.commit();
		dbHelper.close();
		Passwords.Reset();

		Toast.makeText(Restore.this, getString(R.string.restore_complete, 
				Integer.toString(totalPasswords)),
			Toast.LENGTH_LONG).show();
		

		// Don't need to show warning anymore to back up, because user has used
		// restore already.
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sp.edit();
		editor.putBoolean(Preferences.PREFERENCE_FIRST_TIME_WARNING, true);
		editor.commit();

		setResult(RESULT_OK);
		finish();
	}

	@Override
	public void onUserInteraction() {
		super.onUserInteraction();

		if (BuildConfig.DEBUG) Log.d(TAG,"onUserInteraction()");

		if (CategoryList.isSignedIn()==false) {
//			startActivity(frontdoor);
		} else {
			if (restartTimerIntent!=null) sendBroadcast (restartTimerIntent);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent i){
		super.onActivityResult(requestCode, resultCode, i);
		switch(requestCode){
		case REQUEST_RESTORE_FILENAME:
            if(resultCode == RESULT_OK){
				String path = i.getData().getPath();
                restoreFromFile(path);
			} else{
				setResult(RESULT_CANCELED);
				finish();
			}
			break;
            case REQUEST_RESTORE_DOCUMENT:
                if(resultCode == RESULT_OK){
                    Uri documentUri = i.getData();
                    filenameText.setText(i.getDataString());
                    try {
                        InputStreamData streamData = new InputStreamData(documentUri, this);
                        restore(streamData);
                        Preferences.setBackupDocumentAndMethod(this, i.getDataString());
                    } catch (FileNotFoundException e) {
                        updateNoRestoreFileUI();
                    }
                } else{
                    setResult(RESULT_CANCELED);
                    finish();
                }
		}
	}

    private void updateNoRestoreFileUI() {
        passwordText.setVisibility(View.INVISIBLE);
        restoreButton.setVisibility(View.INVISIBLE);
        restoreInfoText.setText(R.string.restore_no_file);
    }

    private void restore(final InputStreamData inputStreamData){

		restoreInfoText.setText(R.string.restore_set_password);
		passwordText.setVisibility(View.VISIBLE);
		restoreButton.setVisibility(View.VISIBLE);

		restoreButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				EditText passwordText;
				passwordText = (EditText) findViewById(R.id.restore_password);

				String masterPassword = passwordText.getText().toString();
				read(inputStreamData, masterPassword);
			}
		});
	}
	
	
	private boolean intentCallable(Intent intent){
		List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,   
				PackageManager.MATCH_DEFAULT_ONLY); 
		return list.size() > 0;
	}
}
