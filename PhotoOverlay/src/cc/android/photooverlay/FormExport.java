package cc.android.photooverlay;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class FormExport extends BaseActivity {
	
	public final static String INTENT_FORM = "FORM";
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.formexport);
		TextView tvTop = (TextView)findViewById(R.id.tvFormTop);
		TextView tvBottom = (TextView)findViewById(R.id.tvFormBottom);
		ImageView [] iv = new ImageView[] {
				(ImageView)findViewById(R.id.ivImage1),
				(ImageView)findViewById(R.id.ivImage2),
		};
		
		Form form = (Form)getIntent().getParcelableExtra(INTENT_FORM);
		String top = "Pressure Test Verification"
				   + "\nCustomer: " + (form.customer == null ? "???" : form.customer)
	
				   ;
//		tvTop.setText()
	}

}
