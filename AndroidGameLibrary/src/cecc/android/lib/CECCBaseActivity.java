package cecc.android.lib;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import cc.lib.android.BuildConfig;
import cc.lib.android.EmailHelper;
import cc.lib.android.R;
import cc.lib.utils.FileUtils;

public abstract class CECCBaseActivity extends BillingActivity {

	public final static int IMAGE_CAPURTE_DIM = 512;
	
	public final static String INTENT_FORM = "iFORM";
	public final static String INTENT_FULL_NAME_STRING = "iFULL_NAME";
	public final static String INTENT_BITMAP_FILE = "iBITMAP_FILE";
	public final static String INTENT_ERROR = "iERROR";
	public final static String PREF_IS_METRIC_UNITS_BOOL = "IS_METRIC_UNITS";
	
	public final static String MIME_TYPE_PDF =  "application/pdf";
	public final static String GOOGLE_PDF_VIEWER_URL = "https://docs.google.com/gview?embedded=true&url=";
	
	private final static boolean INAPP_ENABLED = !BuildConfig.DEBUG;
	
	static SimpleDateFormat dateFormat = new SimpleDateFormat("E M/d/yy h:mm a", Locale.US); 

	public final DateFormat getDateFormatter() {
		return dateFormat;
	}
	
	@Override
	public final AlertDialog.Builder newDialogBuilder() {
		if (Build.VERSION.SDK_INT > 10)
			return new AlertDialog.Builder(this, R.style.DialogTheme);
		else
			return new AlertDialog.Builder(this);
	}
	
	public final void showInfoDialogBuilderWithDontShowAgainCB(int titleId, int msgId, final String boolPrefToNotShowAgain) {
		if (getPrefs().getBoolean(boolPrefToNotShowAgain, true)) {
 		
			final View v = View.inflate(this, R.layout.popup_message_info, null);
			((TextView)v.findViewById(R.id.tvMessage)).setText(msgId);
			final CheckBox cbDontShowAgain = (CheckBox)v.findViewById(R.id.cbDontShowAgain);
			newDialogBuilder()
    			.setTitle(titleId)
    			.setView(v)
        		.setNegativeButton(R.string.popup_button_ok, new DialogInterface.OnClickListener() {
    				
    				@Override
    				public void onClick(DialogInterface dialog, int which) {
    					if (cbDontShowAgain.isChecked()) {
    						getPrefs().edit().putBoolean(boolPrefToNotShowAgain, false).commit();
    					}
    				}
    			}).show();
		}
	}
	
	private SensorManager ambientSensor;
	private Sensor temp;
	
	public final boolean isMetricUnits() {
		return getPrefs().getBoolean(PREF_IS_METRIC_UNITS_BOOL, getResources().getInteger(R.integer.units_metric_int) != 0);
	}
	
	public final void toggleUnits() {
		boolean isMetric = isMetricUnits();
		getPrefs().edit().putBoolean(PREF_IS_METRIC_UNITS_BOOL, !isMetric).commit();
	}

	public final void setUnitsMetric(boolean metric) {
		getPrefs().edit().putBoolean(PREF_IS_METRIC_UNITS_BOOL, metric).commit();
	}

	public final String getTemperatureString(float degreesCelcius) {
		if (isMetricUnits()) {
			return String.format("%.1f&deg;C", degreesCelcius);
		} else {
			return String.format("%d&deg;F", Math.round(degreesCelcius * 9f/5 + 32));
		}
	}
	
	class AmbientListener implements SensorEventListener, Runnable {

		float degreesC = 0;
		
