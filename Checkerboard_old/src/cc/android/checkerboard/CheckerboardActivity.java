package cc.android.checkerboard;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cc.lib.android.AndroidLogWriter;
import cc.lib.android.CCActivityBase;
import cc.lib.android.SwipeGestureListener;
import cc.lib.checkers.*;
import cc.lib.game.MiniMaxTree;
import cc.lib.game.Utils;
import cc.lib.utils.FileUtils;

public class CheckerboardActivity extends CCActivityBase implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public final static int THINKING_PROGRESS_DELAY = 500;

    private CheckerboardView pbv;
    private View bEndTurn;
    private TextView tvDebug;
    private View bUp, bDown, bLeft, bRight;
    private View bRobot;
    private CompoundButton tbDebug;
    private ViewGroup vgButtons;
    private GestureDetector gesture;

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
        vgButtons = (ViewGroup)findViewById(R.id.vgButtons);
        tvDebug = (TextView)findViewById(R.id.tvDebug);
        tbDebug = (CompoundButton)findViewById(R.id.toggleButtonDebug);
        tbDebug.setOnCheckedChangeListener(this);
        bRobot.setVisibility(View.GONE);
        gesture = new GestureDetector(this, new SwipeGestureListener() {
            @Override
            public void onSwipeLeft() {
                vgButtons.setVisibility(View.VISIBLE);
            }

            @Override
            public void onSwipeRight() {
                vgButtons.setVisibility(View.GONE);
            }
        });
        pbv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gesture.onTouchEvent(event);

                pbv.onTouchEvent(event);

                return true;
            }
        });

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

    class MoveTask extends AsyncTask<Move,Void,Void> {

        MoveTask(ACheckboardGame game) {
            this.game = new WeakReference<>(game);
        }

        final WeakReference<ACheckboardGame> game;

        @Override
        protected void onPostExecute(Void aVoid) {
            pbv.invalidate();
            updateButtons();
            checkForRobotTurn();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            pbv.invalidate();
        }

        @Override
        protected Void doInBackground(Move... moves) {
            game.get().executeMove(moves[0]);
            publishProgress();
            File file = getSaveFile(game.get());
            game.get().trySaveToFile(file);
            try {
                FileUtils.copyFile(file, Environment.getExternalStorageDirectory());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    };

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

        @Override
        public ACheckboardGame deepCopy() {
            Chess c = new Chess();
            c.copyFrom(this);
            return c;
        }

        public void run() {
            dialog.show();
        }

        @Override
        public void onGameOver() {
            // check for a draw game
            pbv.startEndgameAnimation();
        }

        @Override
        public void executeMove(final Move move) {
            if (task == null)
                task = new MoveTask(this).execute(move);
            else {
                super.executeMove(move);
                task = null;
            }
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
        public ACheckboardGame deepCopy() {
            Checkers c = new Checkers();
            c.copyFrom(this);
            return c;
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
            if (task == null)
                task = new MoveTask(this).execute(move);
            else {
                super.executeMove(move);
                task = null;
            }
        }

        @Override
        protected void onPiecesCaptured(List<int[]> pieces) {
            for (int [] pos : pieces) {
                pbv.startCapturedAnimation(pos, getPiece(pos), null);
            }
            pbv.invalidate();

        }

        @Override
        public void endTurn() {
            super.endTurn();
            checkForRobotTurn();
        }
    }

    public class MyDraughts extends Draughts implements Runnable, DialogInterface.OnCancelListener {
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
        public ACheckboardGame deepCopy() {
            Draughts c = new Draughts();
            c.copyFrom(this);
            return c;
        }

        @Override
        public void onGameOver() {
            if (robot != null && robot.type.ordinal() > 0 && pbv.getGame().getTurn() == ROBOT_PLAYER_NUM) {
                getPrefs().edit().putBoolean("wondraughts", true).apply();
            }
            pbv.post(new Runnable() {
                public void run() {
                    showWinnerDialog();
                }
            });
        }

        @Override
        public void executeMove(final Move move) {
            if (task == null)
                task = new MoveTask(this).execute(move);
            else {
                super.executeMove(move);
                task = null;
            }
        }

        @Override
        public void endTurn() {
            super.endTurn();
            checkForRobotTurn();
        }

        @Override
        protected void onPiecesCaptured(final List<int[]> pieces) {
            if (pieces.size() > 0) {
                final Object lock = new Object();
                Runnable whenDone = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                };


                for (int i=0; i<pieces.size(); i++) {
                    int [] pos= pieces.get(i);
                    Piece p = getPiece(pos);
                    pbv.startCapturedAnimation(pos, p, whenDone);
                    whenDone = null;
                }

                synchronized (lock) {
                    try {
                        lock.wait(2000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void onPieceUncaptured(int[] pos, PieceType type) {
            //super.onPieceUncaptured(pos, type);
            //pbv.startCapturedAnimation(pos);
        }
    }

    public class MyDama extends Dama implements Runnable, DialogInterface.OnCancelListener {
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
        public ACheckboardGame deepCopy() {
            Dama c = new Dama();
            c.copyFrom(this);
            return c;
        }


        @Override
        public void onGameOver() {
            if (robot != null && robot.type.ordinal() > 0 && pbv.getGame().getTurn() == ROBOT_PLAYER_NUM) {
                getPrefs().edit().putBoolean("wondraughts", true).apply();
            }
            pbv.post(new Runnable() {
                public void run() {
                    showWinnerDialog();
                }
            });
        }

        @Override
        public void executeMove(final Move move) {
            if (task == null)
                task = new MoveTask(this).execute(move);
            else {
                super.executeMove(move);
                task = null;
            }
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

    class RobotTask extends AsyncTask<ACheckboardGame,Move,Void> implements DialogInterface.OnCancelListener, Runnable {

        boolean debugMode = tbDebug.isChecked();

        @Override
        protected void onPreExecute() {
            if (thinking== null) {
                thinking = new AlertDialog.Builder(CheckerboardActivity.this, R.style.DialogWithTransparentBackground)
                        .setTitle("Thinking")
                        .setView(new ProgressBar(CheckerboardActivity.this))
                        .setOnCancelListener(this)
                        .create();
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
            super.onCancelled(result);
            pbv.removeCallbacks(this);
            thinking.dismiss();
            for (Move m : getPath()) {
                pbv.getGame().executeMove(m);
            }
            pbv.invalidate();
        }

        @Override
        protected void onProgressUpdate(Move... moves) {
            pbv.removeCallbacks(this);
            thinking.dismiss();
            if (debugMode) {
                updateButtons();
            } else {
                pbv.animateMoveAndThen(moves[0], new Runnable() {
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
            ACheckboardGame game = pbvGame.deepCopy();
            root = new MMTreeNode(game);
            robot.doRobot(game, root);
            if (debugMode) {
                root.dumpTree(new AndroidLogWriter("Robot"));
                publishProgress(root.getMove());
            } else if (game.computeMoves() > 0) {
                for (Move m : getPath()) {
                    publishProgress(m);
                    Utils.waitNoThrow(this, 10000);
                    game.executeMove(m);
                    pbvGame.copyFrom(game);
                    pbv.postInvalidate();
                    if (pbvGame.getTurn() == 0) {
                        break;
                    }
                }
            }
            return null;
        }

        List<Move> getPath() {
            // full search the tree to find highest value child
            MMTreeNode<Move,ACheckboardGame> ch = root.findDominantChild();
            LinkedList<Move> path  = new LinkedList<>();
            while (ch != root) {
                path.addFirst(ch.getMove());
                ch = ch.getParent();
            }
            return path;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            cancel(true);
        }
    }

    Dialog winnerDialog;

    void showWinnerDialog() {
        if (winnerDialog != null && winnerDialog.isShowing())
            return;

        winnerDialog = newDialogBuilder(true).setMessage(pbv.getGame().getPlayerColor(pbv.getGame().getTurn()).name() + " Lost")
                .setPositiveButton("Play again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showChoosePlayersDialog(pbv.getGame());
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
        newDialogBuilder(true).setItems(Utils.toStringArray(Robot.RobotType.values()), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                robot = new Robot(which);
                updateButtons();
                checkForRobotTurn();
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                showChoosePlayersDialog(pbv.getGame());
            }
        }).show();
    }

    private AlertDialog.Builder newDialogBuilder(boolean cancelable) {
        return new AlertDialog.Builder(this, R.style.DialogTheme).setCancelable(cancelable);
    }

    void showChooseGameDialog() {
        String [] items = {"Checkers", "Chess", "Draughts", "Dama" };
        newDialogBuilder(false).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        showChoosePlayersDialog(new MyCheckers());
                        break;
                    case 1:
                        showChoosePlayersDialog(new MyChess());
                        break;
                    case 2:
                        showChoosePlayersDialog(new MyDraughts());
                        break;
                    case 3:
                        showChoosePlayersDialog(new MyDama());
                        break;
                }
            }
        }).show();
    }

    void showChooseChessVersion(final Chess chess) {
        String [] items = { "No Timer", "30 minute", "15 minute", "Speed Chess (5 min)"};
        newDialogBuilder(true).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                switch (which) {
                    case 1: // 30 min
                        chess.setTimer(30 * 60);
                        break;
                    case 2: // 15 min
                        chess.setTimer(15 * 60);
                        break;
                    case 3: // 5 min
                        chess.setTimer(5 * 60);
                        break;
                    case 0: // No Timer
                    default:
                }
                startGame(chess);
            }
        }).show();
    }


    void showChoosePlayersDialog(final ACheckboardGame game) {
        tbDebug.setVisibility(View.GONE);
        ArrayList<String> list = new ArrayList<>();
        list.add("One Player");
        list.add("Two Players");
        if (getSaveFile(game).exists())
            list.add("Resume");
        newDialogBuilder(true).setItems(list.toArray(new String[list.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: {
                        tbDebug.setVisibility(View.VISIBLE);
                        newDialogBuilder(true).setItems(Utils.toStringArray(Robot.RobotType.values()), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, final int which) {
                                game.newGame();
                                startGame(game);
                                robot = new Robot(which);
                                updateButtons();
                                checkForRobotTurn();
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                showChoosePlayersDialog(game);
                            }
                        }).show();
                        break;
                    }
                    case 1: {// 2 players
                        robot = null;
                        game.newGame();
                        updateButtons();
                        if (game instanceof Chess) {
                            showChooseChessVersion((Chess)game);
                        } else {
                            startGame(game);
                        }
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
                //finish();
            }
        }).show();

    }

	public File getSaveFile(ACheckboardGame game) {
        return new File(getFilesDir(), game.getClass().getSimpleName() + ".save");
    }

	@Override
    public void onPause() {
        super.onPause();
        if (pbv != null && pbv.getGame() != null)
            pbv.getGame().trySaveToFile(getSaveFile(pbv.getGame()));
    }

    @Override
    public void onResume() {
        super.onResume();
        vgButtons.setVisibility(View.VISIBLE);
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
                    newDialogBuilder(true)
                            .setTitle("Error")
                            .setMessage(e.getClass().getSimpleName() + " : " + e.getMessage())
                            .setNegativeButton("Ok", null)
                            .show();
                } else {
                    startGame(game);
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
        if (pbv.getGame() != null && pbv.getGame().canUndo()) {
            pbv.undo();
            updateButtons();
        } else {
            super.onBackPressed();
        }
    }

    private static final int ROBOT_PLAYER_NUM = 1;

    void setEndTurnButton(Move m) {
        if (m == null) {
            bEndTurn.setEnabled(false);
            bEndTurn.setTag(null);
            if (!tbDebug.isChecked())
                vgButtons.setVisibility(View.GONE);
        } else {
            bEndTurn.setEnabled(true);
            bEndTurn.setTag(m);
            vgButtons.setVisibility(View.VISIBLE);
        }
    }

    void updateButtons() {
        setEndTurnButton(null);
        if (robot != null && pbv.getGame().getTurn() == ROBOT_PLAYER_NUM)
        {} else if (pbv.getGame()!=null) {
            Piece lock = pbv.getGame().getLocked();
            if (lock != null) {
                if (lock.getPlayerNum() != pbv.getGame().getTurn())
                    throw new cc.lib.utils.GException();
                if (lock.getType() != PieceType.PAWN_TOSWAP) {
                    for (Move m : lock.getMoves()) {
                        switch (m.getMoveType()) {
                            case SWAP:
                                // TODO What????
                                if (m.getEndType()== lock.getType()) {
                                    setEndTurnButton(m);
                                }
                                break;
                            case END:
                                setEndTurnButton(m);
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

    void startGame(ACheckboardGame game) {
        pbv.setGame(game);
        vgButtons.setVisibility(View.GONE);
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.buttonNewGame) {
            if (pbv.getGame()==null)
                showChooseGameDialog();
            else newDialogBuilder(true).setMessage("Start a " + pbv.getGame().getClass().getSimpleName() + " game?")
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
                            showChoosePlayersDialog(pbv.getGame());
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
                throw new cc.lib.utils.GException();
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

    public boolean isMultiPlayer() {
        return robot == null;
    }
}
