package cc.game.android.risk;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.IVector2D;
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

public class RiskActivity extends DroidActivity implements ListView.OnItemClickListener {

    File saveGame = null;
    RiskGame game = new RiskGame() {
        @Override
        protected void onDiceRolled(Army attacker, int[] attackingDice, Army defender, int[] defendingDice, boolean[] result) {
            super.onDiceRolled(attacker, attackingDice, defender, defendingDice, result);
            Lock lock = new Lock();
            DiceDialog [] dialog = new DiceDialog[1];
            runOnUiThread(() -> {
                dialog[0] = new DiceDialog(lock, RiskActivity.this, attacker, defender, attackingDice, defendingDice, result);
            });

            Utils.waitNoThrow(this, 2000);
            lock.block();
            runOnUiThread(() -> {
                dialog[0].showResult();
            });
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
    private final ArrayBlockingQueue fileWriterQueue = new ArrayBlockingQueue(1);
    private Thread fileWriterThread = null;

    static RiskActivity instance;

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
                    convertView = View.inflate(RiskActivity.this, R.layout.list_item, null);
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
        g.setTextModePixels(true);
        GRectangle imageRect = new GRectangle(game.getBoard().getDimension());
        RiskBoard board = game.getBoard();
        g.ortho(imageRect);
        g.pushMatrix();
        {
            while (dragDelta.getX() > imageRect.w) {
                dragDelta.subEq(imageRect.w, 0);
            }
            while (dragDelta.getX() < 0) {
                dragDelta.addEq(imageRect.w, 0);
            }
            g.translate(dragDelta.getX(), 0);
            g.pushMatrix();
            {
                g.translate(-imageRect.w, 0);
                for (int i = 0; i < 2; i++) {
                    g.drawImage(R.drawable.risk_board, imageRect);
                    g.translate(imageRect.w, 0);
                }
            }
            g.popMatrix();
            g.pushMatrix();
            {
                g.translate(-imageRect.w, 0);
                for (int i = 0; i < 2; i++) {
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
        g.setTextHeight(getResources().getDimension(R.dimen.text_height_info));
        if (game.getNumPlayers() > 0)
            game.getSummary().draw(g, 10, g.getViewportHeight()-10, Justify.LEFT, Justify.BOTTOM);
        if (message != null) {
            g.drawJustifiedStringOnBackground(g.getViewportWidth()-20, 20, Justify.RIGHT, Justify.TOP, message, GColor.TRANSLUSCENT_BLACK, 10, 5);
        }
    }

    void drawRomanNumeral(APGraphics g, IVector2D cell, String numerals) {
        float thickness = getResources().getDimension(R.dimen.roman_number_thickness);
        AGraphics.Border [] borders = {
                new AGraphics.Border(AGraphics.BORDER_FLAG_NORTH, thickness, thickness, 0, -thickness/2, thickness*2/3),
                new AGraphics.Border(AGraphics.BORDER_FLAG_SOUTH, thickness, thickness, 0, -thickness/2, 0),
        };
        g.drawJustifiedStringBordered(cell, Justify.CENTER, Justify.CENTER, numerals, borders);
    }

    void drawCells(DroidGraphics g, RiskBoard board) {
        g.getPaint().setStrokeWidth(getResources().getDimension(R.dimen.roman_number_thickness));
        float boardWidth = board.getDimension().width;
        for (RiskCell cell : board.getCells()) {
            if (cell.getOccupier() != null) {
                String numerals = roman.toRoman(cell.getNumArmies());
                g.setColor(GColor.BLACK);
                float th = getResources().getDimension(R.dimen.text_height_roman);
                g.setTextHeight(th + 4);
                g.setTextStyles(AGraphics.TextStyle.BOLD);
                drawRomanNumeral(g, cell, numerals);
                g.setTextHeight(th);
                g.setTextStyles(AGraphics.TextStyle.NORMAL);
                g.setColor(cell.getOccupier().getColor());
                drawRomanNumeral(g, cell, numerals);
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
        //Log.d("TAP", "Viewport: " + g.getViewport());
        //Log.d("TAP", "BoardDim: " + board.getDimension());
        //Log.d("TAP", "tap Befone: " + tapPos);
        //Log.d("TAP", "DragDelta: " + dragDelta);
        MutableVector2D tap = new MutableVector2D(tapPos);
        g.screenToViewport(tap);
        //Log.d("TAP", "Tap Viewport: " + tap);
        g.setColor(GColor.WHITE);
        for (int idx : pickableTerritories) {
            RiskCell cell = board.getCell(idx);
            board.renderCell(cell, g);
            g.drawLineLoop(getResources().getDimension(R.dimen.cell_width));
            if (board.isPointInsideCell(tap, idx)) {
                tapPos.zero();
                setGameResult(idx);
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
        if (fileWriterThread != null) {
            fileWriterThread.interrupt();
            fileWriterThread = null;
        }
        setGameResult(null);
    }

    synchronized void startGameThread() {
        if (running)
            return;
        initMenu(Collections.emptyList());
        fileWriterThread = startFileWriterThread();
        running = true;
        new Thread() {
            @Override
            public void run() {
                while (running) {
                    try {
                        game.runGame();
                        getContent().postInvalidate();
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
        //game.trySaveToFile(saveGame);
        try {
            fileWriterQueue.put(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        synchronized (monitor) {
            monitor.notify();
        }
    }

    Thread startFileWriterThread() {
        Thread t = new Thread(() -> {
            log.debug("fileWriterThread ENTER");
            while (running) {
                try {
                    fileWriterQueue.take();
                } catch (InterruptedException e) {
                    break;
                }
                log.debug("Backingup ... ");
                //FileUtils.backupFile(gameFile, 32);
                game.trySaveToFile(saveGame);
            }
            log.debug("fileWriterThread EXIT");
        });
        t.start();
        return t;
    }

    Integer pickTerritory(List<Integer> options, String msg) {
        message = msg;
        pickableTerritories.clear();
        pickableTerritories.addAll(options);
        runOnUiThread(()->initMenu(Arrays.asList(Action.CANCEL)));
        return waitForUser(Integer.class);
    }

    Action pickAction(List<Action> options, String msg) {
        message = msg;
        runOnUiThread(() -> initMenu(options));
        return waitForUser(Action.class);
    }


}

