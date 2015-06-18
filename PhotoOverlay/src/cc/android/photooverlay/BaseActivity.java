package cc.android.photooverlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

public class BaseActivity extends Activity {

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
	

}
