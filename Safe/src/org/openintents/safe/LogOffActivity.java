package org.openintents.safe;

import org.openintents.safe.service.ServiceDispatchImpl;
import org.openintents.util.VersionUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class LogOffActivity extends Activity {
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
}
