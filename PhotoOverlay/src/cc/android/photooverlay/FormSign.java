package cc.android.photooverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cc.lib.android.EmailHelper;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class FormSign extends BaseActivity {

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.formsign);
		findViewById(R.id.buttonAddSignature).setOnClickListener(this);
		findViewById(R.id.buttonEmail).setOnClickListener(this);
		findViewById(R.id.buttonESignInfo).setOnClickListener(this);
		Form form = (Form)getIntent().getParcelableExtra(INTENT_FORM);
		((TextView)findViewById(R.id.tvDate)).setText("Inspected on: " + getDateFormatter().format(form.editDate));
		((TextView)findViewById(R.id.tvInspector)).setText(form.inspector);
		((TextView)findViewById(R.id.tvCustomer)).setText(form.customer);
		((TextView)findViewById(R.id.tvProject)).setText(form.project);
		((TextView)findViewById(R.id.tvLocation)).setText(form.location);
		((TextView)findViewById(R.id.tvPlan)).setText(form.plan);
		((TextView)findViewById(R.id.tvDetail)).setText(form.detail);
		ImageView [] images = new ImageView[] {
				(ImageView)findViewById(R.id.ivGauge1),
				(ImageView)findViewById(R.id.ivGauge2),
				(ImageView)findViewById(R.id.ivGauge3)
		};
		TextView [] meta = new TextView[] {
				(TextView)findViewById(R.id.tvImage1Meta),
				(TextView)findViewById(R.id.tvImage2Meta),
				(TextView)findViewById(R.id.tvImage3Meta),
		};
		for (int i=0; i<images.length; i++) {
			if (form.imagePath[i] != null) {
				images[i].setImageBitmap(BitmapFactory.decodeFile(getImagesPath() + "/" + form.imagePath[i]));
				meta[i].setText(form.imageMeta[i]);
			}
		}
		((TextView)findViewById(R.id.tvType)).setText(form.type.name());
		((TextView)findViewById(R.id.tvComments)).setText(form.comments);
		TextView tvStatus = (TextView)findViewById(R.id.tvStatus);
		if (form.passed) {
			tvStatus.setText("PASSED");
			tvStatus.setTextColor(Color.GREEN);
		} else {
			tvStatus.setText("FAILED");
			tvStatus.setTextColor(Color.RED);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.buttonAddSignature:
				startActivityForResult(new Intent(this, ESign.class), 0);
				break;
			case R.id.buttonEmail: {
				// generate an image of the form and send it off
				final ProgressDialog dialog = ProgressDialog.show(getActivity(), "Generating", "Generating your form for email");
				new AsyncTask<Void,Void,Void>() {

					@Override
					protected void onPreExecute() {
					}
					
					
					@Override
					protected Void doInBackground(Void... params) {
						try {
							View form = findViewById(R.id.layoutForm);
							Bitmap bm = Bitmap.createBitmap(form.getWidth(), form.getHeight(), Bitmap.Config.ARGB_8888);
							form.draw(new Canvas(bm));
    						File file = File.createTempFile("form", ".jpg", getCacheDir());
    						OutputStream out = new FileOutputStream(file);
    						try {
    							bm.compress(CompressFormat.JPEG, 90, out);
    							EmailHelper.sendEmail(getActivity(), file, null, "Signed form on " + new Date(), "There is a signed form attached for interested parties.  Any disputes as to the validity of the attached document should be reported at once.");
    						} finally {
    							bm.recycle();
    							out.close();
    						}
						} catch (Exception e) {
							e.printStackTrace();
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						dialog.dismiss();
					}
					
				}.execute();
				
				
				break;
			}
			case R.id.buttonESignInfo: {
				newDialogBuilder().setTitle("How eSigning Works").setMessage(R.string.popup_msg_how_esign_works).setNegativeButton("Ok", null).show();
				
				break;
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case 0:
    			if (resultCode == RESULT_OK) {
    				
    				String fullName = data.getStringExtra(INTENT_FULL_NAME_STRING);
    				
    				View view = View.inflate(getActivity(), R.layout.signaturelistitem, null);
    				
    				TextView name = (TextView)view.findViewById(R.id.tvFullname);
    				TextView date = (TextView)view.findViewById(R.id.tvDate);
    				ImageView sig = (ImageView)view.findViewById(R.id.ivSignature);

    				name.setText(fullName);
    				sig.setImageURI(Uri.fromFile(new File(data.getStringExtra(INTENT_BITMAP_FILE))));
    				date.setText(DateFormat.getDateFormat(getActivity()).format(new Date()));
    				
    				ViewGroup vg = (ViewGroup)findViewById(R.id.layoutSignatures);
    				vg.addView(view);
    			}
    			break;
    			
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}
}
