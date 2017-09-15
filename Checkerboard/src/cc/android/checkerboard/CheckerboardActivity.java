package cc.android.checkerboard;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;

import java.io.File;

import cc.lib.android.CCActivityBase;
import cc.lib.utils.FileUtils;

public class CheckerboardActivity extends CCActivityBase implements OnClickListener {

	private CheckerboardView pbv;
	public static Checkers game;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
        game = new Checkers();
		setContentView(R.layout.cb_activity);
		pbv = (CheckerboardView)findViewById(R.id.cbView);
		findViewById(R.id.buttonNewGame).setOnClickListener(this);
		findViewById(R.id.buttonEndTurn).setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
        try {
            game.loadFromFile(getSaveFile());
            FileUtils.copyFile(getSaveFile(), Environment.getExternalStorageDirectory());
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
