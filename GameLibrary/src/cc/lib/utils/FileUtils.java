package cc.lib.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

	public static void stringToFile(String s, File dest) throws IOException {
		if (dest.isDirectory())
			throw new IOException("stringToFile writing to an existing directory '" + dest + "'");
    	FileOutputStream out = new FileOutputStream(dest);
    	try {
    		
    		out.write(s.getBytes());
    		
    	} finally {
    		out.close();
    	}
	}
	
	public static String fileToString(File file) throws IOException {
		FileInputStream in = new FileInputStream(file);
		try {
			byte [] buffer = new byte[in.available()];
			in.read(buffer);
			return new String(buffer);
		} finally {
			in.close();
		}
	}

	public static boolean backupFile(String fileName, int maxNum) {

		File file = new File(fileName);
		if (file.exists()) {
    		int index = fileName.lastIndexOf('.');
    		try {
    			int num= Integer.valueOf(fileName.substring(index+1));
    			if (num++ < maxNum) {
    				String newName = fileName.substring(0, index) + "." + num;
    				backupFile(newName, maxNum);
    				return file.renameTo(new File(newName));
    			}
    		} catch (NumberFormatException e) {
    			String newName = fileName + ".0";
    			backupFile(newName, maxNum);
    			return file.renameTo(new File(newName));
    		} 
		}		
		return false;
	}
	
	public static boolean restoreFile(String fileName) {
		System.err.println("restore file " + fileName);
		File file = new File(fileName);
		int index = fileName.lastIndexOf('.');
		int num = 0;
		String fileNamePrefix = fileName;
		try {
			num= Integer.valueOf(fileName.substring(index+1));
			fileNamePrefix = fileName.substring(0, index);
			num ++;
		} catch (NumberFormatException e) {
			
		}
		String copiedName = fileNamePrefix + "." + num;
		File copiedFile = new File(copiedName);
		boolean success = false;
		if (copiedFile.exists()) {
			file.delete();
			success = copiedFile.renameTo(file);
			System.err.println("Renaming " + copiedFile + " too " + fileName + " returns " + success);
			restoreFile(copiedName);
		}
		return success;
	}
		
	
}
