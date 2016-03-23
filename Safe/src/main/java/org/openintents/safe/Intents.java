package org.openintents.safe;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;

public class Intents {
    static android.content.Intent createPickFileIntent(String filename, int titleResource) {
        android.content.Intent intent;
        intent = new android.content.Intent("org.openintents.action.PICK_FILE");
        intent.setData(Uri.parse("file://" + filename));
        intent.putExtra("org.openintents.extra.TITLE", titleResource);
        return intent;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    static android.content.Intent createCreateDocumentIntent() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        return intent;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static android.content.Intent createOpenDocumentIntents(String backupDocument) {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
        if (backupDocument != null) {
            intent.setData(Uri.parse(backupDocument));
        }
        intent.setType("text/*");
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        return intent;
    }
}
