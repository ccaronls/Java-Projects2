package cecc.android.mechdeficiency;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.lib.utils.Convert;
import cecc.android.lib.TemperatureChooserView;
import cecc.android.mechdeficiency.DBHelper.FormColumn;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannedString;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class FormEdit extends BaseActivity {

	TextView tvDate;
	AutoCompleteTextView etCustomer;
	AutoCompleteTextView etProject;
	AutoCompleteTextView etLocation;
	AutoCompleteTextView etRepresentative;
	AutoCompleteTextView etPlan;
	Button bType;
	
	EditText etComments;
	CompoundButton cbFix, cbFixed;
	ImageButton [] ibImage;
	TextView [] tvImageMeta;
	
	Form form;
	
	private ArrayAdapter<String> getAutoCompleteAdapter(DBHelper.FormColumn column) {
		return new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, getDBHelper().getDistictValuesForColumn(column));
	}
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.formedit);

		findViewById(R.id.buttonCancel).setOnClickListener(this);
		findViewById(R.id.buttonSave).setOnClickListener(this);

		tvDate = (TextView)findViewById(R.id.tvDate);
		
		etRepresentative = (AutoCompleteTextView)findViewById(R.id.etRepresentative);
		etRepresentative.setAdapter(getAutoCompleteAdapter(DBHelper.FormColumn.REPRESENTATIVE));
		
		etCustomer = (AutoCompleteTextView)findViewById(R.id.etCustomer);
		etCustomer.setAdapter(getAutoCompleteAdapter(DBHelper.FormColumn.CUSTOMER));

		etLocation = (AutoCompleteTextView)findViewById(R.id.etLocation);
		etLocation.setAdapter(getAutoCompleteAdapter(DBHelper.FormColumn.LOCATION));
		
		etProject = (AutoCompleteTextView)findViewById(R.id.etProject);
		etProject.setAdapter(getAutoCompleteAdapter(DBHelper.FormColumn.PROJECT));

		etPlan = (AutoCompleteTextView)findViewById(R.id.etPlan);
		etPlan.setAdapter(getAutoCompleteAdapter(DBHelper.FormColumn.PLAN));
		
		etComments = (EditText)findViewById(R.id.etComments);
		etPlan.setNextFocusForwardId(etComments.getId());
		
		cbFix = (CompoundButton)findViewById(R.id.cbFix);
		cbFixed = (CompoundButton)findViewById(R.id.cbFixed);

		bType = (Button)findViewById(R.id.bShowTypePopupup);
		bType.setOnClickListener(this);
		
		ibImage = new ImageButton[] { 
				(ImageButton)findViewById(R.id.ibImage1),
				(ImageButton)findViewById(R.id.ibImage2),
				(ImageButton)findViewById(R.id.ibImage3),
				(ImageButton)findViewById(R.id.ibImage4),
				(ImageButton)findViewById(R.id.ibImage5),
				(ImageButton)findViewById(R.id.ibImage6),
		};
		for (View v : ibImage) {
			v.setOnClickListener(this);
		}
		tvImageMeta = new TextView[] {
				(TextView)findViewById(R.id.tvImage1Meta),
				(TextView)findViewById(R.id.tvImage2Meta),
				(TextView)findViewById(R.id.tvImage3Meta),
				(TextView)findViewById(R.id.tvImage4Meta),
				(TextView)findViewById(R.id.tvImage5Meta),
				(TextView)findViewById(R.id.tvImage6Meta),
		};
		// got the hoppity ....
		
		form = getIntent().getParcelableExtra(INTENT_FORM);
		if (form == null) { 
			form = new Form();
			// new form
			form.createDate = new Date();
		}
		
		// got the soppity, populate it with the fillopitty
		
		tvDate.setText(getDateFormatter().format(form.createDate));
		etRepresentative.setText(form.representative);
		etCustomer.setText(form.customer);
		etLocation.setText(form.location);
		etProject.setText(form.project);
		etPlan.setText(form.plan);
		etComments.setText(form.comments);
		if (form.type == null || form.type.length() == 0) {
			bType.setText(getResources().getStringArray(R.array.popup_items_choose_type)[0]);
		} else {
			bType.setText(form.type);
		}
		cbFix.setChecked(form.fix);
		cbFixed.setChecked(form.fixed);

		for (int i=0; i<ibImage.length; i++) {
			Image image = form.getImageForIndex(i);
			if (image != null) {
				try {
					ibImage[i].setImageURI(Uri.fromFile(new File(getImagesPath(), image.path)));
					setImageData(tvImageMeta[i], image);
//					tvImageMeta[i].setText(image.data);
				} catch (Exception e) {
					e.printStackTrace();
					form.images.remove(image);
				}
			}
		}

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}
	
	private void setImageData(TextView tv, Image i) {
		if (i != null && i.data != null) {
			tv.setText(Html.fromHtml(i.data));
		}
	}
	

	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
			case R.id.buttonSave:
				save();
				finish();
				break;
			case R.id.buttonCancel:
				finish();
				break;
			case R.id.ibImage1:
				editImage(0, true);
				break;
			case R.id.ibImage2:
				editImage(1, true);
				break;
			case R.id.ibImage3:
				if (isPremiumEnabled(true))
					editImage(2, true);
				break;
			case R.id.ibImage4:
				editImage(3, true);
				break;
			case R.id.ibImage5:
				editImage(4, true);
				break;
			case R.id.ibImage6:
				if (isPremiumEnabled(true))
					editImage(5, true);
				break;
			case R.id.bShowTypePopupup:
				showChooseTypeDialog();
				break;
		}
	}
	
	@Override
	public void onBackPressed() {
		save();
		super.onBackPressed();
	}
	
	private void editImage(final int index, final boolean tempEditable) {
		final Image image = form.getImageForIndex(index);
		if (image != null) {
			View view = View.inflate(getActivity(), R.layout.popup_image_enlarge, null);
			ImageView iv = (ImageView)view.findViewById(R.id.ivPhoto);
			final EditText et = (EditText)view.findViewById(R.id.etMeta);
			if (image.data != null)
				et.setText(Html.fromHtml(image.data));
			
			final Button buttonTemp = (Button)view.findViewById(R.id.buttonTemp);
			
			String notes = image.data;
			
			String tempStr = getTemperatureString(Convert.degreesToCelcius(70));
			final float [] tempCelcius = new float[] { Convert.degreesToCelcius(70) }; 
			
			if (notes != null) {
				Pattern p = Pattern.compile("^\\-?[0-9]+(\\.[0-9])?");
				String s = Html.fromHtml(notes).toString();
				Matcher m = p.matcher(s);
				if (m.find()) {
					tempCelcius[0] = Float.parseFloat(m.group());
					if (!isMetricUnits()) {
						tempCelcius[0] = Convert.degreesToCelcius(Math.round(tempCelcius[0]));
					}
					tempStr = getTemperatureString(tempCelcius[0]);
				}
			}
			
			if (tempEditable) {
				if (isAmbientTempAvailable()) {
					tempStr = getTemperatureString(getAmbientTempCelcius());
					tempCelcius[0] = getAmbientTempCelcius();
					showInfoDialogBuilderWithDontShowAgainCB(R.string.popup_title_important, R.string.popup_msg_tempinfo, "PREF_TEMP_INFO_DONT_SHOW_AGAIN_BOOL");
				}
			}
				
			buttonTemp.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (tempEditable) {
						new TemperatureChooserView(FormEdit.this) {
							
							@Override
							protected float getInitialTempCelcius() {
								return tempCelcius[0];
							}

							@Override
							protected void onTemperature(float temp) {
								tempCelcius[0] = getTempCelcius();
								buttonTemp.setText(Html.fromHtml(getTemperatureString(getTempCelcius())));
							}
						};
					} else {
						newDialogBuilder().setTitle(R.string.popup_title_temp_locked).setMessage(R.string.popup_msg_temp_locked)
							.setNegativeButton(R.string.popup_button_ok, null).show();
					}
				}
			});
			
			buttonTemp.setText(Html.fromHtml(tempStr));
			if (notes != null) {
				String s = Html.fromHtml(notes).toString();
				int spc = s.indexOf(' ');
				if (spc > 0) {
					String note = s.substring(spc+1);
					et.setText(note);
				}
			}

			
			iv.setImageURI(Uri.fromFile(new File(getImagesPath(), image.path)));
			newDialogBuilder()
				.setView(view)
				.setNegativeButton(R.string.popup_button_ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SpannedString str = new SpannedString(buttonTemp.getText() + " " + et.getText());
						image.data = Html.toHtml(str);
						setImageData(tvImageMeta[index], image);
					}
				})
				.setPositiveButton(R.string.popup_button_change, new DialogInterface.OnClickListener() {
				
    				@Override
    				public void onClick(DialogInterface dialog, int which) {
    					pickImage(index);
    				}
    			})
    			.setNeutralButton(R.string.popup_button_remove, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						form.images.remove(image);
						ibImage[index].setImageBitmap(null);
						tvImageMeta[index].setText("");
					}
				})
    			.show();
		} else {
			pickImage(index);
		}
	}
	
	public void save() {
		form.representative = etRepresentative.getText().toString().trim();
		form.customer = etCustomer.getText().toString().trim();
		form.editDate = new Date();
		form.comments = etComments.getText().toString().trim();
		form.location = etLocation.getText().toString().trim();
		form.project = etProject.getText().toString().trim();
		form.fix = cbFix.isChecked();
		form.fixed = cbFixed.isChecked();
		form.plan = etPlan.getText().toString().trim();
		form.type = bType.getText().toString().trim();
		getDBHelper().addOrUpdateForm(form);
	}
	
	public void pickImage(final int id) {
		try {
			startTakePictureActivity(id);
		} catch (Exception e) {
			showAlert(R.string.popup_title_error, R.string.popup_msg_operation_not_supported);
		}
	}
	
	
	
	@Override
	protected void onPictureTaken(Bitmap bitmap, File bitmapFile, int index) {
		ibImage[index].setImageBitmap(bitmap);
		Image image = form.getImageForIndex(index);
		if (image == null) {
			image = new Image();
			image.index = index;
			form.images.add(image);
		}
		image.path = bitmapFile.getName();
		if (image.data == null) {
			if (isAmbientTempAvailable()) {
				image.data = getTemperatureString(getAmbientTempCelcius());
			}
		}
		editImage(index, true);
		
	}

	private void showChooseTypeDialog() {
		
		TreeSet<String> all = new TreeSet<String>();
		all.add(bType.getText().toString());
		all.addAll(Arrays.asList(getResources().getStringArray(R.array.popup_items_choose_type)));
		all.addAll(Arrays.asList(getDBHelper().getDistictValuesForColumn(FormColumn.TYPE)));
		final String [] items = all.toArray(new String[all.size()]);
		
		newDialogBuilder().setTitle(R.string.popup_title_choose_type).setItems(items, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				bType.setText(items[which]);
			}
		}).setNegativeButton(R.string.popup_button_cancel, null)
		.setPositiveButton(R.string.popup_button_new, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final EditText etType = new EditText(getActivity());
				newDialogBuilder()
					.setTitle(R.string.popup_title_new_type)
					.setView(etType)
					.setNegativeButton(R.string.popup_button_cancel, null)
					.setPositiveButton(R.string.popup_button_ok, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String s = etType.getText().toString().trim();
							if (s.length() > 0)
								bType.setText(s);
						}
					}).show();
			}
		}).show();
	}
}
