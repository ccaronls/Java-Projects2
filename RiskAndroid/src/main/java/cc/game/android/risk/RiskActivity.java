package cc.game.android.risk;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.IInterpolator;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.RomanNumeral;
import cc.lib.game.Utils;
import cc.lib.math.Bezier;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.risk.Action;
import cc.lib.risk.Army;
import cc.lib.risk.RiskBoard;
import cc.lib.risk.RiskCell;
import cc.lib.risk.RiskGame;
import cc.lib.risk.RiskPlayer;
import cc.lib.utils.Lock;
import cc.lib.utils.Pair;

public class RiskActivity extends DroidActivity implements ListView.OnItemClickListener {

    File saveGame = null;
    RiskGame game = new RiskGame() {
        @Override
        protected void onDiceRolled(Army attacker, int[] attackingDice, Army defender, int[] defendingDice, boolean[] result) {
            super.onDiceRolled(attacker, attackingDice, defender, defendingDice, result);
            if (getPlayerOrNull(attacker) instanceof UIRiskPlayer || getPlayerOrNull(defender) instanceof UIRiskPlayer) {
                new DiceDialog(RiskActivity.this, attacker, defender, attackingDice, defendingDice, result);
            }
        }

        @Override
        protected void onPlaceArmy(Army army, int cellIdx) {
            RiskCell cell = getBoard().getCell(cellIdx);
            GRectangle rect = new GRectangle(getBoard().getDimension());
            Vector2D start = Utils.randItem(Utils.toArray(
                    rect.getBottomLeft(),
                    rect.getBottomRight()
            ));
            IInterpolator<Vector2D> interp = Bezier.build(
                    new Vector2D(start), new Vector2D(cell), .4f);
            addAnimation(new AAnimation<AGraphics>(1000) {
                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    g.setColor(army.getColor());
                    //drawRomanNumeral(g, interp.getAtPosition(position), r);
                    drawArmy(g, interp.getAtPosition(position), army, 1);
                }
            });
        }

