package cc.game.android.risk;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.game.RomanNumeral;
import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.risk.Action;
import cc.lib.risk.Army;
import cc.lib.risk.RiskBoard;
import cc.lib.risk.RiskCell;
import cc.lib.risk.RiskGame;
import cc.lib.utils.Lock;

public class MainActivity extends DroidActivity implements ListView.OnItemClickListener {

    File saveGame = null;
    RiskGame game = new RiskGame() {
        @Override
        protected void onDiceRolled(Army attacker, int[] attackingDice, Army defender, int[] defendingDice, boolean[] result) {
            super.onDiceRolled(attacker, attackingDice, defender, defendingDice, result);
            View view = View.inflate(MainActivity.this, R.layout.dice_dialog, null);
            Lock lock = new Lock();
            Dialog [] dialog = new Dialog[1];
            runOnUiThread(() -> {
                dialog[0] = newDialogBuilder().setTitle(attacker.name() + " Attacks " + defender.name())
                        .setView(view)
                        .show();
                DiceView [] red = {
                        view.findViewById(R.id.red1),
                        view.findViewById(R.id.red2),
                        view.findViewById(R.id.red3)
                };

                DiceView [] white = {
                        view.findViewById(R.id.white1),
                        view.findViewById(R.id.white2)
                };

                for (int i=0; i<red.length; i++) {
                    if (i >= attackingDice.length) {
                        red[i].setVisibility(View.INVISIBLE);
                    } else {
                        red[i].rollDice(attackingDice[i], lock);
                    }
                }

                for (int i=0; i<white.length; i++) {
                    if (i >= defendingDice.length)
                        white[i].setVisibility(View.INVISIBLE);
                    else
                        white[i].rollDice(defendingDice[i], lock);
                }
            });

            Utils.waitNoThrow(this, 2000);
            lock.block();
            Utils.waitNoThrow(this, 5000);
            runOnUiThread(() -> dialog[0].dismiss());
        }
    };
    boolean running = false;
    private Object monitor = new Object();
    private Object result = null;
    private ListView listView;
    private List<Integer> pickableTerritories = new ArrayList<>();
    private RomanNumeral roman = new RomanNumeral();

    static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveGame = new File(getFilesDir(), "save.game");
        try (InputStream in = getAssets().open("risk.board")) {
            game.getBoard().deserialize(in);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
        listView = findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);
        hideNavigationBar();

