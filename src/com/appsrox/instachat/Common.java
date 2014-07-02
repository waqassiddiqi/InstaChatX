/*
 * 
 */
package com.appsrox.instachat;

import java.util.ArrayList;
import java.util.List;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Patterns;

import com.appsrox.instachat.client.Constants;

// TODO: Auto-generated Javadoc
//URL to bugs repository in case of application crash
/**
 * The Class Common.
 */
@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=24f0928b", formKey="")
public class Common extends Application {
	
	/* profile id field  */
	/** The Constant PROFILE_ID. */
	public static final String PROFILE_ID = "profile_id";
	
	/* broadcast action used to inform receivers interested in GAE registration status  */
	/** The Constant ACTION_REGISTER. */
	public static final String ACTION_REGISTER = "com.appsrox.instachat.REGISTER";
	
	/** The Constant EXTRA_STATUS. */
	public static final String EXTRA_STATUS = "status";
	
	/** The Constant STATUS_SUCCESS. */
	public static final int STATUS_SUCCESS = 1;
	
	/** The Constant STATUS_FAILED. */
	public static final int STATUS_FAILED = 0;
	
	/** The email_arr. */
	public static String[] email_arr;
	
	/** The prefs. */
	private static SharedPreferences prefs;

	
	/** Application instance **/
	private static Common instance;
	
	
	public Common() {
		instance = this;
	}
	
	public static Context getContext() {
		return instance.getApplicationContext();
	}
	
	/* (non-Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
	
		//Initialize crash reporter service
		ACRA.init(this);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		List<String> emailList = getEmailList();
		email_arr = emailList.toArray(new String[emailList.size()]);
	}
	
	/**
	 * Fetch all the accounts associated with device.
	 *
	 * @return the email list
	 */
	private List<String> getEmailList() {
		List<String> lst = new ArrayList<String>();
		Account[] accounts = AccountManager.get(this).getAccounts();
		for (Account account : accounts) {
		    if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches()) {
		        lst.add(account.name);
		    }
		}
		return lst;
	}
	
	/**
	 * Returns preferred email.
	 *
	 * @return the preferred email
	 */
	public static String getPreferredEmail() {
		return prefs.getString("chat_email_id", email_arr.length==0 ? "" : email_arr[0]);
	}
	
	/**
	 * Returns display name of user.
	 *
	 * @return the display name
	 */
	public static String getDisplayName() {
		String email = getPreferredEmail();
		return prefs.getString("display_name", email.substring(0, email.indexOf('@')));
	}
	
	/**
	 * Returns if notification of new message is enable or not.
	 *
	 * @return true, if is notify
	 */
	public static boolean isNotify() {
		return prefs.getBoolean("notifications_new_message", true);
	}
	
	/**
	 * Returns default ringtone.
	 *
	 * @return the ringtone
	 */
	public static String getRingtone() {
		return prefs.getString("notifications_new_message_ringtone", android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString());
	}
	
	/**
	 * Returns GAE server URL.
	 *
	 * @return the server url
	 */
	public static String getServerUrl() {
		return prefs.getString("server_url_pref", Constants.SERVER_URL);
	}
	
	/**
	 * Returns sender ID registered with GCM to send GCMs.
	 *
	 * @return the sender id
	 */
	public static String getSenderId() {
		return prefs.getString("sender_id_pref", Constants.SENDER_ID);
	}	
	
}