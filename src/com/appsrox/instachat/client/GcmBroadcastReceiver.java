/*
 * 
 */
package com.appsrox.instachat.client;

import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.appsrox.instachat.Common;
import com.appsrox.instachat.DataProvider;
import com.appsrox.instachat.DataProvider.MessageType;
import com.appsrox.instachat.MainActivity;
import com.appsrox.instachat.R;
import com.appsrox.instachat.model.Message;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;

// TODO: Auto-generated Javadoc
/**
 * The Class GcmBroadcastReceiver.
 */
public class GcmBroadcastReceiver extends BroadcastReceiver {
	
	/** The Constant TAG. */
	private static final String TAG = "GcmBroadcastReceiver";
	
	/** The ctx. */
	private Context ctx;	

	/**
	 * Called when com.google.android.c2dm.intent.RECEIVE action is raised
	 * Intent contains the payload of GCM message
	 *
	 * @param context the context
	 * @param intent the intent
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		ctx = context;
		
		//Prevent cell phone from sleeping during the parsing of received GCM message
		PowerManager mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		WakeLock mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		mWakeLock.acquire();
		
		try {
			
			//Get type of GCM message
			GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
			String messageType = gcm.getMessageType(intent);
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				sendNotification("Send error", false);
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				sendNotification("Deleted messages on server", false);
			} else {
				//Get payload in json format
				//this payload is the json representaion of Message class
				String msg = intent.getStringExtra(DataProvider.COL_MESSAGE);
				
				//Deserialize json to Message object
				Gson gson = new Gson();
				Message obj = gson.fromJson(msg, Message.class);
				
				//Check the message action
				//If is an a chat message (instant message) then
				if(obj.getAction() == Message.ACTION_IM) {
					
					//parse the display name from message text and
					//normalize the message
					String displayName = obj.getSender().substring(0, obj.getSender().indexOf('@'));
					
					if(obj.getMessage().startsWith("{")) {
						displayName = obj.getMessage().substring(1, obj.getMessage().indexOf("}"));
						
						obj.setMessage(obj.getMessage().substring(obj.getMessage().indexOf("}") + 1));
					}
					
					//Look for this contact in profile table
					Cursor c = context.getContentResolver().query(
							DataProvider.CONTENT_URI_PROFILE, 
							new String[] { DataProvider.COL_ID, DataProvider.COL_EMAIL }, DataProvider.COL_EMAIL + " = ?" , 
							new String[] { obj.getSender() }, null);
					
					//If this is new contact add it in profile table and set the accepted field to 0
					//marking this contact as new contact request
					if(c.moveToFirst() == false) {
						ContentValues values = new ContentValues(2);
						values.put(DataProvider.COL_NAME, displayName);				
						values.put(DataProvider.COL_EMAIL, obj.getSender());
						values.put(DataProvider.COL_ACCEPTED, 0);
						context.getContentResolver().insert(DataProvider.CONTENT_URI_PROFILE, values);
					} else {
						
						//Otherwise update the display name of contact
						ContentValues values = new ContentValues(1);
						values.put(DataProvider.COL_NAME, displayName);
						
						context.getContentResolver().update(Uri.withAppendedPath(DataProvider.CONTENT_URI_PROFILE, 
								Integer.toString(c.getInt(0))), values, null, null);						
					}
					
					//Insert received message in messages table
					String senderEmail = obj.getSender();
					String receiverEmail = Common.getPreferredEmail(); //intent.getStringExtra(DataProvider.COL_RECEIVER_EMAIL);
					ContentValues values = new ContentValues(2);
					values.put(DataProvider.COL_TYPE,  MessageType.INCOMING.ordinal());				
					values.put(DataProvider.COL_MESSAGE, obj.getMessage());
					values.put(DataProvider.COL_SENDER_EMAIL, senderEmail);
					values.put(DataProvider.COL_RECEIVER_EMAIL, receiverEmail);
					values.put(DataProvider.COL_UUID, obj.getUuid());
					
					//Look if mesage contains any attachment
					if(obj.getAttachment() != null && obj.getAttachment().trim().length() > 0)
						values.put(DataProvider.COL_ATTACHMENT, obj.getAttachment());
					else
						values.put(DataProvider.COL_ATTACHMENT, "");
					
					//If message is self destructing message set the expiry time to current time + number of second (ttl)
					if(obj.getTtl() > -1) {
						Calendar cal = Calendar.getInstance();
						cal.setTime(new Date());
						cal.add(Calendar.SECOND, obj.getTtl());
						
						values.put(DataProvider.COL_TTL, cal.getTimeInMillis());
						
					} else {
						values.put(DataProvider.COL_TTL, -1);
					}
					
					
					context.getContentResolver().insert(DataProvider.CONTENT_URI_MESSAGES, values);
					
					//Display notification
					if (Common.isNotify()) {
						sendNotification("New message", true);
					}
					
				} else if(obj.getAction() == Message.ACTION_DELETE) {
					
					//else if message action is delete, delete the message from messages database
					//Unique messageId is used to delete message
					context.getContentResolver().delete(DataProvider.CONTENT_URI_MESSAGES
							, "uuid = ?", new String[] { obj.getUuid() });
				}
			}
			setResultCode(Activity.RESULT_OK);
		} finally {
			//release wakelock so cell phone can go to sleep when required
			mWakeLock.release();
		}
	}

	/**
	 * *
	 * Construct and show notification.
	 *
	 * @param text Notification text to display
	 * @param launchApp on true launch Simply Chat when notifcation is tapped
	 */
	private void sendNotification(String text, boolean launchApp) {
		NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder notification = new NotificationCompat.Builder(ctx);
		notification.setContentTitle(ctx.getString(R.string.app_name));
		notification.setContentText(text);
		notification.setAutoCancel(true);
		notification.setSmallIcon(R.drawable.ic_launcher);
		if (!TextUtils.isEmpty(Common.getRingtone())) {
			notification.setSound(Uri.parse(Common.getRingtone()));
		}
		
		if (launchApp) {
			Intent intent = new Intent(ctx, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			notification.setContentIntent(pi);
		}
	
		mNotificationManager.notify(1, notification.build());
	}
}