package cc.game.monopoly.android;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.NumberPicker;

import java.io.File;
import java.util.List;

import cc.lib.android.CCNumberPicker;
import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.game.Utils;
import cc.lib.monopoly.Card;
import cc.lib.monopoly.Monopoly;
import cc.lib.monopoly.MoveType;
import cc.lib.monopoly.Piece;
import cc.lib.monopoly.Player;
import cc.lib.monopoly.PlayerUser;
import cc.lib.monopoly.Rules;
import cc.lib.monopoly.UIMonopoly;
import cc.lib.utils.FileUtils;

/**
 * Created by chriscaron on 2/15/18.
 */

public class MonopolyActivity extends DroidActivity {

    private final static String TAG = MonopolyActivity.class.getSimpleName();

    private File saveFile=null;

    private UIMonopoly monopoly = new UIMonopoly() {
        @Override
        public void repaint() {
            getContent().postInvalidate();
        }

        @Override
        public void runGame() {
            monopoly.trySaveToFile(saveFile);
            super.runGame();
        }

        @Override
        public int getImageId(Piece p) {
            switch (p) {

                case CAR:
                    return R.drawable.car;
                case BOAT:
                    return R.drawable.ship;
                case HAT:
                    return R.drawable.tophat;
                case DOG:
                    return R.drawable.dog;
                case THIMBLE:
                    return R.drawable.thimble;
                case SHOE:
                    return R.drawable.shoe;
                case WHEELBARROW:
                    return R.drawable.wheelbarrow;
                case IRON:
                    return R.drawable.iron;
            }
            return 0;
        }

        @Override
        public int getBoardImageId() {
            return R.drawable.board;
        }

        @Override
        protected MoveType showChooseMoveMenu(final Player player, final List<MoveType> moves) {

            final String [] items = Utils.toStringArray(moves, true);
            final MoveType [] result = new MoveType[1];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    newDialogBuilder().setTitle(player.getPiece() + " Choose Move ")
                            .setItems(items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    result[0] = moves.get(which);
                                    synchronized (monopoly) {
                                        monopoly.notify();
                                    }
                                }
                            }).setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (monopoly.canCancel()) {
                                monopoly.cancel();
                            } else {
                                monopoly.stopGameThread();
                            }
                            synchronized (monopoly) {
                                monopoly.notify();
                            }
                        }
                    }).show();
                }
            });
            Utils.waitNoThrow(monopoly, -1);
            return result[0];
        }

        @Override
        protected Card showChooseCardMenu(final Player player, final List<Card> cards, final Player.CardChoiceType type) {
            final String [] items = Utils.toStringArray(cards, true);
            final Card [] result = new Card[1];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    newDialogBuilder().setTitle(player.getPiece() + Utils.getPrettyString(type.name()))
                            .setItems(items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    result[0] = cards.get(which);
                                    synchronized (monopoly) {
                                        monopoly.notify();
                                    }
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (monopoly.canCancel()) {
                                monopoly.cancel();
                            } else {
                                monopoly.stopGameThread();
                            }
                            synchronized (monopoly) {
                                monopoly.notify();
                            }
                        }
                    }).show();
                }
            });
            Utils.waitNoThrow(monopoly, -1);
            return result[0];
        }

        @Override
        protected void onError(Throwable t) {
            FileUtils.tryCopyFile(saveFile, new File(Environment.getExternalStorageDirectory(), "monopoly_error.txt"));
            newDialogBuilder().setTitle("ERROR").setMessage(t.toString()).setNegativeButton("Ok", null).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AndroidLogger.setLogFile(new File(Environment.getExternalStorageDirectory(), "monopoly.log"));
        saveFile = new File(getFilesDir(),"monopoly.save");
    }

    @Override
    protected void onResume() {
        super.onResume();
        monopoly.repaint();
        if (saveFile.exists()) {
            showOptionsMenu();
        } else {
            showPlayerSetupMenu();
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (monopoly.isGameRunning()) {
            monopoly.stopGameThread();
        }
    }

    int tx=-1, ty=-1;
    boolean dragging = false;

    @Override
    protected void onDraw(DroidGraphics g) {
        synchronized (this) {
            monopoly.paint(g, tx, ty);
        }
    }

    @Override
    protected void onTouchDown(float x, float y) {
        tx = Math.round(x);
        ty = Math.round(y);
        getContent().postInvalidate();
    }

    @Override
    protected void onTouchUp(float x, float y) {
        if (dragging) {
            monopoly.stopDrag();
            dragging = false;
        }
        tx = -1;//Math.round(x);
        ty = -1;//Math.round(y);
        getContent().postInvalidate();
    }

    @Override
    protected void onDrag(float x, float y) {
        if (!dragging) {
            monopoly.startDrag();
            dragging = true;
        }
        tx = Math.round(x);
        ty = Math.round(y);
        getContent().postInvalidate();
    }

    @Override
    protected void onTap(float x, float y) {
        tx = Math.round(x);
        ty = Math.round(y);
        /*
        getContent().postInvalidate();
        getContent().postDelayed(new Runnable() {
            public void run() {
                tx = ty = -1;
                monopoly.onClick();
            }
        }, 100);*/
        if (!monopoly.isGameRunning()) {
            if (saveFile.exists()) {
                showOptionsMenu();
            } else {
                showPlayerSetupMenu();
            }
        } else {
            for (int i=0; i<monopoly.getNumPlayers(); i++) {
                if (monopoly.getPlayer(i) instanceof PlayerUser)
                    return;
            }
            monopoly.stopGameThread();
        }
    }

    void showOptionsMenu() {
        File fixed = new File(Environment.getExternalStorageDirectory(), "monopoly_fixed.txt");
        if (fixed.exists()) {
            FileUtils.tryCopyFile(fixed, saveFile);
            fixed.delete();
        }

        newDialogBuilder().setTitle("OPTIONS")
                .setItems(new String[]{"New Game", "Resume"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                showGameSetupDialog();
                                break;
                            case 1:
                                if (monopoly.tryLoadFromFile(saveFile)) {
                                    monopoly.startGameThread();
                                } else {
                                    saveFile.delete();
                                    newDialogBuilder().setTitle("ERROR").setMessage("Unable to load from save game.")
                                            .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    showGameSetupDialog();
                                                }
                                            }).show();
                                }
                        }
                    }
                }).setNegativeButton("Cancel", null).show();

    }

    void showPlayerSetupMenu() {
        final View v = View.inflate(this, R.layout.players_setup_dialog, null);
        final NumberPicker realPlayers = (NumberPicker)v.findViewById(R.id.npLivePlayers);
        final NumberPicker compPlayers = (NumberPicker)v.findViewById(R.id.npCompPlayers);

        realPlayers.setMinValue(0);
        realPlayers.setMaxValue(Monopoly.MAX_PLAYERS);
        realPlayers.setValue(1);
        compPlayers.setMinValue(0);
        compPlayers.setMaxValue(Monopoly.MAX_PLAYERS);
        compPlayers.setValue(1);

        realPlayers.setOnValueChangedListener(
            new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    int total = newVal + compPlayers.getValue();
                    if (total > Monopoly.MAX_PLAYERS) {
                        compPlayers.setValue(compPlayers.getValue()-1);
                    } else if (total < 2) {
                        compPlayers.setValue(compPlayers.getValue()+1);
                    }
                }
            });

        compPlayers.setOnValueChangedListener(
            new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    int total = newVal + realPlayers.getValue();
                    if (total > Monopoly.MAX_PLAYERS) {
                        realPlayers.setValue(realPlayers.getValue()-1);
                    } else if (total < 2) {
                        realPlayers.setValue(realPlayers.getValue()+1);
                    }
                }
            });

        newDialogBuilder().setTitle("NEW GAME").setView(v).setNegativeButton("Cancel", null)
            .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    monopoly.clear();
                    for (int i=0; i<realPlayers.getValue(); i++)
                        monopoly.addPlayer(new PlayerUser());
                    for (int i=0; i<compPlayers.getValue(); i++)
                        monopoly.addPlayer(new Player());
                    showPieceChooser(new Runnable() {
                        @Override
                        public void run() {
                            monopoly.startGameThread();
                        }
                    });
                }
            }).show();
    }

    void showGameSetupDialog() {
        final int [] startMoneyValues = getResources().getIntArray(R.array.start_money_values);
        final View v = View.inflate(this, R.layout.game_config_dialog, null);
        final CCNumberPicker npStartMoney = (CCNumberPicker)v.findViewById(R.id.npStartMoney);
        final CompoundButton cbJailBumpEnabled = (CompoundButton)v.findViewById(R.id.cbJailBumpEnabled);
        final Rules rules = monopoly.getRules();
        npStartMoney.init(startMoneyValues, rules.startMoney, null);
        cbJailBumpEnabled.setChecked(rules.jailBumpEnabled);
        newDialogBuilder().setTitle("Configure").setView(v).setNegativeButton("Cancel", null)
                .setPositiveButton("Setup Players", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        rules.startMoney = startMoneyValues[npStartMoney.getValue()];
                        rules.jailBumpEnabled = cbJailBumpEnabled.isChecked();
                        showPlayerSetupMenu();
                    }
                }).show();
    }

    void showPieceChooser(final Runnable andThen) {
        for (int i=0; i<monopoly.getNumPlayers(); i++) {
            final Player p = monopoly.getPlayer(i);
            if (p instanceof PlayerUser && p.getPiece() == null) {
                View.OnClickListener listener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismissCurrentDialog();
                        p.setPiece((Piece)v.getTag());
                        showPieceChooser(andThen);
                    }
                };
                final List<Piece> pieces = monopoly.getUnusedPieces();
                GridLayout gl = new GridLayout(MonopolyActivity.this);
                gl.setColumnCount(4);
                gl.setRowCount(2);
                for (Piece pc : pieces) {
                    ImageButton iv = new ImageButton(MonopolyActivity.this);
                    iv.setTag(pc);
                    iv.setImageResource(monopoly.getImageId(pc));
                    gl.addView(iv);
                    iv.setOnClickListener(listener);
                }

                String [] items = Utils.toStringArray(pieces, true);
                newDialogBuilder().setTitle("CHOOSE PIECE " + (i+1))
                    .setView(gl)
                        /*.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        p.setPiece(pieces.get(which));
                        showPieceChooser(andThen);
                    }
                })*/.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        monopoly.clear();
                    }
                }).show();
                return;
            }
        }
        andThen.run();
    }

    @Override
    protected int getDialogTheme() {
        return R.style.MonopolyDialogTheme;
    }

    @Override
    protected void onDialogShown(Dialog d) {

        // Creating Dynamic
        Rect displayRectangle = new Rect();

        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        float DIM = Math.min(displayRectangle.width(), displayRectangle.height());
        d.getWindow().setLayout((int) (DIM/2), d.getWindow().getAttributes().height);

        //d.getWindow().setLayout(getResources().getDimension(R.dimen.dialog_width), getResources().getDimension(R.dimen.dialog_height));
        //d.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }
}
