package cc.android.checkerboard;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
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

public class CheckerboardActivity extends CCActivityBase implements OnClickListener {

	private CheckerboardView pbv;
    private Button bEndTurn;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.cb_activity);
		pbv = findViewById(R.id.cbView);
		findViewById(R.id.buttonNewGame).setOnClickListener(pbv);
        (bEndTurn = findViewById(R.id.buttonEndTurn)).setOnClickListener(this);
        bEndTurn.setVisibility(View.GONE);
	}

	@Override
	protected void onPoll() {
	}

    @Override
    public void onClick(View view) {
        pbv.onClick(view);
        bEndTurn.setVisibility(pbv.canEndTurn() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBackPressed() {
    }


}
