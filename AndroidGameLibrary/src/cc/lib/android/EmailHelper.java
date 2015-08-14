package cc.lib.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.MimeTypeMap;

/**
 * This file allows external applications to read files we place into the cache directory.
 * 
 * The benefit of this is that we dont need to expose potentially sensitive data by copying files to external storage
 * 
 * Need following entry on the manifest inside <application> tag
 * 
 * <provider android:name="CachedFileProvider" android:authorities="@string/cached_file_provider_authority" android:exported="true"/>
 * 
 * @author chriscaron
 *
 */
public class EmailHelper extends ContentProvider {

    private static final String LOG_TAG = "CachedFileProvider";

    // UriMatcher used to match against incoming requests
    private UriMatcher uriMatcher;

    @Override
    public boolean onCreate() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Add a URI to the matcher which will match against the form
        // 'content://com.stephendnicholas.gmailattach.provider/*'
        // and return 1 in the case that the incoming Uri matches this pattern
        uriMatcher.addURI(getAuthority(getContext()), "*", 1);

        return true;
    }

    public static String getAuthority(Context context) {
    	return context.getResources().getString(R.string.cached_file_provider_authority);
    }
    
    private File getFile(Uri uri) throws FileNotFoundException {
    	int n = uriMatcher.match(uri);
        switch (n) {

        // If it returns 1 - then it matches the Uri defined in onCreate
        case 1:

            // The desired file name is specified by the last segment of the
            // path
            // E.g.
            // 'content://com.stephendnicholas.gmailattach.provider/Test.txt'
            // Take this and build the path to the file
            String segment = uri.getLastPathSegment();
            String fileLocation = getContext().getCacheDir() + File.separator
                    + segment;

            Log.d(LOG_TAG, "Returning file descriptor for file: " + fileLocation);
            File file =  new File(fileLocation);
            if (!file.exists())
            	throw new FileNotFoundException("Does not exist: " + file.getAbsolutePath());
            if (!file.isFile())
            	throw new FileNotFoundException("Not a file: " + file.getAbsolutePath());
            if (!file.canRead())
            	throw new FileNotFoundException("Not readable: " + file.getAbsolutePath());
            return file;
            
        default:
            Log.v(LOG_TAG, "Unsupported uri: '" + uri + "'.");
            throw new FileNotFoundException("Unsupported uri: "
                    + uri.toString());
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Log.i(LOG_TAG, "openFile uri=" + uri + " mode=" + mode);
        File file = getFile(uri);
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return pfd;
    }

    @Override
    public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
    	Log.d(LOG_TAG, "update uri=" + uri + " s=" + s + " as=" + Arrays.toString(as));
        return 0;
    }

    @Override
    public int delete(Uri uri, String s, String[] as) {
    	Log.d(LOG_TAG, "delete uri=" + uri + " s=" + s + " as=" + Arrays.toString(as));
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentvalues) {
    	Log.d(LOG_TAG, "insert uri=" + uri);
        return null;
    }

    @Override
    public String getType(Uri uri) {
    	try {
    		File file = getFile(uri);
    		String name = file.getName();
    		int dot = name.lastIndexOf('.');
    		if (dot > 0) {
    			String extension = file.getName().substring(dot);
    			String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    			Log.d(LOG_TAG, "getType uri=" + uri + " ext=" + extension + " mimeType=" + mimeType);
    			if (mimeType == null)
    				return "text/plain";
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String s, String[] as1, String s1) {
    	Log.d(LOG_TAG, "query uri=" + uri + " proj=" + Arrays.toString(projection) + " s=" + s + " as1=" + Arrays.toString(as1) + " s1=" + s1);
    	
    	try {
    		File file = getFile(uri);
    		
    		// observe logs to determine the columns email app asks for
    		final String [] cols = { "_display_name", "_size" };
    		final String [] vals = { file.getName(), String.valueOf(file.length()) };
    		
        	return new AbstractCursor() {
    			
    			@Override
    			public boolean isNull(int column) {
    				return column < 0 && column >= cols.length;
    			}
    			
    			@Override
    			public String getString(int column) {
    				return vals[column];
    			}
    			
    			@Override
    			public short getShort(int column) {
    				return Short.valueOf(vals[column]);
    			}
    			
    			@Override
    			public long getLong(int column) {
    				return Long.valueOf(vals[column]);
    			}
    			
    			@Override
    			public int getInt(int column) {
    				return Integer.valueOf(vals[column]);
    			}
    			
    			@Override
    			public float getFloat(int column) {
    				return Float.valueOf(vals[column]);
    			}
    			
    			@Override
    			public double getDouble(int column) {
    				return Double.valueOf(vals[column]); 
    			}
    			
    			@Override
    			public int getCount() {
    				return 1;
    			}
    			
    			@Override
    			public String[] getColumnNames() {
    				return cols;
    			}
    		};
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	return null;
    }
    
    /**
     * Open email application
     * @param context
     * @param to
     * @param subject
     * @param body
     * @param attachmentInCacheDir the file attachment to include.  Must be located in the cache dir (@see Context.getCacheDir())
     */
    public static void sendEmail(Context context, File attachmentInCacheDir, String to, String subject, String body) {
        Intent intent  = new Intent(Intent.ACTION_SEND);//TO, Uri.fromParts("mailto",to, null));
        if (to != null)
        	intent.setData(Uri.fromParts("mailto",to, null));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { to });
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        intent.setType("message/email");
        if (attachmentInCacheDir != null) {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://" + EmailHelper.getAuthority(context) + "/" + attachmentInCacheDir.getName()));
        }
        context.startActivity(Intent.createChooser(intent, "Send Email"));
    }
}
