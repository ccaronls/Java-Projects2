package cc.android.photooverlay;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import cc.android.photooverlay.BillingTask.Op;
import cc.android.photooverlay.BillingTask.Purchase;

import com.android.vending.billing.IInAppBillingService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;

public class BaseActivity extends Activity implements OnClickListener {

	final String TAG = getClass().getSimpleName();
	
	public final static String INTENT_FORM = "iFORM";
	public final static String INTENT_FULL_NAME_STRING = "iFULL_NAME";
	public final static String INTENT_BITMAP_FILE = "iBITMAP_FILE";
	public final static String INTENT_ERROR = "iERROR";
	
	static SimpleDateFormat dateFormat = new SimpleDateFormat("E M/d/yy h:mm a", Locale.US); 

	private SQLHelper helper;

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
	
	final BaseActivity getActivity() {
		return this;
	}
	
	final boolean isDebug() {
		return BuildNum.buildNum.equals("DEBUG");
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
	
	private Timer pollingTimer = null;
	
	/**
	 * Start polling.  onPoll will be called on UI thread at regular intervals until the
	 * activity is paused or user calls stopPolling.
	 * 
	 * @param intervalSeconds
	 */
	protected void startPolling(int intervalSeconds) {
		if (pollingTimer == null) {
			pollingTimer = new Timer();
			pollingTimer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					runOnUiThread(pollRunnable);
				}
			}, 1000, intervalSeconds * 1000);
		}
	}
	
	protected void stopPolling() {
		if (pollingTimer != null) {
			pollingTimer.cancel();
			pollingTimer = null;
		}
	}
	
	private Runnable pollRunnable = new Runnable() {
		public void run() {
			onPoll();
		}
	};
	
	/**
	 * Override this method to handle your polling needs.  Base method just logs to LogCat a warning.
	 */
	protected void onPoll() {
		Log.w(TAG, "onPoll not handled");
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
	
	public final SQLHelper getFormHelper() {
		if (helper == null || !helper.getDB().isOpen())
			helper = new SQLHelper(this);
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
    	            	newDialogBuilder().setTitle("Error")
    	            		.setMessage("There seems to ba a problem with your purchase.  Please try your request again")
    	            		.setNegativeButton("Ok", null)
    	            		.show();
    	            getPrefs().edit().remove(PREF_PURCHASE_RANDOM_STRING).commit();
	        	 }	            
	          }
	          catch (JSONException e) {
	             //alert("Failed to parse purchase data.");
	        	  newDialogBuilder().setTitle("Error")
	        	  	.setMessage("There was an error parsing purchase data.  Please contact support")
	        	  	.setNegativeButton("Ok", null)
	        	  	.show();
	             e.printStackTrace();
	          }
	      }
	   }
	}
	
	private final static String PREF_PREMIUM_UNLOCKED_BOOL = "PREF_PREMIUM_UNLOCKED";
	private final static String PREF_PREMIUM_EXPIRE_TIME_LONG = "PREF_PREMIUM_EXPIRE_TIME";
	private final static String PREF_PURCHASE_SKU = "PREF_PURCHASE_SKU";
	public final static String PREF_PURCHASE_RANDOM_STRING = "PREF_PURCHASE_RANDOM";
	
	public void finalizePurchase(String sku, long purchaseTime) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(purchaseTime);
		Log.d(TAG, "finalizePurchase sku=" + sku + " datePurchased = " + cal.getTime());
		Purchase p = Purchase.getPurchaseFromSku(sku);
		if (p != null) {
    		Editor edit = getPrefs().edit();
    		edit.putString(PREF_PURCHASE_SKU, sku);
    		edit.putBoolean(PREF_PREMIUM_UNLOCKED_BOOL, true);
    		Date expireTime = null;
    		// set to midnight
    		cal.set(Calendar.HOUR_OF_DAY, 23);
    		cal.set(Calendar.MINUTE, 59);
    		cal.set(Calendar.SECOND, 59); 
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
    			default:
    				break;
    		}
    		
    		if (expireTime != null && expireTime.getTime() > System.currentTimeMillis()) {
    			edit.putLong(PREF_PREMIUM_EXPIRE_TIME_LONG, expireTime.getTime());
    			SimpleDateFormat fmt = new SimpleDateFormat("EEEE MMMM dd");
    			newDialogBuilder().setTitle("Purchase Complete")
    				.setMessage(getString(R.string.popup_msg_subscription_activiated, fmt.format(expireTime)))
    				.setNegativeButton("Ok", null)
    				.show();
    		} else {
    			edit.remove(PREF_PREMIUM_EXPIRE_TIME_LONG);
    			newDialogBuilder().setTitle("Purchase Complete").setMessage(R.string.popup_msg_premium_permanently_unlocked)
    				.setNegativeButton("Ok", null)
    				.show();
    		}
    		edit.commit();
		}
	}

	protected boolean isSubscription() {
		return getPrefs().getLong(PREF_PREMIUM_EXPIRE_TIME_LONG, -1) > 0 && !isSubscriptionExpired();
	}
	
	private boolean isSubscriptionExpired() {
		long expireTime = getPrefs().getLong(PREF_PREMIUM_EXPIRE_TIME_LONG, -1);
		if (expireTime > 0) {
			
			if (expireTime < System.currentTimeMillis()) {
    			String sku = getPrefs().getString(PREF_PURCHASE_SKU, "");
    			new BillingTask(Op.CONSUME_SKU, getActivity()).execute(sku);
    			getPrefs().edit().remove(PREF_PREMIUM_UNLOCKED_BOOL)
    				.remove(PREF_PREMIUM_EXPIRE_TIME_LONG)
    				.remove(PREF_PURCHASE_SKU).commit();
    			
    			newDialogBuilder().setTitle("Subscription Expired")
    				.setMessage("Your subscription has expired.  Would you like to re-activate it?")
    				.setNegativeButton("Not now", null)
    				.setPositiveButton("Purchase options", new DialogInterface.OnClickListener() {
    					
    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    						new BillingTask(Op.QUERY_PURCHASABLES, getActivity()).execute();
    					}
    				}).show();
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
		
		if (days > 1)
			return String.format("%d days", days);
		if (days > 0)
			return "1 day";
		if (hours > 1)
			return String.format("%d hours", hours);
		if (hours > 0)
			return "1 hour";
		if (mins > 1)
			return String.format("%d minutes", mins);
		return "1 minute";
	}
	
	protected final boolean isPremiumEnabled() {
		if (!getPrefs().getBoolean(PREF_PREMIUM_UNLOCKED_BOOL, false)) {
			showPremiumLockedDialog();
			return false;
		}
		
		if (isSubscriptionExpired()) {
			return false;
		}
		
		return true;
	}
	
	protected void showPremiumLockedDialog() {
		newDialogBuilder().setTitle("Premium upgrade")
			.setMessage(R.string.popup_msg_premium_feature_locked)
			.setNegativeButton("Not now", null)
			.setPositiveButton("Purchase Options", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					new BillingTask(Op.QUERY_PURCHASABLES, BaseActivity.this).execute();
				}
			}).show();
	}
}
