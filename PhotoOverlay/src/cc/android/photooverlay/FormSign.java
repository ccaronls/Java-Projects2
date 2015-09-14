package cc.android.photooverlay;

import java.io.*;
import java.util.*;

import cc.lib.android.EmailHelper;
import cc.lib.utils.FileUtils;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

public class FormSign extends BaseActivity {

	private Form form;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.formsign);
		findViewById(R.id.buttonAddSignature).setOnClickListener(this);
		findViewById(R.id.buttonEmail).setOnClickListener(this);
		findViewById(R.id.buttonESignInfo).setOnClickListener(this);
		form = (Form)getIntent().getParcelableExtra(INTENT_FORM);
		((TextView)findViewById(R.id.tvDate)).setText("Certified on: " + getDateFormatter().format(form.editDate));
		((TextView)findViewById(R.id.tvInspector)).setText(form.inspector);
		((TextView)findViewById(R.id.tvCustomer)).setText(form.customer);
		((TextView)findViewById(R.id.tvProject)).setText(form.project);
		((TextView)findViewById(R.id.tvLocation)).setText(form.location);
		((TextView)findViewById(R.id.tvSystem)).setText(form.system);
		((TextView)findViewById(R.id.tvPlan)).setText(form.plan);
		((TextView)findViewById(R.id.tvSpec)).setText(form.spec);
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
		((TextView)findViewById(R.id.tvType)).setText(form.type);
		((TextView)findViewById(R.id.tvComments)).setText(form.comments);
		TextView tvStatus = (TextView)findViewById(R.id.tvStatus);
		if (form.passed) {
			tvStatus.setText(R.string.labelPassed);
			tvStatus.setTextColor(Color.GREEN);
		} else {
			tvStatus.setText(R.string.labelFailed);
			tvStatus.setTextColor(Color.RED);
		}
		newDialogBuilder().setTitle(R.string.popup_title_important)
			.setMessage(R.string.popup_msg_signing_important)
			.setNegativeButton(R.string.popup_button_iunderstand, null).show();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.buttonAddSignature:
				startActivityForResult(new Intent(this, ESign.class), 0);
				break;
			case R.id.buttonEmail: {
				if (isPremiumEnabled(true)) {
    				generateImagePages();
				}				
				
				break;
			}
			case R.id.buttonESignInfo: {
				WebView wv = new WebView(this);
				wv.loadUrl("file:///android_asset/about_esigning.html");
				newDialogBuilder().setTitle(R.string.popup_title_how_esign_works).setView(wv).setNegativeButton(R.string.popup_button_ok, null).show();
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
	
	
	private void generateImagePages() {
		// generate an image of the form and send it off
		final ProgressDialog dialog = ProgressDialog.show(getActivity(), getString(R.string.popup_title_processing), getString(R.string.popup_msg_processing_email));
		new AsyncTask<Void,Integer,Void>() {

			int numSignatures = 0;
			int numPages = 0;
			final int sigsPerPage = 3;
			ViewGroup layoutForm;
			int formWidth, formHeight;
			
			OnGlobalLayoutListener listener = new OnGlobalLayoutListener() {
				
				@Override
				public void onGlobalLayout() {
					synchronized (this) {
						notify();
					}
				}
			};

			private void setVisibilities(int vis) {
				
				int [] ids = {
						R.id.layoutImages,
						R.id.layoutMeta,
						R.id.textView13,
						R.id.tvType,
						R.id.textView15,
						R.id.tvComments,
						R.id.textView17,
						R.id.tvStatus
				};
				for (int id : ids) {
					findViewById(id).setVisibility(vis);
				}
			}
			
			@Override
			protected void onProgressUpdate(Integer... values) {
				// using this to make changes on the UI thread
				layoutForm = (ViewGroup)findViewById(R.id.layoutForm);
				layoutForm.getViewTreeObserver().addOnGlobalLayoutListener(listener);
				int page = values[0];
				ViewGroup sigs = (ViewGroup)findViewById(R.id.layoutSignatures);
				for (int i=0; i<sigs.getChildCount(); i++) {
					sigs.getChildAt(i).setVisibility(View.GONE);
				}

				int startIndex = (page*sigsPerPage) + 1;
				for (int i=0; i<sigsPerPage; i++) {
					View s = sigs.getChildAt(i+startIndex);
					if (s == null)
						break;
					s.setVisibility(View.VISIBLE);
				}
				
				setVisibilities(View.GONE);
				((TextView)findViewById(R.id.tvDate)).setText("Certified on: " + getDateFormatter().format(form.editDate) + " Page " + (2+page) + " of " + numPages);

				getContentView().forceLayout();
			}

			@Override
			protected void onPreExecute() {
				
				layoutForm = (ViewGroup)findViewById(R.id.layoutForm);
				ViewGroup vg = (ViewGroup)findViewById(R.id.layoutSignatures); 
				numSignatures = vg.getChildCount();
				numPages =  1 + (numSignatures+(sigsPerPage-2)) / sigsPerPage;
				
				int signatureHeight = (numSignatures > 0 ? vg.getChildAt(0).getHeight() : 0);

				formHeight = layoutForm.getHeight();
				formHeight -= numSignatures*signatureHeight;
				formWidth = layoutForm.getWidth();
				
				if (numSignatures > 0)
					formHeight += signatureHeight;
				
				if (numPages > 1) {
					((TextView)findViewById(R.id.tvDate)).setText("Certified on: " + getDateFormatter().format(form.editDate) + " Page 1 of " + numPages);
				}
				
				Log.d(TAG, "form dim = " + formWidth + " x " + formHeight);
			}
			
			private void makeJPEG(File file) throws IOException {
				Bitmap bm = Bitmap.createBitmap(formWidth, formHeight, Bitmap.Config.ARGB_8888);
				Rect clipRect = new Rect(0,0,formWidth, formHeight);
				//layoutForm.setClipBounds(clipRect);
				Canvas canvas = new Canvas(bm);
				Paint p = new Paint();
				p.setColor(Color.WHITE);
				p.setStyle(Style.FILL);
				canvas.drawRect(0, 0, formWidth, formHeight, p);
				canvas.clipRect(clipRect, Op.REPLACE);
				
				layoutForm.draw(canvas);
				//File file = File.createTempFile("form", ".jpg", getCacheDir());
				OutputStream out = new FileOutputStream(file);
				try {
					bm.compress(CompressFormat.JPEG, 90, out);
				} finally {
					bm.recycle();
					out.close();
				}				
			}
			
			@Override
			protected Void doInBackground(Void... params) {
				try {
					
					if (numPages == 1) {
					
						File file = File.createTempFile("form", ".jpg", getCacheDir());
						makeJPEG(file);
						EmailHelper.sendEmail(getActivity(), file, null, getString(R.string.emailSubjectSignedPTCform), getString(R.string.email_body_signed_form));
    					
					} else {
						
						File tempDir = File.createTempFile("forms", ".tmp", getCacheDir());
						tempDir.delete();
						tempDir.mkdir();
						for (int i=0; i<numPages; i++) {
    						File page = new File(tempDir, "page" + i + ".jpg");
    						makeJPEG(page);
							publishProgress(i);
    						synchronized (listener) {
    							try {
    								listener.wait(500);
    							} catch (Exception e) {
    								
    							}
    						}
						}
						File tempFile = File.createTempFile("form", ".zip");
						FileUtils.zipFiles(tempFile, Arrays.asList(tempDir.listFiles()));
						EmailHelper.sendEmail(getActivity(), tempFile, null, getString(R.string.emailSubjectSignedPTCform), getString(R.string.email_body_signed_form));

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@SuppressWarnings("deprecation")
			@Override
			protected void onPostExecute(Void result) {
				layoutForm.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
				((TextView)findViewById(R.id.tvDate)).setText("Certified on: " + getDateFormatter().format(form.editDate));
				setVisibilities(View.VISIBLE);
				ViewGroup sigs = (ViewGroup)findViewById(R.id.layoutSignatures);
				for (int i=0; i<sigs.getChildCount(); i++) {
					sigs.getChildAt(i).setVisibility(View.VISIBLE);
				}
				dialog.dismiss();
			}
			
		}.execute();		
	}
}
