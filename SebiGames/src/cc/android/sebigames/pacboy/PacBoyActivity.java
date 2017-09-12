package cc.android.sebigames.pacboy;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import cc.android.sebigames.R;
import cc.lib.android.CCActivityBase;

public class PacBoyActivity extends CCActivityBase {

	public final static String INTENT_EXTRA_INT_WIDTH = "width";
	public final static String INTENT_EXTRA_INT_HEIGHT = "height";
	public final static String INTENT_EXTRA_INT_DIFFUCULTY = "difficulty";
	
	private final static String PREF_HIGH_SCORE_INT = "HIGH_SCORE";
	
	private PacBoyView pbv;
	private TextView tvScore;
	private TextView tvHighScore;
	private int highScore = 0;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.pacboy_activity);
		pbv = (PacBoyView)findViewById(R.id.pacBoyView);
		tvScore = (TextView)findViewById(R.id.textViewScore);
		tvHighScore = (TextView)findViewById(R.id.textViewHighScore);
	}

	@Override
	protected void onResume() {
		super.onResume();
		int width = getIntent().getIntExtra(INTENT_EXTRA_INT_WIDTH, 10);
		int height = getIntent().getIntExtra(INTENT_EXTRA_INT_HEIGHT, 10);
		int difficulty = getIntent().getIntExtra(INTENT_EXTRA_INT_DIFFUCULTY, 0);
		pbv.initMaze(width, height, difficulty);
		pbv.setPaused(false);
		highScore = getPrefs().getInt(PREF_HIGH_SCORE_INT, 0);
		tvHighScore.setText("" + highScore);
		tvScore.setText("0");
		startPolling(1);
	}

	@Override
	protected void onPause() {
		super.onPause();
		pbv.setPaused(true);
		int score = pbv.getScore();
		int highScore = getPrefs().getInt(PREF_HIGH_SCORE_INT, 0);
		if (score > highScore) {
			getPrefs().edit().putInt(PREF_HIGH_SCORE_INT, score).commit();
		}
	}

	@Override
	protected void onPoll() {
		int score = pbv.getScore();
		tvScore.setText("" + score);
		if (score > highScore) {
			highScore = score;
			tvHighScore.setText("" + score);
		}
		if (pbv.getDifficulty() < PacBoyRenderer.DIFFICULTY_NO_CHASE) {
			tvHighScore.setVisibility(View.INVISIBLE);
			tvScore.setVisibility(View.INVISIBLE);
		} else {
			tvHighScore.setVisibility(View.VISIBLE);
			tvScore.setVisibility(View.VISIBLE);
		}
	}
	
}
