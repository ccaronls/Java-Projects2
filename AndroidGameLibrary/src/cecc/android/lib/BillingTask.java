package cecc.android.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import cc.lib.android.BuildConfig;
import cc.lib.android.R;
import cc.lib.game.Utils;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

/**
 * Wrapper task for the billing operations.
 * 
 * App must have this line in their manifest:
 * <uses-permission android:name="com.android.vending.BILLING" />
 * 
 * Since most billing operation use internet, we use a task to keep the network 
 * traffic off the UI thread.
 * 
 * @author chriscaron
 *
 */
public class BillingTask extends AsyncTask<String,Integer,Object> implements OnClickListener {

	private final static int BILLING_API = 3; // Used for calls into GooglePlay
	private final static int RESULT_OK = 0;

	private final String TAG;
	private String purchaseType = null;
	
	public enum Op {
		QUERY_PURCHASABLES,
		QUERY_PURCHASABLES_DEBUG,
		QUERY_PROMOTION,
		REFRESH_PURCHASED,
		DISPLAY_PURCHASES,
		QUERY_SUBSCRIPTION,
		PURCHASE, // Additional param is the SKU to purchase. @see enum Purchase.sku	
		CONSUME_TOKEN, // Internal: Consume a purchase.  Parameter to execute is the token.  Use CONSUME_SKU
		CONSUME_SKU, // Consume a purchase.  Parameter to execute is the sku of the purchase to consume.
	}

	// ordinal aligned with google documentation for 'purchaseState'
	enum PurchaseState {
		PURCHASED,
		CANCELLED,
		REFUNDED,
	}
	
	
	private final BillingActivity activity;
	private Dialog dialog;
	private final Op op;
	
	public BillingTask(Op op, BillingActivity activity) {
		this.activity = activity;
		this.op = op;
		TAG = "BillingTask(" + activity.getClass().getSimpleName() + ")";
	}
	
	private static String [] responseCodes = {
		 "SUCCESS",// * RESULT_OK = 0 - success
		 "RESULT_USER_CANCELED",// = 1 - user pressed back or canceled a dialog
		 "RESULT_BILLING_UNAVAILABLE",// = 3 - this billing API version is not supported for the type requested
		 "RESULT_ITEM_UNAVAILABLE",// = 4 - requested SKU is not available for purchase
		 "RESULT_DEVELOPER_ERROR",// = 5 - invalid arguments provided to the API
		 "RESULT_ERROR",// = 6 - Fatal error during the API action
		 "RESULT_ITEM_ALREADY_OWNED",// = 7 - Failure to purchase since item is already owned
		 "RESULT_ITEM_NOT_OWNED",// = 8 - Failure to consume since item is not owned		
	};

	private String getResponse(int code) {
		if (code > 0 && code < responseCodes.length) {
			return responseCodes[code];
		}
		return "UNKNOWN";
	}
	
	
	private final static String PURCHASE_TYPE_INAPP = "inapp";
	private final static String PURCHASE_TYPE_SUBSCRIPTION = "subs";
	
	private String getPackageName() {
		return activity.getPackageName();
	}

	public static class PurchaseInfo {
		String sku;
		long purchaseTime;
	}
	
	private final List<String> skusToConsume = new ArrayList<String>();
	
