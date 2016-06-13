package cecc.android.mechdeficiency;

import java.io.*;
import java.util.*;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

public class FormSign extends BaseActivity {

	private Form form;
	private List<FormExport.Signature> signatures = new ArrayList<FormExport.Signature>();
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.formsign);
		findViewById(R.id.buttonAddSignature).setOnClickListener(this);
		findViewById(R.id.buttonEmail).setOnClickListener(this);
		findViewById(R.id.buttonESignInfo).setOnClickListener(this);
		form = (Form)getIntent().getParcelableExtra(INTENT_FORM);
		((TextView)findViewById(R.id.tvDate)).setText(getString(R.string.tvFormsignCertifiedOn, getDateFormatter().format(form.editDate)));
		//((TextView)findViewById(R.id.tvInspector)).setText(form.inspector);
		((TextView)findViewById(R.id.tvCustomer)).setText(form.customer);
		((TextView)findViewById(R.id.tvProject)).setText(form.project);
		((TextView)findViewById(R.id.tvLocation)).setText(form.location);
		((TextView)findViewById(R.id.tvInspector)).setText(form.representative);
		((TextView)findViewById(R.id.tvPlan)).setText(form.plan);
		//((TextView)findViewById(R.id.tvSpec)).setText(form.spec);
		ImageView [] images = new ImageView[] {
				(ImageView)findViewById(R.id.ivPhoto1),
				(ImageView)findViewById(R.id.ivPhoto2),
				(ImageView)findViewById(R.id.ivPhoto3),
				(ImageView)findViewById(R.id.ivPhoto4),
				(ImageView)findViewById(R.id.ivPhoto5),
				(ImageView)findViewById(R.id.ivPhoto6)
		};
		TextView [] meta = new TextView[] {
				(TextView)findViewById(R.id.tvPhotoData1),
				(TextView)findViewById(R.id.tvPhotoData2),
				(TextView)findViewById(R.id.tvPhotoData3),
				(TextView)findViewById(R.id.tvPhotoData4),
				(TextView)findViewById(R.id.tvPhotoData5),
				(TextView)findViewById(R.id.tvPhotoData6),
		};
		for (int i=0; i<images.length; i++) {
			Image image = form.getImageForIndex(i);
			if (image != null && image.path != null) {
				images[i].setImageBitmap(BitmapFactory.decodeFile(getImagesPath() + "/" + image.path));
				if (image.data != null)
					meta[i].setText(Html.fromHtml(image.data));
			}
		}
		((TextView)findViewById(R.id.tvType)).setText(form.type);
		((TextView)findViewById(R.id.tvComments)).setText(form.comments);
		TextView tvFix = (TextView)findViewById(R.id.tvFix);
		tvFix.setText(form.fix ? R.string.labelNeedsFixYes : R.string.labelNeedsFixNo);
		TextView tvFixed = (TextView)findViewById(R.id.tvFixed);
		tvFixed.setText(form.fixed ? R.string.labelFixDoneYes : R.string.labelFixDoneNo);
		
		showInfoDialogBuilderWithDontShowAgainCB(R.string.popup_title_important, R.string.popup_msg_signing_important, "PREF_HOW_E_SIGN_WORKS_DONT_SHOW_AGAIN_BOOL");
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.buttonAddSignature:
				startActivityForResult(new Intent(this, ESign.class), 0);
				break;
			case R.id.buttonEmail: {
				if (isPremiumEnabled(true)) {
					new FormExport(this, form, signatures.toArray(new FormExport.Signature[this.signatures.size()])).execute();
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
    				
    				FormExport.Signature signature = new FormExport.Signature(new File(data.getStringExtra(INTENT_BITMAP_FILE)), data.getStringExtra(INTENT_FULL_NAME_STRING), new Date());
    				
    				View view = View.inflate(getActivity(), R.layout.signaturelistitem, null);
    				
    				TextView name = (TextView)view.findViewById(R.id.tvFullname);
    				TextView date = (TextView)view.findViewById(R.id.tvDate);
    				ImageView sig = (ImageView)view.findViewById(R.id.ivSignature);

    				name.setText(signature.fullName);
    				sig.setImageURI(Uri.fromFile(signature.signatureFile));
    				date.setText(DateFormat.getDateFormat(getActivity()).format(signature.date));
    				
    				ViewGroup vg = (ViewGroup)findViewById(R.id.layoutSignatures);
    				vg.addView(view);
    				
    				this.signatures.add(signature);
    			}
    			break;
    			
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
}
