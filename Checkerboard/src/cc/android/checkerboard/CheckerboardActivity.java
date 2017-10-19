package cc.android.checkerboard;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import cc.lib.android.AndroidLogWriter;
import cc.lib.android.CCActivityBase;
import cc.lib.game.Utils;
import cc.lib.utils.FileUtils;
import cc.lib.game.MiniMaxTree.*;

public class CheckerboardActivity extends CCActivityBase implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public final static int THINKING_PROGRESS_DELAY = 500;

	private CheckerboardView pbv;
    private View bEndTurn;
    private MMTreeNode<Move, ACheckboardGame> root = null;
    private TextView tvDebug;
    private View bUp, bDown, bLeft, bRight;
    private View bRobot;
    private CompoundButton tbDebug;
    private Robot robot = null;

    @Override
    protected void onCreate(Bundle bundle) {
        Utils.setDebugEnabled(BuildConfig.DEBUG);
        super.onCreate(bundle);
        setContentView(R.layout.cb_activity);
        pbv = (CheckerboardView)findViewById(R.id.cbView);
        pbv.setOnClickListener(this);
        tvDebug = (TextView)findViewById(R.id.tvDebug);
        findViewById(R.id.buttonNewGame).setOnClickListener(this);
        (bEndTurn = findViewById(R.id.buttonEndTurn)).setOnClickListener(this);
        (bUp = findViewById(R.id.buttonUp)).setOnClickListener(this);
        (bLeft = findViewById(R.id.buttonLeft)).setOnClickListener(this);
        (bRight = findViewById(R.id.buttonRight)).setOnClickListener(this);
        (bDown = findViewById(R.id.buttonDown)).setOnClickListener(this);
        (bRobot = findViewById(R.id.buttonRobot)).setOnClickListener(this);
        tvDebug = (TextView)findViewById(R.id.tvDebug);
        tbDebug = (CompoundButton)findViewById(R.id.toggleButtonDebug);
        tbDebug.setOnCheckedChangeListener(this);
        bRobot.setVisibility(View.GONE);

        bEndTurn.setEnabled(false);
        setDebugMode(false);
        showChooseGameDialog();
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

    public class MyChess extends Chess implements Runnable, DialogInterface.OnCancelListener {
        private Dialog dialog = null;
        private AsyncTask task = null;

        @Override
        public void onCancel(DialogInterface dialog) {
            if (task != null) {
                task.cancel(true);
                //undo();
                if (root != null && root.getFirst() != null) {
                    pbv.animateAndExecuteMove(root.getFirst().getMove());
                }
            }
        }

        public void run() {
            dialog.show();
        }

        @Override
        public void onGameOver() {
            pbv.post(new Runnable() {
                public void run() {
                    showWinnerDialog();
                }
            });
        }

        @Override
        public void executeMove(final Move move) {
            new AsyncTask<ACheckboardGame,Void,Void>() {
                @Override
                protected void onPostExecute(Void aVoid) {
                    pbv.invalidate();
                    updateButtons();
                    checkForRobotTurn();
                }

                @Override
                protected Void doInBackground(ACheckboardGame... params) {
                    MyChess.super.executeMove(move);
                    File file = getSaveFile(params[0]);
                    params[0].trySaveToFile(file);
                    try {
                        FileUtils.copyFile(file, Environment.getExternalStorageDirectory());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute(pbv.getGame());
        }

    }


    public class MyCheckers extends Checkers implements Runnable, DialogInterface.OnCancelListener {
        private Dialog dialog = null;
        private AsyncTask task = null;

        @Override
        public void onCancel(DialogInterface dialog) {
            if (task != null) {
                task.cancel(true);
                //undo();
                if (root != null && root.getFirst() != null) {
                    pbv.animateAndExecuteMove(root.getFirst().getMove());
                }
            }
        }

        public void run() {
            dialog.show();
        }

        @Override
        public void onGameOver() {
            if (robot != null && robot.type.ordinal() > 0 && pbv.getGame().getTurn() == ROBOT_PLAYER_NUM) {
                getPrefs().edit().putBoolean("woncheckers", true).apply();
            }
            pbv.post(new Runnable() {
                public void run() {
                    showWinnerDialog();
                }
            });
        }

        @Override
        public void executeMove(final Move move) {
            new AsyncTask<ACheckboardGame,Void,Void>() {
                @Override
                protected void onPostExecute(Void aVoid) {
                    pbv.invalidate();
                    updateButtons();
                    checkForRobotTurn();
                }

                @Override
                protected Void doInBackground(ACheckboardGame... params) {
                    MyCheckers.super.executeMove(move);
                    File file = getSaveFile(params[0]);
                    params[0].trySaveToFile(file);
                    try {
                        FileUtils.copyFile(file, Environment.getExternalStorageDirectory());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute(pbv.getGame());
        }

        @Override
        public void endTurn() {
            super.endTurn();
            checkForRobotTurn();
        }
    }

    Dialog thinking = null;

    void checkForRobotTurn() {
        if (robot != null && pbv.getGame().getTurn() == ROBOT_PLAYER_NUM) {
            new RobotTask().execute(pbv.getGame());
        }
    }

    class RobotTask extends AsyncTask<ACheckboardGame,MMTreeNode<Move,ACheckboardGame>,Void> implements DialogInterface.OnCancelListener, Runnable {

        boolean debugMode = tbDebug.isChecked();

        @Override
        protected void onPreExecute() {
            if (thinking== null) {
                thinking = new AlertDialog.Builder(CheckerboardActivity.this).setTitle("Thinking").setView(new ProgressBar(CheckerboardActivity.this)).setOnCancelListener(this).create();
            }
            pbv.postDelayed(this, 500);
        }

        public void run() {
            thinking.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            pbv.removeCallbacks(this);
            thinking.dismiss();
            if (!debugMode)
                root = null;
            if (pbv.getGame().computeMoves() == 0)
                pbv.getGame().onGameOver();
            pbv.invalidate();
        }

        @Override
        protected void onCancelled(Void result) {
            super.onCancelled();
            pbv.removeCallbacks(this);
            thinking.dismiss();
            root = root.getRoot(); // make sure at top of tree
            for (MMTreeNode t : getPath()) {
                pbv.getGame().executeMove(root.getMove());
            }
            pbv.invalidate();
        }

        @Override
        protected void onProgressUpdate(MMTreeNode<Move, ACheckboardGame>... root) {
            pbv.removeCallbacks(this);
            thinking.dismiss();
            Move move = root[0].getMove();
            if (debugMode) {
                updateButtons();
            } else {
                pbv.animateMoveAndThen(move, new Runnable() {
                    public void run() {
                        synchronized (RobotTask.this) {
                            RobotTask.this.notifyAll();
                        }
                    }
                });
            }
        }

        @Override
        protected Void doInBackground(ACheckboardGame... params) {
            ACheckboardGame pbvGame = params[0];
            ACheckboardGame game = (pbvGame instanceof Checkers) ? new Checkers() : new Chess();
            game.copyFrom(pbvGame);
            root = new MMTreeNode(game);
            robot.doRobot(game, root);
            if (debugMode) {
                root.dumpTree(new AndroidLogWriter("Robot"));
                publishProgress(root);
            } else if (game.computeMoves() > 0) {
                for (MMTreeNode<Move,ACheckboardGame> p : getPath()) {
                    publishProgress(p);
                    synchronized (this) {
                        try {
                            wait(10000);
                        } catch (Exception e) {
                        }
                    }
                    game.executeMove(p.getMove());
                    pbvGame.copyFrom(game);
                    pbv.postInvalidate();
                }
            }
            return null;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            cancel(true);
        }
    }

    List<MMTreeNode<Move,ACheckboardGame>> getPath() {
        List<MMTreeNode<Move,ACheckboardGame>> list = new ArrayList<>();
        if (root.getFirst()==null) {
            if (root.getMove() != null)
                list.add(root);
        } else {
            int playerNum = root.getFirst().getMove().playerNum;
            for (MMTreeNode<Move,ACheckboardGame> t = root.getFirst(); t != null; t = t.getFirst()) {
                if (t.getMove().getPlayerNum() != playerNum)
                    break;
                list.add(t);
            }
        }
        return list;
    }

    Dialog winnerDialog;

    void showWinnerDialog() {
        if (winnerDialog != null && winnerDialog.isShowing())
            return;

        winnerDialog = newDialogBuilder().setMessage(pbv.getGame().getPlayerColor(pbv.getGame().getTurn()).name() + " Lost")
                .setPositiveButton("Play again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showChoosePlayersDialog(true, pbv.getGame());
                    }
                })
                .setNeutralButton("Home Menu", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        showChooseGameDialog();
                    }
                }).setCancelable(false).show();
    }

    void showDifficultyDialog() {
        newDialogBuilder().setItems(Utils.toStringArray(Robot.RobotType.values()), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                robot = new Robot(which);
                updateButtons();
                checkForRobotTurn();
            }
        }).show();
    }

    private AlertDialog.Builder newDialogBuilder() {
        return new AlertDialog.Builder(this, R.style.DialogTheme).setCancelable(false);
    }

    void showChooseGameDialog() {
        String [] items = {"Checkers", "Chess" };
        newDialogBuilder().setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        showChoosePlayersDialog(false, new MyCheckers());
                        break;
                    case 1:
                        showChoosePlayersDialog(false, new MyChess());
                        break;
                }
            }
        }).show();
    }

    void showChoosePlayersDialog(final  boolean finishOnCancel, final ACheckboardGame game) {
        tbDebug.setVisibility(View.GONE);
        ArrayList<String> list = new ArrayList<>();
        list.add("One Player");
        list.add("Two Players");
        if (getSaveFile(game).exists())
            list.add("Resume");
        newDialogBuilder().setItems(list.toArray(new String[list.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: {
                        tbDebug.setVisibility(View.VISIBLE);
                        newDialogBuilder().setItems(Utils.toStringArray(Robot.RobotType.values()), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, final int which) {
                                game.newGame();
                                pbv.setGame(game);
                                pbv.invalidate();
                                robot = new Robot(which);
                                updateButtons();
                                checkForRobotTurn();
                            }
                        }).setOnCancelListener(!finishOnCancel ? null : new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                showChoosePlayersDialog(finishOnCancel, game);
                            }
                        }).show();
                        break;
                    }
                    case 1: {// 2 players
                        robot = null;
                        game.newGame();
                        pbv.setGame(game);
                        pbv.invalidate();
                        updateButtons();
                        break;
                    }
                    case 2:
                        loadAsync(game);
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

	private File getSaveFile(ACheckboardGame game) {
        if (game instanceof Checkers)
            return new File(getFilesDir(), SAVEFILE_CHECKERS);
        return new File(getFilesDir(), SAVEFILE_CHESS);
    }

    public final String SAVEFILE_CHECKERS = "checkers.save";
    public final String SAVEFILE_CHESS    = "chess.save";



	@Override
    public void onPause() {
        super.onPause();
        if (pbv != null && pbv.getGame() != null)
            pbv.getGame().trySaveToFile(getSaveFile(pbv.getGame()));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void loadAsync(final ACheckboardGame game) {
        final Dialog d = new ProgressDialog.Builder(this).setCancelable(true).setMessage("Loading...").show();
        new AsyncTask<Void, Void, Exception>() {

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
            protected void onPostExecute(Exception e) {
                d.dismiss();
                if (e != null) {
                    newDialogBuilder()
                            .setTitle("Error")
                            .setMessage(e.getClass().getSimpleName() + " : " + e.getMessage())
                            .setNegativeButton("Ok", null)
                            .show();
                } else {
                    pbv.setGame(game);
                }
                robot = null; //  TODO: Save robot or difficulty on the checkers object
                pbv.invalidate();
                updateButtons();
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
            }

            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    File file = getSaveFile(game);
                    FileUtils.copyFile(file, Environment.getExternalStorageDirectory());
                    game.loadFromFile(file);
                } catch (FileNotFoundException e) {
                    return e;
                } catch (Exception e) {
                    e.printStackTrace();
                    return e;
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
        if (pbv.getGame().canUndo()) {
            pbv.undo();
            updateButtons();
        } else {
            super.onBackPressed();
        }
    }

    private static final int ROBOT_PLAYER_NUM = 1;

    void updateButtons() {
        bEndTurn.setEnabled(false);
        bEndTurn.setTag(null);
        if (robot != null && pbv.getGame().getTurn() == ROBOT_PLAYER_NUM)
        {} else if (pbv.getGame()!=null) {
            Piece lock = pbv.getGame().getLocked();
            if (lock != null) {
                if (lock.playerNum != pbv.getGame().getTurn())
                    throw new AssertionError();
                if (lock.type != PieceType.PAWN_TOSWAP) {
                    for (Move m : lock.moves) {
                        switch (m.type) {
                            case SWAP:
                                if (m.nextType == lock.type) {
                                    bEndTurn.setTag(m);
                                    bEndTurn.setEnabled(true);
                                }
                                break;
                            case END:
                                bEndTurn.setTag(m);
                                bEndTurn.setEnabled(true);
                                break;
                        }
                    }
                }
            }
        }
        if (root != null) {
            bUp.setEnabled(root.getParent() != null);
            bLeft.setEnabled(root.getPrev() != null);
            bRight.setEnabled(root.getNext() != null);
            bDown.setEnabled(root.getFirst() != null);
            pbv.highlightMove(root.getMove());
            tvDebug.setText(root.getMeta());
        }
        bRobot.setVisibility(robot == null ? View.VISIBLE :View.GONE);
        tbDebug.setVisibility(robot == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.buttonNewGame) {
            if (pbv.getGame()==null)
                showChooseGameDialog();
            else newDialogBuilder().setMessage("Start a " + pbv.getGame().getClass().getSimpleName() + " game?")
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Home Menu", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pbv.setGame(null);
                            showChooseGameDialog();
                        }
                    }).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showChoosePlayersDialog(false, pbv.getGame());
                        }
                    }).show();

        } else if (v.getId() == R.id.buttonUp) {
            root = root.getParent();
            root.getGame().undo();
            pbv.getGame().copyFrom(root.getGame());
        } else if (v.getId() == R.id.buttonLeft) {
            root.getGame().undo();
            root = root.getPrev();
            root.getGame().executeMove(root.getMove());
            pbv.getGame().copyFrom(root.getGame());
        } else if (v.getId() == R.id.buttonRight) {
            root.getGame().undo();
            root = root.getNext();
            root.getGame().executeMove(root.getMove());
            pbv.getGame().copyFrom(root.getGame());
        } else if (v.getId() == R.id.buttonDown) {
            root = root.getFirst();
            root.getGame().executeMove(root.getMove());
            pbv.getGame().copyFrom(root.getGame());
        } else if (v.getId() == R.id.buttonRobot) {
            showDifficultyDialog();
        } else if (v.getId() == R.id.buttonEndTurn) {
            Move move = (Move)v.getTag();
            if (move == null)
                throw new AssertionError();
            pbv.getGame().executeMove(move);
        }
        updateButtons();
        pbv.invalidate();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setDebugMode(isChecked);
        root = null;
        checkForRobotTurn();
    }
}
