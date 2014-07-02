/*
 * 
 */
package com.appsrox.instachat.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.DisplayMetrics;

import com.appsrox.instachat.Common;


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
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		
		Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
		options.inSampleSize = Util.calculateInSampleSize(options, 1024, 786);			
		options.inJustDecodeBounds = false;
		bitmap = BitmapFactory.decodeFile(filePath, options);
		
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
	
	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and
			// keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}
}