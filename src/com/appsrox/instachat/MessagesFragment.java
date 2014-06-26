/*
 * 
 */
package com.appsrox.instachat;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.appsrox.instachat.client.ServerUtilities;
import com.appsrox.instachat.model.Message;
import com.google.gson.Gson;

// TODO: Auto-generated Javadoc
/**
 * Chat fragment holding a single conversation.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class MessagesFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	/** The Constant sdf. */
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/** The Constant df. */
	private static final DateFormat[] df = new DateFormat[] {
		DateFormat.getDateInstance(), DateFormat.getTimeInstance()};

	/** The m listener. */
	private OnFragmentInteractionListener mListener;
	
	/** The chat cursor adapter. */
	private CursorAdapter chatCursorAdapter;
	
	/** The now. */
	private Date now;
	
	/** The profile id. */
	private String profileId;
	
	/** The scheduler. */
	private ScheduledExecutorService scheduler;
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
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
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		now = new Date();
		chatCursorAdapter = new ChatCursorAdapter(getActivity(), null);
		setListAdapter(chatCursorAdapter);
	}	

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setDivider(null);
		getListView().setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		getListView().setStackFromBottom(true);		
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		getListView().setLayoutParams(params);
		Bundle args = new Bundle();
		args.putString(DataProvider.COL_EMAIL, mListener.getProfileEmail());
		getLoaderManager().initLoader(0, args, this);
		
		//associate context menu with listview
		registerForContextMenu(getListView());
		
		//initialize and start scheduler to run every second
		//looks for expired message and removes them from chat widnows as well as from message table
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				
				//fetch records that already expired
				Cursor c = getActivity().getContentResolver().query(
						DataProvider.CONTENT_URI_MESSAGES, 
						new String[] { DataProvider.COL_TYPE },
						DataProvider.COL_TTL + " > -1 AND " + DataProvider.COL_TTL + " < ?", 
						new String[] { Long.toString(new Date().getTime()) }, null);
				
				//if any such record found
				if(c.moveToFirst()) {
					int rows = getActivity().getContentResolver().delete(DataProvider.CONTENT_URI_MESSAGES
							, DataProvider.COL_TTL + " > -1 AND " + DataProvider.COL_TTL + " < ?", 
							new String[] { Long.toString(new Date().getTime()) });
					
					if(rows > 0) {
						
						//remove the message from messages table
						getActivity().runOnUiThread(new Runnable() {
			                public void run() {
			                	Bundle extra = new Bundle();
								extra.putString(DataProvider.COL_EMAIL, profileId);
								MessagesFragment.this.getLoaderManager().restartLoader(0, extra, MessagesFragment.this);
			                }
			            });
					}
				}						
			}
		}, 1, 1, TimeUnit.SECONDS);
		
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	      super.onCreateContextMenu(menu, v, menuInfo);
	      if (v.getId() == getListView().getId()) {
	          MenuInflater inflater = getActivity().getMenuInflater();
	          inflater.inflate(R.menu.chat_message, menu);
	      }
	}
	
	/**
	 * *
	 * Context menu actions, on delete alerts user for delete confirmation
	 * on OK, deletes message from local messages table and send delete action message
	 * to other chat participant.
	 *
	 * @param item the item
	 * @return true, if successful
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		

		switch (item.getItemId()) {
		case R.id.action_add:
			// add stuff here
			return true;
		case R.id.action_delete:
			
			Cursor cursor = (Cursor) chatCursorAdapter.getItem(info.position);
			final String uuid = cursor.getString(cursor.getColumnIndex(DataProvider.COL_UUID));
			
			Toast.makeText(getActivity(), uuid, Toast.LENGTH_SHORT).show();
			
			
			AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
			adb.setMessage("This message will be deleted");

			adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					getActivity().getContentResolver().delete(DataProvider.CONTENT_URI_MESSAGES
							, "uuid = ?", new String[] { uuid });
					
					new AsyncTask<String, Void, String>() {
						
						@Override
						protected String doInBackground(String... params) {
							String msg = "";
							
							try {
								Message message = new Message();
								message.getRecepient().add(params[0]);
								message.setTtl(-1);
								message.setType(Message.UNICAST);
								message.setUuid(uuid);
								message.setAction(Message.ACTION_DELETE);
								
								Gson gson = new Gson();
								String json = gson.toJson(message);
								
								ServerUtilities.send("", params[0], json);
								
								msg = "Message deleted";

							} catch (IOException ex) {
								msg = "Unable to delete message from server";
							}
							
							return msg;
						}

						@Override
						protected void onPostExecute(String msg) {
							if (msg != null && !TextUtils.isEmpty(msg)) {
								Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
							}
						}
					}.execute(new String[] { profileId });
					
					Bundle extra = new Bundle();
					extra.putString(DataProvider.COL_EMAIL, profileId);
					MessagesFragment.this.getLoaderManager().restartLoader(0, extra, 
							MessagesFragment.this);
				}
			});

			adb.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					});
			adb.show();
			return true;
			
			
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	/**
	 * *
	 * Shutdown scheduler on activity destroy.
	 */
	@Override
	public void onDetach() {
		scheduler.shutdown();
		
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
		 * Gets the profile email.
		 *
		 * @return the profile email
		 */
		public String getProfileEmail();
	}

	/**
	 * Gets the display time.
	 *
	 * @param datetime the datetime
	 * @return the display time
	 */
	private String getDisplayTime(String datetime) {
		try {
			Date dt = sdf.parse(datetime);
			if (now.getYear()==dt.getYear() && now.getMonth()==dt.getMonth() && now.getDate()==dt.getDate()) {
				return df[1].format(dt);
			}
			return df[0].format(dt);
		} catch (ParseException e) {
			return datetime;
		}
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
	 */
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if(args.containsKey(DataProvider.COL_EMAIL))
			profileId = args.getString(DataProvider.COL_EMAIL);
		
		try {
		CursorLoader loader = new CursorLoader(getActivity(), 
				DataProvider.CONTENT_URI_MESSAGES, 
				null, 
				"( " + DataProvider.COL_SENDER_EMAIL + " = ? or " + DataProvider.COL_RECEIVER_EMAIL + " = ? ) AND ( " + 
				DataProvider.COL_TTL + " > ? OR " + DataProvider.COL_TTL + " = -1 )",
				new String[]{ profileId, profileId, Long.toString(new Date().getTime()) }, 
				DataProvider.COL_TIME + " ASC"); 
		return loader;
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
	 */
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		chatCursorAdapter.swapCursor(data);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
	 */
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		chatCursorAdapter.swapCursor(null);
	}

	/**
	 * The Class ChatCursorAdapter.
	 */
	public class ChatCursorAdapter extends CursorAdapter {
	    
		/**
		 * Instantiates a new chat cursor adapter.
		 *
		 * @param context the context
		 * @param c the c
		 */
		public ChatCursorAdapter(Context context, Cursor c) {
			super(context, c, 0);
		}

		/* (non-Javadoc)
		 * @see android.support.v4.widget.CursorAdapter#getCount()
		 */
		@Override public int getCount() {
			return getCursor() == null ? 0 : super.getCount();
		}

		/* (non-Javadoc)
		 * @see android.widget.BaseAdapter#getViewTypeCount()
		 */
		@Override
		public int getViewTypeCount() {
			return 2;
		}

		/* (non-Javadoc)
		 * @see android.widget.BaseAdapter#getItemViewType(int)
		 */
		@Override
		public int getItemViewType(int _position) {
			Cursor cursor = (Cursor) getItem(_position);
			return getItemViewType(cursor);
		}

	    /**
    	 * Gets the item view type.
    	 *
    	 * @param _cursor the _cursor
    	 * @return the item view type
    	 */
    	private int getItemViewType(Cursor _cursor) {
	        int typeIdx = _cursor.getColumnIndex(DataProvider.COL_TYPE);
	        int type = _cursor.getInt(typeIdx);
	        return type == 0 ? 0 : 1;
	    }
	    
		/* (non-Javadoc)
		 * @see android.support.v4.widget.CursorAdapter#newView(android.content.Context, android.database.Cursor, android.view.ViewGroup)
		 */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			ViewHolder holder = new ViewHolder();
			View itemLayout = null;
			switch(getItemViewType(cursor)){
		    case 0:
				itemLayout = LayoutInflater.from(context).inflate(R.layout.chat_list_item_left_aligned, parent, false);
		    	break;
		    case 1:
				itemLayout = LayoutInflater.from(context).inflate(R.layout.chat_list_item_right_aligned, parent, false);
		    	break;
			}
			itemLayout.setTag(holder);
			holder.avatar = (ImageView) itemLayout.findViewById(R.id.avatar);			
			holder.text1 = (TextView) itemLayout.findViewById(R.id.text1);
			holder.text2 = (TextView) itemLayout.findViewById(R.id.text2);
			holder.attachment = (TextView) itemLayout.findViewById(R.id.attachment);
			holder.imgViewAttachment = (ImageView) itemLayout.findViewById(R.id.imgViewAttachment);
			return itemLayout;
		}
		
		/* (non-Javadoc)
		 * @see android.support.v4.widget.CursorAdapter#bindView(android.view.View, android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			String email = cursor.getString(cursor.getColumnIndex(DataProvider.COL_SENDER_EMAIL));
			holder.text1.setText(getDisplayTime(cursor.getString(cursor.getColumnIndex(DataProvider.COL_TIME))));
			holder.text2.setText(cursor.getString(cursor.getColumnIndex(DataProvider.COL_MESSAGE)));
						
			final String attachment = cursor.getString(cursor.getColumnIndex(DataProvider.COL_ATTACHMENT));
			String messageId = cursor.getString(cursor.getColumnIndex(DataProvider.COL_UUID));
						
			MainActivity.photoCache.DisplayBitmap(requestPhoto(email), holder.avatar);
			
			//In case if this message has an attachment
			if(attachment != null && attachment.trim().length() > 0) {
				
				//Check if this is attachment send by another user
				if(attachment.startsWith("http")) {
					
					//Download attachment form GAE server
					new DisplayAttachmentTask(attachment, messageId, holder.attachment, holder.imgViewAttachment).execute();
					
				} else if(isFileExists(attachment)) {
					//If this is atatchment send by us, display image from local drive instead of downlaoding
					viewAttachment(attachment, holder.imgViewAttachment);
				} else {
					holder.attachment.setVisibility(View.GONE);
					holder.imgViewAttachment.setVisibility(View.GONE);
				}				
				
			} else {
				holder.attachment.setVisibility(View.GONE);
				holder.imgViewAttachment.setVisibility(View.GONE);
			}
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
		
		/** The avatar. */
		ImageView avatar;
		
		/** The attachment. */
		TextView attachment;
		
		/** The img view attachment. */
		ImageView imgViewAttachment;
	}
	
	/**
	 * Gets the associated profile image by querying Android Contacts.
	 *
	 * @param email the email
	 * @return the uri
	 */
	@SuppressLint("InlinedApi")
	private Uri requestPhoto(String email){
		Cursor emailCur = null;
		Uri uri = null;
		try{
			int SDK_INT = android.os.Build.VERSION.SDK_INT;
			if(SDK_INT >= 11){
				String[] projection = { ContactsContract.CommonDataKinds.Email.PHOTO_URI };
				ContentResolver cr = getActivity().getContentResolver();
				emailCur = cr.query(
						ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection,
						ContactsContract.CommonDataKinds.Email.ADDRESS + " = ?", 
								new String[]{email}, null);
				if (emailCur != null && emailCur.getCount() > 0) {	
					if (emailCur.moveToNext()) {
						String photoUri = emailCur.getString( emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.PHOTO_URI));
						if(photoUri != null)
							uri = Uri.parse(photoUri);
					}
				}
			}else if(SDK_INT < 11) {
				String[] projection = { ContactsContract.CommonDataKinds.Photo.CONTACT_ID };
				ContentResolver cr = getActivity().getContentResolver();
				emailCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, 
						projection,
						ContactsContract.CommonDataKinds.Email.ADDRESS + " = ?",
						new String[]{email}, null);
				if (emailCur.moveToNext()) {
					int columnIndex = emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Photo.CONTACT_ID);
					long contactId = emailCur.getLong(columnIndex);
					uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,	contactId);
					uri = Uri.withAppendedPath(uri,	ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
				}
			}	
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try{
				if(emailCur != null)
					emailCur.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return uri;
	}
	
	/**
	 * *
	 * Check if file exists.
	 *
	 * @param path Path to file
	 * @return true if file exists, false otherwise
	 */
	private boolean isFileExists(String path) {
		File file = new File(path);
		return file.exists();
	}
	
	/**
	 * *
	 * Display attachment as an image and register open image in default image viewer when clicked.
	 *
	 * @param attachment File name of attachment
	 * @param imgView ImageView object to display image
	 */
	private void viewAttachment(final String attachment, ImageView imgView) {
		
		Bitmap myBitmap = BitmapFactory.decodeFile(new File(attachment).getAbsolutePath());
		imgView.setImageBitmap(myBitmap);
		
		imgView.setVisibility(View.VISIBLE);
		
		imgView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse(attachment), "image/*");
				
				Intent chooser = Intent.createChooser(intent, "View attachment");
				
				if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
				    startActivity(chooser);
				} else {
					Toast.makeText(getActivity(), "No suitable application found to view attachment", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
	}
	
	/**
	 * *
	 * Downloads the attachment in case if attachment has not already been downloaded
	 * and on successful download dislplay the image.
	 *
	 * @author waqas.siddiqui
	 */
	private class DisplayAttachmentTask extends AsyncTask<Void, Void, String> {

		/**  Url to download image from *. */
		private String imageUrl;
		
		/**  Unique message ID associated with this image *. */
		private String messageId;
		
		/**  TextView to display download status *. */
		private TextView textView;
		
		/**  ImageView to display image *. */
		private ImageView imgViewAttachment;
		
		/**
		 * Instantiates a new display attachment task.
		 *
		 * @param imageUrl the image url
		 * @param messageId the message id
		 * @param textView the text view
		 * @param imgViewAttachment the img view attachment
		 */
		public DisplayAttachmentTask(String imageUrl, String messageId, TextView textView, ImageView imgViewAttachment) {
			this.textView = textView;
			this.imageUrl = imageUrl;
			this.messageId = messageId;
			this.imgViewAttachment = imgViewAttachment;
		}
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			textView.setVisibility(View.VISIBLE);
			textView.setText("Downloading attachment...");
		}
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected String doInBackground(Void... params) {
			//Check if attachment has already been downloaded
			File file = new File(Environment.getExternalStorageDirectory() + "/instaChat/" + messageId + ".png");
			if(file.exists()) 
				return Environment.getExternalStorageDirectory() + "/instaChat/" + messageId + ".png";
			
			return ServerUtilities.downloadFile(imageUrl, messageId);
		}
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(final String path) {
			
			//on sucessful download display image in ImageView
			if (path != null && !TextUtils.isEmpty(path)) {
				textView.setText("Tap here to view attachement");
				

				Bitmap myBitmap = BitmapFactory.decodeFile(new File(path).getAbsolutePath());
				imgViewAttachment.setImageBitmap(myBitmap);
				
				textView.setVisibility(View.GONE);
				
				imgViewAttachment.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						
						Intent intent = new Intent();
						intent.setAction(android.content.Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.fromFile(new File(path)), "image/*");
						
						Intent chooser = Intent.createChooser(intent, "View attachment");
						
						if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
						    startActivity(chooser);
						} else {
							Toast.makeText(getActivity(), "No suitable application found to view attachment", Toast.LENGTH_SHORT).show();
						}
					}
				});
				
			} else {
				textView.setText("Downloading failed");
			}
		}
		
	}
}
