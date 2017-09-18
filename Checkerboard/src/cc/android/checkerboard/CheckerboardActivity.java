package cc.android.checkerboard;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.File;

import cc.lib.android.CCActivityBase;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Reflector;

public class CheckerboardActivity extends CCActivityBase {

	private CheckerboardView pbv;
    private Button bEndTurn;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.cb_activity);
		pbv = findViewById(R.id.cbView);
		findViewById(R.id.buttonNewGame).setOnClickListener(pbv);
        (bEndTurn = findViewById(R.id.buttonEndTurn)).setOnClickListener(pbv);
	}

	@Override
	protected void onResume() {
		super.onResume();
		pbv.resume(getSaveFile());
	}

	private File getSaveFile() {
        return new File(getFilesDir(), "save.game");
    }

	@Override
	protected void onPause() {
		super.onPause();
        pbv.pause(getSaveFile());
	}

	@Override
	protected void onPoll() {
	}


    @Override
    public void onBackPressed() {
        // eat this since it is too easy to press and close the game.
    }
}
