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
package org.openintents.safe.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class RestoreDataSet {

    private static boolean debug = false;
    private static final String TAG = "RestoreDataSet";

    private int version = 0;
    private String date = null;
    private String salt = null;
    private String masterKeyEncrypted = null;
    private Long currentCategoryId = Long.valueOf(0);
    private CategoryEntry currentCategory = null;
    private ArrayList<CategoryEntry> categoryEntries = new ArrayList<>();
    private PassEntry currentEntry = null;
    private String currentRowID;
    private String currentPackageAccess;
    private ArrayList<PassEntry> passEntries = new ArrayList<>();
    private int totalEntries = 0;

    public int getVersion() {
        return version;
    }

    public void setVersion(int extractedVersion) {
        version = extractedVersion;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String extractedDate) {
        date = extractedDate;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String extractedSalt) {
        salt = extractedSalt;
    }

    public String getMasterKeyEncrypted() {
        return masterKeyEncrypted;
    }

    public void setMasterKeyEncrypted(String extractedKey) {
        masterKeyEncrypted = extractedKey;
    }

    public ArrayList<CategoryEntry> getCategories() {
        return categoryEntries;
    }

    public void newCategory(String extractedCategory) {
        currentCategory = new CategoryEntry();
        currentCategoryId++;
        currentCategory.id = currentCategoryId;
        currentCategory.name = extractedCategory;
    }

    public void storyCategory() {
        if (currentCategory != null) {
            categoryEntries.add(currentCategory);
            currentCategory = null;
        }
    }

    public ArrayList<PassEntry> getPass() {
        return passEntries;
    }

    public void newEntry() {
        currentEntry = new PassEntry();
        currentEntry.category = currentCategoryId;
        currentRowID = "";
        currentEntry.description = "";
        currentEntry.website = "";
        currentEntry.username = "";
        currentEntry.password = "";
        currentEntry.note = "";
        currentEntry.uniqueName = "";
        currentEntry.packageAccess = null;
        currentPackageAccess = "";
    }

    public void storeEntry() {
        // only add an entry if we had all the fields
        if (debug) {
            Log.d(
                    TAG, currentEntry.description + " " + currentEntry.website + " " +
                            currentEntry.username + " " + currentEntry.password + " " +
                            currentEntry.note + " " + currentPackageAccess
            );
        }
        if ((currentEntry != null) &&
                (currentEntry.description != "")) {
            try {
                currentEntry.id = Long.parseLong(currentRowID);
            } catch (NumberFormatException e) {
                currentEntry.id = 0;
            }
            if (currentPackageAccess != "") {
                // strip the brackets [ and ]
                String packageList = currentPackageAccess.substring(
                        1,
                        currentPackageAccess.length() - 1
                );
                String[] packages = packageList.split(",");
                currentEntry.packageAccess = new ArrayList<String>(Arrays.asList(packages));
                if (debug) {
                    Log.d(TAG, "packageAccess=" + currentEntry.packageAccess.toString());
                }
            }
            passEntries.add(currentEntry);
            totalEntries++;
        }
        currentEntry = null;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setRowID(String extractedRowID) {
        if (debug) {
            Log.d(TAG, "setRowID(" + extractedRowID + ")");
        }
        if (currentEntry != null) {
            currentRowID += extractedRowID;
        }
    }

    public void setDescription(String extractedDescription) {
        if (debug) {
            Log.d(TAG, "setDescription(" + extractedDescription + ")");
        }
        if (currentEntry != null) {
            currentEntry.description += extractedDescription;
        }
    }

    public void setWebsite(String extractedWebsite) {
        if (debug) {
            Log.d(TAG, "setWebsite(" + extractedWebsite + ")");
        }
        if (currentEntry != null) {
            currentEntry.website += extractedWebsite;
        }
    }

    public void setUsername(String extractedUsername) {
        if (currentEntry != null) {
            currentEntry.username += extractedUsername;
        }
    }

    public void setPassword(String extractedPassword) {
        if (currentEntry != null) {
            currentEntry.password += extractedPassword;
        }
    }

    public void setNote(String extractedNote) {
        if (currentEntry != null) {
            currentEntry.note += extractedNote;
        }
    }

    public void setUniqueName(String extractedUniqueName) {
        if (currentEntry != null) {
            currentEntry.uniqueName += extractedUniqueName;
        }
    }

    public void setPackageAccess(String extractedPackageAccess) {
        if (debug) {
            Log.d(TAG, "setPackageAccess(" + extractedPackageAccess + ")");
        }
        if (currentPackageAccess != null) {
            currentPackageAccess += extractedPackageAccess;
        }
    }
}
