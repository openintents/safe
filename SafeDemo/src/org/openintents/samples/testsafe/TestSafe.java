package org.openintents.samples.testsafe;

import org.openintents.intents.CryptoIntents;
import org.openintents.safe.service.ServiceDispatch;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class TestSafe extends Activity {
	public final String TAG="SERVICE_TEST";
	public final Integer ENCRYPT_REQUEST = 1;
	public final Integer DECRYPT_REQUEST = 2;
	public final Integer GET_PASSWORD_REQUEST = 3;
	public final Integer SET_PASSWORD_REQUEST = 4;
	public final Integer SPOOF_REQUEST = 5;
	public final String desc = "opensocial";
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		EditText inputText = (EditText) findViewById(R.id.input_entry);
		inputText.setText(desc,
			android.widget.TextView.BufferType.EDITABLE);
		
// ---------------- clicky
		Button encryptIntentButton = (Button) findViewById(R.id.encrypti);
		Button decryptIntentButton = (Button) findViewById(R.id.decrypti);
		Button getButton           = (Button) findViewById(R.id.get);
		Button setButton           = (Button) findViewById(R.id.set);
		Button outToInButton       = (Button) findViewById(R.id.outToIn);
		Button spoofme             = (Button) findViewById(R.id.spoof_me);
		
		encryptIntentButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				clickMaster (ENCRYPT_REQUEST);
			}});
		decryptIntentButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				clickMaster (DECRYPT_REQUEST);
			}});
		getButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				clickMaster (GET_PASSWORD_REQUEST);
			}});
		setButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				clickMaster (SET_PASSWORD_REQUEST);
			}});
		//move output text box to input:
		outToInButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				EditText outputText = (EditText) findViewById(R.id.output_entry);
				EditText inputText = (EditText) findViewById(R.id.input_entry);
				String newInputStr = outputText.getText().toString();
				inputText.setText(newInputStr, android.widget.TextView.BufferType.EDITABLE);
				
			}});	
		spoofme.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				Intent i = new Intent();
				i.setAction(Intent.ACTION_MAIN);
				i.setClassName("org.openintents.safe", "org.openintents.safe.AskPassword");
				startActivityForResult(i, SPOOF_REQUEST);
			}});	
		
    }//oncreate

    private void clickMaster (Integer request) {
    	CheckBox checkbox = (CheckBox) findViewById(R.id.check_service);
    	if (checkbox.isChecked()) {
    		clickMasterService(request);
    	} else {
    		clickMasterIntent(request);
    	}
    }

    private int serviceRequest;
    
    private void clickMasterService (Integer request) {
    	serviceRequest = request;
    	
    	initService(); // calls clickMasterServiceDispatch() upon connection
    	
    	//releaseService();
    }
    
    private void clickMasterServiceDispatch () {
		EditText inputText = (EditText) findViewById(R.id.input_entry);
		String inputStr = inputText.getText().toString();
		
		try {
			String masterKey = service.getPassword();
			String resultText = "?";
			
			if (serviceRequest == ENCRYPT_REQUEST) {
				resultText = service.encrypt(inputStr);
	    	} else if (serviceRequest == DECRYPT_REQUEST) {
				resultText = service.decrypt(inputStr);
	    	} else {
				Toast.makeText(TestSafe.this,
						"This is not yet supported!",
						Toast.LENGTH_SHORT).show();
	    	}
	
			EditText outputText = (EditText) findViewById(R.id.output_entry);
			outputText.setText(resultText, android.widget.TextView.BufferType.EDITABLE);
		} catch (RemoteException e) {
			Log.e(TAG, "Remote exception", e);
		}
	}
    
    private void clickMasterIntent (Integer request) {
		EditText inputText = (EditText) findViewById(R.id.input_entry);
		String inputStr = inputText.getText().toString();
		
		Intent i = new Intent();
		i.putExtra(CryptoIntents.EXTRA_TEXT, inputStr);
		
    	if (request == ENCRYPT_REQUEST) {
            i.setAction(CryptoIntents.ACTION_ENCRYPT);
    	} else if (request == DECRYPT_REQUEST) {
            i.setAction(CryptoIntents.ACTION_DECRYPT);
    	} else if (request == GET_PASSWORD_REQUEST) {
    		i.putExtra(CryptoIntents.EXTRA_UNIQUE_NAME, inputStr);
    		i.setAction (CryptoIntents.ACTION_GET_PASSWORD);
    	} else if (request == SET_PASSWORD_REQUEST) {
    		String uniqueNameStr = ((EditText) findViewById(R.id.unique_name_entry)).getText().toString();
    		String passwordStr = ((EditText) findViewById(R.id.password_entry)).getText().toString();
    		String usernameStr = ((EditText) findViewById(R.id.username_entry)).getText().toString();


    		i.putExtra(CryptoIntents.EXTRA_UNIQUE_NAME, uniqueNameStr);
    		i.putExtra(CryptoIntents.EXTRA_PASSWORD, passwordStr);
    		i.putExtra(CryptoIntents.EXTRA_USERNAME, usernameStr);

    		i.setAction (CryptoIntents.ACTION_SET_PASSWORD);
    	}
        try {
        	startActivityForResult(i, request);
        } catch (ActivityNotFoundException e) {
			Log.e(TAG, "failed to invoke intent: " + e.toString());
        }
    }
    
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
    	String resultText = "";
    	if (resultCode != RESULT_OK) {
    		resultText = "An error occured while contacting OI Safe. Does it allow remote access? (Please check in the settings of OI Safe).";
    	} else {
    		if (requestCode == ENCRYPT_REQUEST || requestCode == DECRYPT_REQUEST) {
    			resultText = data.getStringExtra (CryptoIntents.EXTRA_TEXT);
    		} else if (requestCode == SET_PASSWORD_REQUEST) {
    			resultText = "Request to set password sent.";
    		} else if (requestCode == GET_PASSWORD_REQUEST){
    			String uname = data.getStringExtra (CryptoIntents.EXTRA_USERNAME);
    			String pwd = data.getStringExtra (CryptoIntents.EXTRA_PASSWORD);
    			resultText = uname + ":" + pwd;
    		} else if (requestCode == SPOOF_REQUEST) {
    			resultText = data.getStringExtra("masterKey");
    		}
    	}
		EditText outputText = (EditText) findViewById(R.id.output_entry);
		outputText.setText(resultText, android.widget.TextView.BufferType.EDITABLE);
    }
    


	//--------------------------- service stuff ------------

	// service elements
    private ServiceDispatch service;
    private ServiceDispatchConnection conn;
    
	private void initService() {

        String action = getIntent().getAction();
        boolean isLocal = action == null || action.equals(Intent.ACTION_MAIN);
		conn = new ServiceDispatchConnection(isLocal);
		Intent i = new Intent();
		i.setClassName("org.openintents.safe", "org.openintents.safe.service.ServiceDispatchImpl");
		try {
			startService(i);
			bindService( i, conn, Context.BIND_AUTO_CREATE);
		} catch (SecurityException e) {
			Log.e(TAG, "SecurityException", e);
			Toast.makeText(TestSafe.this,
					"SecurityException: Not allowed to connect!",
					Toast.LENGTH_SHORT).show();
		}
	}

	private void releaseService() {
		if (conn != null ) {
			unbindService( conn );
			conn = null;
		}
	}

	class ServiceDispatchConnection implements ServiceConnection
	{
		boolean askPassIsLocal = false;
		public ServiceDispatchConnection (Boolean isLocal) {
			askPassIsLocal = isLocal;
		}
		public void onServiceConnected(ComponentName className, 
				IBinder boundService )
		{
			service = ServiceDispatch.Stub.asInterface((IBinder)boundService);
			
			boolean promptforpassword = getIntent().getBooleanExtra(CryptoIntents.EXTRA_PROMPT, true);
			
			try {
				if (service.getPassword() == null) {

        			Toast.makeText(TestSafe.this,
        					"Service not running.",
        					Toast.LENGTH_SHORT).show();
				} else {
					//service already started, so don't need to ask pw.
					clickMasterServiceDispatch();
				}
			} catch (RemoteException e) {
				Log.d(TAG, e.toString());
			}
			Log.d( TAG,"onServiceConnected" );
		}
		
		public void onServiceDisconnected(ComponentName className)
		{
			service = null;
			Log.d( TAG,"onServiceDisconnected" );
		}
	};
}
