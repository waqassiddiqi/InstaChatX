/*
 * 
 */
package com.appsrox.instachat.client;

import java.io.File;


/**
 * The Class Util.
 */
public class Util {
	
	/**
	 * Returns file size in KBs.
	 *
	 * @param filePath the file path
	 * @return the file size
	 */
	public static long getFileSize(String filePath) {
		File f = new File(filePath);
		return f.length() / 1024;
	}
	
	
}