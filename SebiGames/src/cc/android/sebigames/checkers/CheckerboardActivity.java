package cc.android.sebigames.checkers;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import cc.android.sebigames.R;
import cc.lib.android.CCActivityBase;

public class CheckerboardActivity extends CCActivityBase implements OnClickListener {

	private CheckerboardView pbv;
	public static final ICheckerboard game = new Checkers();
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.cb_activity);
		pbv = findViewById(R.id.cbView);
		findViewById(R.id.buttonNewGame).setOnClickListener(this);
		findViewById(R.id.buttonEndTurn).setOnClickListener(this);
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

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.buttonEndTurn) {
			game.endTurn();
		} else if (v.getId() == R.id.buttonNewGame) {
			game.newGame();
		}
	}
	
}
