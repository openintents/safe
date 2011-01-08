/* $Id$
 * 
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

import java.util.ArrayList;

import org.openintents.intents.CryptoIntents;
import org.openintents.safe.SimpleGestureFilter.SimpleGestureListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

/**
 * PassView Activity
 * 
 * @author Randy McEoin
 */
public class PassView extends Activity implements SimpleGestureListener {

	private static boolean debug = false;
	private static String TAG = "PassView";

	public static final int EDIT_PASSWORD_INDEX = Menu.FIRST;
	public static final int DEL_PASSWORD_INDEX = Menu.FIRST + 1;

	public static final int REQUEST_EDIT_PASS = 1;
	
	private Long RowId;
	private Long CategoryId;
	public static boolean entryEdited=false;
	private long[] rowids=null;
	private int listPosition=-1;

	ViewFlipper flipper;
	private SimpleGestureFilter detector;
	private static int ANIMATION_DURATION=300;
	private boolean usernameCopiedToClipboard=false;
	
	Intent frontdoor;
    private Intent restartTimerIntent=null;

    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)) {
            	 if (debug) Log.d(TAG,"caught ACTION_CRYPTO_LOGGED_OUT");
            	 startActivity(frontdoor);
            }
        }
    };

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		if (debug) Log.d(TAG,"onCreate("+icicle+")");
		
		frontdoor = new Intent(this, Safe.class);
		frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
		restartTimerIntent = new Intent (CryptoIntents.ACTION_RESTART_TIMER);

		String title = getResources().getString(R.string.app_name) + " - "
				+ getResources().getString(R.string.view_entry);
		setTitle(title);
		
		RowId = icicle != null ? icicle.getLong(PassList.KEY_ID) : null;
		if (RowId == null) {
			Bundle extras = getIntent().getExtras();
			RowId = extras != null ? extras.getLong(PassList.KEY_ID) : null;
		}
		CategoryId = icicle != null ? icicle.getLong(PassList.KEY_CATEGORY_ID) : null;
		if (CategoryId == null) {
			Bundle extras = getIntent().getExtras();
			CategoryId = extras != null ? extras.getLong(PassList.KEY_CATEGORY_ID) : null;
		}
		rowids = icicle != null ? icicle.getLongArray(PassList.KEY_ROWIDS) : null;
		if (rowids == null) {
			Bundle extras = getIntent().getExtras();
			rowids = extras != null ? extras.getLongArray(PassList.KEY_ROWIDS) : null;
		}
		listPosition = icicle != null ? icicle.getInt(PassList.KEY_LIST_POSITION) : -1;
		if (listPosition == -1) {
			Bundle extras = getIntent().getExtras();
			listPosition = extras != null ? extras.getInt(PassList.KEY_LIST_POSITION) : -1;
		}

		if (debug) Log.d(TAG,"RowId="+RowId+" CategoryId="+CategoryId+" rowids="+rowids+
				" listPosition="+listPosition);
		if ((RowId==null) || (CategoryId==null) || (rowids==null) || (listPosition==-1) ||
				(RowId<1) || (CategoryId<1)) {
			// invalid Row or Category
			finish();
			return;
		}

		if (debug) {
			for (int i=0; i<rowids.length; i++) {
				Log.d(TAG,"rowids["+i+"]="+rowids[i]);
			}
		}
		if (debug) Log.d(TAG,"rowids.length="+rowids.length);

		if (debug) Log.d(TAG,"creating flipper");
		flipper = new ViewFlipper(this);
		View currentView = createView(listPosition,null);
		flipper.addView(currentView);

		if (listPosition>0) { // is there a previous?
			if (debug) Log.d(TAG,"add previous");
			View prevView = createView(listPosition-1,null);
			flipper.addView(prevView,0);
			flipper.showNext();
		}

		// are we starting at the end and we have more than 2 entries?
		if (((listPosition+1)==rowids.length) && (rowids.length > 2)) { 
			if (debug) Log.d(TAG,"add prev prev");
			View prevView = createView(listPosition-2,null);
			flipper.addView(prevView,0);
			flipper.showNext();
		}

		if (rowids.length > (listPosition+1)) {  // is there a next?
			if (debug) Log.d(TAG,"add next");
			View nextView = createView(listPosition+1,null);
			if (debug) Log.d(TAG,"flipper ChildCount="+flipper.getChildCount());
			flipper.addView(nextView, flipper.getChildCount());
		}

		// are we starting at the start and we have more than 2 entries?
		if ((listPosition==0) && (rowids.length > 2)) { 
			if (debug) Log.d(TAG,"add next next");
			View nextView = createView(listPosition+2,null);
			flipper.addView(nextView,flipper.getChildCount());
		}

		detector = new SimpleGestureFilter(this,this);
		
		setContentView(flipper);
		if (debug) Log.d(TAG,"flipper.getChildCount="+flipper.getChildCount());

		entryEdited=false;

	}
	private View createView(int position, View view) {
		if (view==null) {
			LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.pass_view,null);
		}
		populateFields(rowids[position],view);
		Button previousButton = (Button) view.findViewById(R.id.prev_pass);
		if (position>0) {	// is there a previous?
			previousButton.setEnabled(true);
			previousButton.setOnClickListener(new prevButtonListener());
		} else {
			previousButton.setEnabled(false);
		}
		Button nextButton = (Button) view.findViewById(R.id.next_pass);
		if (rowids.length > (position+1)) {  // is there a next?
			nextButton.setEnabled(true);
			nextButton.setOnClickListener(new nextButtonListener());
		} else {
			nextButton.setEnabled(false);
		}
		
		Button goButton = (Button) view.findViewById(R.id.go);
		goButton.setOnClickListener(new goButtonListener());

		TextView usernameText = (TextView) view.findViewById(R.id.username);
		usernameText.setOnClickListener(new usernameTextListener());
		TextView passwordText = (TextView) view.findViewById(R.id.password);
		passwordText.setOnClickListener(new passwordTextListener());
		return view;
	}
	
	class goButtonListener implements View.OnClickListener {
		public void onClick(View arg0) {
			View current=flipper.getCurrentView();
			TextView passwordText;
    		TextView websiteText;
    		websiteText = (TextView) current.findViewById(R.id.website);
    		passwordText = (TextView) current.findViewById(R.id.password);

			String link = websiteText.getText().toString();
			if (link == null || link.equals("") || link.equals("http://")) {
				return;
			}

			if (usernameCopiedToClipboard==false) {
				// don't copy the password if username was already copied
				clipboard(getString(R.string.password), passwordText.getText().toString());
			}

			Intent i = new Intent(Intent.ACTION_VIEW);
			Uri u = Uri.parse(link);
			i.setData(u);
			try {
				startActivity(i);
			} catch (ActivityNotFoundException e) {
				// Let's try to catch the most common mistake: omitting http:
				u = Uri.parse("http://" + link);
				i.setData(u);
				try {
					startActivity(i);
				} catch (ActivityNotFoundException e2) {
					Toast.makeText(PassView.this, R.string.invalid_website,
						Toast.LENGTH_SHORT).show();
				}
			}
		}
	}
	
	private void showPreviousView(boolean isRightLeft) {
		if (listPosition==0) {
			// already at the beginning
			return;
		}
		if (isRightLeft) {
			flipper.setInAnimation(inFromLeftAnimation());
			flipper.setOutAnimation(outToRightAnimation());
		} else {
			flipper.setInAnimation(inFromTopAnimation());
			flipper.setOutAnimation(outToBottomAnimation());
		}
		flipper.showPrevious();
		listPosition--;
		RowId=rowids[listPosition];
		int displayedChild=flipper.getDisplayedChild();
		if (debug) Log.d(TAG,"previousButton displayedChild="+displayedChild+" listPosition="+listPosition);
		if ((displayedChild==0) && (listPosition>0)) {
			View last=flipper.getChildAt(flipper.getChildCount()-1);
			flipper.removeViewAt(flipper.getChildCount()-1);
			View prevView = createView(listPosition-1, last);
			flipper.addView(prevView,0);
			flipper.showNext();
		}
	}
	class prevButtonListener implements View.OnClickListener {
		public void onClick(View arg0) {
			if (debug) Log.d(TAG,"previousButton getDisplayedChild="+flipper.getDisplayedChild());
			showPreviousView(true);
		}
	}
	private void showNextView(boolean isRightLeft) {
		if ((listPosition+1)==rowids.length) {
			// already at the end
			return;
		}
		int displayedChild=flipper.getDisplayedChild();
		if (isRightLeft) {
			flipper.setInAnimation(inFromRightAnimation());
			flipper.setOutAnimation(outToLeftAnimation());
		} else {
			flipper.setInAnimation(inFromBottomAnimation());
			flipper.setOutAnimation(outToTopAnimation());
		}
		flipper.showNext();
		listPosition++;
		RowId=rowids[listPosition];
		// did we move beyond something more than the 2nd entry?
		if ((rowids.length>3) && (displayedChild>0) && (rowids.length-listPosition>1)){
			View first=flipper.getChildAt(0);
			flipper.removeViewAt(0);
			View nextView = createView(listPosition+1, first);
			flipper.addView(nextView,flipper.getChildCount());
		}
	}
	class nextButtonListener implements View.OnClickListener {
		public void onClick(View arg0) {
			if (debug) Log.d(TAG,"nextButton getDisplayedChild="+flipper.getDisplayedChild());
			showNextView(true);
		}
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (RowId != null) {
			outState.putLong(PassList.KEY_ID, RowId);
		} else {
			outState.putLong(PassList.KEY_ID, -1);
		}
		outState.putLong(PassList.KEY_CATEGORY_ID, CategoryId);
		outState.putLongArray(PassList.KEY_ROWIDS, rowids);
		outState.putInt(PassList.KEY_LIST_POSITION, listPosition);
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (debug) Log.d(TAG,"onResume()");

		try {
			unregisterReceiver(mIntentReceiver);
		} catch (IllegalArgumentException e) {
			//if (debug) Log.d(TAG,"IllegalArgumentException");
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (debug) Log.d(TAG,"onResume()");

		if (CategoryList.isSignedIn()==false) {
			startActivity(frontdoor);
			return;
		}
        IntentFilter filter = new IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        registerReceiver(mIntentReceiver, filter);

		Passwords.Initialize(this);

		// update in case it was edited
//		View current=this.flipper.getCurrentView();
//		populateFields(RowId,current);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, EDIT_PASSWORD_INDEX, 0, R.string.password_edit)
			.setIcon(android.R.drawable.ic_menu_edit).setShortcut('1', 'e');
		menu.add(0, DEL_PASSWORD_INDEX, 0, R.string.password_delete)
			.setIcon(android.R.drawable.ic_menu_delete).setShortcut('2', 'd');

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Prompt the user with a dialog asking them if they really want
	 * to delete the password.
	 */
	public void deletePassword(){
		Dialog about = new AlertDialog.Builder(this)
			.setIcon(R.drawable.passicon)
			.setTitle(R.string.dialog_delete_password_title)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					deletePassword2();
				}
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// do nothing
				}
			}) 
			.setMessage(R.string.dialog_delete_password_msg)
			.create();
		about.show();
	}
	
	/**
	 * Follow up for the Delete Password dialog.  If we have a RowId then
	 * delete the password, otherwise just finish this Activity.
	 */
	public void deletePassword2(){
		if ((RowId != null) && (RowId > 0)) {
			delPassword(RowId);
		} else {
			// user specified to delete a new entry
			// so simply exit out
			finish();
		}
	}
	/**
	 * Delete the password entry from the database given the row id within the
	 * database.
	 * 
	 * @param Id
	 */
	private void delPassword(long Id) {
		Passwords.deletePassEntry(Id);
		setResult(RESULT_OK);
		finish();
	}

	/**
	 * Handler for when a MenuItem is selected from the Activity.
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case EDIT_PASSWORD_INDEX:
			Intent i = new Intent(getApplicationContext(), PassEdit.class);
			i.putExtra(PassList.KEY_ID, RowId);
			i.putExtra(PassList.KEY_CATEGORY_ID, CategoryId);
			startActivityForResult(i, REQUEST_EDIT_PASS);
			break;
		case DEL_PASSWORD_INDEX:
			deletePassword();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
		
	/**
	 * 
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent i) {
		super.onActivityResult(requestCode, resultCode, i);

		if (debug) Log.d(TAG,"onActivityResult()");
		if (requestCode == REQUEST_EDIT_PASS) {
	    	if (resultCode==PassEdit.RESULT_DELETED) {
				entryEdited=true;
	    		finish();
	    	}
			if ((resultCode == RESULT_OK) || (PassEdit.entryEdited)){
				View current=this.flipper.getCurrentView();
				populateFields(RowId,current);
				entryEdited=true;
			}
		}
	}

	/**
	 * 
	 */
	private void populateFields(long rowIdx, View view) {
		if (debug) Log.d(TAG,"populateFields("+rowIdx+","+view+")");
		if (rowIdx > 0) {
			if (debug) Log.d(TAG,"rowIdx="+rowIdx);
			PassEntry row = Passwords.getPassEntry(rowIdx, true, false);
			if (row==null) {
				if (debug) Log.d(TAG,"populateFields: row=null");
				return;
			}
    		ArrayList<String> packageAccess = Passwords.getPackageAccess(rowIdx);
    		
    		TextView descriptionText;
    		TextView passwordText;
    		TextView usernameText;
    		TextView websiteText;
    		TextView noteText;
    		TextView lastEditedText;
    		TextView uniqueNameText;
    		TextView packageAccessText;

    		descriptionText = (TextView) view.findViewById(R.id.description);
    		websiteText = (TextView) view.findViewById(R.id.website);
    		usernameText = (TextView) view.findViewById(R.id.username);
    		passwordText = (TextView) view.findViewById(R.id.password);
    		noteText = (TextView) view.findViewById(R.id.note);
    		lastEditedText = (TextView) view.findViewById(R.id.last_edited);
    		uniqueNameText = (TextView) view.findViewById(R.id.uniquename);
    		packageAccessText = (TextView) view.findViewById(R.id.packageaccess);

			descriptionText.setText(row.plainDescription);
			websiteText.setText(row.plainWebsite);
			usernameText.setText(row.plainUsername);
			passwordText.setText(row.plainPassword);
			noteText.setText(row.plainNote);
			String lastEdited;
			if (row.lastEdited!=null) {
				lastEdited=row.lastEdited;
			} else {
				lastEdited=getString(R.string.last_edited_unknown);
			}
			lastEditedText.setText(getString(R.string.last_edited)+": "+lastEdited);
			if (row.plainUniqueName!=null) {
				uniqueNameText.setText(getString(R.string.uniquename)+
						": "+row.plainUniqueName);
			}
			String packages="";
			if (packageAccess!=null) {
				for (String packageName : packageAccess) {
					if (debug) Log.d(TAG,"packageName="+packageName);
					PackageManager pm=getPackageManager();
					String appLabel="";
					try {
						ApplicationInfo ai=pm.getApplicationInfo(packageName,0);
						appLabel=pm.getApplicationLabel(ai).toString();
					} catch (NameNotFoundException e) {
						appLabel="("+getString(R.string.not_found)+")";
					}
					packages+=packageName+" "+appLabel+" ";
				}
			}
			if (packages!="") {
				packageAccessText.setText(getString(R.string.package_access)+
						": "+packages);
			}
		}
	}

	/**
	 * 
	 * @author Billy Cui
	 * refactored by Randy McEoin
	 */
	class usernameTextListener implements View.OnClickListener {
		public void onClick(View arg0) {
			TextView usernameText = (TextView) arg0.findViewById(R.id.username);
			if (debug) Log.d(TAG, "click " + usernameText.getText());
			clipboard(getString(R.string.username),usernameText.getText().toString());
			usernameCopiedToClipboard=true;
		}
	}
	class passwordTextListener implements View.OnClickListener {
		public void onClick(View arg0) {
			TextView passwordText = (TextView) arg0.findViewById(R.id.password);
			if (debug) Log.d(TAG, "click " + passwordText.getText());
			clipboard(getString(R.string.password),passwordText.getText().toString());
		}
	}
	/**
	 * Copy to clipboard and toast to let user know that we have done so.
	 * 
	 * @author Billy Cui
	 * @param fieldName Name of the field copied from
	 * @param value String to copy to clipboard
	 */
	private void clipboard(String fieldName, String value) {
		Toast.makeText(PassView.this, fieldName+" "+getString(R.string.copied_to_clipboard),
				Toast.LENGTH_SHORT).show();

		ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		cb.setText(value);
	}

	@Override
	public void onUserInteraction() {
		super.onUserInteraction();

		if (debug) Log.d(TAG,"onUserInteraction()");

		if (CategoryList.isSignedIn()==false) {
//			startActivity(frontdoor);
		}else{
			if (restartTimerIntent!=null) sendBroadcast (restartTimerIntent);
		}
	}
	private Animation inFromRightAnimation() {
		Animation inFromRight = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, +1.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromRight.setDuration(ANIMATION_DURATION);
		inFromRight.setInterpolator(new AccelerateInterpolator());
		return inFromRight;
	}

	private Animation outToLeftAnimation() {
		Animation outtoLeft = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, -1.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f);
			outtoLeft.setDuration(ANIMATION_DURATION);
		outtoLeft.setInterpolator(new AccelerateInterpolator());
		return outtoLeft;
	}

	private Animation inFromLeftAnimation() {
		Animation inFromLeft = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, -1.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f);
			inFromLeft.setDuration(ANIMATION_DURATION);
		inFromLeft.setInterpolator(new AccelerateInterpolator());
		return inFromLeft;
	}

	private Animation outToRightAnimation() {
		Animation outtoRight = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, +1.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f);
			outtoRight.setDuration(ANIMATION_DURATION);
		outtoRight.setInterpolator(new AccelerateInterpolator());
		return outtoRight;
	}
	
	private Animation inFromBottomAnimation() {
		Animation inFromBottom = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, +1.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromBottom.setDuration(ANIMATION_DURATION);
		inFromBottom.setInterpolator(new AccelerateInterpolator());
		return inFromBottom;
	}

	private Animation outToTopAnimation() {
		Animation outtoTop = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, -1.0f);
			outtoTop.setDuration(ANIMATION_DURATION);
		outtoTop.setInterpolator(new AccelerateInterpolator());
		return outtoTop;
	}

	private Animation inFromTopAnimation() {
		Animation inFromTop = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, -1.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f);
			inFromTop.setDuration(ANIMATION_DURATION);
		inFromTop.setInterpolator(new AccelerateInterpolator());
		return inFromTop;
	}

	private Animation outToBottomAnimation() {
		Animation outToBottom = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, +1.0f);
			outToBottom.setDuration(ANIMATION_DURATION);
		outToBottom.setInterpolator(new AccelerateInterpolator());
		return outToBottom;
	}
	@Override 
	public boolean dispatchTouchEvent(MotionEvent me){ 
		this.detector.onTouchEvent(me);
		return super.dispatchTouchEvent(me); 
	}

	public void onSwipe(int direction) {
		switch (direction) {
		case SimpleGestureFilter.SWIPE_RIGHT:
			showPreviousView(true);
			break;
		case SimpleGestureFilter.SWIPE_DOWN:
			showPreviousView(false);
			break;
		case SimpleGestureFilter.SWIPE_LEFT:
			showNextView(true);
			break;
		case SimpleGestureFilter.SWIPE_UP:
			showNextView(false);
			break;
		} 
	}

	public void onDoubleTap() {
	}
}
