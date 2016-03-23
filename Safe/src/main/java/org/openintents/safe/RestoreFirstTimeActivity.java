package org.openintents.safe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.openintents.util.VersionUtils;

public class RestoreFirstTimeActivity extends Activity {
    private Button restore;
    private Button cancel;
    private String path;

    private static final int REQUEST_RESTORE = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restore_first_time);
        path = PreferenceActivity.getBackupPath(this);
        restore = (Button) findViewById(R.id.restore);
        cancel = (Button) findViewById(R.id.cancel);

        ((TextView) findViewById(R.id.filename)).setText(path);
        restore.setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        Intent i = new Intent(RestoreFirstTimeActivity.this, Restore.class);
                        i.putExtra(Restore.KEY_FILE_PATH, path);
                        i.putExtra(Restore.KEY_FIRST_TIME, true);
                        startActivityForResult(i, REQUEST_RESTORE);
                    }
                }
        );
        cancel.setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
        );

		/* Copied from AskPassword.java - normalInit() */
        TextView header = (TextView) findViewById(R.id.entry_header);
        String version = VersionUtils.getVersionNumber(this);
        String appName = VersionUtils.getApplicationName(this);
        String head = appName + " " + version;
        header.setText(head);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i) {
        super.onActivityResult(requestCode, resultCode, i);
        switch (requestCode) {
            case REQUEST_RESTORE:
                setResult(resultCode);
                finish();
                break;
        }
    }
}