	@Override
	protected Object doInBackground(String... params) {
		
		try {
    		switch (op) {
    			case CONSUME_TOKEN: {
    				String token = params[0];
    				int response = activity.getBilling().consumePurchase(BILLING_API, getPackageName(), token);
    				if (response != RESULT_OK) {
    					throw new Exception("Failed to consume last purchase");
    				}
    				break;
    			}
    			
    			case QUERY_PURCHASABLES: {
    				Bundle skusBundle = new Bundle();
    				ArrayList<String> skus = new ArrayList<String>(Arrays.asList(params));
    				skusBundle.putStringArrayList("ITEM_ID_LIST", skus);
    				return activity.getBilling().getSkuDetails(BILLING_API, getPackageName(), PURCHASE_TYPE_INAPP, skusBundle);
    			}
    			
    			case QUERY_PURCHASABLES_DEBUG: {
    				String [] skus = {
    						"android.test.purchased",
    						"android.test.canceled",
    						"android.test.refunded",
    						"android.test.item_unavailable",
    				};
    				Bundle skusBundle = new Bundle();
    				skusBundle.putStringArrayList("ITEM_ID_LIST", new ArrayList<String>(Arrays.asList(skus)));
    				return activity.getBilling().getSkuDetails(BILLING_API, getPackageName(), PURCHASE_TYPE_INAPP, skusBundle);
    			}
    			
    			case QUERY_PROMOTION: {
    				Bundle skusBundle = new Bundle();
    				ArrayList<String> skus = new ArrayList<String>(Arrays.asList(params));
    				skusBundle.putStringArrayList("ITEM_ID_LIST", skus);
    				return activity.getBilling().getSkuDetails(BILLING_API, getPackageName(), PURCHASE_TYPE_SUBSCRIPTION, skusBundle);
    			}
    			
    			case CONSUME_SKU:
    				skusToConsume.addAll(Arrays.asList(params));
    			case DISPLAY_PURCHASES:
    			case REFRESH_PURCHASED: {
    				return activity.getBilling().getPurchases(BILLING_API, getPackageName(), PURCHASE_TYPE_INAPP, null);
    			}
    			
    			case QUERY_SUBSCRIPTION:
					return activity.getBilling().getPurchases(BILLING_API, getPackageName(), PURCHASE_TYPE_SUBSCRIPTION, null);
    			
    			case PURCHASE: {
    				String randomStr = generateRandomString();
    				activity.getPrefs().edit().putString(BillingActivity.PREF_PURCHASE_RANDOM_STRING, randomStr).commit();
    				return activity.getBilling().getBuyIntent(3, getPackageName(), params[0], params[1], randomStr);
    			}
    		}
		} catch (Exception e) {
			e.printStackTrace();
			return e;
		}
		
		// TODO Auto-generated method stub
		return null;
	}
	
	private String generateRandomString() {
		StringBuffer buf = new StringBuffer(256);
		String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV1234567890-=!@#$%^&*()_+,./,/[];'{}:";
		for (int i=0; i<256; i++) {
			buf.append(chars.charAt(Utils.rand() % chars.length()));
		}
		return buf.toString();
	}

	@Override
	protected void onPreExecute() {
		switch (op) {
			case CONSUME_SKU:
			case CONSUME_TOKEN:
			case REFRESH_PURCHASED:
				break;
			case DISPLAY_PURCHASES:
			case PURCHASE:
			case QUERY_PURCHASABLES:
			case QUERY_PURCHASABLES_DEBUG:
			case QUERY_PROMOTION:
				dialog = new ProgressDialog(activity);
				dialog.setTitle("Processing Billing Request");
				dialog.setCancelable(false);
				dialog.setCanceledOnTouchOutside(false);
				dialog.show();
				break;
		}
	}

