package cc.android.checkerboard;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.io.File;

import cc.lib.android.CCActivityBase;

public class CheckerboardActivity extends CCActivityBase implements OnClickListener {

	private CheckerboardView pbv;
	public static final Checkers game = new Checkers();
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.cb_activity);
        game.newGame();
		pbv = (CheckerboardView)findViewById(R.id.cbView);
		findViewById(R.id.buttonNewGame).setOnClickListener(this);
		findViewById(R.id.buttonEndTurn).setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
        try {
            game.loadFromFile(getSaveFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
		pbv.onResume();
	}

	private File getSaveFile() {
        return new File(getFilesDir(), "save.game");
    }

	@Override
	protected void onPause() {
		super.onPause();
		pbv.onPause();
        try {
            game.saveToFile(getSaveFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
