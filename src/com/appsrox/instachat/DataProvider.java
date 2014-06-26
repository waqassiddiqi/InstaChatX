/*
 * 
 */
package com.appsrox.instachat;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

// TODO: Auto-generated Javadoc
/**
 * The Class DataProvider.
 */
public class DataProvider extends ContentProvider {

	/** The Constant CONTENT_URI_MESSAGES. */
	public static final Uri CONTENT_URI_MESSAGES = Uri.parse("content://com.appsrox.instachat.provider/messages");
	
	/** The Constant CONTENT_URI_PROFILE. */
	public static final Uri CONTENT_URI_PROFILE = Uri.parse("content://com.appsrox.instachat.provider/profile");

	/** The Constant COL_ID. */
	public static final String COL_ID = "_id";

	/**
	 * The Enum MessageType.
	 */
	public enum MessageType {

    	/** The incoming. */
	    INCOMING, /** The outgoing. */
 OUTGOING
    }
	
	//parameters recognized by GAE server
	/** The Constant SENDER_EMAIL. */
	public static final String SENDER_EMAIL 		= "chatId";
	
	/** The Constant RECEIVER_EMAIL. */
	public static final String RECEIVER_EMAIL 		= "chatId2";	
	
	/** The Constant REG_ID. */
	public static final String REG_ID 				= "regId";
	
	/** The Constant MESSAGE. */
	public static final String MESSAGE 				= "msg";
	
	/** The Constant DATA. */
	public static final String DATA 				= "data";
	
	// TABLE MESSAGE
	/** The Constant TABLE_MESSAGES. */
	public static final String TABLE_MESSAGES 		= "messages";
	
	/** The Constant COL_TYPE. */
	public static final String COL_TYPE				= "type";
	
	/** The Constant COL_SENDER_EMAIL. */
	public static final String COL_SENDER_EMAIL 	= "chatId";
	
	/** The Constant COL_RECEIVER_EMAIL. */
	public static final String COL_RECEIVER_EMAIL 	= "chatId2";
	
	/** The Constant COL_MESSAGE. */
	public static final String COL_MESSAGE 			= "msg";
	
	/** The Constant COL_TIME. */
	public static final String COL_TIME 			= "time";
	
	/** The Constant COL_UUID. */
	public static final String COL_UUID				= "uuid";
	
	/** The Constant COL_TTL. */
	public static final String COL_TTL				= "ttl";
	
	/** The Constant COL_ATTACHMENT. */
	public static final String COL_ATTACHMENT		= "attachment";
	

	// TABLE PROFILE
	/** The Constant TABLE_PROFILE. */
	public static final String TABLE_PROFILE = "profile";
	
	/** The Constant COL_NAME. */
	public static final String COL_NAME = "name";
	
	/** The Constant COL_EMAIL. */
	public static final String COL_EMAIL = "email";
	
	/** The Constant COL_ACCEPTED. */
	public static final String COL_ACCEPTED = "accepted";
	
	/** The Constant COL_COUNT. */
	public static final String COL_COUNT = "count";

	/** The db helper. */
	private DbHelper dbHelper;

	/** The Constant MESSAGES_ALLROWS. */
	private static final int MESSAGES_ALLROWS = 1;
	
	/** The Constant MESSAGES_SINGLE_ROW. */
	private static final int MESSAGES_SINGLE_ROW = 2;
	
	/** The Constant PROFILE_ALLROWS. */
	private static final int PROFILE_ALLROWS = 3;
	
	/** The Constant PROFILE_SINGLE_ROW. */
	private static final int PROFILE_SINGLE_ROW = 4;

