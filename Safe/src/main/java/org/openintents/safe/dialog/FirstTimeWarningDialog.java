package org.openintents.safe.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v7.app.AlertDialog;
import android.widget.CheckBox;

import org.openintents.safe.R;

public class FirstTimeWarningDialog extends AlertDialog implements OnClickListener {
//	private static final String TAG = "FilenameDialog";

//    private static final String BUNDLE_TAGS = "tags";

    protected static final int DIALOG_ID_NO_FILE_MANAGER_AVAILABLE = 2;

    Context mContext;

    CheckBox mCheckBox;

    public FirstTimeWarningDialog(Context context) {
        super(context);
        mContext = context;

        setTitle(context.getText(R.string.dialog_title_first_time_warning));
        setButton(BUTTON_POSITIVE, context.getText(android.R.string.ok), (OnClickListener) null);
        //setButton2(context.getText(android.R.string.cancel), (OnClickListener) null);
        setIcon(android.R.drawable.ic_dialog_alert);
        setMessage(context.getText(R.string.dialog_summary_first_time_warning));

    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON_POSITIVE) {
            // User pressed OK
        }

    }
}
