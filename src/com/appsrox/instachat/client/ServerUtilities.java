/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appsrox.instachat.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.appsrox.instachat.Common;
import com.appsrox.instachat.DataProvider;

// TODO: Auto-generated Javadoc
/**
 * Helper class to communicate with GAE server.
 */
public final class ServerUtilities {
	
	/** The Constant MAX_ATTEMPTS. */
	private static final int MAX_ATTEMPTS = 5;				//Max number of tries to send data to GAE server
	
	/** The Constant BACKOFF_MILLI_SECONDS. */
	private static final int BACKOFF_MILLI_SECONDS = 2000;
	
	/** The Constant random. */
	private static final Random random = new Random();		//Class to generate random number
	
	/** The Constant TAG. */
	public static final String TAG = "ServerUtilities";
	
	/**
	 * *
	 * On successful registration of email account with GCM, register this device on GAE server .
	 *
	 * @param email Account to register
	 * @param regId Registration ID returned by GCM on successful registration of this email account
	 */
	public static void register(final String email, final String regId) {
		Log.i(TAG, "registering device (regId = " + regId + ")");
		String serverUrl = Common.getServerUrl() + "/register";
		Map<String, String> params = new HashMap<String, String>();
		params.put(DataProvider.SENDER_EMAIL, email);
		params.put(DataProvider.REG_ID, regId);
		
		try {
			post(serverUrl, params, MAX_ATTEMPTS);
		} catch (IOException e) {
		}
	}

	
	/**
	 * *
	 * Once email account is unregistered from server, unregister it from GAE server as well.
	 *
	 * @param email Account to unregister
	 */
	public static void unregister(final String email) {
		Log.i(TAG, "unregistering device (email = " + email + ")");
		
		String serverUrl = Common.getServerUrl() + "/unregister";
		Map<String, String> params = new HashMap<String, String>();
		params.put(DataProvider.SENDER_EMAIL, email);
		try {
			post(serverUrl, params, MAX_ATTEMPTS);
		} catch (IOException e) {
		}
	}

	/**
	 * *
	 * Send chat message.
	 *
	 * @param msg Message to send
	 * @param to Address of recepient(s)
	 * @param data the data
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void send(String msg, String to, String data) throws IOException {
		Log.i(TAG, "sending message (msg = " + msg + ")");
		
		String serverUrl = Common.getServerUrl() + "/chat";
		Map<String, String> params = new HashMap<String, String>();
		params.put(DataProvider.MESSAGE, msg);
		params.put(DataProvider.SENDER_EMAIL, Common.getPreferredEmail());
		params.put(DataProvider.RECEIVER_EMAIL, to);
		params.put(DataProvider.DATA, data);
		
		post(serverUrl, params, MAX_ATTEMPTS);
	}


	/**
	 *  Issue a POST with exponential backoff.
	 *
	 * @param endpoint the endpoint
	 * @param params the params
	 * @param maxAttempts the max attempts
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void post(String endpoint, Map<String, String> params, int maxAttempts) throws IOException {
		long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
		for (int i = 1; i <= maxAttempts; i++) {
			Log.d(TAG, "Attempt #" + i);
			
			try {
				post(endpoint, params);
				return;
			} catch (IOException e) {
				Log.e(TAG, "Failed on attempt " + i + ":" + e);
				
				if (i == maxAttempts) {
					throw e;
				}
				try {
					Thread.sleep(backoff);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
					return;
				}
				backoff *= 2;    			
			} catch (IllegalArgumentException e) {
				throw new IOException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Issue a POST request to the server.
	 *
	 * @param endpoint POST address.
	 * @param params request parameters.
	 *
	 * @throws IOException propagated from POST.
	 */
	private static void post(String endpoint, Map<String, String> params) throws IOException {
		URL url;
		try {
			url = new URL(endpoint);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("invalid url: " + endpoint);
		}
		StringBuilder bodyBuilder = new StringBuilder();
		Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
		// constructs the POST body using the parameters
		while (iterator.hasNext()) {
			Entry<String, String> param = iterator.next();
			bodyBuilder.append(param.getKey()).append('=').append(param.getValue());
			if (iterator.hasNext()) {
				bodyBuilder.append('&');
			}
		}
		String body = bodyBuilder.toString();
		//Log.v(TAG, "Posting '" + body + "' to " + url);
		byte[] bytes = body.getBytes();
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setFixedLengthStreamingMode(bytes.length);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			// post the request
			OutputStream out = conn.getOutputStream();
			out.write(bytes);
			out.close();
			// handle the response
			int status = conn.getResponseCode();
			if (status != 200) {
				throw new IOException("Post failed with error code " + status);
			}
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	/**
	 * *
	 * Uploads file to server by calling uploadToSever along with unique message identifier to associate 
	 * uploaded file with chat message.
	 *
	 * @param file Path to file
	 * @param messageId Unique identifier to distinguish uploaded file and associate with chat message
	 * @return Remote path to uploaded file on successful upload, null otherwise
	 */
	public static String uplaod(String file, String messageId) {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("messageId", messageId));
		nameValuePairs.add(new BasicNameValuePair("attachment", file));
		
		//send file to server
		return uploadToSever(Common.getServerUrl() + "/attachment", nameValuePairs);
	}
	
