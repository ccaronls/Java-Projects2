package cc.android.photooverlay;

import java.text.SimpleDateFormat;
import java.util.Date;

import cc.lib.android.EmailHelper;
import cc.lib.android.SortButtonGroup;
import cc.lib.android.SortButtonGroup.OnSortButtonListener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FormsList extends BaseActivity implements OnSortButtonListener {

	final static String SORT_FIELD_STR = "SORT_FIELD";
	final static String SORT_ASCENDING_BOOL = "SORT_ASCENDING";
	
	TextView tvFormCount;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.formslist);
		findViewById(R.id.buttonNewForm).setOnClickListener(this);
		findViewById(R.id.buttonOptions).setOnClickListener(this);

		String sortField = getPrefs().getString(SORT_FIELD_STR, FormHelper.Column.EDIT_DATE.name());
		boolean ascending = getPrefs().getBoolean(SORT_ASCENDING_BOOL, false);
		
		SortButtonGroup sg = (SortButtonGroup)findViewById(R.id.sortButtonGroup);
		sg.setSelectedSortButton(sortField, ascending);
		sg.setOnSortButtonListener(this);
		
		startActivity(new Intent(this, Splash.class));
		tvFormCount = (TextView)findViewById(R.id.tvFormCount);
	}

	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
			case R.id.buttonOptions: {
				String [] items = new String[] {
						"About",
						"Send Feedback",
						"Reset all Forms"
				};
				newDialogBuilder().setTitle("Options").setItems(items, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0: { // about
								String email = getResources().getString(R.string.app_email);
								PackageInfo pInfo;
								String version = "???";
								try {
									pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
									version = pInfo.versionName;
								} catch (NameNotFoundException e) {
									e.printStackTrace();
								}
								newDialogBuilder().setTitle("About").setMessage("Pressure Test Verification Utility\n\nCECC Solutions\n\n" + email + "\nApplication Version: " + version)
									.setNegativeButton("Ok", null).show();
								break;
							}
							
							case 1: { // feedback
								EmailHelper.sendEmail(getActivity(), null, getResources().getString(R.string.app_email), "Pressure Test Verification Utility Feedback", "Let us know what you think!");
								break;
							}
							
							case 2: { // reset
								newDialogBuilder().setTitle("Confirm Reset").setMessage("Are you sure?  This will delete all application data.").setNegativeButton("Cancel", null)
								.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										final ProgressDialog spinner = new ProgressDialog(getActivity());
										spinner.show();
										new AsyncTask<Void,Void,Void>() {

											@Override
											protected void onPostExecute(Void result) {
												spinner.dismiss();
												refresh();
												super.onPostExecute(result);
											}

											@Override
											protected Void doInBackground(Void... params) {
												getFormHelper().reset();
												return null;
											}
										}.execute();
										
									}
								}).show();
								break;
							}
						}
					}
				}).setNegativeButton("Cancel", null).show();
				
				break;
			}

			default: {
		
        		if (v.getTag() != null) {
        			final int formId = (Integer)v.getTag();
        			String [] items = new String[] {
        					"Edit",
        					"Duplication",
        					"Delete",
        					"Export"
        			};
        			newDialogBuilder()
            			.setItems(items, new DialogInterface.OnClickListener() {
            				
            				@Override
            				public void onClick(DialogInterface dialog, int which) {
            					switch (which) {
            						case 0: { // Edit
            							Intent i = new Intent(getActivity(), FormEdit.class);
            							i.putExtra(FormEdit.INTENT_FORM, getFormHelper().getFormById(formId));
            							startActivity(i);
            							break;
            						}
            						case 1: { // Dup
            							Form form = getFormHelper().getFormById(formId);
            							form.id = null; // clear id and add to duplicate
            							form.createDate = form.editDate = new Date();
            							Intent i = new Intent(getActivity(), FormEdit.class);
            							i.putExtra(FormEdit.INTENT_FORM, form);
            							startActivity(i); // let the edit activity save
            							break;
            						}
            						case 2: { // Delete
            							newDialogBuilder().setTitle("Confirm").setMessage("Are you sure?").setNegativeButton("Cancel", null).setPositiveButton("Delete", new DialogInterface.OnClickListener() {
        									
        									@Override
        									public void onClick(DialogInterface dialog, int which) {
        										getFormHelper().deleteForm(formId);
        										refresh();
        									}
        								}).show();
            							break;
            						}
            						case 3: { // Export to email
            							break;
            						}
            					}
            				}
            			})
            			.setNegativeButton("Cancel", null)
            			.show();
        		} else {
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
		
		tvFormCount.setText("Form Count: " + getFormHelper().getFormCount());
		ListView lv = (ListView)findViewById(R.id.formList);
		
		String sortField = getPrefs().getString(SORT_FIELD_STR, FormHelper.Column.EDIT_DATE.name());
		boolean ascending = getPrefs().getBoolean(SORT_ASCENDING_BOOL, false);
		
		Cursor cursor = getFormHelper().listForms(sortField, ascending, 0, 100);
		lv.setAdapter(new CursorAdapter(this, cursor) {
			
			@Override
			public View newView(Context context, Cursor cursor, ViewGroup parent) {
				View view = View.inflate(context, R.layout.listitem, null);
				bindView(view, context, cursor);
				return view;
			}
			
			@Override
			public void bindView(View view, Context context, Cursor cursor) {
				
				TextView tvDate = (TextView)view.findViewById(R.id.tvDate);
				TextView tvAddress = (TextView)view.findViewById(R.id.tvAddress);
				TextView tvCustomer = (TextView)view.findViewById(R.id.tvCustomer);
				CompoundButton cbPassed = (CompoundButton)view.findViewById(R.id.cbPassed);
				
				int id = cursor.getInt(FormHelper.Column._id.getColumnIndex(cursor));
				Date date = new Date(cursor.getLong(FormHelper.Column.EDIT_DATE.getColumnIndex(cursor)));
				String addr = cursor.getString(FormHelper.Column.LOCATION.getColumnIndex(cursor));
				String customer = cursor.getString(FormHelper.Column.CUSTOMER.getColumnIndex(cursor));
				boolean passed = cursor.getInt(FormHelper.Column.PASSED.getColumnIndex(cursor)) != 0;
				
				tvDate.setText(getDateFormatter().format(date));
				tvAddress.setText(addr);
				tvCustomer.setText(customer);
				cbPassed.setChecked(passed);
				
				view.setTag(id);
				view.setOnClickListener(FormsList.this);
			}
		});
	}
}