		@Override
		public void run() {
			onAmbientTemperature(degreesC);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ambientSensor = (SensorManager) getSystemService(SENSOR_SERVICE);
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
		if (ambientSensor != null) {
			ambientSensor.unregisterListener(ambientListener);
		}
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
	 */
	protected void onAmbientTemperature(float celcius) {}
	
	/**
	 * Return whether this device supports temp
	 * @return
	 */
	public final boolean isAmbientTempAvailable() {
		return ambientSensor != null && temp != null;
	}
	
	/**
	 * Will throw NPE if isAmbientTempAvailable
	 * @return
	 */
	public final float getAmbientTempCelcius() {
		return ambientListener.degreesC;
	}
	
	public final File getImagesPath() {
		File path = new File(getFilesDir(), "images");
		if (!path.isDirectory()) {
			if (!path.mkdir())
				throw new RuntimeException("Path '" + path + "' cannot be created and is not a directory");
		}
		return path;
	}
	
	public final static String PREF_PREMIUM_UNLOCKED_BOOL 			= "PREF_PREMIUM_UNLOCKED";
	public final static String PREF_PREMIUM_EXPIRE_TIME_LONG 		= "PREF_PREMIUM_EXPIRE_TIME";
	public final static String PREF_PURCHASE_SKU 					= "PREF_PURCHASE_SKU";
	public final static String PREF_PURCHASE_RANDOM_STRING 			= "PREF_PURCHASE_RANDOM";
	public final static String PREF_PREMIUM_OPTION_AVAILABLE_BOOL 	= "PREF_PREMIUM_OPTION_AVAILABLE";
	
	public void clearPurchaseData() {
		getPrefs().edit().remove(PREF_PREMIUM_UNLOCKED_BOOL)
                		.remove(PREF_PREMIUM_EXPIRE_TIME_LONG)
                		.remove(PREF_PURCHASE_SKU)
                		.remove(PREF_PREMIUM_OPTION_AVAILABLE_BOOL).commit();
	}
	
	public final boolean isPremiumOptionAvailable() {
		return getPrefs().getBoolean(PREF_PREMIUM_OPTION_AVAILABLE_BOOL, false);
	}

	public enum Purchase {
		THREEMONTH		("threemonth",		true, false, true),
		ONEWEEK			("oneweek", 		true, false, false),
		ONEMONTH		("onemonth", 		true, false, false),
		SIXMONTH		("sixmonth", 		true, false, true),
		ONEYEAR			("oneyear", 		true, false, true),
		PREMIUM			("premium", 		true, false, false),
		PREMIUM_REDUCED	("premium.reduced", false, true, false), // available only while weekly or monthly subscription is active.
		TENMINUTES		("ten.mins.debug", 	true, false, false) // DEBUG ONLY
		;
		
		public static Purchase getPurchaseFromSku(String sku) {
			for (Purchase p : values()) {
				if (sku.equals(p.sku))
					return p;
			}
			return null;
		}

		private Purchase(String sku, boolean availableWhenLocked, boolean availableWhenSubscription, boolean availableInRelease) {
			this.sku = sku;
			this.availableWhenLocked = availableWhenLocked;
			this.availableWhenSubscription = availableWhenSubscription;
			this.availableInRelease = availableInRelease;
		}

		final String sku;
		final boolean availableWhenLocked;
		final boolean availableWhenSubscription;
		final boolean availableInRelease;
	}	
	
	protected String [] getPurchasableSkus() {
		boolean premium = isPremiumEnabled(false);
		boolean subscription = isSubscription();
		boolean debug = BuildConfig.DEBUG;
		
		ArrayList<String> skus = new ArrayList<String>();
		for (Purchase p : Purchase.values()) {
			if (!debug && !p.availableInRelease)
				continue;
			if (!premium && p.availableWhenLocked)
				skus.add(p.sku);
			else if (subscription && p.availableWhenSubscription)
				skus.add(p.sku);
		}
		return skus.toArray(new String[skus.size()]);
	}
		
	public void finalizePurchase(String sku, long purchaseTime) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(purchaseTime);
		log.debug("finalizePurchase sku=" + sku + " datePurchased = " + cal.getTime());
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
    			case THREEMONTH:
    				cal.add(Calendar.MONTH, 3);
    				expireTime = cal.getTime();
    				break;
    			case SIXMONTH:
    				cal.add(Calendar.MONTH, 6);
    				expireTime = cal.getTime();
    				break;
    			case ONEYEAR:
    				cal.add(Calendar.YEAR, 1);
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
    				cal.add(Calendar.MINUTE, 5);
    				expireTime = cal.getTime();
    				break;
    		}
    		
