package org.openintents.safe.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import org.openintents.safe.Preferences;
import org.openintents.safe.R;

/**
 * This dialog is presented when an external application wants to use OI Safe for
 * encryption.   An example is OI Notepad.  When a user tells Notepad to encrypt
 * a note and OI Safe does not already allow for external access, this dialog will
 * come up.
 */
public class AllowExternalAccessDialog extends AlertDialog implements OnClickListener {
//	private static final String TAG = "FilenameDialog";

    private static final String BUNDLE_TAGS = "tags";

    protected static final int DIALOG_ID_NO_FILE_MANAGER_AVAILABLE = 2;

    Context mContext;

    CheckBox mCheckBox;

    public AllowExternalAccessDialog(Context context) {
        super(context);
        mContext = context;

        setTitle(context.getText(R.string.dialog_title_external_access));
        setButton(BUTTON_POSITIVE, context.getText(android.R.string.ok), this);
        setButton(BUTTON_NEGATIVE, context.getText(android.R.string.cancel), (OnClickListener) null);
        setIcon(android.R.drawable.ic_dialog_alert);

        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_allow_access, null);
        setView(view);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean externalAccess = sp.getBoolean(Preferences.PREFERENCE_ALLOW_EXTERNAL_ACCESS, false);

        mCheckBox = (CheckBox) view.findViewById(R.id.checkbox);
        // mCheckBox.setText(R.string.pref_summary_external_access);
        mCheckBox.setChecked(externalAccess);

    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON_POSITIVE) {
            // User pressed OK
            boolean externalAccess = mCheckBox.isChecked();

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(Preferences.PREFERENCE_ALLOW_EXTERNAL_ACCESS, externalAccess);
            editor.commit();

        }

    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putString(BUNDLE_TAGS, "");
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
//        String tags = savedInstanceState.getString(BUNDLE_TAGS);
    }
}
