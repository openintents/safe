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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openintents.intents.CryptoIntents;

import org.openintents.safe.model.PassEntry;
import org.openintents.safe.model.Passwords;
import org.openintents.safe.wrappers.CheckWrappers;
import org.openintents.safe.wrappers.honeycomb.WrapActionBar;

/**
 * PassList Activity
 * <p/>
 * This is the main activity for PasswordSafe all other activities are
 * spawned as sub-activities of this one.  The basic application
 * skeleton was based on google's notepad example.
 *
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class PassList extends ListActivity {

    private static final boolean debug = false;
    private static final String TAG = "PassList";

    // Menu Item order
    public static final int VIEW_PASSWORD_INDEX = Menu.FIRST;
    public static final int EDIT_PASSWORD_INDEX = Menu.FIRST + 1;
    public static final int ADD_PASSWORD_INDEX = Menu.FIRST + 2;
    public static final int DEL_PASSWORD_INDEX = Menu.FIRST + 3;
    public static final int MOVE_PASSWORD_INDEX = Menu.FIRST + 4;

    public static final int REQUEST_VIEW_PASSWORD = 1;
    public static final int REQUEST_EDIT_PASSWORD = 2;
    public static final int REQUEST_ADD_PASSWORD = 3;
    public static final int REQUEST_MOVE_PASSWORD = 4;

    protected static final int MSG_UPDATE_LIST = 0x101;

    public static final String KEY_ID = "id";  // Intent keys
    public static final String KEY_CATEGORY_ID = "categoryId";  // Intent keys
    public static final String KEY_ROWIDS = "rowids";
    public static final String KEY_LIST_POSITION = "position";

    private Long CategoryId = null;

    Intent frontdoor;
    private Intent restartTimerIntent = null;

    private static fillerTask taskFiller = null;
    private ProgressDialog decryptProgress = null;

    private List<PassEntry> rows = null;
    private int lastPosition = 0;

    // passDescriptions is updated by the background thread
    List<String> passDescriptions = new ArrayList<String>();
    // passDescriptions4Adapter must only be modified by the UI thread
    List<String> passDescriptions4Adapter = new ArrayList<String>();

    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)) {
                if (debug) {
                    Log.d(TAG, "caught ACTION_CRYPTO_LOGGED_OUT");
                }
                startActivity(frontdoor);
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (debug) {
            Log.d(TAG, "onCreate(" + icicle + ")");
        }

        CategoryId = icicle != null ? icicle.getLong(CategoryList.KEY_ID) : null;
        if (CategoryId == null) {
            Bundle extras = getIntent().getExtras();
            CategoryId = extras != null ? extras.getLong(CategoryList.KEY_ID) : null;
        }
        if (debug) {
            Log.d(TAG, "CategoryId=" + CategoryId);
        }
        if ((CategoryId == null) || (CategoryId < 1)) {
            finish();    // no valid category
            return;
        }

        frontdoor = new Intent(this, Safe.class);
        frontdoor.setAction(CryptoIntents.ACTION_AUTOLOCK);
        restartTimerIntent = new Intent(CryptoIntents.ACTION_RESTART_TIMER);

        setContentView(R.layout.pass_list);

        final ListView list = getListView();
        list.setFocusable(true);
        list.setOnCreateContextMenuListener(this);
        list.setTextFilterEnabled(true);
        registerForContextMenu(list);

        if (CheckWrappers.mActionBarAvailable) {
            WrapActionBar bar = new WrapActionBar(this);
            bar.setDisplayHomeAsUpEnabled(true);
        }

        sendBroadcast(restartTimerIntent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (debug) {
            Log.d(TAG, "onSaveInstanceState(): CategoryId=" + CategoryId);
        }
        // remember which Category we're looking at
        if (CategoryId != null) {
            outState.putLong(CategoryList.KEY_ID, CategoryId);
        } else {
            outState.putLong(CategoryList.KEY_ID, -1);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (debug) {
            Log.d(TAG, "onPause()");
        }
        try {
            unregisterReceiver(mIntentReceiver);
        } catch (IllegalArgumentException e) {
            //if (debug) Log.d(TAG,"IllegalArgumentException");
        }
        if (taskFiller != null) {
            decryptProgress.dismiss();
            taskFiller.setActivity(null);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (debug) {
            Log.d(TAG, "onResume()");
        }

        if (!CategoryList.isSignedIn()) {
            startActivity(frontdoor);
            return;
        }
        IntentFilter filter = new IntentFilter(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        registerReceiver(mIntentReceiver, filter);

        Passwords.Initialize(this);

        String categoryName = Passwords.getCategoryEntry(CategoryId).plainName;
        String title = getResources().getString(R.string.app_name) + " - " +
                getResources().getString(R.string.passwords) + " -" +
                categoryName;
        setTitle(title);

        if (taskFiller != null) {
            // taskFiller still running
            taskFiller.setActivity(this);
            startDecryptProgressDialog();
        }
        ListAdapter la = getListAdapter();
        if (la != null) {
            if (debug) {
                Log.d(TAG, "onResume: count=" + la.getCount());
            }
        } else {
            if (debug) {
                Log.d(TAG, "onResume: no list");
            }
            /* HACK to make textFilter work!!!
             * It somehow doesn't work, when there's an empty Adapter after the onResume.
			 */
