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
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 6);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 4);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0);
				break;
			case R.id.buttonSmallHard:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 6);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 4);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 1);
				break;
			case R.id.buttonMedEasy:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 10);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 6);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0);
				break;
			case R.id.buttonMedHard:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 10);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 6);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 1);
				break;
			case R.id.buttonLargeEasy:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 16);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 10);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0);
				break;
			case R.id.buttonLargeHard:
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_WIDTH, 16);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_HEIGHT, 10);
				i.putExtra(PacBoyActivity.INTENT_EXTRA_INT_DIFFUCULTY, 0);
				break;
		}
		startActivity(i);
	}

}