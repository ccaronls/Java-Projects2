package cc.android.photooverlay;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class BaseActivity extends Activity implements OnClickListener, DatabaseErrorHandler {

	final String TAG = getClass().getSimpleName();
	
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
		// TODO Auto-generated method stub
		
	}

	/*
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		helper = new FormHelper(this, this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		helper = new FormHelper(this, this);
	}*/
	
	@Override
	protected void onPause() {
		super.onPause();
		helper.close();
	}
	
	public final FormHelper getFormHelper() {
		if (helper == null || !helper.getDB().isOpen())
			helper = new FormHelper(this, this);
		return helper;
	}
	
	public File getImagesPath() {
		File path = new File(getFilesDir(), "images");
		path.mkdir();
		return path;
	}
	
	void cleanupUnusedImages() {
		HashSet<String> usedImages = new HashSet<String>();
		usedImages.addAll(Arrays.asList(getFormHelper().getDistictValuesForColumn(FormHelper.Column.IMAGE1)));
		usedImages.addAll(Arrays.asList(getFormHelper().getDistictValuesForColumn(FormHelper.Column.IMAGE2)));
		File [] images = getImagesPath().listFiles();
		for (File f : images) {
			if (!usedImages.contains(f.getName())) {
				Log.i(TAG, "Deleting unused image " + f);
				f.delete();
			}
		}
	}

	
	private FormHelper helper;
}
