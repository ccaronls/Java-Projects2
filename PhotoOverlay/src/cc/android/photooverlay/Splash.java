package cc.android.photooverlay;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class Splash extends Activity {

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		View v = View.inflate(this, R.layout.splash, null);
		setContentView(v);
		v.postDelayed(new Runnable() {
			public void run() {
				finish();
			}
		}, 4000);
		
		System.err.println("Form class=" + Form.class.getSimpleName());
	}
	
}