	/**
	 * *
	 * Uploads file to server.
	 *
	 * @param url URL to post file content and message id to
	 * @param nameValuePairs file path and message ID in name-value pair format
	 * @return Remote path to uploaded file on successful upload, null otherwise
	 */
	private static String uploadToSever(String url, List<NameValuePair> nameValuePairs) {
		HttpClient httpClient = new DefaultHttpClient();
	    HttpContext localContext = new BasicHttpContext();
	    HttpPost httpPost = new HttpPost(url);

	    try {
	    	
	    	//build multipart enitity
	    	MultipartEntityBuilder builder = MultipartEntityBuilder.create();    
	    	builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

	    	
	    	
	    	//iterate through form fields and append them to POST request depending on type of field
	        for(int index=0; index < nameValuePairs.size(); index++) {
	            if(nameValuePairs.get(index).getName().equalsIgnoreCase("attachment")) {
	            	
	            	
	            	if(Util.getFileSize(nameValuePairs.get(index).getValue()) >= 1024) {
		            	Bitmap bmp = BitmapFactory.decodeFile(nameValuePairs.get(index).getValue());
		            	ByteArrayOutputStream bos = new ByteArrayOutputStream();
		            	bmp.compress(CompressFormat.PNG, 100, bos);
		            	InputStream in = new ByteArrayInputStream(bos.toByteArray());
		            	
		            	ContentBody foto = new InputStreamBody(in, "image/png", "filename");
		            	builder.addPart(nameValuePairs.get(index).getName(), foto);
	            	} else {
	            		builder.addPart(nameValuePairs.get(index).getName(), new FileBody(new File (nameValuePairs.get(index).getValue())));
	            	}	            	
	            	
	            } else {
	            	builder.addPart(nameValuePairs.get(index).getName(), new StringBody(nameValuePairs.get(index).getValue()));
	            }
	        }

	        //set httpentity
	        HttpEntity entity = builder.build();
	        httpPost.setEntity(entity);

	        //send request to server
	        HttpResponse response = httpClient.execute(httpPost, localContext);
	        
	        //get response from server
	        HttpEntity responseEntity = response.getEntity();
	        
	        String strResponse = "";
	        
	        if(responseEntity != null) {
	        	strResponse = EntityUtils.toString(responseEntity);
	        }
	        
	        return strResponse;
	        
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		
		return "";
	}
	
	/**
	 * *
	 * Downloads file from server and save it's content in filename provided.
	 *
	 * @param imageUrl URL to file to download
	 * @param filename Path to destination file on local SD card
	 * @return Path to file downloaded on local SD card
	 */
	public static String downloadFile(String imageUrl, String filename) {
		boolean b = false;
		
		//Check if directory exists, creates otherwise
		File direct = new File(Environment.getExternalStorageDirectory()
				+ "/instaChat");
		if (!direct.exists()) {
			b = direct.mkdirs();
		}

		try {
			//Contruct URL object and open connection
			URL url = new URL(imageUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			
			//Download file content from server
			InputStream input = connection.getInputStream();
			
			//Construct new image file from downloaded content
			Bitmap myBitmap = BitmapFactory.decodeStream(input);

			//Construct path on local SD card to save downloaded image
			String path = Environment.getExternalStorageDirectory() + "/instaChat/" + filename + ".png";
			
			//Write to file
			FileOutputStream stream = new FileOutputStream(path);

			ByteArrayOutputStream outstream = new ByteArrayOutputStream();
			myBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outstream);
			byte[] byteArray = outstream.toByteArray();

			stream.write(byteArray);
			stream.close();

			return path;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
