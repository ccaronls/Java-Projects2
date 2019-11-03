package cc.game.monopoly.android;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import cc.lib.android.CCNumberPicker;
import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.android.DroidView;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.monopoly.Card;
import cc.lib.monopoly.Monopoly;
import cc.lib.monopoly.MoveType;
import cc.lib.monopoly.Piece;
import cc.lib.monopoly.Player;
import cc.lib.monopoly.PlayerUser;
import cc.lib.monopoly.Rules;
import cc.lib.monopoly.Square;
import cc.lib.monopoly.Trade;
import cc.lib.monopoly.UIMonopoly;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 2/15/18.
 */

public class MonopolyActivity extends DroidActivity {

    private final static String TAG = MonopolyActivity.class.getSimpleName();

    private File saveFile=null;
    private File rulesFile=null;

    private UIMonopoly monopoly = new UIMonopoly() {
        @Override
        public void repaint() {
            getContent().postInvalidate();
        }

        @Override
        public void runGame() {
            monopoly.trySaveToFile(saveFile);
            if (BuildConfig.DEBUG) {
                checkPermissionAndThen(() -> {
                    try {
                        FileUtils.copyFile(saveFile, Environment.getExternalStorageDirectory());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
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

            final String [] items = new String[moves.size()];
            int index=0;
            for (MoveType mt : moves) {
                switch (mt) {
                    case PURCHASE: {
                        Square sq = player.getSquare();
                        items[index++] = "Purchase " + Utils.getPrettyString(sq.name()) + " for $" + sq.getPrice();
                        break;
                    }
                    case PURCHASE_UNBOUGHT: {
                        Square sq = getPurchasePropertySquare();
                        items[index++] = "Purchase " + Utils.getPrettyString(sq.name()) + " for $" + sq.getPrice();
                        break;
                    }
                    case PAY_BOND: {
                        items[index++] = String.format("%s $%d", Utils.getPrettyString(mt.name()), player.getJailBond());
                        break;
                    }
                    default:
                        items[index++] = Utils.getPrettyString(mt.name());
                        break;
                }
            }
            final MoveType [] result = new MoveType[1];
            runOnUiThread(() ->{
                    newDialogBuilderINGAME().setTitle(Utils.getPrettyString(player.getPiece().name()) + " Choose Move ")
                            .setItems(items, (DialogInterface dialog, int which) -> {
                                    final MoveType mt = moves.get(which);
                                    switch (mt) {
                                        case PURCHASE_UNBOUGHT: {
                                            Square property = getPurchasePropertySquare();
                                            showPurchasePropertyDialog("Buy Property", "Buy", property,  () -> { result[0] = mt; });
                                            break;
                                        }
                                        case PURCHASE:
                                            Square property = player.getSquare();
                                            showPurchasePropertyDialog("Buy Property", "Buy", property, () -> { result[0] = mt; });
                                            break;
                                        default:
                                            result[0] = mt;
                                            synchronized (monopoly) {
                                                monopoly.notify();
                                            }

                                    }
                            }).setNegativeButton("Exit", (DialogInterface dialog, int which) -> {
                            if (monopoly.canCancel()) {
                                monopoly.cancel();
                            } else {
                                monopoly.stopGameThread();
                            }
                    }).show();

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
                    newDialogBuilderINGAME().setTitle(player.getPiece() + Utils.getPrettyString(type.name()))
                            .setItems(items, (DialogInterface dialog, int which) -> {
                                    result[0] = cards.get(which);
                                    synchronized (monopoly) {
                                        monopoly.notify();
                                    }
                            }).setNegativeButton("Cancel", (DialogInterface dialog, int which) -> {
                                if (monopoly.canCancel()) {
                                    monopoly.cancel();
                                } else {
                                    monopoly.stopGameThread();
                                }
                                synchronized (monopoly) {
                                    monopoly.notify();
                                }
                        }).show();
                }
            });
            Utils.waitNoThrow(monopoly, -1);
            return result[0];
        }

        @Override
        protected Trade showChooseTradeMenu(final Player player, final List<Trade> list) {
            final String [] items = Utils.toStringArray(list, true);
            final Trade [] result = new Trade[1];
            runOnUiThread(() -> {
                    newDialogBuilderINGAME().setTitle(player.getPiece() + " Choose Trade")
                            .setItems(items, (DialogInterface dialog, int which) -> {
                                    final Trade t = list.get(which);
                                    showPurchasePropertyDialog("Buy " + Utils.getPrettyString(t.getCard().getProperty().name()) + " from " + Utils.getPrettyString(t.getTrader().getPiece().name()),
                                            "Buy for $" + t.getPrice(), t.getCard().getProperty(), () -> { result[0] = t; });

                            }).show();
            });
            Utils.waitNoThrow(monopoly, -1);
            return result[0];
        }

        private void openMarkSellableMenu(final PlayerUser playerUser, final List<Card> list) {
            ListView view = new ListView(MonopolyActivity.this);
            view.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return list.size();
                }

                @Override
                public Object getItem(int position) {
                    return list.get(position);
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View v, ViewGroup parent) {
                    if (v == null) {
                        v = View.inflate(MonopolyActivity.this, R.layout.mark_sellable_listitem, null);
                    }
                    Card card = list.get(position);
                    TextView tvLabel = (TextView)v.findViewById(R.id.tvLabel);
                    TextView tvCost  = (TextView)v.findViewById(R.id.tvCost);
                    tvLabel.setText(Utils.getPrettyString(card.getProperty().name()));
                    int cost = playerUser.getSellableCardCost(card);
                    tvCost.setText(cost <= 0 ? "Not For Sale" : String.valueOf(cost));
                    return v;
                }
            });
            view.setOnItemClickListener((AdapterView<?> parent, View adapterView, final int position, long id) -> {
                    final Card card = list.get(position);
                    int cost = playerUser.getSellableCardCost(card);
                    final int STEP = 50;
                    final NumberPicker np = CCNumberPicker.newPicker(MonopolyActivity.this, cost, 0, 5000, STEP, null);
                    newDialogBuilderINGAME().setTitle("Set Cost for " + card.getProperty().name())
                            .setView(np).setNeutralButton("Done", (DialogInterface dialog, int which) -> {
                            playerUser.setSellableCard(card, np.getValue()*STEP);
                            openMarkSellableMenu(playerUser, list);

                    }).show();

            });
            newDialogBuilderINGAME().setTitle(playerUser.getPiece() + " Mark Sellable").setView(view).setNeutralButton("Done", (DialogInterface dialog, int which) -> {
                    synchronized (monopoly) {
                        monopoly.notify();
                    }
            }).show();
        }

        @Override
        protected boolean showMarkSellableMenu(final PlayerUser playerUser, final List<Card> list) {
            runOnUiThread(() -> { openMarkSellableMenu(playerUser, list); });
            Utils.waitNoThrow(monopoly, -1);
            return true;
        }

        @Override
        protected void onError(final Throwable t) {
            t.printStackTrace();
            checkPermissionAndThen(()-> { FileUtils.tryCopyFile(saveFile, new File(Environment.getExternalStorageDirectory(), "monopoly_error.txt")); }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            stopGameThread();
            runOnUiThread(() -> { newDialogBuilder().setTitle("ERROR").setMessage(t.toString()).setNegativeButton("Ok", null).show(); });
        }

        @Override
        public void drawPropertyCard(AGraphics g, Square property, float w, float h) {
            ((DroidGraphics)g).setTextHeight(getResources().getDimension(R.dimen.dialog_purchase_text_size));
            ((DroidGraphics)g).getPaint().setTypeface(Typeface.DEFAULT_BOLD);
            super.drawPropertyCard(g, property, w, h);
        }
    };

    AlertDialog.Builder newDialogBuilderINGAME() {
        AlertDialog.Builder builder = super.newDialogBuilder();
        if (monopoly.canCancel()) {
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                monopoly.cancel();
                synchronized (monopoly) {
                    monopoly.notify();
                }
            });
        } else {
            builder.setNegativeButton("Exit", (dialog, which) -> { monopoly.stopGameThread(); });
        }
        return builder;
    }

    void showPurchasePropertyDialog(String title, String buyLabel, final Square property, final Runnable onBuyRunnable) {
        newDialogBuilder().setTitle(title).setView(new DroidView(this) {
            @Override
            protected void onPaint(DroidGraphics g) {
                monopoly.drawPropertyCard(g, property, g.getViewportWidth(), g.getViewportHeight());
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                switch (MeasureSpec.getMode(widthMeasureSpec)) {
                    case MeasureSpec.AT_MOST:
                    case MeasureSpec.EXACTLY:
                        int width = MeasureSpec.getSize(widthMeasureSpec);
                        setMeasuredDimension(width, width*3/2);
                        return;
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }).setNegativeButton("Don't Buy", (DialogInterface dialog, int which) -> {
                synchronized (monopoly) {
                    monopoly.notify();
                }
        }).setPositiveButton(buyLabel, (DialogInterface dialog, int which) -> {
                onBuyRunnable.run();
                synchronized (monopoly) {
                    monopoly.notify();
                }
        }).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AndroidLogger.setLogFile(new File(Environment.getExternalStorageDirectory(), "monopoly.log"));
        saveFile = new File(getFilesDir(),"monopoly.save");
        rulesFile = new File(getFilesDir(), "rules.save");
        try {
            if (rulesFile.exists())
                monopoly.getRules().copyFrom(Reflector.deserializeFromFile(rulesFile));
        } catch (Exception e) {
            rulesFile.delete();
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
            showPlayerSetupMenu();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (monopoly.isGameRunning()) {
            monopoly.stopGameThread();
        }
    }

    @Override
    protected boolean shouldDialogAddBackButton() {
        return !monopoly.isGameRunning();
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
            monopoly.onClick();
            for (int i=0; i<monopoly.getNumPlayers(); i++) {
                if (monopoly.getPlayer(i) instanceof PlayerUser)
                    return;
            }
            monopoly.stopGameThread();
        }
    }

    void showOptionsMenu() {
        checkPermissionAndThen(() -> {
                File fixed = new File(Environment.getExternalStorageDirectory(), "monopoly_fixed.txt");
                if (fixed.exists()) {
                    FileUtils.tryCopyFile(fixed, saveFile);
                    fixed.delete();
                }
        }, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        newDialogBuilder().setTitle("OPTIONS")
                .setItems(new String[]{"New Game", "Resume"}, (DialogInterface dialog, int which) -> {
                        switch (which) {
                            case 0:
                                showGameSetupDialog();
                                break;
                            case 1:
                                if (monopoly.tryLoadFromFile(saveFile)) {
                                    monopoly.repaint();
                                    monopoly.startGameThread();
                                } else {
                                    saveFile.delete();
                                    newDialogBuilder().setTitle("ERROR").setMessage("Unable to load from save game.")
                                            .setNegativeButton("Ok", (DialogInterface dialog2, int which2) -> { showGameSetupDialog(); }).show();
                                }
                        }

                }).show();

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

        realPlayers.setOnValueChangedListener((NumberPicker picker, int oldVal, int newVal) -> {
                    int total = newVal + compPlayers.getValue();
                    if (total > Monopoly.MAX_PLAYERS) {
                        compPlayers.setValue(compPlayers.getValue()-1);
                    } else if (total < 2) {
                        compPlayers.setValue(compPlayers.getValue()+1);
                    }

            });

        compPlayers.setOnValueChangedListener((NumberPicker picker, int oldVal, int newVal) -> {
                    int total = newVal + realPlayers.getValue();
                    if (total > Monopoly.MAX_PLAYERS) {
                        realPlayers.setValue(realPlayers.getValue()-1);
                    } else if (total < 2) {
                        realPlayers.setValue(realPlayers.getValue()+1);
                    }
            });

        newDialogBuilder().setTitle("NEW GAME").setView(v)
            .setPositiveButton("Start", (DialogInterface dialog, int which) -> {
                    monopoly.clear();
                    for (int i=0; i<realPlayers.getValue(); i++)
                        monopoly.addPlayer(new PlayerUser());
                    for (int i=0; i<compPlayers.getValue(); i++)
                        monopoly.addPlayer(new Player());
                    showPieceChooser(() -> {
                        monopoly.newGame();
                        monopoly.startGameThread();
                    });

            }).show();
    }

    void showGameSetupDialog() {
        final Rules rules = monopoly.getRules();
        final int [] startMoneyValues = getResources().getIntArray(R.array.start_money_values);
        final int [] winMoneyValues = getResources().getIntArray(R.array.win_money_values);
        final int [] taxPercentValues = getResources().getIntArray(R.array.tax_scale_percent_values);
        final View v = View.inflate(this, R.layout.game_config_dialog, null);
        final CCNumberPicker npStartMoney = (CCNumberPicker)v.findViewById(R.id.npStartMoney);
        final CCNumberPicker npWinMoney = (CCNumberPicker)v.findViewById(R.id.npWinMoney);
        final CCNumberPicker npTaxScale = (CCNumberPicker)v.findViewById(R.id.npTaxScale);
        final CompoundButton cbJailBumpEnabled = (CompoundButton)v.findViewById(R.id.cbJailBumpEnabled);
        final CompoundButton cbJailMultiplierEnabled = (CompoundButton)v.findViewById(R.id.cbJailMultiplierEnabled);
        npStartMoney.init(startMoneyValues, rules.startMoney, (int value) -> "$" + value, (picker, oldVal, newVal) -> {
            getPrefs().edit().putInt("startMoney", startMoneyValues[newVal]).apply();
        });
        npWinMoney.init(winMoneyValues, rules.valueToWin, (int value) -> "$" + value, (picker, oldVal, newVal) -> {
            getPrefs().edit().putInt("winMoney", winMoneyValues[newVal]).apply();
        });
        npTaxScale.init(taxPercentValues, Math.round(100f * rules.taxScale), (int value) -> value + "%", (picker, oldVal, newVal) -> {
            getPrefs().edit().putInt("taxPercent", taxPercentValues[newVal]).apply();
        });
        cbJailBumpEnabled.setChecked(rules.jailBumpEnabled);
        cbJailMultiplierEnabled.setChecked(rules.jailMultiplier);
        cbJailBumpEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> { getPrefs().edit().putBoolean("jailBump", isChecked).apply(); });
        newDialogBuilder().setTitle("Configure").setView(v)
                .setPositiveButton("Setup Players", (DialogInterface dialog, int which) -> {
                        rules.startMoney = startMoneyValues[npStartMoney.getValue()];
                        rules.valueToWin = winMoneyValues[npWinMoney.getValue()];
                        rules.taxScale = 0.01f * taxPercentValues[npTaxScale.getValue()];
                        rules.jailBumpEnabled = cbJailBumpEnabled.isChecked();
                        rules.jailMultiplier = cbJailMultiplierEnabled.isChecked();
                        try {
                            Reflector.serializeToFile(rules, rulesFile);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        showPlayerSetupMenu();
                }).show();
    }

    private Dialog pieceDialog = null;

    void showPieceChooser(final Runnable andThen) {
        for (int i=0; i<monopoly.getNumPlayers(); i++) {
            final Player p = monopoly.getPlayer(i);
            if (p instanceof PlayerUser && p.getPiece() == null) {
                View.OnClickListener listener = (View v) -> {
                        if (pieceDialog != null)
                            pieceDialog.dismiss();
                        p.setPiece((Piece)v.getTag());
                        showPieceChooser(andThen);
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

                pieceDialog = newDialogBuilder().setTitle("CHOOSE PIECE " + (i+1)).setView(gl).show();
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

//        d.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        Display display = getWindowManager().getDefaultDisplay();
        WindowManager.LayoutParams lp = d.getWindow().getAttributes();
        Point size = new Point();
        display.getSize(size);
        if (isPortrait()) {
            //lp.gravity = Gravity.TOP;
            lp.y = size.y/5;
            lp.width = size.x/2;
        } else {
            lp.width = size.y/2;
        }
        d.getWindow().setAttributes(lp);
    }
}
