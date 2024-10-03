package cc.lib.android;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import cc.lib.utils.FileUtils;

/**
 * Created by chriscaron on 9/30/17.
 */

public class MediaScannerAsyncTask extends AsyncTask<String, Integer, Object> implements FilenameFilter, MediaScannerConnection.MediaScannerConnectionClient {

    private final String TAG = getClass().getSimpleName();

    private final Context context;
    private MediaScannerConnection mConnection;
    private int totalFilesFound = 0;

    public MediaScannerAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected Object doInBackground(String... params) {
        try {

            final long t = SystemClock.uptimeMillis();
            setProgress("Searching SD Card");
            for (File f : Environment.getExternalStorageDirectory().listFiles(this)) {
                addFilesR(f);
            }
            Context ctx = context;//getActivityBase();

            ContentResolver r = ctx.getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            try (Cursor ca = r.query(uri, new String [] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA
            }, null, null, null)) {
                Log.d(TAG, "Found " + ca.getCount() + " matches");
                setProgress("Media DB has " + ca.getCount() + " Records");
                int deletedFiles = 0;
                for (ca.moveToFirst(); !ca.isAfterLast(); ca.moveToNext()) {
                    String data = ca.getString(ca.getColumnIndex(MediaStore.Audio.Media.DATA));
                    File file = new File(data);
                    if (!file.exists()) {
                        deletedFiles++;
                        setProgress("Deleted " + deletedFiles + " records");
                        Log.d(TAG, "Trying to delete record for non-existant file: " + data);
                        r.delete(uri, MediaStore.MediaColumns.DATA + "=?", new String[]{
                                data
                        });
                    }
                }
                setProgress("Deleted " + deletedFiles + " records");
            }

            // at this point it is possible we have broadcast 1000's of intents to the media store.
            // May be prudent to wait a couple secs for things to cool off.
            //setProgress("Scanning", 5, 3);
            mConnection = new MediaScannerConnection(ctx, this);
            mConnection.connect();
            synchronized (mConnection) {
                mConnection.wait(60*1000);
            }
            final long dt = SystemClock.uptimeMillis() - t;
            Log.d(TAG, "Scanned media in " + dt/1000 + " secs " + dt%1000 + " MSecs");
            synchronized (this) {
                wait(2000);
            }
            mConnection.disconnect();

        } catch (Exception e) {
            return e;
        }

        return null;
    }


    // start the scan when scanner is ready
    @Override
    public void onMediaScannerConnected() {
        mConnection.scanFile(Environment.getExternalStorageDirectory().getPath(), "audio/*");
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        setProgress("Scan Completed");
        Log.d(TAG, "onScanCompleted '" + path + "' uri: " + uri);
        synchronized (mConnection) {
            mConnection.notify();
        }
    }

    private final static String [] fileExtensions = {
            "wav", "aiff", "amr", "mp3", "ogg", "oga", "wav", "wma"
    };

    static {
        Arrays.sort(fileExtensions);
    }

    @Override
    public boolean accept(File file, String s) {
        if (!file.canRead() || file.isHidden())
            return false;
        if (file.isDirectory())
            return true;
        return Arrays.binarySearch(fileExtensions, FileUtils.fileExtension(s)) >= 0;
    }

    protected void setProgress(String message) {}

    private void addFilesR(File dir) {
        File [] files = dir.listFiles();
        if (files != null) {
            if (files.length > 0) {
                totalFilesFound += files.length;
                setProgress("Found " + totalFilesFound + " Files");
            }
            for (File f : files) {
                if (f.isDirectory()) {
                    addFilesR(f);
                    continue;
                }
                if (f.isFile() && f.canRead()) {
                    String fileName = f.getPath();
                    Log.d(TAG, "Adding file '" + fileName + "'");
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(new File(fileName)));
                    context.sendBroadcast(intent);
                }
            }
        }
    }
}
