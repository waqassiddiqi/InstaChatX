/*
 * 
 */
package com.appsrox.instachat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

// TODO: Auto-generated Javadoc
/**
 * The Class MainActivity.
 */
public class MainActivity extends ActionBarActivity implements
		LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {
	
	/** The disclaimer. */
	private AlertDialog disclaimer;
	
	/** The list view. */
	ListView listView;
	
	/** The action bar. */
	private ActionBar actionBar;
	
	/** The Contact cursor adapter. */
	private ContactCursorAdapter ContactCursorAdapter;
	
	/** The photo cache. */
	public static PhotoCache photoCache;

	/* (non-Javadoc)
	 * @see android.support.v7.app.ActionBarActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		listView = (ListView) findViewById(R.id.contactslist);
		listView.setOnItemClickListener(this);
		ContactCursorAdapter = new ContactCursorAdapter(this, null);
		listView.setAdapter(ContactCursorAdapter);
		actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#3483A1")));
		
		getSupportActionBar().setTitle((Html.fromHtml("<font color=\"#FFFFFF\">" + getString(R.string.app_name) + "</font>")));
		
		actionBar.show();
		photoCache = new PhotoCache(this);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME,
				ActionBar.DISPLAY_SHOW_CUSTOM);
		
		actionBar.setTitle((Html.fromHtml("<font color=\"#FFFFFF\">" + "You are" + "</font>")));
		actionBar.setSubtitle((Html.fromHtml("<font color=\"#FFFFFF\">" + Common.getDisplayName() + "</font>")));
		
		//Attach this activity with load manager to fetch Cursor from profile table
		getSupportLoaderManager().initLoader(0, null, this);		
	}
	

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		Intent intent = null;
		
		switch (item.getItemId()) {
		case R.id.action_add:
			AddContactDialog newFragment = AddContactDialog.newInstance();
			newFragment.show(getSupportFragmentManager(), "AddContactDialog");
			return true;
			
		case R.id.action_settings:
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
			
		case R.id.action_send:
			intent = new Intent(this, BroadcastActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Starts ChatAcitvity.
	 *
	 * @param arg0 the arg0
	 * @param view the view
	 * @param arg2 the arg2
	 * @param arg3 the arg3
	 */
	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int arg2, long arg3) {
		Intent intent = new Intent(this, ChatActivity.class);
		intent.putExtra(Common.PROFILE_ID, String.valueOf(arg3));
		startActivity(intent);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		if (disclaimer != null)
			disclaimer.dismiss();
		super.onDestroy();
	}

	/**
	 * fetch profile ID, email and display name of contacts to display as list.
	 *
	 * @param id the id
	 * @param args the args
	 * @return the loader
	 */
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader loader = new CursorLoader(this,
				DataProvider.CONTENT_URI_PROFILE, new String[] {
						DataProvider.COL_ID, DataProvider.COL_NAME,
						DataProvider.COL_EMAIL, DataProvider.COL_COUNT }, null,
				null, DataProvider.COL_ID + " DESC");
		
		
		
		return loader;
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
	 */
	@Override
	public void onLoadFinished(android.support.v4.content.Loader<Cursor> arg0,
			Cursor arg1) {
		ContactCursorAdapter.swapCursor(arg1);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
	 */
	@Override
	public void onLoaderReset(android.support.v4.content.Loader<Cursor> arg0) {
		ContactCursorAdapter.swapCursor(null);
	}

	/**
	 * Class to display available contacts in ListView.
	 */
	public class ContactCursorAdapter extends CursorAdapter {

		/** The m inflater. */
		private LayoutInflater mInflater;

		/**
		 * Instantiates a new contact cursor adapter.
		 *
		 * @param context the context
		 * @param c the c
		 */
		public ContactCursorAdapter(Context context, Cursor c) {
			super(context, c, 0);
			this.mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		/* (non-Javadoc)
		 * @see android.support.v4.widget.CursorAdapter#getCount()
		 */
		@Override
		public int getCount() {
			return getCursor() == null ? 0 : super.getCount();
		}

		/**
		 * Create new view by inflating main_list_item layout.
		 *
		 * @param context the context
		 * @param cursor the cursor
		 * @param parent the parent
		 * @return the view
		 */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View itemLayout = mInflater.inflate(R.layout.main_list_item,
					parent, false);
			ViewHolder holder = new ViewHolder();
			itemLayout.setTag(holder);
			holder.text1 = (TextView) itemLayout.findViewById(R.id.text1);
			holder.text2 = (TextView) itemLayout.findViewById(R.id.text2);
			holder.textEmail = (TextView) itemLayout
					.findViewById(R.id.textEmail);
			holder.avatar = (ImageView) itemLayout.findViewById(R.id.avatar);
			return itemLayout;
		}
		
		/**
		 * Bind values to listview row.
		 *
		 * @param view the view
		 * @param context the context
		 * @param cursor the cursor
		 */
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.text1.setText(cursor.getString(cursor
					.getColumnIndex(DataProvider.COL_NAME)));
			holder.textEmail.setText(cursor.getString(cursor
					.getColumnIndex(DataProvider.COL_EMAIL)));
			int count = cursor.getInt(cursor
					.getColumnIndex(DataProvider.COL_COUNT));
			if (count > 0) {
				holder.text2.setVisibility(View.VISIBLE);
				holder.text2.setText(String.format("%d new message%s", count,
						count == 1 ? "" : "s"));
			} else
				holder.text2.setVisibility(View.GONE);

			photoCache.DisplayBitmap(requestPhoto(cursor.getString(cursor
					.getColumnIndex(DataProvider.COL_EMAIL))), holder.avatar);

		}
	}

	/**
	 * The Class ViewHolder.
	 */
	private static class ViewHolder {
		
		/** The text1. */
		TextView text1;
		
		/** The text2. */
		TextView text2;
		
		/** The text email. */
		TextView textEmail;
		
		/** The avatar. */
		ImageView avatar;
	}

	/**
	 * Gets the associated profile image by querying Android Contacts.
	 *
	 * @param email the email
	 * @return the uri
	 */
	@SuppressLint("InlinedApi")
	private Uri requestPhoto(String email) {
		Cursor emailCur = null;
		Uri uri = null;
		try {
			int SDK_INT = android.os.Build.VERSION.SDK_INT;
			if (SDK_INT >= 11) {
				String[] projection = { ContactsContract.CommonDataKinds.Email.PHOTO_URI };
				ContentResolver cr = getContentResolver();
				emailCur = cr
						.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
								projection,
								ContactsContract.CommonDataKinds.Email.ADDRESS
										+ " = ?", new String[] { email }, null);
				if (emailCur != null && emailCur.getCount() > 0) {
					if (emailCur.moveToNext()) {
						String photoUri = emailCur
								.getString(emailCur
										.getColumnIndex(ContactsContract.CommonDataKinds.Email.PHOTO_URI));
						if (photoUri != null)
							uri = Uri.parse(photoUri);
					}
				}
			} else if (SDK_INT < 11) {
				String[] projection = { ContactsContract.CommonDataKinds.Photo.CONTACT_ID };
				ContentResolver cr = getContentResolver();
				emailCur = cr
						.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
								projection,
								ContactsContract.CommonDataKinds.Email.ADDRESS
										+ " = ?", new String[] { email }, null);
				if (emailCur.moveToNext()) {
					int columnIndex = emailCur
							.getColumnIndex(ContactsContract.CommonDataKinds.Photo.CONTACT_ID);
					long contactId = emailCur.getLong(columnIndex);
					uri = ContentUris.withAppendedId(
							ContactsContract.Contacts.CONTENT_URI, contactId);
					uri = Uri.withAppendedPath(uri,
							ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (emailCur != null)
					emailCur.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return uri;
	}
}
