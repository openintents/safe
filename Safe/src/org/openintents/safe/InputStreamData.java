package org.openintents.safe;

import android.content.Context;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class InputStreamData {
    private String filename;
    private InputStream stream;

    public InputStreamData(String filename) throws FileNotFoundException {
        this.filename = filename;
        this.stream = new FileInputStream(filename);
    }

    public InputStreamData(Uri documentUri, Context context) throws FileNotFoundException {
        this.filename = documentUri.toString();
        this.stream = context.getContentResolver().openInputStream(documentUri);
    }

    public String getFilename() {
        return filename;
    }

    public InputStream getStream() {
        return stream;
    }
}
