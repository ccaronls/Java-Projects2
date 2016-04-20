package cecc.android.lib;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import cc.lib.android.BuildConfig;
import cc.lib.android.CCActivityBase;
import cc.lib.android.R;

public abstract class BillingActivity extends CCActivityBase {

	public final static String PREF_PURCHASE_SKU = "PREF_PURCHASE_SKU";
	public final static String PREF_PURCHASE_RANDOM_STRING = "PREF_PURCHASE_RANDOM";
	public final static String PREF_LAST_SUBSCRIPTION_CHECK_LONG = "PREF_LAST_SUBSCRIPTION_CHECK";
	public final static String PREF_SUBSCRIPTION_ACTIVE_BOOL = "PREF_SUBSCRIPTION_ACTIVE";

	private IInAppBillingService mBillingService;

	@Override
	protected void onResume() {
		super.onResume();
		isAutoClockEnabled(true);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unbindFromBilling();
	}
	
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
	
	public final IInAppBillingService getBilling() {
		if (mBillingService == null) {
			bindToBilling();
		}
		if (mBillingService == null)
			throw new BillingNotAvailableException();
		return mBillingService;
	}
	
	public final static int REQUEST_PURCHASE = 98989871;
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult code=" + requestCode + " result=" + resultCode);
		switch (requestCode) {
			case REQUEST_PURCHASE: {
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
    						getPrefs().edit().remove(PREF_PURCHASE_RANDOM_STRING).apply();
    					}	            
    				}
    				catch (JSONException e) {
    					//alert("Failed to parse purchase data.");
    					showAlert(R.string.popup_title_error, R.string.popup_msg_billing_err_general);
    					e.printStackTrace();
    				}
    			}
    		
    			break;
			}
			
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	protected void showAlert(final int titleResId, final int messageResId, final Object ... params) {
		if (Build.VERSION.SDK_INT < 11) {
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				public void run() {
					newDialogBuilder().setTitle(titleResId).setMessage(getString(messageResId, params)).setNegativeButton(R.string.popup_button_ok, null).show();
				}
			});
		} else {
			newDialogBuilder().setTitle(titleResId).setMessage(getString(messageResId, params)).setNegativeButton(R.string.popup_button_ok, null).show();
		}
	}
	
	protected abstract void finalizePurchase(String sku, long purchaseTime);
	
	protected abstract AlertDialog.Builder newDialogBuilder();
	
	private static AlertDialog settingsDialog = null;
	
	protected boolean isAutoClockEnabled(boolean showBlockingPopupIfItsNot) {
		
		boolean enabled =false;
		if (android.os.Build.VERSION.SDK_INT >= 17) {
			enabled = isAutoClockEnabledAPI17AndHigher();
		} else {
			enabled = isAutoClockEnabledAPI16AndLower();
		}
		if (!enabled && showBlockingPopupIfItsNot) {
			if (settingsDialog == null) {
    			settingsDialog = newDialogBuilder()
    				.setCancelable(false)
    				.setTitle("Auto Clock Disabled")
    				.setMessage("This app requires automatic date and time to be enabled in order to work correctly.  Please enable auto clock to continue.")
    				.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
    					
    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    						settingsDialog = null;
    						startActivity(new Intent(Settings.ACTION_DATE_SETTINGS));
    					}
    				}).show();
			}
		}
		return enabled;
	}
	
	@TargetApi(17)
	private boolean isAutoClockEnabledAPI17AndHigher() {
		return android.provider.Settings.Global.getInt(getContentResolver(), android.provider.Settings.Global.AUTO_TIME, 0) != 0;
	}

	@SuppressWarnings("deprecation")
	private boolean isAutoClockEnabledAPI16AndLower() {
		return android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.AUTO_TIME, 0) != 0;
	}

}
