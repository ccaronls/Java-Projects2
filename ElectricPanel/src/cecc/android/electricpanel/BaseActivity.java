package cecc.android.electricpanel;

import java.io.File;
import cecc.android.lib.CECCBaseActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class BaseActivity extends CECCBaseActivity implements OnClickListener {

	final String TAG = getClass().getSimpleName();
	
	public final static String IMAGE_PREFIX = "capture";
	public final static int    IMAGE_CAPTURE_DIM = 512;
	
	private DBHelper helper;

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
	
	
	/**
	 * Called on ui thread when the ambient sensor reports.  base method does nothing.
	 * @param celcius
	 * @param farhenheit
	 */
	protected void onAmbientTemperature(float celcius, int farhenheit) {}

	public final DBHelper getDBHelper() {
		if (helper == null || !helper.getDB().isOpen())
			helper = new DBHelper(this);
		return helper;
	}
	
	void cleanupUnusedImages() {
		File [] images = getImagesPath().listFiles();
		for (File f : images) {
			if (!f.getName().startsWith(IMAGE_PREFIX))
				f.delete();
		}
	}
	
}
