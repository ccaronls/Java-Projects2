package cecc.android.mechdeficiency;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cc.lib.android.EmailHelper;
import cc.lib.android.SortButtonGroup;
import cc.lib.android.SortButtonGroup.OnSortButtonListener;
import cc.lib.utils.FileUtils;
import cecc.android.mechdeficiency.BillingTask.Op;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FormsList extends BaseActivity implements OnSortButtonListener {

	final static String SORT_FIELD_STR = "SORT_FIELD";
	final static String SORT_ASCENDING_BOOL = "SORT_ASCENDING";
	final static String EULA_ACCEPTED_BOOL = "EULA_ACCEPTED";
		
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.formslist);
		findViewById(R.id.buttonNewForm).setOnClickListener(this);
		findViewById(R.id.buttonOptions).setOnClickListener(this);

		String sortField = getPrefs().getString(SORT_FIELD_STR, DBHelper.FormColumn.EDIT_DATE.name());
		boolean ascending = getPrefs().getBoolean(SORT_ASCENDING_BOOL, false);
		
		SortButtonGroup sg = (SortButtonGroup)findViewById(R.id.sortButtonGroup);
		sg.setSelectedSortButton(sortField, ascending);
		sg.setOnSortButtonListener(this);

		if (!BuildConfig.DEBUG) {

			try {
				final String version = getVersionString();
        		if (!getPrefs().getBoolean(EULA_ACCEPTED_BOOL + version, false)) {
        			WebView v = new WebView(getActivity());
        			v.loadUrl("file:///android_asset/eula.html");
        			newDialogBuilder().setView(v).setCancelable(false)
        			.setNegativeButton(R.string.popup_button_eula_decline, new DialogInterface.OnClickListener() {
        				@Override
        				public void onClick(DialogInterface dialog, int which) {
        					finish();
        				}        				
        			})
        			.setPositiveButton(R.string.popup_button_eula_accept, new DialogInterface.OnClickListener() {
        				
        				@Override
        				public void onClick(DialogInterface dialog, int which) {
        					getPrefs().edit().putBoolean(EULA_ACCEPTED_BOOL + version, true).commit();
        					dialog.dismiss();
        					checkShowWelcome();
        				}
        			}).show();
        		}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		new Thread() {
			public void run() {
				cleanupUnusedImages();
			}
		}.start();
	}
	
	@Override
	protected void onAmbientTemperature(float celcius, int farhenheit) {
		if (!showSubscription) {
    		final TextView tvAmbient = (TextView)findViewById(R.id.tvAmbient);
    		tvAmbient.setText(getString(R.string.label_ambient_temp, farhenheit));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		refresh();
		startPolling(5);
	}
	
	private void checkShowWelcome() {
		if (getPrefs().getBoolean("FIRST_LAUNCH_BOOL", true)) {
			getPrefs().edit().putBoolean("FIRST_LAUNCH_BOOL", false).commit();
			if (!isPremiumEnabled(false))
    			newDialogBuilder().setTitle(R.string.popup_title_welcome)
    				.setMessage(R.string.popup_msg_welcome)
    				.setNegativeButton(R.string.popup_button_ok, null)
    				.setPositiveButton(R.string.popup_button_purchase_options, new DialogInterface.OnClickListener() {
    					
    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    						new BillingTask(Op.QUERY_PURCHASABLES, getActivity()).execute();
    					}
    				}).show();
		}		
	}
	
	private boolean showSubscription = false;
	
	@Override
	protected void onPoll() {
		Log.d(TAG, "onPoll");
		final TextView tvAmbient = (TextView)findViewById(R.id.tvAmbient);
		if (isSubscription()) {
    		showSubscription = !showSubscription;
    		if (showSubscription) {
    			tvAmbient.setText(getString(R.string.tvSubscriptionExpiresIn, getSubscriptionExpireTimeColloquial()));
    		}
		} else {
			if (showSubscription) {
				showSubscription = false;
				tvAmbient.setText("");
			}
			stopPolling();
		}
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
			case R.id.buttonOptions: {
				showOptionsDialog();
				break;
			}

			default: {
		
        		if (v.getTag() != null) {
        			final int formId = (Integer)v.getTag();
        			String [] items = getResources().getStringArray(R.array.form_item_options); 
        			newDialogBuilder()
            			.setItems(items, new DialogInterface.OnClickListener() {
            				
            				@Override
            				public void onClick(DialogInterface dialog, int which) {
            					switch (which) {
            						case 0: { // Edit
            							Intent i = new Intent(getActivity(), FormEdit.class);
            							i.putExtra(INTENT_FORM, getDBHelper().getFormById(formId));
            							startActivity(i);
            							break;
            						}
            						case 1: { // Dup
            							if (isPremiumEnabled(true)) {
                							Form form = getDBHelper().getFormById(formId);
                							form.id = null; // clearing the id will cause a duplicate
                							form.createDate = form.editDate = new Date();
                							form.images.clear();
                							Intent i = new Intent(getActivity(), FormEdit.class);
                							i.putExtra(INTENT_FORM, form);
                							startActivity(i); // let the edit activity save
            							}
            							break;
            						}
            						case 2: { // Delete
            							newDialogBuilder().setTitle(R.string.popup_title_confirm)
            								.setMessage(R.string.popup_msg_are_you_sure)
            								.setNegativeButton(R.string.popup_button_cancel, null)
            								.setPositiveButton(R.string.popup_button_delete, new DialogInterface.OnClickListener() {
        									
            									@Override
            									public void onClick(DialogInterface dialog, int which) {
            										getDBHelper().deleteForm(formId);
            										refresh();
            									}
            								})
            								.show();
            							break;
            						}
            						case 3: { // Export to email
            							Intent i = new Intent(getActivity(), FormSign.class);
            							i.putExtra(INTENT_FORM, getDBHelper().getFormById(formId));
            							startActivity(i);
            							break;
            						}
            					}
            				}
            			})
            			.setNegativeButton(R.string.popup_button_cancel, null)
            			.show();
        		} else {
        			if (getDBHelper().getFormCount() < 3 || isPremiumEnabled(true))
        				startActivity(new Intent(this, FormEdit.class));
        		}
        		break;
			}
		}
	}
	
	public void sortButtonChanged(SortButtonGroup group, int checkedId, String sortField, boolean ascending) {
		getPrefs().edit().putString(SORT_FIELD_STR, sortField)
			.putBoolean(SORT_ASCENDING_BOOL, ascending)
			.commit();
		refresh();
	}
	
	private void refresh() {
		
		TextView tvEmptyList = (TextView)findViewById(R.id.tvEmptyList);
		
//		((TextView)findViewById(R.id.tvFormCount)).setText("Form Count: " + getFormHelper().getFormCount());
		ListView lv = (ListView)findViewById(R.id.formList);
		
		String sortField = getPrefs().getString(SORT_FIELD_STR, DBHelper.FormColumn.EDIT_DATE.name());
		boolean ascending = getPrefs().getBoolean(SORT_ASCENDING_BOOL, false);
		
		Cursor cursor = getDBHelper().listForms(sortField, ascending);
		tvEmptyList.setVisibility(cursor.getCount() > 0 ? View.INVISIBLE : View.VISIBLE);
		
		lv.setAdapter(new CursorAdapter(this, cursor, 0) {
			
			@Override
			public View newView(Context context, Cursor cursor, ViewGroup parent) {
				View view = View.inflate(context, R.layout.formlistitem, null);
				bindView(view, context, cursor);
				return view;
			}
			
			@Override
			public void bindView(View view, Context context, Cursor cursor) {
				
				TextView tvDate = (TextView)view.findViewById(R.id.tvDate);
				TextView tvCustomer = (TextView)view.findViewById(R.id.tvCustomer);
				TextView tvProject = (TextView)view.findViewById(R.id.tvProject);
				CompoundButton cbFix = (CompoundButton)view.findViewById(R.id.cbFix);
				CompoundButton cbFixed = (CompoundButton)view.findViewById(R.id.cbFixed);
				
				int id = cursor.getInt(DBHelper.FormColumn._id.getColumnIndex(cursor));
				Date date = new Date(cursor.getLong(DBHelper.FormColumn.EDIT_DATE.getColumnIndex(cursor)));
				String project = cursor.getString(DBHelper.FormColumn.PROJECT.getColumnIndex(cursor));
				String customer = cursor.getString(DBHelper.FormColumn.CUSTOMER.getColumnIndex(cursor));
				boolean fix = cursor.getInt(DBHelper.FormColumn.FIX.getColumnIndex(cursor)) != 0;
				boolean fixed = cursor.getInt(DBHelper.FormColumn.FIXED.getColumnIndex(cursor)) != 0;
				
				tvDate.setText(getDateFormatter().format(date));
				tvProject.setText(project);
				tvCustomer.setText(customer);
				cbFix.setChecked(fix);
				cbFixed.setChecked(fixed);
				
				view.setTag(id);
				view.setOnClickListener(FormsList.this);
			}
		});
	}
	
	private void showOptionsDialog() {
		String [] items;
		
		if (BuildConfig.DEBUG) {
			items = getResources().getStringArray(R.array.form_list_options_debug);
		} else if (isSubscription() || !isPremiumEnabled(false)) {
			items = getResources().getStringArray(R.array.form_list_options_subscription);
		} else {
			items = getResources().getStringArray(R.array.form_list_options_unlocked);
		}
		
		newDialogBuilder().setTitle(R.string.popup_title_options).setItems(items, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case 0: { // about
						String email = getResources().getString(R.string.app_email);
						String version = "???";
						String googlePlayVersion = "???";
						try {
							version = getVersionString();
							googlePlayVersion = getPackageManager().getPackageInfo("com.google.android.gms", 0 ).versionName;
						} catch (NameNotFoundException e) {
							e.printStackTrace();
						}
						newDialogBuilder().setTitle(R.string.popup_title_about)
							.setMessage(getString(R.string.popup_msg_about, email, version, googlePlayVersion))
							.setNegativeButton(R.string.popup_button_ok, null)
							.setPositiveButton(R.string.popup_button_view_eula,new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									WebView v = new WebView(getActivity());
				        			v.loadUrl("file:///android_asset/eula.html");
				        			newDialogBuilder()
				        				.setTitle(R.string.popup_title_eula)
				        				.setView(v)
    				        			.setNegativeButton(R.string.popup_button_ok, null)
    				        			.show();
								}
							}).show();
						break;
					}
					
					case 1: { // feedback
						EmailHelper.sendEmail(getActivity(), null, getResources().getString(R.string.app_email), 
								getString(R.string.email_subject_feedback), getString(R.string.email_body_feedback));
						break;
					}
					
					case 2: { // zip
						if (isPremiumEnabled(true)) {
    						List<File> files = new ArrayList<File>();
    						files.add(new File(getDBHelper().getReadableDatabase().getPath()));
    						String timeStamp = new SimpleDateFormat("mmddyy_hhmm", Locale.US).format(new Date());
    						File target = new File(getCacheDir(), "PressureValidationDB_" + timeStamp + ".zip");
    						files.addAll(Arrays.asList(getImagesPath().listFiles()));
    						try {
    							FileUtils.zipFiles(target, files);
    							EmailHelper.sendEmail(getActivity(), target, null, getString(R.string.email_subject_export), getString(R.string.email_body_export));
    						} catch (Exception e) {
    							e.printStackTrace();
    							newDialogBuilder().setTitle(R.string.popup_title_error).setMessage(getString(R.string.popup_msg_dbexport_error, e.getMessage())).setNegativeButton(R.string.popup_button_ok, null).show();
    						}
						}
						break;
					}
					
					case 3: { // nuke
						newDialogBuilder().setTitle(R.string.popup_title_confirm).setMessage(R.string.popup_msg_nuke).setNegativeButton(R.string.popup_button_cancel, null)
						.setPositiveButton(R.string.popup_button_reset, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								final ProgressDialog spinner = new ProgressDialog(getActivity());
								spinner.show();
								new AsyncTask<Void,Void,Void>() {

									@Override
									protected void onPostExecute(Void result) {
										spinner.dismiss();
										refresh();
									}

									@Override
									protected Void doInBackground(Void... params) {
										getActivity().deleteDatabase(getDBHelper().getDB().getPath());
										for (File f : getImagesPath().listFiles()) {
											f.delete();
										}
										getDBHelper().close();
										return null;
									}
								}.execute();
								
							}
						}).show();
						break;
					}
					
					case 4: { // Purchases
						new BillingTask(Op.DISPLAY_PURCHASES, getActivity()).execute();
						break;
					}
					
					case 5: { // Upgrade
						new BillingTask(Op.QUERY_PURCHASABLES, getActivity()).execute();
						break;
					}
					
					case 6: { // Purchases DEBUG
						new BillingTask(Op.QUERY_PURCHASABLES_DEBUG, getActivity()).execute();
						break;
					}
					
					case 7: {
						clearPurchaseData();
						new BillingTask(Op.REFRESH_PURCHASED, getActivity()).execute();
						break;
					}
				}
			}
		}).setNegativeButton(R.string.popup_button_cancel, null).show();		
	}
}
