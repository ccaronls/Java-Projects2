package cc.android.checkerboard;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import cc.lib.android.CCActivityBase;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Reflector;

public class CheckerboardActivity extends CCActivityBase {

	private CheckerboardView pbv;
    private Button bEndTurn;
    private Checkers game;

    class MyCheckers extends Checkers {
        @Override
        public void executeMove(Move move) {
            super.executeMove(move);
            bEndTurn.setVisibility(pbv.canEndTurn() ? View.VISIBLE : View.GONE);
        }
    }

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
        game = new MyCheckers();
        game.newGame();
		setContentView(R.layout.cb_activity);
		pbv = findViewById(R.id.cbView);
        pbv.game = game;
		findViewById(R.id.buttonNewGame).setOnClickListener(pbv);
        (bEndTurn = findViewById(R.id.buttonEndTurn)).setOnClickListener(pbv);
        bEndTurn.setVisibility(View.GONE);
	}

	private File getSaveFile() {
        return new File(getFilesDir(), "checkers.save");
    }

	@Override
    public void onPause() {
        super.onPause();
        game.trySaveToFile(getSaveFile());
    }

    @Override
    public void onResume() {
        super.onResume();
        game.tryLoadFromFile(getSaveFile());
    }

	@Override
	protected void onPoll() {
	}

    @Override
    public void onBackPressed() {
        if (game.canUndo()) {
            game.undo();
            pbv.invalidate();
        } else {
            super.onBackPressed();
        }
    }



}
