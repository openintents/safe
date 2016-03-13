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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

public class CryptoContentProvider extends ContentProvider {

    private static final boolean debug = false;
    private static final String TAG = "CryptoContentProvider";

    public static CryptoHelper ch = null;

    public static final String AUTHORITY = "org.openintents.safe";
    public static final Uri CONTENT_URI
            = Uri.parse("content://" + AUTHORITY);

    public static final String SESSION_FILE = "session";

    private static final int ENCRYPT_ID = 2;
    private static final int DECRYPT_ID = 3;
    private static final int DECRYPT_FILE_ID = 4;

    private static final UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "encrypt/*", ENCRYPT_ID);
        sUriMatcher.addURI(AUTHORITY, "decrypt/*", DECRYPT_ID);
        sUriMatcher.addURI(AUTHORITY, "decryptfile", DECRYPT_FILE_ID); // [Peli] can be renamed to "decrypt/*" if first two options are made obsolete
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public int delete(Uri uri, String s, String[] as) {
        // not supported
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // return file extension (uri.lastIndexOf("."))
        return null; //mMimeTypes.getMimeType(uri.toString());
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentvalues) {
        // not supported
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
        /*
        if (uri.toString().startsWith(
				MIME_TYPE_PREFIX)) {
			MatrixCursor c = new MatrixCursor(new String[] { Images.Media.DATA,
					Images.Media.MIME_TYPE });
			// data = absolute path = uri - content://authority/mimetype
			String data = uri.toString().substring(20 + AUTHORITY.length());
			String mimeType = mMimeTypes.getMimeType(data);
			c.addRow(new String[] { data, mimeType });
			return c;
		} else {
			throw new RuntimeException("Unsupported uri");
		}
		*/
        return null;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        if (debug) {
            Log.d(TAG, "openFile(" + uri.toString() + "," + mode + ")");
        }

        ParcelFileDescriptor pfd = null;
        try {
            // get the /files/ directory for our application, which
            // for us is /data/data/org.openintents.safe/files/
            String filesDir = getContext().getFilesDir().toString();

            String path = filesDir;
            String cryptSession;
            String sessionFile;
            String originalFile;
            int modeBits = 0;
            switch (sUriMatcher.match(uri)) {
                case ENCRYPT_ID:
                    if (debug) {
                        Log.d(TAG, "openFile: ENCRYPT");
                    }
                    modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY |
                            ParcelFileDescriptor.MODE_CREATE;
                    cryptSession = uri.getPathSegments().get(1);
                    sessionFile = SESSION_FILE + "." + cryptSession;
                    path += "/" + sessionFile;
                    break;
                case DECRYPT_ID:
                    if (debug) {
                        Log.d(TAG, "openFile: DECRYPT");
                    }
                    modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
                    cryptSession = uri.getPathSegments().get(1);
                    sessionFile = SESSION_FILE + "." + cryptSession;
                    path += "/" + sessionFile;
                    break;
                case DECRYPT_FILE_ID:
                    if (debug) {
                        Log.d(TAG, "openFile: DECRYPT_FILE");
                    }
                    modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
                    originalFile = "file://" + uri.getQueryParameter("file");
                    String sessionKey = uri.getQueryParameter("sessionkey");
                    // TODO: Check that sessionKey is valid.

                    // Decrypt file
                    //				CryptoHelper ch = ServiceDispatchImpl.ch; // Use the global crypto helper that is connected to the single service we have.

                    if (ch == null) {
                        if (debug) {
                            Log.d(TAG, "OI Safe currently logged out.");
                        }
                        return null;
                    }

                    if (!sessionKey.equals(ch.getCurrentSessionKey())) {
                        if (debug) {
                            Log.d(TAG, "Session keys do not match! " + sessionKey + " != " + ch.getCurrentSessionKey());
                        }
                        return null;
                    }

                    if (debug) {
                        Log.d(TAG, "Original file path: " + originalFile);
                    }
                    if (CategoryList.isSignedIn() == false) {
                        Intent frontdoor = new Intent(getContext(), Safe.class);
                        frontdoor.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getContext().startActivity(frontdoor);
                        throw new CryptoHelperException("Not logged in.");
                    }

                    if (debug) {
                        Log.d(TAG, "Decrypt..");
                    }
                    Uri newuri = ch.decryptFileWithSessionKeyThroughContentProvider(this.getContext(), Uri.parse(originalFile));
                    cryptSession = newuri.getPathSegments().get(1);
                    sessionFile = SESSION_FILE + "." + cryptSession;
                    path += "/" + sessionFile;
                    if (debug) {
                        Log.d(TAG, "New path: " + path);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown URI " + uri);
            }

            if (debug) {
                Log.d(TAG, "openFile: path=" + path);
            }
            pfd = ParcelFileDescriptor.open(new File(path), modeBits);
            // This is pretty sneaky.  After opening the file,
            // we'll immediately delete the file.
            // Files are not truly deleted if there is an open file
            // handle.   The file will not be deleted until the
            // file handle is closed.   And then it magically
            // disappears.   This makes for a one time use
            // content provider.
            if (!getContext().deleteFile(sessionFile)) {
                if (debug) {
                    Log.e(TAG, "openFile: unable to delete: " + sessionFile);
                }
            }
        } catch (FileNotFoundException e) {
            if (debug) {
                Log.d(TAG, "openFile: FileNotFound");
            }
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (CryptoHelperException e) {
            if (debug) {
                Log.d(TAG, "openFile: CryptoHelperException:" + e.getLocalizedMessage());
            }
            pfd = null;
            //throw e;
        }

        return pfd;
    }

    @Override
    public int update(Uri uri, ContentValues contentvalues, String s,
                      String[] as) {
        // not supported
        return 0;
    }

}
