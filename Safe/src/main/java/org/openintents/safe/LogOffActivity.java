package org.openintents.safe;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.openintents.safe.service.AutoLockService;
import org.openintents.util.VersionUtils;

public class LogOffActivity extends AppCompatActivity {

    private Handler mHandler = new Handler();
    public final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {

            TextView time = (TextView) findViewById(R.id.lock_timeout);
            long millis = AutoLockService.getTimeRemaining();
            int seconds = (int) (millis / 1000) % 60;
            int minutes = (int) (millis / 60000);

            if (seconds < 10) {
                time.setText(
                        getString(
                                R.string.lock_timeout,
                                Integer.toString(minutes) + ":0" + Integer.toString(seconds)
                        )
                );
            } else {
                time.setText(
                        getString(
                                R.string.lock_timeout,
                                Integer.toString(minutes) + ":" + Integer.toString(seconds)
                        )
                );
            }

            mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_off);
        TextView header = (TextView) findViewById(R.id.logoff_header);
        String version = VersionUtils.getVersionNumber(this);
        String appName = VersionUtils.getApplicationName(this);
        String head = appName + " " + version;
        header.setText(head);
        Button logoffButton = (Button) findViewById(R.id.logoff_button);
        Button gotoPWS = (Button) findViewById(R.id.goto_pws);

        mHandler.postDelayed(mUpdateTimeTask, 0);

        logoffButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {

				/* Clear the clipboard, if it contains the last password used */
                        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                        if (cb != null && cb.hasPrimaryClip()) {
                            String clipboardText = cb.getPrimaryClip().toString();
                            if (clipboardText.equals(Safe.last_used_password)) {
                                cb.setPrimaryClip(null);
                            }
                        }

                        Intent autoLockIntent = new Intent(getApplicationContext(), AutoLockService.class);
                        stopService(autoLockIntent);

                        CategoryList.setSignedOut();

				/*
                Intent intent = new Intent(LogOffActivity.this, FrontDoor.class);
				//intent.setClass (LogOffActivity.this, FrontDoor.class );
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.setAction(Intent.ACTION_MAIN);
				startActivity(intent);
				*/
                        finish();
                    }
                }
        );

        gotoPWS.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View arg0) {
                        Intent intent = new Intent(LogOffActivity.this, Safe.class);
                        //intent.setClass (LogOffActivity.this, FrontDoor.class );
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        intent.setAction(Intent.ACTION_MAIN);
                        startActivity(intent);
                        finish();
                    }
                }
        );
    }
}
