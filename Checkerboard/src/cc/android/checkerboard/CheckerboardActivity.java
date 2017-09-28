package cc.android.checkerboard;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import cc.lib.android.CCActivityBase;
import cc.lib.utils.FileUtils;

public class CheckerboardActivity extends CCActivityBase implements View.OnClickListener {

	private CheckerboardView pbv;
    private View bEndTurn;
    private MyCheckers game;

    public static class MyCheckers extends Checkers {
        CheckerboardActivity ctxt;

        @Override
        protected void onGameOver() {
            ctxt.choosePlayers();
        }

        @Override
        protected void onRobotMoved(Move move) {
            ctxt.pbv.animateAndExecuteMove(move);
        }
    }

    void choosePlayers() {
        ArrayList<String> list = new ArrayList<>();
        list.add("One Player");
        list.add("Two Players");
        if (getSaveFile().exists())
            list.add("Resume");
        new AlertDialog.Builder(this).setItems(list.toArray(new String[list.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // one player
                        game.newGame();
                        game.newSinglePlayerGame(0);
                        break;
                    case 1:
                        game.newGame();
                        break;

                    case 2:
                        loadAsync();
                        break;
                }
            }
        }).setCancelable(false).show();

    }

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
        game = new MyCheckers();
        game.ctxt = this;
        game.newGame();
		setContentView(R.layout.cb_activity);
		pbv = (CheckerboardView)findViewById(R.id.cbView);
        pbv.game = game;
		findViewById(R.id.buttonNewGame).setOnClickListener(this);
        (bEndTurn = findViewById(R.id.buttonEndTurn)).setOnClickListener(pbv);
        bEndTurn.setVisibility(View.GONE);
        pbv.bEndTurn = bEndTurn;
        choosePlayers();
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
    }

    private void loadAsync() {
        final Dialog d = new ProgressDialog.Builder(this).setCancelable(true).setMessage("Loading...").show();
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                d.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancel(true);
                    }
                });
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                d.dismiss();
                getSaveFile().delete();
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    game.loadFromFile(getSaveFile());
                    FileUtils.copyFile(getSaveFile(), Environment.getExternalStorageDirectory());
                } catch (FileNotFoundException e) {

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

        }.execute();

    }

	@Override
	protected void onPoll() {
	}

    @Override
    public void onBackPressed() {
        if (game.canUndo()) {
            pbv.undo();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonNewGame) {
            pbv.onClick(v);
            choosePlayers();
        }
    }
}
