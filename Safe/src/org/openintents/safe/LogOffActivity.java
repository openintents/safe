package org.openintents.safe;

import org.openintents.safe.service.ServiceDispatchImpl;
import org.openintents.util.VersionUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class LogOffActivity extends Activity {
	private Handler mHandler = new Handler();
	
	   @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.log_off);
			ImageView icon = (ImageView) findViewById(R.id.logoff_icon);
			icon.setImageResource(R.drawable.passicon);
			TextView header = (TextView) findViewById(R.id.logoff_header);
			String version = VersionUtils.getVersionNumber(this);
			String appName = VersionUtils.getApplicationName(this);
			String head = appName + " " + version + "\n";
			header.setText(head);
	        Button logoffButton = (Button) findViewById(R.id.logoff_button);
	        Button gotoPWS      = (Button) findViewById(R.id.goto_pws);

	        mHandler.postDelayed(mUpdateTimeTask, 0);
	        
	    	logoffButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View arg0) {
			        
			        /* Clear the clipboard, if it contains the last password used */
			        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			        
			        if (cb.hasText()) {
			            String clipboardText = cb.getText().toString();
			            if (clipboardText.equals(Safe.last_used_password))
			                cb.setText("");
			        }
			        
					Intent serviceIntent = new Intent();
					serviceIntent.setClass(LogOffActivity.this, ServiceDispatchImpl.class );
					stopService(serviceIntent);
					CategoryList.setSignedOut();
					
					/*
					Intent intent = new Intent(LogOffActivity.this, FrontDoor.class);
	    			//intent.setClass (LogOffActivity.this, FrontDoor.class );
	    			intent.addCategory(Intent.CATEGORY_LAUNCHER);
	    			intent.setAction(Intent.ACTION_MAIN);
	    			startActivity(intent);
	    			*/
					finish();
				}});	        
	        
	    	gotoPWS.setOnClickListener(new View.OnClickListener() {
	    		public void onClick(View arg0) {
	    			Intent intent = new Intent(LogOffActivity.this, Safe.class);
	    			//intent.setClass (LogOffActivity.this, FrontDoor.class );
	    			intent.addCategory(Intent.CATEGORY_LAUNCHER);
	    			intent.setAction(Intent.ACTION_MAIN);
	    			startActivity(intent);
	    			finish();
	    		}});
	   }
	
	
	public final Runnable mUpdateTimeTask = new Runnable() {
		public void run() {

			TextView time = (TextView) findViewById(R.id.lock_timeout);
			long millis = ServiceDispatchImpl.timeRemaining;
			int seconds = (int) (millis / 1000) % 60;
			int minutes = (int) (millis / 60000);

			if (seconds < 10) {
				time.setText(getString(R.string.lock_timeout, 
					Integer.toString(minutes) + ":0" + Integer.toString(seconds)));
			} else {
				time.setText(getString(R.string.lock_timeout, 
					Integer.toString(minutes) + ":" + Integer.toString(seconds)));
			}

			mHandler.postDelayed(this, 1000);
		}
	};
}