        @Override
        protected void onMoveTroops(int startIdx, int endIdx, int numTroops) {
            RiskCell start = getBoard().getCell(startIdx);
            Army army = start.getOccupier();
            MutableVector2D endV = new MutableVector2D(getBoard().getCell(endIdx));
            MutableVector2D delta = endV.sub(start);
            if (delta.getX() > getBoard().getDimension().getWidth()/2) {
                delta.subEq(getBoard().getDimension().getWidth(), 0);
            } else if (delta.getX() < -getBoard().getDimension().getWidth()/2) {
                delta.addEq(getBoard().getDimension().getWidth(), 0);
            }
            if (delta.getY() > getBoard().getDimension().getHeight()/2) {
                delta.subEq(0, getBoard().getDimension().getHeight());
            } else if (delta.getY() < -getBoard().getDimension().getHeight()/2) {
                delta.addEq(0, getBoard().getDimension().getHeight());
            }
            IInterpolator<Vector2D> interp = Bezier.build(
                    new Vector2D(start), new Vector2D(start).add(delta), .4f);
            addAnimation(new AAnimation<AGraphics>(1000) {
                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    g.setColor(army.getColor());
                    //drawRomanNumeral(g, interp.getAtPosition(position), r);
                    drawArmy(g, interp.getAtPosition(position), army, numTroops);
                }
            });
            Utils.waitNoThrow(this, 1000);
        }

        @Override
        protected void onStartAttackTerritoryChosen(int cellIdx) {
            RiskCell start = getBoard().getCell(cellIdx);
            GRectangle zoom = getBoard().getCellBoundingRect(cellIdx);
            GRectangle startRect = new GRectangle(getBoard().getDimension());
            for (Integer idx : start.getAllConnectedCells()) {
                GRectangle rect2 = getBoard().getCellBoundingRect(idx);
                Vector2D dv = rect2.getCenter().subEq(start);
                if (dv.getX() < -getBoard().getDimension().getWidth()/2) {
                    rect2.moveBy(getBoard().getDimension().getWidth(), 0);
                } else if (dv.getX() > getBoard().getDimension().getWidth()/2) {
                    rect2.moveBy(-getBoard().getDimension().getWidth(), 0);
                }
                if (dv.getY() < -getBoard().getDimension().getHeight()/2) {
                    rect2.moveBy(0, getBoard().getDimension().getHeight());
                } else if (dv.getY() > getBoard().getDimension().getHeight()/2) {
                    rect2.moveBy(0, -getBoard().getDimension().getHeight());
                }
                zoom.addEq(rect2);
            }
            zoom.setAspect(startRect.getAspect());

            GRectangle endRect = new GRectangle(zoom);
            IInterpolator<GRectangle> rectInterp = GRectangle.getInterpolator(startRect, endRect);
            IInterpolator<Vector2D> dragDeltaInterp = Vector2D.getLinearInterpolator(dragDelta, Vector2D.ZERO);

            addAnimation(new AAnimation<AGraphics>(1000) {

                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    zoomRect = rectInterp.getAtPosition(position);
                    dragDelta.set(dragDeltaInterp.getAtPosition(position));
                }
            });
            highlightedCells.add(new Pair(cellIdx, GColor.RED));
            Utils.waitNoThrow(this, 1000);
        }

        @Override
        protected void onEndAttackTerritoryChosen(int startIdx, int endIdx) {
            highlightedCells.add(new Pair(endIdx, GColor.GOLD));
            super.onEndAttackTerritoryChosen(startIdx, endIdx);
        }

        @Override
        protected void onBeginTurn(int playerNum) {
            zoomRect = null;
            highlightedCells.clear();
            RiskPlayer pl = getPlayer(playerNum);
            GColor color = pl.getArmy().getColor();
            addOverlayAnimation(new ExpandingTextOverlayAnimation(pl.getArmy() + " 's Turn", color));
            Utils.waitNoThrow(this, 1000);
        }

        @Override
        protected void onBeginMove() {
            zoomRect = null;
            highlightedCells.clear();
        }

        @Override
        protected void onStartMoveTerritoryChosen(int cellIdx) {
            highlightedCells.add(new Pair(cellIdx, GColor.CYAN));
        }

        @Override
        protected void onArmiesDestroyed(Army defender, int cellIdx, int numArmiesLost) {
            Lock lock = new Lock(numArmiesLost);
            RiskCell cell = getBoard().getCell(cellIdx);
            GRectangle rect = getBoard().getCellBoundingRect(cellIdx).scaledBy(.3f);
            for (int i=0; i<numArmiesLost; i++) {
                Vector2D exploLoc = rect.getRandomPointInside();
                addAnimation(new AAnimation<AGraphics>(2000) {
                    @Override
                    protected void draw(AGraphics g, float position, float dt) {
                        float step = 1f/exploAnim.length;
                        int idx = Utils.clamp(Math.round(position/step), 0, exploAnim.length-1);
                        AImage img = g.getImage(exploAnim[idx]);
                        g.drawImage(exploAnim[idx], exploLoc, Justify.CENTER, Justify.BOTTOM, .3f);
                    }

                    @Override
                    protected void onDone() {
                        super.onDone();
                        lock.release();
                    }
                });
                Utils.waitNoThrow(this, 500);
            }
            lock.block();
        }

        @Override
        protected void onGameOver(Army winner) {
            super.onGameOver(winner);
            addOverlayAnimation(new ExpandingTextOverlayAnimation(winner.name() + " WINS!!!", GColor.BLUE)
            .setOscillating(true)
            .setRepeats(-1));
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
    private final List<AAnimation<AGraphics>> animations = new ArrayList<>();
    private final List<AAnimation<AGraphics>> overlayAnimations = new ArrayList<>();
    private GRectangle zoomRect = null;
    private final List<Pair<Integer, GColor>> highlightedCells = new ArrayList<>();

    static RiskActivity instance;

    void clearAnimations() {
        synchronized (animations) {
            animations.clear();
        }
        synchronized (overlayAnimations) {
            animations.clear();
        }
    }

    void init() {
        clearAnimations();
        highlightedCells.clear();
        zoomRect = null;
    }

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
        try {
            if (saveGame.exists()) {
                RiskGame t = new RiskGame();
                t.loadFromFile(saveGame);
                game.copyFrom(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
            saveGame.delete();
        }
        initHomeMenu();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopGameThread();
    }

    enum Buttons {
        NEW_GAME,
        RESUME,
        ABOUT,
    }

    void initHomeMenu() {
        clearAnimations();
        zoomRect = null;
        highlightedCells.clear();
        if (saveGame.exists()) {
            initMenu(Utils.toList(Buttons.values()));
        } else {
            initMenu(Utils.toList(Buttons.NEW_GAME, Buttons.ABOUT));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("TAP", "onItemClick");
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
                new PlayerChooserDialog(this);
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

    int [] exploAnim = null;

    @Override
    protected void onDraw(DroidGraphics g) {
        if (exploAnim == null) {
            int [][] cells = {
                    { 57,94,115-57,125-94}, {138,73,236-138,125-73},{252,70,370-252,125-70},{408,48,572-408,125-48},
                    { 47,200,187-47,266-200}, {200,177,341-200,266-177},{353,193,400-353,266-193},{493,197,592-493,266-197}
            };

            exploAnim = g.loadImageCells(R.drawable.blowup_anim, cells);
        }


        g.setTextModePixels(true);
//        g.getPaint().setStrokeWidth(getResources().getDimension(R.dimen.roman_number_thickness));
        RiskBoard board = game.getBoard();
        GRectangle imageRect = new GRectangle(board.getDimension());
        GRectangle boardRect = zoomRect == null ? imageRect : zoomRect;
        g.ortho(boardRect);
        g.pushMatrix();
        {
            dragDelta.wrap(Vector2D.ZERO, new Vector2D(imageRect.w, imageRect.h));
            g.translate(dragDelta);
            int culled = 0;
            g.pushMatrix();
            {
                g.translate(0, -imageRect.h);
                for (int ii = 0; ii < 3; ii++) {
                    g.pushMatrix();
                    g.translate(-imageRect.w, 0);
                    for (int i = 0; i < 3; i++) {
                        // dont render if target rect is not visible
                        MutableVector2D tl = imageRect.getTopLeft();
                        MutableVector2D br = imageRect.getBottomRight();

                        g.transform(tl);
                        g.transform(br);

                        if (tl.getX() >= g.getViewportWidth() ||
                            tl.getY() >= g.getViewportHeight() ||
                            br.getX() <= 0 ||
                            br.getY() <= 0) {
                            culled++;
                        } else {
                            g.drawImage(R.drawable.risk_board, imageRect);
                            pickCell(g, board);
                            drawCells(g, board);
                            drawHighlightedCells(g, board);
                        }
                        drawAnimations(animations, g);
                        g.translate(imageRect.w, 0);
                    }
                    g.popMatrix();
                    g.translate(0, imageRect.h);
                }
                Log.d("CULL", "Culled: " + culled);
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

        drawAnimations(overlayAnimations, g);
    }

    void drawHighlightedCells(AGraphics g, RiskBoard board) {
        for (Pair<Integer, GColor> pair : highlightedCells) {
            RiskCell cell = board.getCell(pair.first);
            board.renderCell(cell, g, 1);
            g.setColor(pair.second);
            g.drawLineLoop(4);
        }
    }

    synchronized void addAnimation(AAnimation<AGraphics> a) {
        animations.add(a);
        redraw();
    }

    synchronized void addOverlayAnimation(AAnimation<AGraphics> a) {
        overlayAnimations.add(a);
        redraw();
    }

    synchronized void drawAnimations(List<AAnimation<AGraphics>> list, AGraphics g) {
        Iterator<AAnimation<AGraphics>> it = list.iterator();
        while (it.hasNext()) {
            AAnimation<AGraphics> a = it.next();
            if (a.isDone()) {
                it.remove();
            } else if (!a.isStarted()) {
                a.start();
            } else {
                a.update(g);
            }
        }
        if (list.size() > 0) {
            redraw();
        }
    }

    /*
    void drawRomanNumeral(AGraphics g, IVector2D cell, String numerals) {
        float thickness = getResources().getDimension(R.dimen.roman_number_thickness);
        AGraphics.Border [] borders = {
                new AGraphics.Border(AGraphics.BORDER_FLAG_NORTH, thickness, thickness, 0, -thickness/2, thickness*2/3),
                new AGraphics.Border(AGraphics.BORDER_FLAG_SOUTH, thickness, thickness, 0, -thickness/2, 0),
        };
        g.drawJustifiedStringBordered(cell, Justify.CENTER, Justify.CENTER, numerals, borders);
    }*/

    // EXPERIMENTAL
    void drawArmy(AGraphics g, IVector2D cell, Army army, int numTroops) {
        String numerals = roman.toRoman(numTroops);
        float thickness = getResources().getDimension(R.dimen.roman_number_thickness);
        AGraphics.Border [] borders = {
                new AGraphics.Border(AGraphics.BORDER_FLAG_NORTH, thickness, thickness, 0, -thickness/2, thickness*2/3),
                new AGraphics.Border(AGraphics.BORDER_FLAG_SOUTH, thickness, thickness, 0, -thickness/2, 0),
        };
        g.setColor(GColor.BLACK);
        float th = getResources().getDimension(R.dimen.text_height_roman);
        g.setTextHeight(th + 4);
        g.setTextStyles(AGraphics.TextStyle.BOLD);
        g.drawJustifiedStringBordered(cell, Justify.CENTER, Justify.CENTER, numerals, borders);
        g.setTextHeight(th);
        g.setTextStyles(AGraphics.TextStyle.NORMAL);
        g.setColor(army.getColor());
        g.drawJustifiedStringBordered(cell, Justify.CENTER, Justify.CENTER, numerals, borders);
    }

    void drawCells(AGraphics g, RiskBoard board) {
        for (RiskCell cell : board.getCells()) {
            if (cell.getOccupier() != null) {
                drawArmy(g, cell, cell.getOccupier(), cell.getNumArmies());
                /*
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
                 */
            }

            /* DRAW THE CELL OUTLINES
            board.renderCell(cell, g, 1);
            g.setColor(cell.getRegion().getColor());
            g.drawLineLoop(getResources().getDimension(R.dimen.cell_width));
            g.begin();
             */
            /* DRAW CONNECTION BETWEEN THE CELLS
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

    void pickCell(AGraphics g, RiskBoard board) {
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
            if (tapped) {
                if (board.isPointInsideCell(tap, idx)) {
                    tapped = false;
                    tapPos.zero();
                    setGameResult(idx);
                    break;
                }
            }
        }
    }

    MutableVector2D dragStart = new MutableVector2D();
    MutableVector2D dragDelta = new MutableVector2D();
    boolean tapped = false;

    final MutableVector2D tapPos = new MutableVector2D();

    @Override
    protected void onTap(float x, float y) {
        Log.d("TAP", "onTap ("+x+","+y+")");
        tapPos.set(x,y);
        tapped = true;
    }

    @Override
    protected void onDragStart(float x, float y) {
        dragStart.set(x, y);
    }

    @Override
    protected void onDrag(float x, float y) {
        if (zoomRect == null) {
            dragDelta.addEq(new Vector2D(x, y).sub(dragStart));
            dragStart.set(x, y);
        }
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
            try {
                fileWriterQueue.put(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
;        }
        setGameResult(null);
        if (!isFinishing()) {
            runOnUiThread(() -> initHomeMenu());
        }
    }

    synchronized void startGameThread() {
        if (running)
            return;
        clearAnimations();
        initMenu(Collections.emptyList());
        fileWriterThread = startFileWriterThread();
        running = true;
        new Thread() {
            @Override
            public void run() {
                while (running && !game.isDone()) {
                    try {
                        game.runGame();
                        redraw();
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
                running = false;
                runOnUiThread(()->initHomeMenu());
            }
        }.start();
    }

    String message = "";

    public <T> T waitForUser(Class<T> expectedType) {
        redraw();
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
                log.debug("Backing up ... ");
                //FileUtils.backupFile(gameFile, 32);
                game.trySaveToFile(saveGame);
            }
            log.debug("fileWriterThread EXIT");
            fileWriterThread = null;
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

    void startGame(List<RiskPlayer> players) {
        init();
        game.clear();
        for (RiskPlayer pl : players)
            game.addPlayer(pl);
        startGameThread();
    }

}