//			List<String> l = new ArrayList<String>();
//			l.add("");
//			setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, l));
			/* /HACK */
            fillData();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (debug) {
            Log.d(TAG, "onStop()");
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenuInfo menuInfo) {

        AdapterView.AdapterContextMenuInfo info;
        info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        menu.setHeaderTitle(rows.get(info.position).plainDescription);
        menu.add(0, VIEW_PASSWORD_INDEX, 0, R.string.password_view)
                .setIcon(android.R.drawable.ic_menu_view)
                .setAlphabeticShortcut('v');
        menu.add(0, EDIT_PASSWORD_INDEX, 0, R.string.password_edit)
                .setIcon(android.R.drawable.ic_menu_edit)
                .setAlphabeticShortcut('e');
        menu.add(0, DEL_PASSWORD_INDEX, 0, R.string.password_delete)
                .setIcon(android.R.drawable.ic_menu_delete)
                .setAlphabeticShortcut('d');
        menu.add(0, MOVE_PASSWORD_INDEX, 0, R.string.move)
                .setIcon(android.R.drawable.ic_menu_more)
                .setAlphabeticShortcut('m');
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        onOptionsItemSelected(item);
        return true;
    }

    private void startDecryptProgressDialog() {
        if (decryptProgress == null) {
            decryptProgress = new ProgressDialog(this);
            decryptProgress.setMessage(getString(R.string.decrypt_progress));
            decryptProgress.setIndeterminate(false);
            decryptProgress.setCancelable(true);
        }
        decryptProgress.show();
    }

    /**
     * Populates the password ListView
     */
    private void fillData() {
        if (taskFiller != null) {
            // there's already a running filler
            return;
        }
        startDecryptProgressDialog();
        taskFiller = new fillerTask();
        taskFiller.setActivity(this);
        taskFiller.execute(new String[]{null});
    }

    private class fillerTask extends AsyncTask<String, Void, String> {
        PassList currentActivity = null;

        public void setActivity(PassList act) {
            currentActivity = act;
        }

        @Override
        protected String doInBackground(String... unused) {

            if (debug) {
                Log.d(TAG, "CategoryId=" + CategoryId);
            }
            rows = Passwords.getPassEntries(CategoryId, true, true);
            passDescriptions.clear();
            if (rows != null) {
                Iterator<PassEntry> passIter = rows.iterator();
                while (passIter.hasNext()) {
                    PassEntry passEntry = passIter.next();
                    passDescriptions.add(passEntry.plainDescription);
                }
            }
            if (debug) {
                Log.d(TAG, "doInBackground complete");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (currentActivity != null) {
                // because the activity can change on orientation change, we
                // need to be sure to set values
                // on the currentActivity. Otherwise the user can change
                // orientation while we're working on the background
                // task and we won't be setting values against the activity
                // that's being displayed
                passDescriptions4Adapter.clear();
                passDescriptions4Adapter.addAll(passDescriptions);
                ArrayAdapter<String> entries = new ArrayAdapter<String>(
                        PassList.this, android.R.layout.simple_list_item_1,
                        passDescriptions4Adapter
                );
                currentActivity.setListAdapter(entries);
                currentActivity.rows = rows;
                if (debug) {
                    Log.d(TAG, "entries.getCount=" + entries.getCount());
                }
                if (debug) {
                    Log.d(TAG, "lastPosition=" + currentActivity.lastPosition);
                }
                if (currentActivity.lastPosition > 2) {
                    currentActivity
                            .setSelection(currentActivity.lastPosition - 1);
                    currentActivity.lastPosition = 0;
                }
                if (currentActivity.decryptProgress != null) {
                    currentActivity.decryptProgress.dismiss();
                }
            }

            taskFiller = null;
        }
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (restartTimerIntent != null) {
            sendBroadcast(restartTimerIntent);
        }

        if (menu == null) {
            return super.onMenuOpened(featureId, menu);
        }
        MenuItem miDel = menu.findItem(DEL_PASSWORD_INDEX);
        MenuItem miMove = menu.findItem(MOVE_PASSWORD_INDEX);
        if (getSelectedItemPosition() > -1) {
            miDel.setEnabled(true);
            miMove.setEnabled(true);
        } else {
            miDel.setEnabled(false);
            miMove.setEnabled(false);
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem item = menu.add(0, ADD_PASSWORD_INDEX, 0, R.string.password_add);
        item.setShortcut('2', 'a');
        if (CheckWrappers.mActionBarAvailable) {
            item.setIcon(R.drawable.ic_menu_add_password);
            WrapActionBar.showIfRoom(item);
        } else {
            item.setIcon(android.R.drawable.ic_menu_add);
        }

        menu.add(0, DEL_PASSWORD_INDEX, 0, R.string.password_delete)
                .setIcon(android.R.drawable.ic_menu_delete)
                .setShortcut('3', 'd');

        menu.add(0, MOVE_PASSWORD_INDEX, 0, R.string.move)
                .setIcon(android.R.drawable.ic_menu_more)
                .setShortcut('4', 'm');

        return super.onCreateOptionsMenu(menu);
    }

    private void addPassword() {
        Intent i = new Intent(this, PassEdit.class);
        i.putExtra(PassList.KEY_ID, (long) -1);
        i.putExtra(PassList.KEY_CATEGORY_ID, CategoryId);
        startActivityForResult(i, REQUEST_ADD_PASSWORD);
    }

    /**
     * Prompt the user with a dialog asking them if they really want
     * to delete the password.
     */
    public void deletePassword(final int position) {
        Dialog about = new AlertDialog.Builder(this)
                .setIcon(R.drawable.passicon)
                .setTitle(R.string.dialog_delete_password_title)
                .setPositiveButton(
                        R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                deletePassword2(position);
                            }
                        }
                )
                .setNegativeButton(
                        R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // do nothing
                            }
                        }
                )
                .setMessage(R.string.dialog_delete_password_msg)
                .create();
        about.show();
    }

    /**
     * Follow up for the Delete Password dialog.  If we have a RowId then
     * delete the password, otherwise just finish this Activity.
     */
    public void deletePassword2(int position) {
        try {
            lastPosition = position;
            delPassword(rows.get(position).id);
        } catch (IndexOutOfBoundsException e) {
            // This should only happen when there are no
            // entries to delete.
            Log.w(TAG, e.toString());
        }
    }

    private void delPassword(long Id) {
        Passwords.deletePassEntry(Id);
        fillData();
    }

    /**
     * Prompt the user with Categories to move the specified
     * password to and then update the password entry accordingly.
     *
     * @param passwordId
     */
    private void movePassword(final long passwordId) {
        final HashMap<String, Long> categoryToId = Passwords.getCategoryNameToId();
        String categoryName = Passwords.getCategoryEntry(CategoryId).plainName;
        categoryToId.remove(categoryName);
        Set<String> categories = categoryToId.keySet();
        final String[] items = (String[]) categories.toArray(new String[categories.size()]);
        Arrays.sort(items, String.CASE_INSENSITIVE_ORDER);

        new AlertDialog.Builder(PassList.this)
                .setTitle(R.string.move_select)
                .setItems(
                        items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                long newCategoryId = categoryToId.get(items[which]);
                                Passwords.updatePassCategory(passwordId, newCategoryId);
                                String result = getString(R.string.moved_to, items[which]);
                                Toast.makeText(
                                        PassList.this, result,
                                        Toast.LENGTH_LONG
                                ).show();
                                fillData();
                            }
                        }
                )
                .show();
    }

    public static long[] getRowsIds(List<PassEntry> rows) {
        if (debug) {
            Log.d(TAG, "getRowsIds() rows=" + rows);
        }
        if (rows != null) {
            long[] ids = new long[rows.size()];
            Iterator<PassEntry> passIter = rows.iterator();
            int i = 0;
            while (passIter.hasNext()) {
                ids[i] = passIter.next().id;
                i++;
            }
            return ids;
        } else {
            return null;
        }
    }

    private void viewPassword(int position) {
        Intent vi = new Intent(this, PassView.class);
        vi.putExtra(KEY_ID, rows.get(position).id);
        vi.putExtra(KEY_CATEGORY_ID, CategoryId);
        vi.putExtra(KEY_ROWIDS, getRowsIds(rows));
        vi.putExtra(KEY_LIST_POSITION, position);
        startActivityForResult(vi, REQUEST_VIEW_PASSWORD);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (restartTimerIntent != null) {
            sendBroadcast(restartTimerIntent);
        }
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = -1;
        if (info == null) {
            position = getSelectedItemPosition();
        } else {
            // used when this is called from a ContextMenu
            position = info.position;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, CategoryList.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            case ADD_PASSWORD_INDEX:
                addPassword();
                break;
            case VIEW_PASSWORD_INDEX:
                viewPassword(position);
                lastPosition = position;
                break;
            case EDIT_PASSWORD_INDEX:
                Intent i = new Intent(this, PassEdit.class);
                i.putExtra(KEY_ID, rows.get(position).id);
                i.putExtra(KEY_CATEGORY_ID, CategoryId);
                startActivityForResult(i, REQUEST_EDIT_PASSWORD);
                lastPosition = position;
                break;
            case DEL_PASSWORD_INDEX:
                deletePassword(position);
                break;
            case MOVE_PASSWORD_INDEX:
                movePassword(rows.get(position).id);
                lastPosition = position;
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        viewPassword(position);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i) {
        super.onActivityResult(requestCode, resultCode, i);
        //Log.d(TAG, "onActivityResult. requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (((requestCode == REQUEST_VIEW_PASSWORD) && (PassView.entryEdited)) ||
                ((requestCode == REQUEST_EDIT_PASSWORD) && (PassEditFragment.entryEdited)) ||
                ((requestCode == REQUEST_ADD_PASSWORD) && (PassEditFragment.entryEdited)) ||
                (resultCode == RESULT_OK)) {
            fillData();
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        if (debug) {
            Log.d(TAG, "onUserInteraction()");
        }

        if (!CategoryList.isSignedIn()) {
//			startActivity(frontdoor);
        } else {
            if (restartTimerIntent != null) {
                sendBroadcast(restartTimerIntent);
            }
        }
    }

    public boolean onSearchRequested() {
        Intent search = new Intent(this, Search.class);
        startActivity(search);
        return true;
    }
}