	/** The Constant uriMatcher. */
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI("com.appsrox.instachat.provider", "messages", MESSAGES_ALLROWS);
		uriMatcher.addURI("com.appsrox.instachat.provider", "messages/#", MESSAGES_SINGLE_ROW);
		uriMatcher.addURI("com.appsrox.instachat.provider", "profile", PROFILE_ALLROWS);
		uriMatcher.addURI("com.appsrox.instachat.provider", "profile/#", PROFILE_SINGLE_ROW);
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		dbHelper = new DbHelper(getContext());
		return true;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch(uriMatcher.match(uri)) {
		case MESSAGES_ALLROWS:
			qb.setTables(TABLE_MESSAGES);
			break;			

		case MESSAGES_SINGLE_ROW:
			qb.setTables(TABLE_MESSAGES);
			qb.appendWhere("_id = " + uri.getLastPathSegment());
			break;

		case PROFILE_ALLROWS:
			qb.setTables(TABLE_PROFILE);
			break;			

		case PROFILE_SINGLE_ROW:
			qb.setTables(TABLE_PROFILE);
			qb.appendWhere("_id = " + uri.getLastPathSegment());
			break;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);			
		}

		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		long id;
		switch(uriMatcher.match(uri)) {
		case MESSAGES_ALLROWS:
			id = db.insertOrThrow(TABLE_MESSAGES, null, values);
			if (values.get(COL_RECEIVER_EMAIL) == null) {
				db.execSQL("update profile set count = count+1 where email = ?", new Object[]{values.get(COL_SENDER_EMAIL)});
				getContext().getContentResolver().notifyChange(CONTENT_URI_PROFILE, null);
			}
			break;

		case PROFILE_ALLROWS:
			id = db.insertOrThrow(TABLE_PROFILE, null, values);
			break;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}

		Uri insertUri = ContentUris.withAppendedId(uri, id);
		getContext().getContentResolver().notifyChange(insertUri, null);
		return insertUri;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		int count;
		switch(uriMatcher.match(uri)) {
		case MESSAGES_ALLROWS:
			count = db.update(TABLE_MESSAGES, values, selection, selectionArgs);
			break;			

		case MESSAGES_SINGLE_ROW:
			count = db.update(TABLE_MESSAGES, values, "_id = ?", new String[]{uri.getLastPathSegment()});
			break;

		case PROFILE_ALLROWS:
			count = db.update(TABLE_PROFILE, values, selection, selectionArgs);
			break;			

		case PROFILE_SINGLE_ROW:
			count = db.update(TABLE_PROFILE, values, "_id = ?", new String[]{uri.getLastPathSegment()});
			break;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);			
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		int count;
		switch(uriMatcher.match(uri)) {
		case MESSAGES_ALLROWS:
			count = db.delete(TABLE_MESSAGES, selection, selectionArgs);
			break;			

		case MESSAGES_SINGLE_ROW:
			count = db.delete(TABLE_MESSAGES, "_id = ?", new String[]{ uri.getLastPathSegment() });
			
			if(count <= 0) {
				count = db.delete(TABLE_MESSAGES, "uuid = ?", new String[]{ uri.getLastPathSegment() });
			}
			
			break;

		case PROFILE_ALLROWS:
			count = db.delete(TABLE_PROFILE, selection, selectionArgs);
			break;			

		case PROFILE_SINGLE_ROW:
			count = db.delete(TABLE_PROFILE, "_id = ?", new String[]{uri.getLastPathSegment()});
			break;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);			
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		return null;
	}	


	/**
	 * The Class DbHelper.
	 */
	private static class DbHelper extends SQLiteOpenHelper {
		
		/** The Constant DATABASE_NAME. */
		private static final String DATABASE_NAME = "instachat.db";
		
		/** The Constant DATABASE_VERSION. */
		private static final int DATABASE_VERSION = 1;
		
		/**
		 * Instantiates a new db helper.
		 *
		 * @param context the context
		 */
		public DbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/* (non-Javadoc)
		 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("create table messages ("
					+ "_id integer primary key autoincrement, "
					+ COL_TYPE        	  	+	" integer, "					
					+ COL_MESSAGE			+	" text, "
					+ COL_SENDER_EMAIL		+	" text, "
					+ COL_RECEIVER_EMAIL	+	" text, "
					+ COL_UUID			  	+	" text, "
					+ COL_TTL			  	+	" integer, "
					+ COL_ATTACHMENT		+	" text, "
					+ COL_TIME 			  	+	" datetime default current_timestamp);");

			db.execSQL("create table profile("
					+ "_id integer primary key autoincrement, "
					+ COL_NAME 	  +" text, "
					+ COL_EMAIL   +" text unique, "
					+ COL_ACCEPTED   +" integer, "
					+ COL_COUNT   +" integer default 0);");
		}

		/* (non-Javadoc)
		 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}
}