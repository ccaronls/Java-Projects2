package cecc.android.mechdeficiency;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

import cc.lib.android.CCActivityBase;
import cecc.android.mechdeficiency.BillingTask.Op;
import cecc.android.mechdeficiency.BillingTask.Purchase;

import com.android.vending.billing.IInAppBillingService;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class BaseActivity extends CCActivityBase implements OnClickListener {

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
	
	final AlertDialog.Builder newDialogBuilder() {
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
		unbindFromBilling();
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
	
	private IInAppBillingService mBillingService;

	private final ServiceConnection mBillingServiceConn = new ServiceConnection() {
	   @Override
	   public void onServiceDisconnected(ComponentName name) {
	       mBillingService = null;
	   }

	   @Override
	   public void onServiceConnected(ComponentName name,
	      IBinder service) {
	       mBillingService = IInAppBillingService.Stub.asInterface(service);
	       synchronized (this) {
	    	   notify();
	       }
	   }
	};
	
	private void bindToBilling() {
		Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
		  serviceIntent.setPackage("com.android.vending");
		  bindService(serviceIntent, mBillingServiceConn, Context.BIND_AUTO_CREATE);
		  try {
			  synchronized (mBillingServiceConn) {
				  mBillingServiceConn.wait(5000);
			  }
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
	}
	
	private void unbindFromBilling() {
		if (mBillingService != null) {
	        unbindService(mBillingServiceConn);
	        mBillingService = null;
	    }		
	}
	
	public IInAppBillingService getBilling() {
		if (mBillingService == null) {
			bindToBilling();
		}
		return mBillingService;
	}
	
	public final static int REQUEST_PURCHASE = 1001;
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult code=" + requestCode + " result=" + resultCode);
	   if (requestCode == REQUEST_PURCHASE) {
	      int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
	      String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
	      String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

	      Log.d(TAG, "purchaseData=" + purchaseData);
	      Log.d(TAG, "dataSignature=" + dataSignature);
	      
	      /*
	       * INAPP_PURCHASE_DATA
            '{
               "orderId":"12999763169054705758.1371079406387615",
               "packageName":"com.example.app",
               "productId":"exampleSku",
               "purchaseTime":1345678900000,
               "purchaseState":0,
               "developerPayload":"bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ",
               "purchaseToken":"opaque-token-up-to-1000-characters" <--- Used for consume
             }'	       
	       */
	      if (resultCode == RESULT_OK && responseCode == 0) {
	         try {
	        	 if (purchaseData != null) {
    	            JSONObject jo = new JSONObject(purchaseData);
    	            Log.d(TAG, "purchase json=" + jo.toString(3));
    	            String sku = jo.getString("productId");
    	            String randomString = jo.getString("developerPayload");
    	            String savedRandom = getPrefs().getString(PREF_PURCHASE_RANDOM_STRING, "");
    	            long purchaseTime = jo.getLong("purchaseTime");
    	            //String token = jo.getString("purchaseToken");
    	            if (savedRandom.equals(randomString))
    	            	finalizePurchase(sku, purchaseTime);
    	            else
    		        	showAlert(R.string.popup_title_error, R.string.popup_msg_billing_err_general);
    	            getPrefs().edit().remove(PREF_PURCHASE_RANDOM_STRING).commit();
	        	 }	            
	          }
	          catch (JSONException e) {
	             //alert("Failed to parse purchase data.");
	        	  showAlert(R.string.popup_title_error, R.string.popup_msg_billing_err_general);
	             e.printStackTrace();
	          }
	      }
	   }
	}
	
	public final static String PREF_PREMIUM_UNLOCKED_BOOL = "PREF_PREMIUM_UNLOCKED";
	public final static String PREF_PREMIUM_EXPIRE_TIME_LONG = "PREF_PREMIUM_EXPIRE_TIME";
	public final static String PREF_PURCHASE_SKU = "PREF_PURCHASE_SKU";
	public final static String PREF_PURCHASE_RANDOM_STRING = "PREF_PURCHASE_RANDOM";
	
	public void clearPurchaseData() {
		getPrefs().edit().remove(PREF_PREMIUM_UNLOCKED_BOOL)
						.remove(PREF_PREMIUM_EXPIRE_TIME_LONG)
						.remove(PREF_PURCHASE_SKU).commit();
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
    			new BillingTask(Op.CONSUME_SKU, getActivity()).execute(sku);
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
        						new BillingTask(Op.QUERY_PURCHASABLES, getActivity()).execute();
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
					new BillingTask(Op.QUERY_PURCHASABLES, BaseActivity.this).execute();
				}
			}).show();
	}
	
	protected void showAlert(int titleResId, int messageResId, Object ... params) {
		newDialogBuilder().setTitle(titleResId).setMessage(getString(messageResId, params)).setNegativeButton(R.string.popup_button_ok, null).show();
	}
	
	protected String getVersionString() throws NameNotFoundException {
		PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		return pInfo.versionName + "." + (BuildConfig.DEBUG ? "DEBUG" : pInfo.versionCode);
	}
}
