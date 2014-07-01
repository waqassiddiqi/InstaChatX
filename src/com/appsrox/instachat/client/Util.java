/*
 * 
 */
package com.appsrox.instachat.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Environment;


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
	
	public static String compressAndCopy(String filePath) throws IOException {
		
		File compressedPictureFile = new File(
				Environment.getExternalStorageDirectory() + "/instaChat/" +
				System.currentTimeMillis() + "_" + new File(filePath).getName());
		
	    
		int compressRatio = (int) Math.floor((float) ((float)1000 / getFileSize(filePath)) * 100);
		
		compressRatio -= 10;
		
		Bitmap bitmap = BitmapFactory.decodeFile(filePath);
		FileOutputStream fOut = new FileOutputStream(compressedPictureFile);
        boolean compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, compressRatio, fOut);
        
        if(compressed) {
        	if (!compressedPictureFile.exists()) {
    	        compressedPictureFile.createNewFile();
    	    }
        }
        
        fOut.flush();
        fOut.close();
        
        return compressedPictureFile.getPath();
	}
}