package com.appsrox.instachat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.appsrox.instachat.DataProvider.MessageType;
import com.appsrox.instachat.client.GcmUtil;
import com.appsrox.instachat.client.ServerUtilities;
import com.appsrox.instachat.client.Util;
import com.appsrox.instachat.model.Contact;
import com.appsrox.instachat.model.Message;
import com.google.gson.Gson;

/**
 * The Class BroadcastActivity to send message to multiple contacts at once.
 */
public class BroadcastActivity extends ActionBarActivity implements
		SeekBar.OnSeekBarChangeListener {

	/** The m seek bar. */
	private SeekBar mSeekBar;
	
	/** The lbl expire. */
	private TextView lblExpire;
	
	/** The list view. */
	private ListView listView;
	
	/** The list to hold all Contacts in profile table. */
	private List<Contact> list;
	
	/** The b send all. */
	private boolean bSendAll = false;
	
	/** The gcm util. */
	private GcmUtil gcmUtil;
	
	/** The btn send. */
	private Button btnSend;
	
	/** The chk expirable. */
	private CheckBox chkExpirable;
	
	/** The attached file path. */
	private String attachedFilePath = "";
	
	/** The Constant REQ_CODE_PICK_IMAGE. */
	private final static int REQ_CODE_PICK_IMAGE = 1;
	
	/** The m progress. */
	private ProgressDialog mProgress;
	
	/* (non-Javadoc)
	 * @see android.support.v7.app.ActionBarActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_broadcast);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#3483A1")));
		
		mSeekBar = (SeekBar) findViewById(R.id.seekBar1);
		mSeekBar.setOnSeekBarChangeListener(this);
		lblExpire = (TextView) findViewById(R.id.lblExpire);

		
		//register broadcast receiver to listen com.appsrox.instachat.REGISTER broadcasts
		registerReceiver(registrationStatusReceiver, new IntentFilter(Common.ACTION_REGISTER));
		
		//Initialize GCM Util object, on initialization it checks if this application has already been
		//registered with to send/receive GCM by checking the GCM registration ID in preference
		//If GCM Registration ID isn't found in preference, make a call to GCM server to get a new registration ID
		//then update the GAE server with this new ID along with the google email account associated with this application
		//then send registration broadcast com.appsrox.instachat.REGISTER to notify receivers that application
		//is not registered to send and receive GCM
		gcmUtil = new GcmUtil(getApplicationContext());

		mProgress = new ProgressDialog(this);
		mProgress.setMessage("Uploading file...");
		
		//populate list of contacts and display them in ListView
		list = new ArrayList<Contact>();
		list.add(new Contact("All", "All"));
		Cursor c = getContentResolver().query(DataProvider.CONTENT_URI_PROFILE,
				new String[] { DataProvider.COL_EMAIL, DataProvider.COL_NAME }, null, null,
				DataProvider.COL_ID + " DESC");

		//Iterate through each row of profile table, construct new object of Contact and add in list 
		c.moveToFirst();
		while (!c.isAfterLast()) {
			list.add(new Contact(c.getString(c.getColumnIndex(DataProvider.COL_EMAIL)), 
					c.getString(c.getColumnIndex(DataProvider.COL_NAME))));
			c.moveToNext();
		}

		//Bind list of Contact with ListView
		ArrayAdapter<Contact> adapter = new ArrayAdapter<Contact>(this,
				android.R.layout.simple_list_item_multiple_choice, list);

		listView = (ListView) findViewById(R.id.listView);
		listView.setAdapter(adapter);

		//Disable Send message button until GCM registration status is known
		btnSend = (Button) findViewById(R.id.send_btn);
		btnSend.setEnabled(false);

		//Send message, in case if user has selected an image, it first uploads the image and then 
		//send message on successful image
		btnSend.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				String text = ((TextView) findViewById(R.id.msg_edit))
						.getText().toString();

				if (text.trim().length() <= 0) {
					Toast.makeText(BroadcastActivity.this,
							"Please enter message", Toast.LENGTH_SHORT).show();
					return;
				}

				String uuid = java.util.UUID.randomUUID().toString().replaceAll("-", "");
				
				if(attachedFilePath != null && attachedFilePath.trim().length() > 0 && 
						new File(attachedFilePath).exists()) {
					
					//Calls upload method along with new generated unique message ID to upload attachement and send message
					upload(uuid, text);
					
				} else {
					sendMessage(uuid, text, "");
					attachedFilePath = "";
				}
				
				
			}
		});

		//Checkbox to toogle expiry seekbar display
		chkExpirable = (CheckBox) findViewById(R.id.chkExpirable);
		chkExpirable.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					mSeekBar.setVisibility(View.VISIBLE);
					lblExpire.setVisibility(View.VISIBLE);
				} else {
					mSeekBar.setVisibility(View.GONE);
					lblExpire.setVisibility(View.GONE);
				}

			}
		});
		
		//Opens the gallery to select an image to upload
		//REQ_CODE_PICK_IMAGE is used to track the result of selected in onActivityResult
		findViewById(R.id.attach_btn).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				//create new intent with action to pick an item
				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
				
				//set the type of pick to an image, so by default selector opens pictures/image gallery
				photoPickerIntent.setType("image/*");
				
				//start image picking activity with result so to get the file path once user has picked
				startActivityForResult(photoPickerIntent, REQ_CODE_PICK_IMAGE); 
			}
		});
	}

	/**
	 * Uploads a file to GAE server in separate thread and on successful upload
	 * calls sendMessage along with text and URL to attachment to send chat message.
	 *
	 * @param messageId the message id
	 * @param text the text
	 */
	private void upload(final String messageId, final String text) {
		new AsyncTask<String, Void, String>() {
			
			String errorMessage = "Unable to upload file";
			
			//Called on UI thread before performing background activity to prepare stuff like show dialog box to user
			@Override
			protected void onPreExecute() {
				mProgress.show();
			}
			
			//Runs in separate background thread
			//First checks for the size of file being uploaded, if it is greater than 1MB, return with an error message
			//that file is too large
			//otherwise calls ServerUtilities.uplaod with path to file to upload and unique message id
			//this message is then used to download this attachment from GAE server
			@Override
			protected String doInBackground(String... params) {
				
				//Return an error if filesize is greater than 1MB
				if(Util.getFileSize(params[0]) >= 1024) {
					errorMessage = "File is too large to be attached";
					return null;
				}
				
				//upload attachment of GAE server and return remote path to attachment
				return ServerUtilities.uplaod(params[0], params[1]);
			}
			
			//Runs on UI thread once background thread completes execution
			//Closes the progress dialog indicating user that operation has been finished
			//if uploading is successful, call sendMessage to send chat message request to GAE server
			//otherwise display an error to user that attaching file has failed
			@Override
			protected void onPostExecute(String url) {
				mProgress.cancel();
				
				if(url != null && url.trim().length() > 0 && url.startsWith("OK")) {
					sendMessage(messageId, text, url.replace("OK|", ""));
				} else {
					Toast.makeText(BroadcastActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
				}
			}
			
		}.execute(new String[] { attachedFilePath, messageId });
	}
	
	/**
	 * Change the expiry text on progessbar change.
	 *
	 * @param seekBar the seek bar
	 * @param progress the progress
	 * @param fromTouch the from touch
	 */
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromTouch) {
		lblExpire
				.setText(String.format("Time to expire: %d seconds", progress));
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		unregisterReceiver(registrationStatusReceiver);
		gcmUtil.cleanup();
		super.onDestroy();
	}

	/**
	 * Broadcast receiver listening to com.appsrox.instachat.REGISTER
	 * on successful registration enables the send text button
	 */
	private BroadcastReceiver registrationStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			//Check for registration status being passed by com.appsrox.instachat.REGISTER broadcast
			//if registration is successful enables the send chat button
			if (intent != null
					&& Common.ACTION_REGISTER.equals(intent.getAction())) {
				switch (intent.getIntExtra(Common.EXTRA_STATUS, 100)) {
				case Common.STATUS_SUCCESS:
					btnSend.setEnabled(true);
					break;

				case Common.STATUS_FAILED:
					Toast.makeText(BroadcastActivity.this,
							"Unable to connect to server", Toast.LENGTH_SHORT)
							.show();
					break;
				}
			}
		}
	};
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		//close the activity when user taps on back icon on actionbar
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;			
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Construct and send message (a Message object) to server in json format.
	 * It iterates through all the items of ListView and add those with a selected checkbox is checked
	 * to list of recipients
	 * @param messageId the message id
	 * @param text the text
	 * @param uploadedUrl the uploaded url
	 */
	private void sendMessage(String messageId, String text, String uploadedUrl) {
		
		//construct new message object
		final Message msg = new Message();
		msg.setAction(Message.ACTION_IM);
		msg.setMessage("{" + Common.getDisplayName() + "}" + text);
		
		//set message type to broadcast
		msg.setType(Message.BROADCAST);
		msg.setUuid(messageId);
		msg.setSender(Common.getPreferredEmail());
		
		//add attachment URL
		msg.setAttachment(uploadedUrl);
		
		//set expiry time if it is self destroyable message
		if(chkExpirable.isChecked() == false) {
			msg.setTtl(-1);
		} else {
			msg.setTtl(mSeekBar.getProgress());
		}
		
		msg.getRecepient().clear();

		//populate recipient list with the selected contacts
		SparseBooleanArray checkedPositions = listView
				.getCheckedItemPositions();

		for (int i = 0; i < listView.getCount(); i++) {
			if (checkedPositions.get(i) == true) {
				if ( ((Contact)list.get(i)).getDisplayName().equals("All")) {
					bSendAll = true;
					break;
				}

				if (list.get(i).equals(Common.getPreferredEmail()) == false)
					msg.getRecepient().add(  ((Contact)list.get(i)).getEmail() );
			}
		}

		if (bSendAll && list.size() > 1) {
			for (int i = 1; i < list.size(); i++) {
				if (Common.getPreferredEmail().equals(list.get(i)) == false)
					msg.getRecepient().add( ((Contact)list.get(i)).getEmail() );
			}
		}

		//if recipient list has some selected contacts proceed with sending
		//request to GAE server to send chat message
		if (msg.getRecepient().size() > 0) {

			Gson gson = new Gson();
			final String json = gson.toJson(msg);

			new AsyncTask<Void, Void, String>() {
				@Override
				protected String doInBackground(Void... params) {
					String message = "Broadcast has been sent";

					try {
						ServerUtilities.send("", "", json);

						ContentValues values;

						//after sending text message request to GAE server persist messages in local message table
						for (int i = 0; i < msg.getRecepient().size(); i++) {
							values = new ContentValues(2);
							values.put(DataProvider.COL_TYPE, MessageType.OUTGOING.ordinal());
							values.put(DataProvider.COL_MESSAGE, msg.getMessage().replaceFirst("\\{.*\\}", ""));
							values.put(DataProvider.COL_RECEIVER_EMAIL, msg.getRecepient().get(i));
							values.put(DataProvider.COL_SENDER_EMAIL, Common.getPreferredEmail());
							values.put(DataProvider.COL_UUID, msg.getUuid());
							values.put(DataProvider.COL_TTL, msg.getTtl());
							
							if(attachedFilePath != null && attachedFilePath.trim().length() > 0) {
								values.put(DataProvider.COL_ATTACHMENT, Uri.fromFile(new File(attachedFilePath)).toString());
							}
							else
								values.put(DataProvider.COL_ATTACHMENT, "");
							
							getContentResolver().insert(DataProvider.CONTENT_URI_MESSAGES, values);
						}

					} catch (IOException ex) {
						message = "Message could not be sent";
					}
					
					attachedFilePath = "";
					
					return message;
				}

				@Override
				protected void onPostExecute(String msg) {
					if (msg != null && !TextUtils.isEmpty(msg)) {
						Toast.makeText(getApplicationContext(), msg,
								Toast.LENGTH_LONG).show();
					}
				}
			}.execute(null, null, null);
		}
	}
	
	/**
	 * Sets the attached image path to the image selected by user to upload.
	 *
	 * @param requestCode the request code
	 * @param resultCode the result code
	 * @param imageReturnedIntent the image returned intent
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

		switch (requestCode) {
		case REQ_CODE_PICK_IMAGE:
			if (resultCode == RESULT_OK) {
				Uri selectedImage = imageReturnedIntent.getData();
				String[] filePathColumn = { MediaStore.Images.Media.DATA };

				Cursor cursor = getContentResolver().query(selectedImage,
						filePathColumn, null, null, null);
				cursor.moveToFirst();

				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				String filePath = cursor.getString(columnIndex);
				cursor.close();

				attachedFilePath = filePath;
				Toast.makeText(BroadcastActivity.this, "Image attached, type message and send", 
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	/* (non-Javadoc)
	 * @see android.widget.SeekBar.OnSeekBarChangeListener#onStartTrackingTouch(android.widget.SeekBar)
	 */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	/* (non-Javadoc)
	 * @see android.widget.SeekBar.OnSeekBarChangeListener#onStopTrackingTouch(android.widget.SeekBar)
	 */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

}
