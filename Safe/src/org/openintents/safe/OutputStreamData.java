package org.openintents.safe;

import android.content.Context;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;


public class OutputStreamData {
    private String filename;
    private OutputStream stream;

    public OutputStreamData(String filename) throws FileNotFoundException {
        this.filename = filename;
        this.stream = new FileOutputStream(filename);
    }

    public OutputStreamData(Uri documentUri, Context context) throws FileNotFoundException {
        this.filename = documentUri.toString();
        this.stream = context.getContentResolver().openOutputStream(documentUri);
    }

    public String getFilename() {
        return filename;
    }

    public OutputStream getStream() {
        return stream;
    }
}
