package cc.android.checkerboard;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import cc.lib.android.CCActivityBase;
import cc.lib.game.Utils;
import cc.lib.utils.FileUtils;

public class CheckerboardActivity extends CCActivityBase implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public final static int THINKING_PROGRESS_DELAY = 500;

	private CheckerboardView pbv;
    private View bEndTurn;
    private TextView tvDebug;
    private Checkers.DescisionTree root = null;
    private View bUp, bDown, bLeft, bRight;
    private CompoundButton tbDebug;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Checkers game = new MyCheckers();
        game.newGame();
        setContentView(R.layout.cb_activity);
        pbv = (CheckerboardView)findViewById(R.id.cbView);
        tvDebug = (TextView)findViewById(R.id.tvDebug);
        pbv.setGame(game);
        findViewById(R.id.buttonNewGame).setOnClickListener(this);
        (bEndTurn = findViewById(R.id.buttonEndTurn)).setOnClickListener(pbv);
        (bUp = findViewById(R.id.buttonUp)).setOnClickListener(this);
        (bLeft = findViewById(R.id.buttonLeft)).setOnClickListener(this);
        (bRight = findViewById(R.id.buttonRight)).setOnClickListener(this);
        (bDown = findViewById(R.id.buttonDown)).setOnClickListener(this);
        tvDebug = (TextView)findViewById(R.id.tvDebug);
        tbDebug = (CompoundButton)findViewById(R.id.toggleButtonDebug);
        tbDebug.setOnCheckedChangeListener(this);
        bEndTurn.setVisibility(View.GONE);
        setDebugMode(false);
        choosePlayers();
    }

    private void setDebugMode(boolean on) {
        tvDebug.setVisibility(on ? View.VISIBLE : View.GONE);
        bUp.setVisibility(on ? View.VISIBLE : View.GONE);
        bLeft.setVisibility(on ? View.VISIBLE : View.GONE);
        bRight.setVisibility(on ? View.VISIBLE : View.GONE);
        bDown.setVisibility(on ? View.VISIBLE : View.GONE);
        bUp.setEnabled(false);
        bLeft.setEnabled(false);
        bRight.setEnabled(false);
        bDown.setEnabled(root != null && root.getFirst() != null);
        pbv.highlightMove(null);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setDebugMode(isChecked);
    }

    private void dumpTree(Checkers.DescisionTree root, int indent) {
        if (root == null)
            return;
        Log.d("Tree", String.format("%sVal: %f\n", Utils.getRepeatingChars(' ', indent*3), root.getValue()));
        dumpTree(root.getFirst(), indent+1);
        dumpTree(root.getNext(), indent);
    }

    public class MyCheckers extends Checkers implements Runnable, DialogInterface.OnCancelListener {
        private Dialog dialog = null;
        private AsyncTask task = null;

        @Override
        public void onCancel(DialogInterface dialog) {
            if (task != null) {
                task.cancel(true);
                undo();
            }
        }

        public void run() {
            dialog.show();
        }

        @Override
        public void executeMove(Move move) {
            if (task != null) {
                throw new AssertionError();
            }
            final CheckerboardActivity act = CheckerboardActivity.this;
            if (dialog == null) {
                dialog = new AlertDialog.Builder(act).setTitle("Thinking").setView(new ProgressBar(act)).setOnCancelListener(this).create();
            }
            task = new AsyncTask<Move, Integer, Object>() {
                @Override
                protected void onPreExecute() {
                    root = null;
                    setDebugMode(false);
                    if (tbDebug.isChecked()) {
                        root = new DescisionTree(MyCheckers.this);
                        root.meta += "ROOT\nPlayer num: " + getCurPlayerNum();
                    }
                    pbv.postDelayed(MyCheckers.this, THINKING_PROGRESS_DELAY);
                }

                @Override
                protected void onPostExecute(Object o) {
                    pbv.removeCallbacks(MyCheckers.this);
                    dialog.dismiss();
                    task = null;
                    if (root != null) {
                        root.meta += "\nValue: " + root.getValue();
                        //dumpTree(root, 0);
                    }
                    updateButtons();
                    pbv.postInvalidate();
                }

                @Override
                protected Object doInBackground(Move... params) {
                    MyCheckers.super.executeMove(params[0]);
                    return null;
                }

            }.execute(move);
        }

        @Override
        protected void onGameOver() {
            runOnUiThread(new Runnable() {
               public void run() {
                   choosePlayers();
               }
            });
        }

        @Override
        protected void onRobotMoved(Move ... moves) {
            final Move move = moves[0];
            runOnUiThread(new Runnable() {
                public void run() {
                    if (root == null)
                        pbv.animateAndExecuteMove(move);
                    else
                        setDebugMode(true);
                    updateButtons();
                }
            });
        }

        @Override
        protected DescisionTree getRoot() {
            return root;
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
                        new AlertDialog.Builder(CheckerboardActivity.this).setItems(Utils.toStringArray(Checkers.RobotType.values()), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, final int which) {
                                // one player
                                new AsyncTask<Checkers, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Checkers... params) {
                                        params[0].newGame();
                                        params[0].newSinglePlayerGame(which);
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        updateButtons();
                                        pbv.invalidate();
                                    }
                                }.execute(pbv.getGame());
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                choosePlayers();
                            }
                        }).show();
                        break;
                    case 1: // 2 players
                        pbv.getGame().newGame();
                        pbv.invalidate();
                        updateButtons();
                        break;

                    case 2:
                        loadAsync();
                        break;
                }
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        }).show();

    }

	private File getSaveFile() {
        return new File(getFilesDir(), "checkers.save");
    }

	@Override
    public void onPause() {
        super.onPause();
        if (pbv != null && pbv.getGame() != null)
            pbv.getGame().trySaveToFile(getSaveFile());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void loadAsync() {
        final Dialog d = new ProgressDialog.Builder(this).setCancelable(true).setMessage("Loading...").show();
        new AsyncTask<Checkers, Void, Void>() {

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
            protected Void doInBackground(Checkers... params) {
                try {
                    params[0].loadFromFile(getSaveFile());
                    FileUtils.copyFile(getSaveFile(), Environment.getExternalStorageDirectory());
                } catch (FileNotFoundException e) {

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

        }.execute(pbv.getGame());

    }

	@Override
	protected void onPoll() {
	}

    @Override
    public void onBackPressed() {
        if (pbv.getGame().canUndo()) {
            pbv.undo();
            updateButtons();
        } else {
            super.onBackPressed();
        }
    }

    void updateButtons() {
        bEndTurn.setVisibility(pbv.isEndTurnButtonAvailable() ? View.VISIBLE : View.GONE);
        if (root != null) {
            bUp.setEnabled(root.getParent() != null);
            bLeft.setEnabled(root.getPrev() != null);
            bRight.setEnabled(root.getNext() != null);
            bDown.setEnabled(root.getFirst() != null);
            pbv.highlightMove(root.getMove());
            tvDebug.setText(root.getMeta());
            pbv.setGame(root.game);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonNewGame) {
            pbv.onClick(v);
            choosePlayers();
        } else if (v.getId() == R.id.buttonUp) {
            root = root.getParent();
        } else if (v.getId() == R.id.buttonLeft) {
            root = root.getPrev();
        } else if (v.getId() == R.id.buttonRight) {
            root = root.getNext();
        } else if (v.getId() == R.id.buttonDown) {
            root = root.getFirst();
        }
        updateButtons();
    }
}
