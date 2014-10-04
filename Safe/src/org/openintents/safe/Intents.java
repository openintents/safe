package org.openintents.safe;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;


public class Intents {
    static android.content.Intent createPickFileIntent(String backupPath, int titleResource) {
        android.content.Intent intent;
        intent = new android.content.Intent("org.openintents.action.PICK_FILE");
        intent.setData(Uri.parse("file://" + backupPath));
        intent.putExtra("org.openintents.extra.TITLE", titleResource);
        return intent;
    }

    static android.content.Intent createCreateDocumentIntent() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/*"});
        return intent;
    }

    public static android.content.Intent createOpenDocumentIntents(String backupDocument) {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
        if (backupDocument != null) {
            intent.setData(Uri.parse(backupDocument));
        }
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/*"});
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        return intent;
    }
}
