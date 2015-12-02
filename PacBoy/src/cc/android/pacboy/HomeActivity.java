package cc.android.pacboy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import cc.lib.android.CCActivityBase;

public class HomeActivity extends CCActivityBase implements OnClickListener {

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.home_activity);
		findViewById(R.id.buttonSmallEasy).setOnClickListener(this);
		findViewById(R.id.buttonSmallHard).setOnClickListener(this);
		findViewById(R.id.buttonMedEasy).setOnClickListener(this);
		findViewById(R.id.buttonMedHard).setOnClickListener(this);
		findViewById(R.id.buttonLargeEasy).setOnClickListener(this);
		findViewById(R.id.buttonLargeHard).setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onPoll() {
		super.onPoll();
	}

	@Override
	public void onClick(View v) {
		Intent i = new Intent(this, PacBoyActivity.class);
		switch (v.getId()) {
			case R.id.buttonSmallEasy:
				i.putExtra("width", 6);
				i.putExtra("height", 4);
				i.putExtra("hard", false);
				break;
			case R.id.buttonSmallHard:
				i.putExtra("width", 6);
				i.putExtra("height", 4);
				i.putExtra("hard", true);
				break;
			case R.id.buttonMedEasy:
				i.putExtra("width", 10);
				i.putExtra("height", 6);
				i.putExtra("hard", false);
				break;
			case R.id.buttonMedHard:
				i.putExtra("width", 10);
				i.putExtra("height", 6);
				i.putExtra("hard", true);
				break;
			case R.id.buttonLargeEasy:
				i.putExtra("width", 16);
				i.putExtra("height", 10);
				i.putExtra("hard", false);
				break;
			case R.id.buttonLargeHard:
				i.putExtra("width", 16);
				i.putExtra("height", 10);
				i.putExtra("hard", true);
				break;
		}
		startActivity(i);
	}

}