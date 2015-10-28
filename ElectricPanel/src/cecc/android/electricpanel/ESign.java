package cecc.android.electricpanel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import cecc.android.lib.ESignView;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class ESign extends BaseActivity {

	ESignView esignView;
	EditText etFullname;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.esign);
		findViewById(R.id.buttonAccept).setOnClickListener(this);
		findViewById(R.id.buttonRestart).setOnClickListener(this);
		esignView = (ESignView)findViewById(R.id.eSign);
		etFullname = (EditText)findViewById(R.id.etFullname);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.buttonAccept:
				Intent i = new Intent();
				try {
					esignView.setDrawingCacheEnabled(true);
    				Bitmap signature = esignView.getDrawingCache();
    				float aspect = (float)signature.getWidth() / signature.getHeight();
    				int newHeight = (int)(256f / aspect);
    				signature = Bitmap.createScaledBitmap(signature, 512, newHeight, false);
    				esignView.setDrawingCacheEnabled(false);
    				i.putExtra(INTENT_FULL_NAME_STRING, etFullname.getText().toString());
    				File file = File.createTempFile("signature", ".jpg", getCacheDir());
    				OutputStream out = new FileOutputStream(file);
    				try {
    					signature.compress(CompressFormat.PNG, 90, out);
    				} finally {
    					out.close();
    					signature.recycle();
    				}
    				i.putExtra(INTENT_BITMAP_FILE, file.getAbsolutePath());
    				setResult(RESULT_OK, i);
				} catch (Exception e) {
					i.putExtra(INTENT_ERROR, e.getClass() + " " + e.getMessage());
					setResult(RESULT_CANCELED);
				}
				finish();
				break;
				
			case R.id.buttonRestart:
				esignView.clearSignature();
				break;
		}
	}
	
}
