package org.openintents.safe;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.openintents.safe.model.PassEntry;
import org.openintents.safe.model.Passwords;
import org.openintents.util.SecureDelete;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


class Import extends AsyncTask<String, Void, String> {

    private static final String TAG = "Import";

    @Override
    protected String doInBackground(String... filenames) {
        String response = "";
        importDatabaseFromCSV(filenames[0]);
        return response;
    }

    private String importedFilename = "";
    private int importedEntries = 0;
    private String importMessage = "";

    CategoryList currentActivity = null;

    /**
     * Set the current activity to be used on PostExecute
     */
    public void setActivity(CategoryList categoryList) {
        currentActivity = categoryList;
    }

    /**
     * While running inside a thread, read from a CSV and import into the
     * database.
     */
    private void importDatabaseFromCSV(String filename) {
        currentActivity.maybRestartTimer();
        try {
            importMessage = "";
            importedEntries = 0;

            final int recordLength = 6;
            CSVReader reader = new CSVReader(createReader(filename));
            String[] nextLine;
            nextLine = reader.readNext();
            if (nextLine == null) {
                importMessage = currentActivity.getString(R.string.import_error_first_line);
                return;
            }
            if (nextLine.length < recordLength) {
                importMessage = currentActivity.getString(R.string.import_error_first_line);
                return;
            }
            if ((nextLine[0]
                    .compareToIgnoreCase(currentActivity.getString(R.string.category)) != 0)
                    || (nextLine[1]
                    .compareToIgnoreCase(currentActivity.getString(R.string.description)) != 0)
                    || (nextLine[2]
                    .compareToIgnoreCase(currentActivity.getString(R.string.website)) != 0)
                    || (nextLine[3]
                    .compareToIgnoreCase(currentActivity.getString(R.string.username)) != 0)
                    || (nextLine[4]
                    .compareToIgnoreCase(currentActivity.getString(R.string.password)) != 0)
                    || (nextLine[5]
                    .compareToIgnoreCase(currentActivity.getString(R.string.notes)) != 0)) {
                importMessage = currentActivity.getString(R.string.import_error_first_line);
                return;
            }
            // Log.i(TAG,"first line is valid");

            HashMap<String, Long> categoryToId = Passwords
                    .getCategoryNameToId();
            //
            // take a pass through the CSV and collect any new Categories
            //
            HashMap<String, Long> categoriesFound = new HashMap<String, Long>();
            int categoryCount = 0;
            // int line=0;
            while ((nextLine = reader.readNext()) != null) {
                // line++;
                if (isCancelled()) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "cancelled");
                    }
                    return;
                }
                // nextLine[] is an array of values from the line
                if ((nextLine == null) || (nextLine[0] == "")) {
                    continue; // skip blank categories
                }
                if (categoryToId.containsKey(nextLine[0])) {
                    continue; // don't recreate existing categories
                }
                // if (debug)
                // Log.d(TAG,"line["+line+"] found category ("+nextLine[0]+")");
                Long passwordsInCategory = Long.valueOf(1);
                if (categoriesFound.containsKey(nextLine[0])) {
                    // we've seen this category before, bump its count
                    passwordsInCategory += categoriesFound.get(nextLine[0]);
                } else {
                    // newly discovered category
                    categoryCount++;
                }
                categoriesFound.put(nextLine[0], passwordsInCategory);
                if (categoryCount > CategoryList.MAX_CATEGORIES) {
                    importMessage = currentActivity.getString(R.string.import_too_many_categories);
                    return;
                }
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "found " + categoryCount + " categories");
            }
            if (categoryCount != 0) {
                Set<String> categorySet = categoriesFound.keySet();
                Iterator<String> i = categorySet.iterator();
                while (i.hasNext()) {
                    currentActivity.addCategory(i.next());
                }
            }
            reader.close();

            // re-read the categories to get id's of new categories
            categoryToId = Passwords.getCategoryNameToId();
            //
            // read the whole file again to import the actual fields
            //
            reader = new CSVReader(createReader(filename));
            nextLine = reader.readNext();
            int newEntries = 0;
            int lineNumber = 0;
            String lineErrors = "";
            int lineErrorsCount = 0;
            final int maxLineErrors = 10;
            while ((nextLine = reader.readNext()) != null) {
                lineNumber++;
                // Log.d(TAG,"lineNumber="+lineNumber);

                if (isCancelled()) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "cancelled");
                    }
                    return;
                }

                // nextLine[] is an array of values from the line
                if (nextLine.length < 2) {
                    if (lineErrorsCount < maxLineErrors) {
                        lineErrors += "line "
                                + lineNumber
                                + ": "
                                + currentActivity.getString(R.string.import_not_enough_fields)
                                + "\n";
                        lineErrorsCount++;
                    }
                    continue; // skip if not enough fields
                }
                if (nextLine.length < recordLength) {
                    // if the fields after category and description are
                    // missing,
                    // just fill them in
                    String[] replacement = new String[recordLength];
                    for (int i = 0; i < nextLine.length; i++) {
                        // copy over the fields we did get
                        replacement[i] = nextLine[i];
                    }
                    for (int i = nextLine.length; i < recordLength; i++) {
                        // flesh out the rest of the fields
                        replacement[i] = "";
                    }
                    nextLine = replacement;
                }
                if ((nextLine == null) || (nextLine[0] == "")) {
                    if (lineErrorsCount < maxLineErrors) {
                        lineErrors += "line " + lineNumber + ": "
                                + currentActivity.getString(R.string.import_blank_category)
                                + "\n";
                        lineErrorsCount++;
                    }
                    continue; // skip blank categories
                }
                String description = nextLine[1];
                if ((description == null) || (description == "")) {
                    if (lineErrorsCount < maxLineErrors) {
                        lineErrors += "line "
                                + lineNumber
                                + ": "
                                + currentActivity.getString(R.string.import_blank_description)
                                + "\n";
                        lineErrorsCount++;
                    }
                    continue;
                }

                PassEntry entry = new PassEntry();
                entry.category = categoryToId.get(nextLine[0]);
                entry.plainDescription = description;
                entry.plainWebsite = nextLine[2];
                entry.plainUsername = nextLine[3];
                entry.plainPassword = nextLine[4];
                entry.plainNote = nextLine[5];
                entry.id = 0;
                Passwords.putPassEntry(entry);
                newEntries++;
            }
            reader.close();
            if (lineErrors != "") {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, lineErrors);
                }
            }

            importedEntries = newEntries;
            if (newEntries == 0) {
                importMessage = currentActivity.getString(R.string.import_no_entries);
                return;
            } else {
                importMessage = currentActivity.getString(R.string.added) + " "
                        + newEntries + " " + currentActivity.getString(R.string.entries);
                importedFilename = filename;
            }
        } catch (IOException e) {
            e.printStackTrace();
            importMessage = currentActivity.getString(R.string.import_file_error);
        }
    }

    private Reader createReader(String filename) throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            InputStream inputStream = currentActivity.getContentResolver().openInputStream(Uri.parse(filename));
            return new BufferedReader(new InputStreamReader(
                    inputStream));

        } else {
            return new FileReader(filename);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (currentActivity == null) {
            return;
        }
        if (importMessage != "") {
            currentActivity.showResultToast(importMessage);
        }
        if (importedFilename != "") {
            String deleteMsg = currentActivity.getString(R.string.import_delete_csv) + " "
                    + importedFilename + "?";
            Dialog about = new AlertDialog.Builder(currentActivity)
                    .setIcon(R.drawable.passicon)
                    .setTitle(R.string.import_complete)
                    .setPositiveButton(
                            R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    File csvFile = new File(
                                            importedFilename
                                    );
                                    // csvFile.delete();
                                    SecureDelete.delete(csvFile);
                                    importedFilename = "";
                                }
                            }
                    )
                    .setNegativeButton(
                            R.string.no,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                }
                            }
                    ).setMessage(deleteMsg).create();
            about.show();
        }

        if ((importedEntries != 0) || (CategoryList.importDeletedDatabase)) {
            if (currentActivity != null) {
                currentActivity.fillData();
                if (currentActivity.importProgress != null) {
                    currentActivity.importProgress.dismiss();
                }
            }
        }
        CategoryList.taskImport = null;
    }
}
