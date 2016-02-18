package cc.android.photooverlay;

import java.io.File;
import cecc.android.lib.CECCBaseActivity;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class BaseActivity extends CECCBaseActivity implements OnClickListener {

	public final static String IMAGE_PREFIX = "guage";
	private SQLHelper helper;

	final BaseActivity getActivity() {
		return this;
	}
	
	@Override
	public void onClick(View v) {
		Log.e(TAG, "Unhandled click");
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (helper != null)
			helper.close();
	}
	
	public final SQLHelper getFormHelper() {
		if (helper == null || !helper.getDB().isOpen())
			helper = new SQLHelper(this);
		return helper;
	}

	void cleanupUnusedImages() {
		File [] images = getImagesPath().listFiles();
		for (File f : images) {
			if (!f.getName().startsWith(IMAGE_PREFIX)) {
				Log.i(TAG, "Deleting unused image " + f);
				f.delete();
			}
		}
	}
	
}
