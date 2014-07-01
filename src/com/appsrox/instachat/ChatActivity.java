/*
 * 
 */
package com.appsrox.instachat;

import java.io.File;
import java.io.IOException;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.appsrox.instachat.DataProvider.MessageType;
import com.appsrox.instachat.client.GcmUtil;
import com.appsrox.instachat.client.ServerUtilities;
import com.appsrox.instachat.client.Util;
import com.appsrox.instachat.model.Message;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;

// TODO: Auto-generated Javadoc
/**
 * The Class ChatActivity.
 */
public class ChatActivity extends ActionBarActivity implements MessagesFragment.OnFragmentInteractionListener, 
EditContactDialog.OnFragmentInteractionListener, OnClickListener {

	/** The msg edit. */
	private EditText msgEdit;
	
	/** The send btn. */
	private Button sendBtn;
	
	/** The profile id. */
	private String profileId;
	
	/** The profile name. */
	private String profileName;
	
	/** The profile email. */
	private String profileEmail;
	
	/** The gcm util. */
	private GcmUtil gcmUtil;
	
	/** The Constant REQ_CODE_PICK_IMAGE. */
	private final static int REQ_CODE_PICK_IMAGE = 1;
	
	/** The attached file path. */
	private String attachedFilePath = "";
	
	/** The m progress. */
	private ProgressDialog mProgress;
	
	/** The m ad view. */
	private AdView mAdView;
	
	/** The is contact. */
	private boolean isContact = false;
	
	/** The layout accept. */
	private View layoutAccept;
	
	/* (non-Javadoc)
	 * @see android.support.v7.app.ActionBarActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_activity);

		layoutAccept = findViewById(R.id.layoutAccept);
		
		//Initialize AdMob view and call loadAd to contact server and fetch ad
		mAdView = (AdView) findViewById(R.id.adView);
        mAdView.loadAd(new AdRequest.Builder().build());
		
		profileId = getIntent().getStringExtra(Common.PROFILE_ID);
		msgEdit = (EditText) findViewById(R.id.msg_edit);
		sendBtn = (Button) findViewById(R.id.send_btn);
		
		//Disable Send message button until GCM registration status is known
		sendBtn.setOnClickListener(this);		
		sendBtn.setEnabled(false);
		
		mProgress = new ProgressDialog(this);
		mProgress.setMessage("Uploading file...");
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#3483A1")));

		//Fetch profile details of this contact from table
		//Dislplay accept/decline options if this profile hasn't already been accepted as contact 
		Cursor c = getContentResolver().query(Uri.withAppendedPath(DataProvider.CONTENT_URI_PROFILE, profileId), null, null, null, null);
		if (c.moveToFirst()) {
			profileName = c.getString(c.getColumnIndex(DataProvider.COL_NAME));
			profileEmail = c.getString(c.getColumnIndex(DataProvider.COL_EMAIL));
			
			isContact = c.getInt(c.getColumnIndex(DataProvider.COL_ACCEPTED)) == 1 ? true : false;
			
			if(isContact) {
				layoutAccept.setVisibility(View.GONE);
			} else {
				
				
				//accept contact request
				findViewById(R.id.btnAccept).setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						acceptRequest(true);
					}
				});
				
				//decline contact request and delete contact from profile table
				findViewById(R.id.btnDecline).setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						acceptRequest(false);
					}
				});
			}
			
			actionBar.setTitle(profileName);
		}
		actionBar.setSubtitle("connecting ...");

		//register broadcast receiver to listen com.appsrox.instachat.REGISTER broadcasts
		registerReceiver(registrationStatusReceiver, new IntentFilter(Common.ACTION_REGISTER));
		
		//Initialize GCM Util object, on initialization it checks if this application has already been
		//registered with to send/receive GCM by checking the GCM registration ID in preference
		//If GCM Registration ID isn't found in preference, make a call to GCM server to get a new registration ID
		//then update the GAE server with this new ID along with the google email account associated with this application
		//then send registration broadcast com.appsrox.instachat.REGISTER to notify receivers that application
		//is not registered to send and receive GCM
		gcmUtil = new GcmUtil(getApplicationContext());
		
		//Opens the gallery to select an image to upload
		//REQ_CODE_PICK_IMAGE is used to track the result of selected in onActivityResult
		findViewById(R.id.attach_btn).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, REQ_CODE_PICK_IMAGE); 
			}
		});
	}

	/**
	 * On accept = true, updates record for this contact in profile table setting accepted to 1
	 * On accept = false, deletes this contact's entry from profile table and closes chat window.
	 *
	 * @param accept the accept
	 */
	private void acceptRequest(boolean accept) {
		ContentValues values = new ContentValues(1);		
		
		if(accept) {
			values.put(DataProvider.COL_ACCEPTED, 1);
			getContentResolver().update(Uri.withAppendedPath(DataProvider.CONTENT_URI_PROFILE, profileId), values, null, null);
			layoutAccept.setVisibility(View.GONE);
		} else {
			getContentResolver().delete(DataProvider.CONTENT_URI_PROFILE
					, DataProvider.COL_EMAIL + " = ?", 
					new String[] { profileEmail });
			
			finish();
		}		
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}	

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_edit:
			EditContactDialog dialog = new EditContactDialog();
			Bundle args = new Bundle();
			args.putString(Common.PROFILE_ID, profileId);
			args.putString(DataProvider.COL_NAME, profileName);
			dialog.setArguments(args);
			dialog.show(getSupportFragmentManager(), "EditContactDialog");
			return true;

		//close the activity when user taps on back icon on actionbar	
		case android.R.id.home:
			
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;			
		}
		return super.onOptionsItemSelected(item);
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.send_btn:			
			//Send message, in case if user has selected an image, it first uploads the image and then 
			//send message on successful image
			send(msgEdit.getText().toString());
			msgEdit.setText(null);
			break;
		}
	}

	/* (non-Javadoc)
	 * @see com.appsrox.instachat.EditContactDialog.OnFragmentInteractionListener#onEditContact(java.lang.String)
	 */
	@Override
	public void onEditContact(String name) {
		getSupportActionBar().setTitle(name);
	}	

	/* (non-Javadoc)
	 * @see com.appsrox.instachat.MessagesFragment.OnFragmentInteractionListener#getProfileEmail()
	 */
	@Override
	public String getProfileEmail() {
		return profileEmail;
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
				
				if(Util.getFileSize(params[0]) >= 1024) {
					try {
						params[0] = Util.compressAndCopy(params[0]);
						attachedFilePath = params[0]; 
					} catch (Exception e) {
						attachedFilePath = "";
						
						errorMessage = "Unable to compress image please try different image";
					}
				}
				
				//Return an error if filesize is greater than 1MB
				//if(Util.getFileSize(params[0]) >= 1024) {
				//	errorMessage = "File is too large to be attached";
				//	return null;
				//}
				
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
					Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
				}
			}
			
		}.execute(new String[] { attachedFilePath, messageId });
	}
	
	/**
	 * Construct and send message (a Message object) to server in json format.
	 *
	 * @param messageId the message id
	 * @param text the text
	 * @param uploadedUrl the uploaded url
	 */
	private void sendMessage(final String messageId, final String text, final String uploadedUrl) {
		
		new AsyncTask<Void, Void, String>() {
			
			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				try {
					
					//construct new message object
					Message message = new Message();
					message.setMessage("{" + Common.getDisplayName() + "}" + text);
					message.setSender(Common.getPreferredEmail());
					message.getRecepient().add(profileEmail);
					
					//make it persitent message that is it won't self destroy
					message.setTtl(-1);
					
					//set message type to unicast
					message.setType(Message.UNICAST);
					message.setUuid(messageId);
					message.setAction(Message.ACTION_IM);
					message.setAttachment(uploadedUrl);
					
					//deserialize Message object to json
					Gson gson = new Gson();
					String json = gson.toJson(message);
					
					//send chat request to GAE server
					ServerUtilities.send(text, profileEmail, json);
					
					//after sending text message request to GAE server persist messages in local message table
					ContentValues values = new ContentValues(2);
					values.put(DataProvider.COL_TYPE,  MessageType.OUTGOING.ordinal());
					values.put(DataProvider.COL_MESSAGE, text.replaceFirst("\\{.*\\}", ""));
					values.put(DataProvider.COL_RECEIVER_EMAIL, profileEmail);
					values.put(DataProvider.COL_SENDER_EMAIL, Common.getPreferredEmail());
					values.put(DataProvider.COL_UUID, messageId);
					values.put(DataProvider.COL_TTL, -1);
					if(attachedFilePath != null && attachedFilePath.trim().length() > 0) {
						values.put(DataProvider.COL_ATTACHMENT, Uri.fromFile(new File(attachedFilePath)).getPath());
					}
					else
						values.put(DataProvider.COL_ATTACHMENT, "");
					
					getContentResolver().insert(DataProvider.CONTENT_URI_MESSAGES, values);

				} catch (IOException ex) {
					msg = "Message could not be sent";
				}
				
				attachedFilePath = "";
				
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
				if (!TextUtils.isEmpty(msg)) {
					Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
				}
			}
		}.execute(null, null, null);
	}
	
	/**
	 * Generates the unique ID for this message then attaches the file if file to attach is selected by calling upload
	 * and finally send calls sendMessage to send message.
	 *
	 * @param text the text
	 */
	private void send(final String text) {
		
		//generate new unique message ID, this ID is then used to delete message from all recipients
		// and to download associated attachment
		String uuid = java.util.UUID.randomUUID().toString().replaceAll("-", "");
		
		
		//if user has selected an attachment, call upload method, this method will itself call sendMessage
		//if uploading was successful
		//otherwise if user hasn't selected any attachment call sendMessage to send chat message request to GAE
		if(attachedFilePath != null && attachedFilePath.trim().length() > 0 && 
				new File(attachedFilePath).exists()) {
			
			upload(uuid, text);
			
		} else {
			sendMessage(uuid, text, "");
			attachedFilePath = "";
		}
			
	}	
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onResume()
	 */
	@Override
    protected void onResume() {
        super.onResume();
        mAdView.resume();
    }
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onPause()
	 */
	@Override
	protected void onPause() {
		mAdView.pause();
		ContentValues values = new ContentValues(1);
		values.put(DataProvider.COL_COUNT, 0);
		getContentResolver().update(Uri.withAppendedPath(DataProvider.CONTENT_URI_PROFILE, profileId), values, null, null);
		super.onPause();
	}

	/**
	 * Detach the broadcast receiver before exiting this activity.
	 */
	@Override
	protected void onDestroy() {
		unregisterReceiver(registrationStatusReceiver);
		gcmUtil.cleanup();
		mAdView.destroy();
		
		super.onDestroy();
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
				
				Toast.makeText(ChatActivity.this, "Image attached, type message and send", 
						Toast.LENGTH_SHORT).show();

			}
		}
	}
	
	/**
	 * Broadcast receiver listening to com.appsrox.instachat.REGISTER
	 * on successful registration enables the send text button
	 */
	private BroadcastReceiver registrationStatusReceiver = new  BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Check for registration status being passed by com.appsrox.instachat.REGISTER broadcast
			//if registration is successful enables the send chat button
			if (intent != null && Common.ACTION_REGISTER.equals(intent.getAction())) {
				switch (intent.getIntExtra(Common.EXTRA_STATUS, 100)) {
				case Common.STATUS_SUCCESS:
					getSupportActionBar().setSubtitle("online");
					sendBtn.setEnabled(true);
					break;

				case Common.STATUS_FAILED:
					getSupportActionBar().setSubtitle("offline");					
					break;					
				}
			}
		}
	};	
}
