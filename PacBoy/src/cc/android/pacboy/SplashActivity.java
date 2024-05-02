package cc.android.pacboy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import cc.lib.android.CCActivityBase;

public class SplashActivity extends CCActivityBase implements Runnable {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PacBoyView v = new PacBoyView(this);
        v.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                v.removeCallbacks(SplashActivity.this);
                v.post(SplashActivity.this);
            }
        });
        v.initIntro();
		setContentView(v);
		v.postDelayed(this, BuildConfig.DEBUG ? 9000 : 9000);
	}

	@Override
	public void run() {
		startActivity(new Intent(this, HomeActivity.class));
		finish();
	}
	
}
