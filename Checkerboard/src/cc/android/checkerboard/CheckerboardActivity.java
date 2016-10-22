package cc.android.checkerboard;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import cc.lib.android.CCActivityBase;

public class CheckerboardActivity extends CCActivityBase {

	private CheckerboardView pbv;
	private final Checkers checkers = new Checkers();
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.cb_activity);
		pbv = (CheckerboardView)findViewById(R.id.cbView);
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
	}
	
}
