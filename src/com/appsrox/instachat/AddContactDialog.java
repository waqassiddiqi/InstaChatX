/*
 * 
 */
package com.appsrox.instachat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * The Class AddContactDialog.
 */
public class AddContactDialog extends DialogFragment {
	
	/** The alert dialog. */
	private AlertDialog alertDialog;
	
	/** The et. */
	private EditText et;

	/**
	 * New instance.
	 *
	 * @return the adds the contact dialog
	 */
	public static AddContactDialog newInstance() {
		AddContactDialog fragment = new AddContactDialog();
		return fragment;
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.DialogFragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	
	/**
	 * Create custom AlertDialog using the add_contact_dialog layout
	 * add associate handler on OK button tap to add contact in profile table.
	 *
	 * @param savedInstanceState the saved instance state
	 * @return the dialog
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		View view = inflater.inflate(R.layout.add_contact_dialog, null);
		
		builder.setView(view);
		builder.setPositiveButton(android.R.string.ok, null);
		builder.setNegativeButton(android.R.string.cancel, null);
		alertDialog = builder.create();		

		et = (EditText) view.findViewById(R.id.txtContact);
		
		alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button okBtn = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
				okBtn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String email = et.getText().toString();
						if (!isEmailValid(email)) {
							et.setError("Invalid email!");
							return;
						}
						try {
							ContentValues values = new ContentValues(2);
							values.put(DataProvider.COL_NAME, email.substring(0, email.indexOf('@')));
							values.put(DataProvider.COL_EMAIL, email);
							values.put(DataProvider.COL_ACCEPTED, 1);
							getActivity().getContentResolver().insert(DataProvider.CONTENT_URI_PROFILE, values);
						} catch (SQLException sqle) {}
						alertDialog.dismiss();
					}
				});
			}
		});
		
		return alertDialog;
	}

	/**
	 * Checks if is email valid.
	 *
	 * @param email the email
	 * @return true, if is email valid
	 */
	private boolean isEmailValid(CharSequence email) {
		return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
	}	
}