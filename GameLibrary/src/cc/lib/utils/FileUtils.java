package cc.lib.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public class FileUtils {

    final static Logger log = LoggerFactory.getLogger(FileUtils.class);

	/**
	 * 
	 * @param s
	 * @param dest
	 * @throws IOException
	 */
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
	
	/**
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static String inputStreamToString(InputStream in) throws IOException {
		try {
			byte [] buffer = new byte[in.available()];
			in.read(buffer);
			return new String(buffer);
		} finally {
			in.close();
		}
		
	}

    /**
     *
     * @param fileName
     * @return
     * @throws IOException
     */
	public static InputStream openFileOrResource(String fileName) throws IOException {
	    InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(fileName);
	    if (in == null) {
	        in = new FileInputStream(new File(fileName));
        }
	    return in;
    }
	
	/**
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String fileToString(File file) throws IOException {
		return inputStreamToString(new FileInputStream(file));
	}

	/**
	 * This is a recursive function with stack depth <= to maxNum
	 * rename a file such that the file of form root.ext is copied too root.num.ext when num is integer between 0 and maxNum.
	 * All other files of form root.num+n.ext are renamed accordingly with the file falling off when backups exceed maxNum
	 * 
	 * @param fileName
	 * @param maxNum
	 * @return true if the file exists and was successfully renamed
	 */
	public static boolean backupFile(String fileName, int maxNum) {
		String root = stripExtension(fileName);
		String extension = fileExtension(fileName);
		return backupFileR(fileName, root, extension, maxNum);
	}

    /**
     *
     * @param file
     * @param maxNum
     * @return
     */
	public static boolean backupFile(File file, int maxNum) {
	    if (file.exists()) {
	        return backupFile(file.getAbsolutePath(), maxNum);
        }
	    return false;
    }
	
	private static boolean backupFileR(String fileName, String root, String extension, int maxNum) {

		File file = new File(fileName);
		if ( file.exists() ) {
    		int index = root.lastIndexOf('.');
    		try {
    			int num= Integer.valueOf(root.substring(index+1));
    			if (num++ < maxNum) {
    				root = root.substring(0, index) + "." + num;
    				String newName = root + extension;
    				backupFileR(newName, root, extension, maxNum);
    				return file.renameTo(new File(newName));
    			}
    		} catch (NumberFormatException e) {
    			root += ".0";
    			String newName = root + extension;
    			backupFileR(newName, root, extension, maxNum);
    			return file.renameTo(new File(newName));
    		} 
		}
		return false;
	}

    /**
     * @see cc.lib.utils.FileUtils#restoreFile(String)
     * @param file
     * @return
     */
	public static boolean restoreFile(File file) {
	    return restoreFile(file.getAbsolutePath());
    }

    /**
     *
     * @param file
     * @return
     */
    public static boolean hasBackupFile(File file) {
	    return hasBackupFile(file.getAbsolutePath());
    }

    /**
     *
     * @param file
     * @return
     */
    public static long deleteFileAndBackups(File file) {
	    String fileName = file.getAbsolutePath();
        String root = stripExtension(file.getName());
        String ext = fileExtension(fileName);
	    File dir = file.getParentFile();
	    if (!dir.isDirectory())
	        return -1;
	    File [] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(root) && name.endsWith(ext);
            }
        });
	    if (files.length == 0)
	        return 0;
	    long bytesDeleted = 0;
	    for (File f : files) {
	        long size = f.length();
	        if (f.delete())
	            bytesDeleted+=size;
        }
	    return bytesDeleted;
    }

    /**
     *
     * @param fileName
     * @return
     */
    public static boolean hasBackupFile(String fileName) {
        String root = stripExtension(fileName);
        String ext = fileExtension(fileName);
        return new File(root + ".0" + ext).exists();
    }

    /**
	 * Reverse operation of backupFile.  For filename of form root.ext, if there exists a file of form
	 * root.0.ext, then it will be renamed to fileName.  For all other files of form root.n.ext they will
	 * be renamed too root.n-1.ext.
	 */
	public static boolean restoreFile(String fileName) {
		String root = stripExtension(fileName);
		String ext = fileExtension(fileName);
		return restoreFileR(fileName, root, ext);
	}
	
	private static boolean restoreFileR(String fileName, String root, String ext) {
		log.verbose("restore file " + fileName);
		File file = new File(fileName);
		int index = root.lastIndexOf('.');
		int num = 0;
		String fileNamePrefix = root;
		try {
			num= Integer.valueOf(root.substring(index+1));
			fileNamePrefix = root.substring(0, index);
			num ++;
		} catch (NumberFormatException e) {
			
		}
		String copiedName = fileNamePrefix + "." + num + ext;
		File copiedFile = new File(copiedName);
		boolean success = false;
		if (copiedFile.exists()) {
			file.delete();
			success = copiedFile.renameTo(file);
			log.verbose("Renaming " + copiedFile + " too " + fileName + " returns " + success);
			restoreFileR(copiedName, fileNamePrefix + "." + num, ext);
		}
		return success;
	}


	/**
	 * Return the string extension from a file including the dot '.' or empty string when no extension exists
	 * @param fileName
	 * @return
	 */
	public static String fileExtension(String fileName) {
		int dot = fileName.lastIndexOf('.');
		if (dot <= 0)
			return "";
		String ext = fileName.substring(dot);
		return ext;
	}

	/**
	 * Return the filename minus the extension including dot '.'
	 * dot must be beyond the front of the filename to omit empty extension hidden files from getting whole filename stripped.
	 * @param fileName
	 * @return
	 */
	public static String stripExtension(String fileName) {
		int dot = fileName.lastIndexOf('.');
		if (dot <= 0)
			return fileName;
		return fileName.substring(0, dot);
	}
	
	/**
	 * 
	 * @param dir
	 */
	public static void deleteDirContents(File dir) throws IOException {
		deleteDirContents(dir, "*");
	}
	
	public static void deleteDirContents(File dir, String matching) throws IOException {
		deleteDirContents(dir, Pattern.compile(matching.replace("*", ".*")));
	}

	public static void deleteDirContents(File dir, Pattern pattern) throws IOException {
	    if (!dir.isDirectory())
	        throw new IOException("Not a directory " + dir.getAbsolutePath());
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				deleteDirContents(f, pattern);
			} else if (pattern.matcher(f.getName()).matches()) {
			    log.verbose("Deleting '" + f.getAbsolutePath() + "'");
    			if (!f.delete())
    				throw new IOException("Failed to delete file '" + f.getAbsolutePath() + "'");
			}
		}
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

   public static long copy(InputStream in, OutputStream out) throws IOException {
       long bytesCopied = 0;
	   byte [] buffer = new byte[1024];
	   while (true) {
		   int len = in.read(buffer);
		   if (len < 0)
			   break;
		   bytesCopied += len;
		   out.write(buffer, 0, len);
	   }
	   return bytesCopied;
   }
   
    public static void copy(InputStream in, File outFile) throws IOException {
    	OutputStream out = new FileOutputStream(outFile);
    	try {
    		copy(in, out);
    	} finally {
    		out.close();
    	}
    }

    public static void copyFile(File inFile, OutputStream out) throws IOException {
        InputStream in = new FileInputStream(inFile);
        try {
            copy(in, out);
        } finally {
            out.close();
        }
    }

    public static void tryCopyFile(File source, File dest) {
       try {
           copyFile(source, dest);
       } catch (Exception e) {
           e.printStackTrace();
       }
    }

    public static void copyFile(File source, File dest) throws IOException {
    	InputStream in = new FileInputStream(source);
    	if (dest.isDirectory()) {
    		dest = new File(dest, source.getName());
    	}
    	try {
    		OutputStream out = new FileOutputStream(dest);
            try {
                copy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").equals("win");
    }

    public static File getOrCreateSettingsDirectory(Class<?> clazz) {
        File homeDir = new File(System.getProperty("user.home"));
        if (!homeDir.isDirectory()) {
            System.err.println("Failed to find users home dir: '" + homeDir);
            homeDir = new File(".");
        }
        String pkg = clazz.getCanonicalName().replace('.', '/');
        File settingsDir = new File(homeDir, "settings/" + pkg);
        if (!settingsDir.isDirectory()) {
            if (!settingsDir.mkdirs())
                throw new GException("Failed to create settings directory: " + settingsDir.getAbsolutePath());
            else
                log.info("Created settings directory: " + settingsDir.getAbsolutePath());
        }
        return settingsDir;
    }

    public static List<File> getFileAndBackups(File file) {
        String fileName = file.getAbsolutePath();
        String root = stripExtension(file.getName());
        String ext = fileExtension(fileName);
        File dir = file.getParentFile();
        if (!dir.isDirectory())
            return Collections.emptyList();
        File [] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(root) && name.endsWith(ext);
            }
        });
        return Arrays.asList(files);
    }
}
