package cc.android.photooverlay;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;

public class BaseActivity extends Activity implements OnClickListener, DatabaseErrorHandler {

	final String TAG = getClass().getSimpleName();
	
	public final static String INTENT_FORM = "iFORM";
	public final static String INTENT_FULL_NAME_STRING = "iFULL_NAME";
	public final static String INTENT_BITMAP_FILE = "iBITMAP_FILE";
	public final static String INTENT_ERROR = "iERROR";
	
	static SimpleDateFormat dateFormat = new SimpleDateFormat("E M/d/yy h:mm a", Locale.US); 
	
	DateFormat getDateFormatter() {
		return dateFormat;
	}
	
	final SharedPreferences getPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(this);
	}

	final boolean isPortrait() {
		return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
	}
	
	final AlertDialog.Builder newDialogBuilder() {
		return new AlertDialog.Builder(this); // TODO: Theme this mutha
	}
	
	final Activity getActivity() {
		return this;
	}

	@Override
	public void onCorruption(SQLiteDatabase dbObj) {
		newDialogBuilder().setTitle("SQL Error").setMessage("DB Corruption Error").setNegativeButton("OK", null).show();
	}

	@Override
	public void onClick(View v) {
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (helper != null)
			helper.close();
	}
	
	public final SQLHelper getFormHelper() {
		if (helper == null || !helper.getDB().isOpen())
			helper = new SQLHelper(this, this);
		return helper;
	}
	
	public File getImagesPath() {
		File path = new File(getFilesDir(), "images");
		if (!path.isDirectory()) {
			if (!path.mkdir())
				throw new RuntimeException("Path '" + path + "' cannot be created and is not a directory");
		}
		return path;
	}
	
	void cleanupUnusedImages() {
		HashSet<String> usedImages = new HashSet<String>();
		usedImages.addAll(Arrays.asList(getFormHelper().getDistictValuesForColumn(SQLHelper.Column.IMAGE1)));
		usedImages.addAll(Arrays.asList(getFormHelper().getDistictValuesForColumn(SQLHelper.Column.IMAGE2)));
		usedImages.addAll(Arrays.asList(getFormHelper().getDistictValuesForColumn(SQLHelper.Column.IMAGE3)));
		File [] images = getImagesPath().listFiles();
		for (File f : images) {
			if (!usedImages.contains(f.getName())) {
				Log.i(TAG, "Deleting unused image " + f);
				f.delete();
			}
		}
		// check the cache.  
		File [] files = getCacheDir().listFiles();
		long size = 0;
		for (File f : files) {
			size += f.length();
		}
		
		Arrays.sort(files, new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {
				return (int)(o1.length() - o2.length());
			}
			
		});
		
		while (size > 10*1024*1024) {
			
		}
	}

	public final void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            View focused = getContentView().findFocus();
            if (focused != null) {
                imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            }
        }
    }
	
	public View getContentView() {
		return getWindow().getDecorView().findViewById(android.R.id.content);
	}
	
	private SQLHelper helper;
}
