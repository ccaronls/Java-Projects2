package cc.android.photooverlay;

import cc.android.photooverlay.BillingTask.Op;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Splash extends BaseActivity {

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		View v = View.inflate(this, R.layout.splash, null);
		setContentView(v);
		v.postDelayed(new Runnable() {
			public void run() {
				startActivity(new Intent(getActivity(), FormsList.class));
				finish();
			}
		}, 4000);
		new BillingTask(Op.REFRESH_PURCHASED, getActivity()).execute();
	}
	
}
