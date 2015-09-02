package cc.android.photooverlay;

import cc.android.photooverlay.BillingTask.Op;
import android.app.Activity;
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
				finish();
			}
		}, 4000);
		/*
		if (getPrefs().getBoolean("FIRST_LAUNCH_BOOL", true)) {
			new BillingTask(Op.REFRESH_PURCHASED, getActivity()).execute();
			getPrefs().edit().putBoolean("FIRST_LAUNCH_BOOL", false).commit();
		}*/
	}
	
}
