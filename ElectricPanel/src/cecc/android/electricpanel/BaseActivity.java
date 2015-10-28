package cecc.android.electricpanel;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import cecc.android.lib.BillingActivity;
import cecc.android.lib.BillingTask;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class BaseActivity extends BillingActivity implements OnClickListener {

	final String TAG = getClass().getSimpleName();
	
	public final static String INTENT_FORM = "iFORM";
	public final static String INTENT_FULL_NAME_STRING = "iFULL_NAME";
	public final static String INTENT_BITMAP_FILE = "iBITMAP_FILE";
	public final static String INTENT_ERROR = "iERROR";
	
	private final static boolean INAPP_ENABLED = !BuildConfig.DEBUG;
	
	static SimpleDateFormat dateFormat = new SimpleDateFormat("E M/d/yy h:mm a", Locale.US); 

	private DBHelper helper;

	DateFormat getDateFormatter() {
		return dateFormat;
	}
	
	protected AlertDialog.Builder newDialogBuilder() {
		return new AlertDialog.Builder(this, R.style.DialogTheme);
	}
	
	final BaseActivity getActivity() {
		return this;
	}
	
/*
	@Override
	public void onCorruption(SQLiteDatabase dbObj) {
		newDialogBuilder().setTitle("SQL Error").setMessage("DB Corruption Error").setNegativeButton("OK", null).show();
	}
*/
	@Override
	public void onClick(View v) {
		Log.e(TAG, "Unhandled click");
	}
	

	private SensorManager ambientSensor;
	private Sensor temp;
	
	public final int convertCelciusToFahrenheit(float celcius) {
		return Math.round(celcius * 9f/5 + 32);
	}
	
	class AmbientListener implements SensorEventListener, Runnable {

		float degreesC = 0;
		
		@Override
		public void run() {
			onAmbientTemperature(degreesC, convertCelciusToFahrenheit(degreesC));
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			degreesC = event.values[0];
			runOnUiThread(this);
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private final AmbientListener ambientListener = new AmbientListener();
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		ambientSensor = (SensorManager)getSystemService(SENSOR_SERVICE);
		if (ambientSensor != null) {
			temp = ambientSensor.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		}
		/*
		 * TODO: Call this from splash only if this is first launch from fresh install
		if (getPrefs().getString(PREF_PURCHASE_SKU, null) == null) {
			// check for purchases in case this is a re-install
			new BillingTask(Op.QUERY_PURCHASED, this).execute();
		}*/
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (helper != null)
			helper.close();
		if (ambientSensor != null) {
			ambientSensor.unregisterListener(ambientListener);
		}
		stopPolling();
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		if (ambientSensor != null) {
    		ambientSensor.registerListener(ambientListener, temp, SensorManager.SENSOR_DELAY_NORMAL);
		}		 
	}
	
	/**
	 * Called on ui thread when the ambient sensor reports.  base method does nothing.
	 * @param celcius
	 * @param farhenheit
	 */
	protected void onAmbientTemperature(float celcius, int farhenheit) {}
	
	public final boolean isAmbientTempAvailable() {
		return ambientSensor != null && temp != null;
	}
	
	public final float getAmbientTempCelcius() {
		return ambientListener.degreesC;
	}
	
	public final DBHelper getDBHelper() {
		if (helper == null || !helper.getDB().isOpen())
			helper = new DBHelper(this);
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
		/*
		HashSet<String> usedImages = new HashSet<String>();
		usedImages.addAll(Arrays.asList(getFormHelper().getDistictValuesForColumn(SQLHelper.FormColumn.IMAGE1)));
		usedImages.addAll(Arrays.asList(getFormHelper().getDistictValuesForColumn(SQLHelper.FormColumn.IMAGE2)));
		usedImages.addAll(Arrays.asList(getFormHelper().getDistictValuesForColumn(SQLHelper.FormColumn.IMAGE3)));
		File [] images = getImagesPath().listFiles();
		for (File f : images) {
			if (!usedImages.contains(f.getName())) {
				Log.i(TAG, "Deleting unused image " + f);
				f.delete();
			}
		}*/
		File [] images = getImagesPath().listFiles();
		for (File f : images) {
			if (getDBHelper().getImageByPath(f.getName()) == null) {
				f.delete();
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult code=" + requestCode + " result=" + resultCode);
		switch (requestCode) {
			
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	public final static String PREF_PREMIUM_UNLOCKED_BOOL = "PREF_PREMIUM_UNLOCKED";
	public final static String PREF_PREMIUM_EXPIRE_TIME_LONG = "PREF_PREMIUM_EXPIRE_TIME";
	
	public void clearPurchaseData() {
		getPrefs().edit().remove(PREF_PREMIUM_UNLOCKED_BOOL)
						.remove(PREF_PREMIUM_EXPIRE_TIME_LONG)
						.remove(PREF_PURCHASE_SKU).commit();
	}
	
	// TODO: Move this outside so that this class can be re-usable
	public enum Purchase {
		PREMIUM("premium"),
		PREMIUM_REDUCED("premium.reduced"), // available only while weekly or monthly subscription is active.
		ONEMONTH("onemonth"),
		ONEWEEK("oneweek"),
		TENMINUTES("ten.mins.debug") // DEBUG ONLY
		;
		
		public static Purchase getPurchaseFromSku(String sku) {
			for (Purchase p : values()) {
				if (sku.equals(p.sku))
					return p;
			}
			return null;
		}
		
		
		private Purchase(String sku) {
			this.sku = sku;
		}
		
		final String sku;
	}

	public void finalizePurchase(String sku, long purchaseTime) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(purchaseTime);
		Log.d(TAG, "finalizePurchase sku=" + sku + " datePurchased = " + cal.getTime());
		Purchase p = Purchase.getPurchaseFromSku(sku);
		if (p != null) {
			
			clearPurchaseData();
    		Editor edit = getPrefs().edit();
    		edit.putString(PREF_PURCHASE_SKU, sku);
    		edit.putBoolean(PREF_PREMIUM_UNLOCKED_BOOL, true);
    		Date expireTime = null;
    		switch (p) {
    			case ONEMONTH:
    				cal.add(Calendar.MONTH, 1);
    				expireTime = cal.getTime();
    				break;
    			case PREMIUM:
    			case PREMIUM_REDUCED:
    				break;
    			case ONEWEEK:
    				cal.add(Calendar.WEEK_OF_YEAR, 1);
    				expireTime = cal.getTime();
    				break;
    			case TENMINUTES:
    				cal.setTimeInMillis(purchaseTime);
    				cal.add(Calendar.MINUTE, 10);
    				expireTime = cal.getTime();
    				break;
    		}
    		
    		if (expireTime != null) {
    			if (expireTime.getTime() > System.currentTimeMillis()) { 
    				edit.putLong(PREF_PREMIUM_EXPIRE_TIME_LONG, expireTime.getTime());
        			SimpleDateFormat fmt = new SimpleDateFormat("EEEE MMMM dd", Locale.US);
        			showAlert(R.string.popup_title_purchase_complete, R.string.popup_msg_subscription_activiated, fmt.format(expireTime));
    			}
    		} else {
    			edit.remove(PREF_PREMIUM_EXPIRE_TIME_LONG);
    			showAlert(R.string.popup_title_purchase_complete, R.string.popup_msg_premium_permanently_unlocked);
    		}
    		edit.commit();
		}
	}

	protected boolean isSubscription() {
		if (!INAPP_ENABLED)
			return false;
		
		return getPrefs().getLong(PREF_PREMIUM_EXPIRE_TIME_LONG, -1) > 0 && !isSubscriptionExpired(true);
	}
	
	private boolean isSubscriptionExpired(boolean showDialog) {
		long expireTime = getPrefs().getLong(PREF_PREMIUM_EXPIRE_TIME_LONG, -1);
		if (expireTime > 0) {
			
			if (expireTime < System.currentTimeMillis()) {
    			String sku = getPrefs().getString(PREF_PURCHASE_SKU, "");
    			new BillingTask(BillingTask.Op.CONSUME_SKU, getActivity()).execute(sku);
    			getPrefs().edit().remove(PREF_PREMIUM_UNLOCKED_BOOL)
    				.remove(PREF_PREMIUM_EXPIRE_TIME_LONG)
    				.remove(PREF_PURCHASE_SKU).commit();
    			
    			if (showDialog) {
        			newDialogBuilder().setTitle(R.string.popup_title_subs_expired)
        				.setMessage(R.string.popup_msg_subs_expired)
        				.setNegativeButton(R.string.popup_button_not_now, null)
        				.setPositiveButton(R.string.popup_button_purchase_options, new DialogInterface.OnClickListener() {
        					
        					@Override
        					public void onClick(DialogInterface dialog, int which) {
        						new BillingTask(BillingTask.Op.QUERY_PURCHASABLES, getActivity()).execute();
        					}
        				}).show();
    			}
    			return true;
			}
		}
		return false;
	}
	
	protected String getSubscriptionExpireTimeColloquial() {
		long expireTime = getPrefs().getLong(PREF_PREMIUM_EXPIRE_TIME_LONG, -1);
		int secsBetween = (int)((expireTime - System.currentTimeMillis()) / 1000);
		
		int days = secsBetween / (24*60*60);
		int hours = secsBetween / (60*60);
		int mins = secsBetween / 60;
		
		if (days >= 1)
			return getResources().getQuantityString(R.plurals.plural_days, days, days);
		if (hours >= 1)
			return getResources().getQuantityString(R.plurals.plural_hours, hours, hours);
		return getResources().getQuantityString(R.plurals.plural_mins, mins, mins);
	}
	
	protected final boolean isPremiumEnabled(boolean showDialog) {
		
		if (!INAPP_ENABLED)
			return true;
		
		if (!getPrefs().getBoolean(PREF_PREMIUM_UNLOCKED_BOOL, false)) {
			if (showDialog)
				showPremiumLockedDialog();
			return false;
		}
		
		if (isSubscriptionExpired(showDialog)) {
			return false;
		}
		
		return true;
	}
	
	protected void showPremiumLockedDialog() {
		newDialogBuilder().setTitle(R.string.popup_title_premium_upgrade)
			.setMessage(R.string.popup_msg_premium_feature_locked)
			.setNegativeButton(R.string.popup_button_not_now, null)
			.setPositiveButton(R.string.popup_button_purchase_options, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					String [] skus = null;
					if (isSubscription()) {
    					skus = new String[] { Purchase.PREMIUM_REDUCED.sku };
    				} else {
    					skus = new String[] {
    							Purchase.ONEWEEK.sku,
    							Purchase.ONEMONTH.sku,
    							Purchase.PREMIUM.sku
    					};
//    					if (BuildConfig.DEBUG) {
  //  						skus.add(Purchase.TENMINUTES.sku);
    //					}
    				}
					new BillingTask(BillingTask.Op.QUERY_PURCHASABLES, BaseActivity.this).execute(skus);
				}
			}).show();
	}
	
	protected String getVersionString() throws NameNotFoundException {
		PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		return pInfo.versionName + "." + (BuildConfig.DEBUG ? "DEBUG" : pInfo.versionCode);
	}
}