        //getContent().setPinchZoomEnabled(true);
        //getContent().setZoomScaleBound(1, 5);
        instance = this;
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (saveGame.exists())
            game.tryLoadFromFile(saveGame);
        initHomeMenu();
    }

    enum Buttons {
        NEW_GAME,
        RESUME,
        ABOUT,
    }

    void initHomeMenu() {
        if (saveGame.exists()) {
            initMenu(Utils.toList(Buttons.values()));
        } else {
            initMenu(Utils.toList(Buttons.NEW_GAME, Buttons.ABOUT));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object tag = view.getTag();
        if (tag == null)
            return;

        if (tag instanceof  Action) {
            setGameResult(tag);
            return;
        }

        if (tag instanceof Buttons) {

        } else {
            return;
        }

        switch ((Buttons)tag) {
            case NEW_GAME: {
                String [] options = { "2", "3", "4", "5", "6" };
                newDialogBuilder().setTitle("Choose Number of Players")
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int num = which+2;
                                game.clear();
                                for (int i=0; i<num; i++)
                                    game.addPlayer(new UIRiskPlayer(Army.values()[i]));
                                startGameThread();
                            }
                        }).setNegativeButton(R.string.popup_button_close, null)
                        .show();
                break;
            }
            case ABOUT:
                newDialogBuilder().setTitle("About")
                        .setMessage("Game written by Chris Caron")
                        .setNegativeButton(R.string.popup_button_close, null)
                        .show();
                break;
            case RESUME: {
                if (game.tryLoadFromFile(saveGame)) {
                    startGameThread();
                }
                break;
            }
        }
    }

    void initMenu(List buttons) {
        listView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return buttons.size();
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = View.inflate(MainActivity.this, R.layout.list_item, null);
                }

                TextView b = convertView.findViewById(R.id.text_view);
                b.setTag(buttons.get(position));
                b.setText(Utils.toPrettyString(buttons.get(position)));
                return b;
            }
        });
    }

    @Override
    protected void onDraw(DroidGraphics g) {
        g.setTextHeight(18);
        GRectangle imageRect = new GRectangle(game.getBoard().getDimension());
        RiskBoard board = game.getBoard();
        g.ortho(imageRect);
        g.pushMatrix();
        {
            while (dragDelta.getX() > imageRect.w) {
                dragDelta.subEq(imageRect.w, 0);
            }
            while (dragDelta.getX() < -imageRect.w) {
                dragDelta.addEq(imageRect.w, 0);
            }
            g.translate(dragDelta.getX(), 0);
            g.pushMatrix();
            {
                g.translate(-imageRect.w, 0);
                for (int i = 0; i < 3; i++) {
                    g.drawImage(R.drawable.risk_board, imageRect);
                    g.translate(imageRect.w, 0);
                }
            }
            g.popMatrix();
            g.pushMatrix();
            {
                g.translate(-imageRect.w, 0);
                for (int i = 0; i < 3; i++) {
                    drawCells(g, board);
                    pickCell(g, board);
                    g.translate(imageRect.w, 0);
                }
            }
            g.popMatrix();
        }
        g.popMatrix();
        g.ortho();
        g.setColor(GColor.CYAN);
        if (game.getNumPlayers() > 0)
            game.getSummary().draw(g, 10, g.getViewportHeight()-10, Justify.LEFT, Justify.BOTTOM);
        if (message != null) {
            g.drawJustifiedStringOnBackground(g.getViewportWidth()-10, g.getViewportHeight()-10, Justify.RIGHT, Justify.BOTTOM, message, GColor.TRANSLUSCENT_BLACK, 10, 5);
        }
    }

    void drawCells(DroidGraphics g, RiskBoard board) {
        g.getPaint().setStrokeWidth(getResources().getDimension(R.dimen.roman_number_thickness));
        float boardWidth = board.getDimension().width;
        for (RiskCell cell : board.getCells()) {
            if (cell.getOccupier() != null) {
                g.setColor(cell.getOccupier().getColor());
                GDimension dim = g.drawJustifiedString(cell, Justify.CENTER, Justify.CENTER, roman.toRoman(cell.getNumArmies()));
                GRectangle rect = new GRectangle(dim).withCenter(cell).scaledBy(1, .4f);
                g.begin();
                g.vertex(rect.getTopLeft());
                g.vertex(rect.getTopRight());
                g.vertex(rect.getBottomLeft());
                g.vertex(rect.getBottomRight());
                g.drawLines();
            }

            /*
            board.renderCell(cell, g, 1);
            g.setColor(cell.getRegion().getColor());
            g.drawLineLoop(getResources().getDimension(R.dimen.cell_width));
            g.begin();
             */
            /*
            for (int idx : cell.getConnectedCells()) {
                MutableVector2D v0 = new MutableVector2D(cell);
                MutableVector2D v1 = new MutableVector2D(board.getCell(idx));
                float maxW = boardWidth / 2;
                if (v0.sub(v1).magSquared() > maxW * maxW) {
                    if (v1.getX() < boardWidth /2)
                        v1.subEq(boardWidth, 0);
                    else {
                        v0.addEq(boardWidth, 0);
                        g.vertex(v0);
                        g.vertex(v1);
                        g.setColor(GColor.CYAN);
                        g.drawLines(getResources().getDimension(R.dimen.cell_width));
                    }
                }
            }*/
        }
    }

    void pickCell(APGraphics g, RiskBoard board) {
        Log.d("TAP", "Viewport: " + g.getViewport());
        Log.d("TAP", "BoardDim: " + board.getDimension());
        Log.d("TAP", "tap Befone: " + tapPos);
        Log.d("TAP", "DragDelta: " + dragDelta);
        MutableVector2D tap = new MutableVector2D(tapPos);
        g.screenToViewport(tap);
        Log.d("TAP", "Tap Viewport: " + tap);
        g.setColor(GColor.WHITE);
        for (int idx : pickableTerritories) {
            RiskCell cell = board.getCell(idx);
            board.renderCell(cell, g);
            g.drawLineLoop(getResources().getDimension(R.dimen.cell_width));
            if (board.isPointInsideCell(tap, idx)) {
                tapPos.zero();
                setGameResult(idx);
                pickableTerritories.clear();
                break;
            }
        }
    }

    MutableVector2D dragStart = new MutableVector2D();
    MutableVector2D dragDelta = new MutableVector2D();

    final MutableVector2D tapPos = new MutableVector2D();

    @Override
    protected void onTap(float x, float y) {
        tapPos.set(x,y);
    }

    @Override
    protected void onDragStart(float x, float y) {
        dragStart.set(x, y);
    }

    @Override
    protected void onDrag(float x, float y) {
        dragDelta.addEq(new Vector2D(x,y).sub(dragStart));
        dragStart.set(x,y);
    }

    @Override
    public void onBackPressed() {
        if (running) {
            stopGameThread();
            initHomeMenu();
        } else {
            super.onBackPressed();
        }
    }

    void stopGameThread() {
        running = false;
        setGameResult(null);
    }

    synchronized void startGameThread() {
        if (running)
            return;
        initMenu(Collections.emptyList());
        running = true;
        new Thread() {
            @Override
            public void run() {
                while (running) {
                    try {
                        game.runGame();
                        getContent().invalidate();
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
                running = false;
            }
        }.start();
    }

    String message = "";

    public <T> T waitForUser(Class<T> expectedType) {
        getContent().postInvalidate();
        synchronized (monitor) {
            try {
                monitor.wait();
            } catch (Exception e) {

            }
        }
        if (result != null && expectedType.isAssignableFrom(result.getClass()))
            return (T)result;
        return null;
    }

    public void setGameResult(Object result) {
        this.result = result;
        runOnUiThread(() -> {initMenu(Collections.emptyList());});
        message = null;
        pickableTerritories.clear();
        game.trySaveToFile(saveGame);
        synchronized (monitor) {
            monitor.notify();
        }
    }

    Integer pickTerritory(List<Integer> options, String msg) {
        message = msg;
        pickableTerritories.clear();
        pickableTerritories.addAll(options);
        return waitForUser(Integer.class);
    }

    Action pickAction(List<Action> options, String msg) {
        message = msg;
        runOnUiThread(() -> initMenu(options));
        return waitForUser(Action.class);
    }


}

