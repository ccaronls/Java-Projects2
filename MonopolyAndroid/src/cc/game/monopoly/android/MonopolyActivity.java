package cc.game.monopoly.android;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.game.Utils;
import cc.lib.monopoly.Card;
import cc.lib.monopoly.Monopoly;
import cc.lib.monopoly.MoveType;
import cc.lib.monopoly.Piece;
import cc.lib.monopoly.Player;
import cc.lib.monopoly.PlayerUser;
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
        protected int getImageId(Piece p) {
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
        protected int getBoardImageId() {
            return R.drawable.board;
        }

        @Override
        protected MoveType showChooseMoveMenu(final Player player, final List<MoveType> moves) {

            final String [] items = Utils.toStringArray(moves);
            final MoveType [] result = new MoveType[1];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    newDialogBuilder().setTitle(player.getPiece() + " CHOOSE MOVE")
                            .setItems(items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    result[0] = moves.get(which);
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
                                monopoly.trySaveToFile(saveFile);
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
        protected Card showChooseCardMenu(final Player player, final List<Card> cards) {
            final String [] items = Utils.toStringArray(cards);
            final Card [] result = new Card[1];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    newDialogBuilder().setTitle(player.getPiece() + " CHOOSE CARD")
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
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AndroidLogger.setLogFile(new File(Environment.getExternalStorageDirectory(), "monopoly.log"));
        int padding = getResources().getDimensionPixelSize(R.dimen.border_padding);
        setMargin(padding);
        saveFile = new File(getFilesDir(),"monopoly.save");
    }

    void copyFileToExt() {
        try {
            FileUtils.copyFile(saveFile, Environment.getExternalStorageDirectory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        monopoly.repaint();
        if (saveFile.exists()) {
            showOptionsMenu();
        } else {
            showNewGameMenu();
        }
    }

    final int PERMISSION_REQUEST_CODE = 1001;

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, R.string.toast_allow_write_external_storage, Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    copyFileToExt();
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MonopolyActivity.this, "Permission Denied, You cannot use local drive .", Toast.LENGTH_LONG);
                        }
                    });
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (monopoly.isGameRunning()) {
            monopoly.stopGameThread();
            monopoly.trySaveToFile(saveFile);
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
                showNewGameMenu();
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
        newDialogBuilder().setTitle("OPTIONS")
                .setItems(new String[]{"New Game", "Resume"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                showNewGameMenu();
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
                                                    showNewGameMenu();
                                                }
                                            }).show();
                                }
                        }
                    }
                }).setNegativeButton("Cancel", null).show();

    }

    void showNewGameMenu() {
        final View v = View.inflate(this, R.layout.new_game_dialog, null);
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

    void showPieceChooser(final Runnable andThen) {
        for (int i=0; i<monopoly.getNumPlayers(); i++) {
            final Player p = monopoly.getPlayer(i);
            if (p instanceof PlayerUser && p.getPiece() == null) {
                final List<Piece> pieces = monopoly.getUnusedPieces();
                String [] items = Utils.toStringArray(pieces);
                newDialogBuilder().setTitle("CHOOSE PIECE " + (i+1)).setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        p.setPiece(pieces.get(which));
                        showPieceChooser(andThen);
                    }
                }).setNegativeButton("Cancel", null).show();
                return;
            }
        }
        andThen.run();
    }

    @Override
    protected int getDialogTheme() {
        return R.style.MonopolyDialogTheme;//android.R.style.Theme_Holo_Dialog_NoActionBar_MinWidth;
    }
}
