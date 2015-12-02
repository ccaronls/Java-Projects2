package cc.android.pacboy;

import android.os.Bundle;
import cc.lib.android.CCActivityBase;

public class PacBoyActivity extends CCActivityBase {

	private PacBoyView pbv;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.pacboy_activity);
		pbv = (PacBoyView)findViewById(R.id.pacBoyView);
		int width = getIntent().getIntExtra("width", 0);
		int height = getIntent().getIntExtra("height", 0);
		boolean hard = getIntent().getBooleanExtra("hard", false);
		pbv.initMaze(width, height, hard);
	}

	@Override
	protected void onResume() {
		super.onResume();
		pbv.setPaused(false);
	}

	@Override
	protected void onPause() {
		super.onPause();
		pbv.setPaused(true);
	}

	@Override
	protected void onPoll() {
		// TODO Auto-generated method stub
		super.onPoll();
	}

	
}
