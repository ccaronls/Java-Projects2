package cc.android.photooverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.text.SpannedString;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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
	AutoCompleteTextView etProject;
	AutoCompleteTextView etSystem;
	AutoCompleteTextView etPlan;
	AutoCompleteTextView etSpec;
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
		
		etProject = (AutoCompleteTextView)findViewById(R.id.etProject);
		etProject.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.PROJECT));

		etSystem  = (AutoCompleteTextView)findViewById(R.id.etSystem);
		etSystem.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.SYSTEM));
		
		etPlan = (AutoCompleteTextView)findViewById(R.id.etPlan);
		etPlan.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.PLAN));
		
		etSpec = (AutoCompleteTextView)findViewById(R.id.etSpec);
		etSpec.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.SPEC));
		
		etComments = (EditText)findViewById(R.id.etComments);
		etCustom = (AutoCompleteTextView)findViewById(R.id.etCustom);
		etCustom.setAdapter(getAutoCompleteAdapter(SQLHelper.Column.TYPE));
		
		etSpec.setNextFocusForwardId(etComments.getId());
		
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
		etProject.setText(form.project);
		etSystem.setText(form.system);
		etPlan.setText(form.plan);
		etSpec.setText(form.spec);
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
			if (form.imageMeta[i] != null)
				tvImageMeta[i].setText(Html.fromHtml(form.imageMeta[i]));
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
				editImage(0, false);
				break;
			case R.id.ibImage2:
				editImage(1, false);
				break;
			case R.id.ibImage3:
				if (isPremiumEnabled(true))
					editImage(2, false);
				break;
		}
	}
	
	@Override
	public void onBackPressed() {
		save();
		super.onBackPressed();
	}
	
	private void editImage(final int index, boolean tempEditable) {
		if (form.imagePath[index] != null) {
			
			View view = View.inflate(getActivity(), R.layout.popup_image_enlarge, null);
			ImageView iv = (ImageView)view.findViewById(R.id.ivPhoto);
			final EditText etNotes = (EditText)view.findViewById(R.id.etMeta);
			final EditText etTemp = (EditText)view.findViewById(R.id.etMetaDegrees);
			String notes = form.imageMeta[index];
			String temp = "70";
			
			if (notes != null) {
				Pattern p = Pattern.compile("^\\-?[0-9]+");
				String s = Html.fromHtml(notes).toString();
				Matcher m = p.matcher(s);
				if (m.find()) {
					temp = m.group();
				}
			}
			
			if (tempEditable) {
				if (isAmbientTempAvailable()) {
					temp = String.valueOf(convertCelciusToFahrenheit(getAmbientTempCelcius()));
					newDialogBuilder().setTitle(R.string.popup_title_important).setMessage(R.string.popup_msg_tempinfo).setNegativeButton(R.string.popup_button_ok, null).show();
				}
				final int [] tempInt = new int[] { Integer.parseInt(temp) };
    			view.findViewById(R.id.buttonTempUp).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						tempInt[0]++;
						etTemp.setText(Html.fromHtml("" + tempInt[0] + "&deg;"));
					}
				});
    			view.findViewById(R.id.buttonTempDown).setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						tempInt[0]--;
						etTemp.setText(Html.fromHtml("" + tempInt[0] + "&deg;"));
					}
				});
			} else {
				view.findViewById(R.id.layoutTempAdjustment).setVisibility(View.GONE);
			}
			
			etTemp.setText(Html.fromHtml(temp + "&deg;"));
			if (notes != null) {
				String s = Html.fromHtml(notes).toString();
				int spc = s.indexOf(' ');
				if (spc > 0) {
					String note = s.substring(spc+1);
					etNotes.setText(note);
				}
			}

			iv.setImageURI(Uri.fromFile(new File(getImagesPath(), form.imagePath[index])));
			newDialogBuilder()
				.setView(view)
				.setNegativeButton(R.string.popup_button_ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SpannedString str = new SpannedString(etTemp.getText() + " " + etNotes.getText());
						form.imageMeta[index] = Html.toHtml(str);
						tvImageMeta[index].setText(Html.fromHtml(str.toString()));
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
						form.imagePath[index] = null;
						form.imageMeta[index] = null;
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
		form.inspector = etInspector.getText().toString().trim();
		form.customer = etCustomer.getText().toString().trim();
		form.editDate = new Date();
		form.comments = etComments.getText().toString().trim();
		form.spec = etSpec.getText().toString().trim();
		form.location = etLocation.getText().toString().trim();
		form.project = etProject.getText().toString().trim();
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
		try {

		/*
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
		}).show();*/
			startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), (id << 1) | 1);
		} catch (Exception e) {
			showAlert(R.string.popup_title_error, R.string.popup_msg_operation_not_supported);
		}
	}
	
	@Override
	public void onActivityResult(int requestCodeAndIndex, int resultCode, Intent data) {
		int index = requestCodeAndIndex >> 1;
		int requestCode = requestCodeAndIndex & 0x1; 
		Bitmap bitmap = null;
		int orientation = 0;
		
		switch (requestCode) {
        	case 0: {

            	if (data != null) {
            		
                    
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
            	} 
    			break;
        	}
            	
            case 1: {
    			if (data != null && data.getExtras() != null) {
        			bitmap = (Bitmap) data.getExtras().get("data");
    			}
    			break;
            }
            
            default:
            	super.onActivityResult(requestCode, resultCode, data);
            	break;
		}

    	if (bitmap != null) {
    		//if (isPremiumEnabled(false))
    			bitmap = ThumbnailUtils.extractThumbnail(bitmap, IMAGE_CAPUTE_DIM, IMAGE_CAPUTE_DIM);
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

			Bitmap newBitmap;
		    newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			if (newBitmap != null) {
				bitmap.recycle();
				bitmap = newBitmap;
			}
			
			// watermark
			//if (isPremiumEnabled(false))
			watermark(bitmap, getDateFormatter().format(new Date()));

			try {
				File destFile = File.createTempFile("guage", ".png", getImagesPath());
				FileOutputStream out = new FileOutputStream(destFile);
				try {
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
					ibImage[index].setImageBitmap(bitmap);
					form.imagePath[index] = destFile.getName();
					editImage(index, true);
					
				} finally {
					out.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			//bitmap.recycle();
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