    		if (expireTime != null) {
    			if (expireTime.getTime() > System.currentTimeMillis()) { 
    				edit.putLong(PREF_PREMIUM_EXPIRE_TIME_LONG, expireTime.getTime());
        			SimpleDateFormat fmt = new SimpleDateFormat("EEEE MMMM dd", Locale.US);
        			boolean premiumOption = getPrefs().getBoolean(PREF_PREMIUM_OPTION_AVAILABLE_BOOL, false);
        			showAlert(R.string.popup_title_purchase_complete, 
        					premiumOption ? R.string.popup_msg_subscription_activiated_premium_option : R.string.popup_msg_subscription_activiated, fmt.format(expireTime));
        			startPolling(5);
    			} else {
    				return;
    			}
    		} else {
    			edit.remove(PREF_PREMIUM_EXPIRE_TIME_LONG);
    			showAlert(R.string.popup_title_purchase_complete, R.string.popup_msg_premium_permanently_unlocked);
    		}
    		edit.commit();
		} else {
			newDialogBuilder().setTitle(R.string.popup_title_error)
				.setMessage(R.string.popup_msg_billing_err_general)
				.setNegativeButton(R.string.popup_button_ok, null)
				.setPositiveButton("Restore", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						clearPurchaseData();
						new BillingTask(BillingTask.Op.REFRESH_PURCHASED, CECCBaseActivity.this).execute();
					}
				}).show();
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
    			new BillingTask(BillingTask.Op.CONSUME_SKU, this).execute(sku);
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
        						new BillingTask(BillingTask.Op.QUERY_PURCHASABLES, CECCBaseActivity.this).execute(getPurchasableSkus());
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

		String q = "";
		if (days >= 1)
			q = getResources().getQuantityString(R.plurals.plural_days, days+1, days+1);
		else if (hours >= 1)
			q = getResources().getQuantityString(R.plurals.plural_hours, hours+1, hours+1);
		else 
			q = getResources().getQuantityString(R.plurals.plural_mins, mins+1, mins+1);
		return getResources().getString(R.string.colloqial_subscription_expire_time, q);
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
					new BillingTask(BillingTask.Op.QUERY_PURCHASABLES, CECCBaseActivity.this).execute(getPurchasableSkus());
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
	
	public void openUserManual() {
		try {
			InputStream in = getAssets().open("user_manual.pdf");
			File cache = new File(getCacheDir(), "user_manual.pdf");
			FileUtils.copy(in, cache);
			String uri = "content://" + EmailHelper.getAuthority(this) + "/user_manual.pdf";
			openPDF(uri);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void openPDF(String url) {
		// when local
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(Uri.parse(url), MIME_TYPE_PDF);
		if (getPackageManager().queryIntentActivities( i, PackageManager.MATCH_DEFAULT_ONLY ).size() > 0) {
			startActivity(i);
		} else {
			newDialogBuilder().setTitle("No PDF Viewer").setMessage("There is no PDF Viewer installed.  You can install the recommended viewer by following the install button below.")
				.setNegativeButton("Cancel", null)
				.setPositiveButton("Install", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.url_marketplace_pdfviewer))));						
					}
				}).show();
		}
	}
	
	public final void startTakePictureActivity(int resultIndex) {
		Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		startActivityForResult(i, resultIndex);
	}

	protected void onPictureTaken(Bitmap bitmap, File bitmapFile, int resultIndex) {
		throw new AssertionError("Not handled");
	}
	
	@Override
	protected void onActivityResult(int resultIndex, int resultCode, Intent data) {
		Bitmap bitmap = null;
		int orientation = 0;

		if (resultCode == Activity.RESULT_OK && data != null)
		{
			Uri image = data.getData();
			if (image != null) {
				try {
					bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), image);
					final Uri imageUri = data.getData();

                    String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION };
                    Cursor cursor = getContentResolver().query(imageUri, columns, null, null, null);
                    if (cursor != null) {
                        cursor.moveToFirst();
                        //int fileColumnIndex = cursor.getColumnIndex(columns[0]);
                        int orientationColumnIndex = cursor.getColumnIndex(columns[1]);
                        //String filePath = cursor.getString(fileColumnIndex);
                        orientation = cursor.getInt(orientationColumnIndex);
                        log.debug("got image orientation "+orientation);
                    }

				} catch (Exception e) {
					e.printStackTrace();
				}
    		}

        	if (bitmap != null) {
        		//if (isPremiumEnabled(false))
        			bitmap = ThumbnailUtils.extractThumbnail(bitmap, IMAGE_CAPURTE_DIM, IMAGE_CAPURTE_DIM);
        		//else
        		//	bitmap = ThumbnailUtils.extractThumbnail(bitmap, 64, 64);
    			Matrix matrix = new Matrix();
    			switch (orientation) {
    				case 90:
    				case 180:
    				case 270:
    					matrix.postRotate(orientation);
    					break;
    			}
    
    			Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    			if (newBitmap != null) {
    				bitmap.recycle();
    				bitmap = newBitmap;
    			}
    			
    			// watermark
    			//if (isPremiumEnabled(false))
    			watermark(bitmap, getDateFormatter().format(new Date()));
    
    			try {
    				File destFile = File.createTempFile(getPhotoPrefix(), ".jpg", getImagesPath());
    				FileOutputStream out = new FileOutputStream(destFile);
    				try {
    					bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
    					onPictureTaken(bitmap, destFile, resultIndex);
    					
    				} finally {
    					out.close();
    				}
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    			//bitmap.recycle();
        	}
		}
	}
	
	private void watermark(Bitmap bitmap, String text) {
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setTextAlign(Align.LEFT);
		paint.setTextSize(48);
		canvas.drawText(text, 2, canvas.getHeight()-2, paint);
	}
	
	protected abstract String getPhotoPrefix();

}