	@Override
	protected void onPostExecute(Object result) {
		if (dialog != null)
			dialog.dismiss();
		
		try {
			if (result == null) {
				// do nothing
			} else if (result instanceof Exception) {
				activity.showAlert(R.string.popup_title_error, R.string.popup_msg_billing_err_general);
			} else if (result instanceof Bundle) {
				Bundle bundle = (Bundle)result;
				int response = bundle.getInt("RESPONSE_CODE");
				if (response == 0) {
					processBundle(bundle);
				} else if (response == 1) {
					// user cancelled at some point.  So dont do anything
				} else {
					activity.showAlert(R.string.popup_title_error, 
							BuildConfig.DEBUG ? R.string.popup_msg_billing_err_argument_debug : R.string.popup_msg_billing_err_argument, getResponse(response));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			activity.showAlert(R.string.popup_title_error, R.string.popup_msg_billing_err_general);
		}
	}
	
	// each row must have these strings in this order:
	// 0   sku
	// 1   orderId
	// 2   purchaseTime (UTC)
	// 3   purchaseState (Purchased, Cancelled, Refunded)
	private View buildPurchasesView(List<String[]> rows) {
		View layout = View.inflate(activity, R.layout.popup_purchase, null);
		TableLayout table = (TableLayout)layout.findViewById(R.id.layoutTable);
		table.addView(View.inflate(activity, R.layout.purchases_listitem, null));

		int [] rowColor = {
			Color.DKGRAY,
			Color.BLACK
		};
		
		int [] resId = {
			R.id.tvPurchaseSku,
			R.id.tvPurchaseId,
			R.id.tvPurchaseDate,
			R.id.tvPurchaseState
		};
		
		int colorIndex = 0;
		
		for (String [] row : rows) {
			View tRow = View.inflate(activity, R.layout.purchases_listitem, null);
			int color = rowColor[colorIndex++ % rowColor.length];
			tRow.setBackgroundColor(color);
			
			for (int i=0; i<resId.length; i++) {
				TextView tv = (TextView)tRow.findViewById(resId[i]);
				tv.setText(row[i]);
			}
			
			table.addView(tRow);
		}
		
		return layout;
	}
	
	// each row must have these strings in this order:
	// 0   sku
	// 1   desc
	// 2   price
	private View buildPurchaseView(List<String[]> rows) {
		View layout = View.inflate(activity, R.layout.popup_purchase, null);
		TableLayout table = (TableLayout)layout.findViewById(R.id.layoutTable);
		table.addView(View.inflate(activity, R.layout.purchasables_listitem, null));

		int [] rowColor = {
				Color.DKGRAY,
				Color.BLACK
		};
		
		int colorIndex = 0;
		
		for (String [] row : rows) {
			View tRow = View.inflate(activity, R.layout.purchasables_listitem, null);
			ViewSwitcher flipper = (ViewSwitcher)tRow.findViewById(R.id.viewFlipper);
			flipper.setDisplayedChild(1);
			int color = rowColor[colorIndex++ % rowColor.length];
			tRow.setBackgroundColor(color);
			Button buy = (Button)tRow.findViewById(R.id.buttonBuy);
			TextView tvDesc = (TextView)tRow.findViewById(R.id.tvDesc);
			//TextView tvPrice = (TextView)tRow.findViewById(R.id.tvPrice);
			

			buy.setTag(row[0]);
			buy.setOnClickListener(this);
			buy.setVisibility(View.VISIBLE);
			
			tvDesc.setText(row[1]);
			//tvPrice.setText(row[2]);
			buy.setText(row[2]);
			
			table.addView(tRow);
		}
		
	
		return layout;
	}
	
	/**
	 * Handle this to do something based on available skus.  BAse method does nothing
	 * 
	 * @param skus
	 */
	protected void onPurchaseOptions(List<String> skus) {}
	
	private void processBundle(Bundle b) throws SendIntentException, JSONException {
		
		int response = b.getInt("RESPONSE_CODE");
		Log.d(TAG, "processBundle for " + op.name() + " response=" + response);
		if (response == 0) {
			switch (op) {
				case CONSUME_TOKEN:
					// the item will be available to purchase again
					break;
				case PURCHASE: {
					PendingIntent pendingIntent = b.getParcelable("BUY_INTENT");
					activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
							   BillingActivity.REQUEST_PURCHASE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
							   Integer.valueOf(0));					
					break;
				}
				case QUERY_PURCHASABLES_DEBUG:
				case QUERY_PURCHASABLES: 
					purchaseType = PURCHASE_TYPE_INAPP;
				case QUERY_PROMOTION: {
					if (purchaseType == null)
						purchaseType = PURCHASE_TYPE_SUBSCRIPTION;
					List<String[]> rows = new ArrayList<String[]>();
					List<String> skus = new ArrayList<String>();
					ArrayList<String> responseList = b.getStringArrayList("DETAILS_LIST");
					Log.d(TAG, "responseList=" + responseList);
					for (String thisResponse : responseList) {
						JSONObject object = new JSONObject(thisResponse);
						Log.d(TAG, "JSON=" + object.toString(3));
						String sku = object.getString("productId");
						//String type = object.getString("type");
						String price = object.getString("price");
						//String price_micros = object.getString("price_amount_micros");
						//String title = object.getString("title");
						String desc = object.getString("description");
						
						rows.add(new String[] { sku, desc, price });
						skus.add(sku);
					}
					onPurchaseOptions(skus);
					if (rows.size() > 0) {
						dialog = activity.newDialogBuilder().setTitle(R.string.popup_title_purchase_options)
							.setView(buildPurchaseView(rows))
							.setNegativeButton(R.string.popup_button_cancel, null)
							.show();
					} else {
						activity.showAlert(R.string.popup_title_error, R.string.popup_msg_no_purchases);
					}
					
					break;
				}
				
				case DISPLAY_PURCHASES: {
					ArrayList<String> ownedSkus = b.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
					ArrayList<String>  purchaseDataList = b.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
					ArrayList<String>  signatureList = b.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
					Log.d(TAG, "ownedSkus=" + ownedSkus);
					Log.d(TAG, "purchaseDataList=" + purchaseDataList);
					Log.d(TAG, "signatureList=" + signatureList);
					String continuationToken = b.getString("INAPP_CONTINUATION_TOKEN");
					Log.d(TAG, "continuationToken=" + continuationToken);

					ArrayList<String[]> purchases = new ArrayList<String[]>();
					java.text.DateFormat fmt = DateFormat.getDateFormat(activity);

					// if anything is a 'Premium' purchase, then they get it.  otherwise consume anything else
					for (int i = 0; i < purchaseDataList.size(); ++i) {
						String purchaseData = purchaseDataList.get(i);
						String signature = signatureList.get(i);
						String sku = ownedSkus.get(i);
						
						JSONObject o = new JSONObject(purchaseData);
						Log.d(TAG, "signature=" + signature);
						Log.d(TAG, "sku=" + sku);
						Log.d(TAG, "purchaseData=" + o.toString(3));
						
						long purchaseTime = o.getLong("purchaseTime");
						String orderId = o.getString("orderId");
						String time = fmt.format(new Date(purchaseTime));
						int stateCode = o.getInt("purchaseState");
						
						String state = "Unknown";
						switch (stateCode) {
							case 0: state="Purchased"; break;
							case 1: state="Cancelled"; break;
							case 2: state="Refunded"; break;
						}
						
						purchases.add(new String[] { sku, orderId, time, state } );
					}
					
					if (purchases.size() > 0) {
						activity.newDialogBuilder()
							.setTitle(R.string.popup_title_purchases)
							.setView(buildPurchasesView(purchases))
							.setNegativeButton(R.string.popup_button_ok, null)
							.show();
					} else {
						//activity.showAlert(R.string.popup_title_purchases, R.string.popup_msg_empty_purchases);
						activity.newDialogBuilder()
							.setTitle(R.string.popup_title_purchases)
							.setMessage(R.string.popup_msg_empty_purchases)
							.setNegativeButton(R.string.popup_button_ok, null)
							.show();
					}
					
					break;
				}
				
				case CONSUME_SKU: {
					ArrayList<String> ownedSkus = b.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
					ArrayList<String>  purchaseDataList = b.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
					ArrayList<String>  signatureList = b.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
					Log.d(TAG, "ownedSkus=" + ownedSkus);
					Log.d(TAG, "purchaseDataList=" + purchaseDataList);
					Log.d(TAG, "signatureList=" + signatureList);
					String continuationToken = b.getString("INAPP_CONTINUATION_TOKEN");
					Log.d(TAG, "continuationToken=" + continuationToken);

					// if anything is a 'Premium' purchase, then they get it.  otherwise consume anything else
					for (int i = 0; i < purchaseDataList.size(); ++i) {
						String purchaseData = purchaseDataList.get(i);
						//String signature = signatureList.get(i);
						String sku = ownedSkus.get(i);
						
						if (skusToConsume.contains(sku)) {
							JSONObject o = new JSONObject(purchaseData);
							Log.d(TAG, "purchaseData=" + o.toString(3));
							String purchaseToken = o.optString("token", o.optString("purchaseToken"));
							new BillingTask(Op.CONSUME_TOKEN, activity).execute(purchaseToken);
						}
					}					
				}
				
				
				case REFRESH_PURCHASED: {
					ArrayList<String> ownedSkus = b.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
					ArrayList<String>  purchaseDataList = b.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
					ArrayList<String>  signatureList = b.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
					Log.d(TAG, "ownedSkus=" + ownedSkus);
					Log.d(TAG, "purchaseDataList=" + purchaseDataList);
					Log.d(TAG, "signatureList=" + signatureList);
					String continuationToken = b.getString("INAPP_CONTINUATION_TOKEN");
					Log.d(TAG, "continuationToken=" + continuationToken);

					for (int i = 0; i < purchaseDataList.size(); ++i) {
						String purchaseData = purchaseDataList.get(i);
						String signature = signatureList.get(i);
						String sku = ownedSkus.get(i);
						
						JSONObject o = new JSONObject(purchaseData);
						Log.d(TAG, "signature=" + signature);
						Log.d(TAG, "sku=" + sku);
						Log.d(TAG, "purchaseData=" + o.toString(3));
						
						long purchaseTime = o.getLong("purchaseTime");
						int state = o.getInt("purchaseState");
						String purchaseToken = o.getString("purchaseToken");
						
						if (state == PurchaseState.PURCHASED.ordinal()) {
							if (!activity.getPrefs().getString(BillingActivity.PREF_PURCHASE_SKU, "").equals(sku))
								activity.finalizePurchase(sku, purchaseTime);
						} else {
							new BillingTask(Op.CONSUME_TOKEN, activity).execute(purchaseToken);
						}
					}					
					
					break;
				}
				
				case QUERY_SUBSCRIPTION: {
					break;
				}
			}
		} else if (response != 1) {
			activity.showAlert(R.string.popup_title_error, R.string.popup_msg_billing_err_general);
		}
	}

	@Override
	protected void onCancelled() {
		// TODO Auto-generated method stub
		super.onCancelled();
	}

	@Override
	public void onClick(View v) {
		if (v.getTag() != null && v.getTag() instanceof String) {
			String sku = (String)v.getTag();
			new BillingTask(Op.PURCHASE, activity).execute(sku, purchaseType);
		}
		if (dialog != null) {
			dialog.dismiss();
		}
	}
	
}
