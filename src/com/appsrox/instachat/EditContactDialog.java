/*
 * 
 */
package com.appsrox.instachat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputFilter;
import android.text.TextUtils;
import android.widget.EditText;

// TODO: Auto-generated Javadoc
/**
 * The Class EditContactDialog.
 */
public class EditContactDialog extends DialogFragment {
	
	/** The m listener. */
	private OnFragmentInteractionListener mListener;
	
	/**
	 * New instance.
	 *
	 * @return the edits the contact dialog
	 */
	public static EditContactDialog newInstance() {
		EditContactDialog fragment = new EditContactDialog();
		return fragment;
	}
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.DialogFragment#onAttach(android.app.Activity)
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
		}
	}	

	/* (non-Javadoc)
	 * @see android.support.v4.app.DialogFragment#onCreateDialog(android.os.Bundle)
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Context ctx = getActivity();
		final String profileId = getArguments().getString(Common.PROFILE_ID);
		String profileName = getArguments().getString(DataProvider.COL_NAME);
		final EditText et = new EditText(ctx);
		et.setText(profileName);
		et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
			return new AlertDialog.Builder(ctx)
			.setTitle("Edit Contact")
			.setView(et)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String name = et.getText().toString();
					if (TextUtils.isEmpty(name)) return;
					
					ContentValues values = new ContentValues(1);
					values.put(DataProvider.COL_NAME, name);
					ctx.getContentResolver().update(Uri.withAppendedPath(DataProvider.CONTENT_URI_PROFILE, profileId), values, null, null);
					
					mListener.onEditContact(name);
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create();
	}
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.DialogFragment#onDetach()
	 */
	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}
	
	/**
	 * The listener interface for receiving onFragmentInteraction events.
	 * The class that is interested in processing a onFragmentInteraction
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addOnFragmentInteractionListener<code> method. When
	 * the onFragmentInteraction event occurs, that object's appropriate
	 * method is invoked.
	 *
	 * @see OnFragmentInteractionEvent
	 */
	public interface OnFragmentInteractionListener {
		
		/**
		 * On edit contact.
		 *
		 * @param name the name
		 */
		public void onEditContact(String name);
	}	
}