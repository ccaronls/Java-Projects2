package cc.android.photooverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class FormEdit extends BaseActivity implements OnCheckedChangeListener {

	TextView tvDate;
	AutoCompleteTextView etInspector;
	AutoCompleteTextView etCustomer;
	AutoCompleteTextView etLocation;
	AutoCompleteTextView etSystem;
	AutoCompleteTextView etPlan;
	AutoCompleteTextView etDetail;
	AutoCompleteTextView etCustom;
	
	EditText etComments;
	RadioGroup rgType;
	CompoundButton cbPassed;
	ImageButton [] ibImage;
	TextView [] tvImageMeta;
	
	Form form;
	
	private ArrayAdapter<String> getAutoCompleteAdapter(SQLHelper.Column column) {
		return new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, getFormHelper().getDistictValuesForColumn(column));
	}
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.formedit);

		findViewById(R.id.buttonCancel).setOnClickListener(this);
		findViewById(R.id.buttonSave).setOnClickListener(this);

		tvDate = (TextView)findViewById(R.id.tvDate);
		
		etInspector = (AutoCompleteTextView)findViewById(R.id.etInspector);
		etInspector.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.INSPECTOR));
		
		etCustomer = (AutoCompleteTextView)findViewById(R.id.etCustomer);
		etCustomer.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.CUSTOMER));

		etLocation = (AutoCompleteTextView)findViewById(R.id.etLocation);
		etLocation.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.LOCATION));
		
		etSystem  = (AutoCompleteTextView)findViewById(R.id.etSystem);
		etSystem.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.SYSTEM));
		
		etPlan = (AutoCompleteTextView)findViewById(R.id.etPlan);
		etPlan.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.PLAN));
		
		etDetail = (AutoCompleteTextView)findViewById(R.id.etDetail);
		etDetail.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.DETAIL));
		
		etComments = (EditText)findViewById(R.id.etComments);
		etCustom = (AutoCompleteTextView)findViewById(R.id.etCustom);
		etCustom.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.DETAIL));
		
		rgType = (RadioGroup)findViewById(R.id.rgType);
		cbPassed = (CompoundButton)findViewById(R.id.cbPassed);

		ibImage = new ImageButton[] { 
				(ImageButton)findViewById(R.id.ibImage1),
				(ImageButton)findViewById(R.id.ibImage2),
				(ImageButton)findViewById(R.id.ibImage3)
		};
		for (View v : ibImage) {
			v.setOnClickListener(this);
		}
		tvImageMeta = new TextView[] {
				(TextView)findViewById(R.id.tvImage1Meta),
				(TextView)findViewById(R.id.tvImage2Meta),
				(TextView)findViewById(R.id.tvImage3Meta)
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
		etInspector.setText(form.inspector);
		etCustomer.setText(form.customer);
		etLocation.setText(form.location);
		etSystem.setText(form.system);
		etPlan.setText(form.plan);
		etDetail.setText(form.detail);
		etComments.setText(form.comments);
		if (form.type == null || form.type.length() == 0) {
			rgType.check(R.id.rbMechanical);
			etCustom.setEnabled(false);
		} else if (form.type.equals("Plumbing")) {
			rgType.check(R.id.rbPlumbing);
			etCustom.setEnabled(false);
		} else if (form.type.equals("Mechanical")) {
			rgType.check(R.id.rbMechanical);
			etCustom.setEnabled(false);
		} else if (form.type.equals("Process")) {
			rgType.check(R.id.rbProcess);
			etCustom.setEnabled(false);
		} else {
			rgType.check(R.id.rbCustom);
			etCustom.setEnabled(true);
			etCustom.setText(form.type);
		}
		cbPassed.setChecked(form.passed);
		for (int i=0; i<form.imagePath.length; i++)
			if (form.imagePath[i] != null) {
				try {
					ibImage[i].setImageURI(Uri.fromFile(new File(getImagesPath(), form.imagePath[i])));
				} catch (Exception e) {
					e.printStackTrace();
					// failing here means the image is lost
					form.imagePath[i] = null;
				}
			}
		
		for (int i=0; i<tvImageMeta.length; i++) {
			tvImageMeta[i].setText(form.imageMeta[i]);
		}

		// do this last so onCheckChanged listener does not get called while we are initing the view
		rgType.setOnCheckedChangeListener(this);

		getWindow().setSoftInputMode(
			    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
			);
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
				editImage(0);
				break;
			case R.id.ibImage2:
				editImage(1);
				break;
			case R.id.ibImage3:
				editImage(2);
				break;
		}
	}
	
	@Override
	public void onBackPressed() {
		save();
		super.onBackPressed();
	}
	
	private void editImage(final int index) {
		if (form.imagePath[index] != null) {
			
			View view = View.inflate(getActivity(), R.layout.popup_image_enlarge, null);
			ImageView iv = (ImageView)view.findViewById(R.id.ivPhoto);
			final EditText et = (EditText)view.findViewById(R.id.etMeta);
			et.setText(form.imageMeta[index]);
			iv.setImageURI(Uri.fromFile(new File(getImagesPath(), form.imagePath[index])));
			newDialogBuilder()
				.setView(view)
				.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						form.imageMeta[index] = et.getText().toString();
						tvImageMeta[index].setText(et.getText());
					}
				})
				.setPositiveButton("Change", new DialogInterface.OnClickListener() {
				
    				@Override
    				public void onClick(DialogInterface dialog, int which) {
    					pickImage(index);
    				}
    			})
    			.setNeutralButton("Remove", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						form.imagePath[index] = null;
						form.imageMeta[index] = null;
						ibImage[index].setImageBitmap(null);
						tvImageMeta[index] = null;
					}
				})
    			.show();
		} else {
			pickImage(index);
		}
	}
	
	public void save() {
		form.inspector = etInspector.getText().toString().trim();
		form.customer = etCustomer.getText().toString().trim();
		form.editDate = new Date();
		form.comments = etComments.getText().toString().trim();
		form.detail = etDetail.getText().toString().trim();
		form.location = etLocation.getText().toString().trim();
		form.passed = cbPassed.isChecked();
		form.plan = etPlan.getText().toString().trim();
		form.system = etSystem.getText().toString().trim();
		switch (rgType.getCheckedRadioButtonId()) {
			case R.id.rbCustom:
				form.type = etCustom.getText().toString();
				break;
			case R.id.rbPlumbing:
				form.type = "Plumbing";
				break;
			case R.id.rbMechanical:
				form.type = "Mechanical";
				break;
			case R.id.rbProcess:
				form.type = "Process";
				break;
		}
		getFormHelper().addOrUpdateForm(form);
	}
	
	public void pickImage(final int id) {
		newDialogBuilder().setItems(new String[] {
				"Choose Existing",
				"Take Photo",
				"Cancel"
		}, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case 0: // choose existing
						startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI) , id << 1);
						break;
					case 1: // take a photo
						startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), (id << 1) | 1);
						break;
					default: // cancel
						dialog.dismiss();
				}
			}
		}).show();
	}
	
	@Override
	public void onActivityResult(int requestCodeAndIndex, int resultCode, Intent data) {
		int index = requestCodeAndIndex >> 1;
		int requestCode = requestCodeAndIndex & 0x1; 
						
    	if (data != null) {
    		Bitmap bitmap = null;
            int orientation = 0;
            switch (requestCode) {
            	case 0: {
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
                                Log.d(TAG, "got image orientation "+orientation);
                            }
    
        				} catch (Exception e) {
        					e.printStackTrace();
        				}
        			}
        			break;
            	} 
            	
            	
            	case 1: {
        			if (data.getExtras() != null) {
            			bitmap = (Bitmap) data.getExtras().get("data");
        			}
        			break;
            	}
            }

        	if (bitmap != null) {
        		bitmap = ThumbnailUtils.extractThumbnail(bitmap, 256, 256);
				Matrix matrix = new Matrix();
				switch (orientation) {
					case 90:
					case 180:
					case 270:
						matrix.postRotate(orientation);
						break;
				}

				Bitmap newBitmap;
			    newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
				if (newBitmap != null) {
					bitmap.recycle();
					bitmap = newBitmap;
				}

				try {
					File destFile = File.createTempFile("guage", ".png", getImagesPath());
					FileOutputStream out = new FileOutputStream(destFile);
					try {
						bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
						ibImage[index].setImageBitmap(bitmap);
						form.imagePath[index] = destFile.getName();
						if (form.imageMeta[index] == null) {
							form.imageMeta[index] = getDateFormatter().format(new Date());
							if (isAmbientTempAvailable()) {
								form.imageMeta[index] += " " + convertCelciusToFahrenheit(getAmbientTempCelcius()) + "\u00B0";
							}
						}
						editImage(index);
						
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

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		switch (checkedId) {
			case R.id.rbCustom:
				etCustom.setEnabled(true);
				form.type = etCustom.getText().toString();
				break;
			case R.id.rbMechanical:
				etCustom.setEnabled(false);
				form.type = "Mechanical";
				break;
			case R.id.rbPlumbing:
				etCustom.setEnabled(false);
				form.type = "Plumbing";
				break;
			case R.id.rbProcess:
				etCustom.setEnabled(false);
				form.type = "Process";
				break;
		}
	}
}
