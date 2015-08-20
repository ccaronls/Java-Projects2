package cc.lib.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
		
	public static String fileExtension(String fileName) {
		int dot = fileName.lastIndexOf('.');
		if (dot <= 0)
			return "";
		String ext = fileName.substring(dot);
		return ext;
	}

	public static String stripExtension(String fileName) {
		int dot = fileName.lastIndexOf('.');
		if (dot <= 0)
			return fileName;
		return fileName.substring(0, dot);
	}
	
    /**
    *
    * @param target
    * @param files
    * @throws IOException
    */
   public static void zipFiles(File target, Collection<File> files) throws IOException {
       // Based on stackoverflow answer:
       // http://stackoverflow.com/a/14350841/642160
       ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(target));
       try {
           byte[] byteBuffer = new byte[1024];

           for (File file : files) {
               String name = file.getName();

               ZipEntry entry = new ZipEntry(name);
               zos.putNextEntry(entry);

               FileInputStream fis = null;
               try {
                   fis = new FileInputStream(file);
                   int bytesRead = -1;
                   while ((bytesRead = fis.read(byteBuffer)) != -1) {
                       zos.write(byteBuffer, 0, bytesRead);
                   }
                   zos.flush();
               } finally {
                   try {
                       if (fis != null)
                           fis.close();
                   } catch (IOException e) {
                       // Ignore. close shouldn't freaking throw exceptions!
                   }
               }
               zos.closeEntry();
           }

           zos.flush();
       } finally {
           zos.close();
       }
   }
   
}
