package cc.applets.zombicide;

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTGraphics2;
import cc.lib.swing.AWTImage;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCellQuadrant;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZTiles;
import cc.lib.zombicide.ZZombieType;

class BoardComponent extends AWTComponent implements ZTiles {

    final Logger log = LoggerFactory.getLogger(getClass());

    String message = "";
    Grid.Pos highlightedCell = null;
    Object highlightedResult = null;
    ZActor highlightedActor = null;
    ZDoor highlightedDoor = null;
    Grid.Pos selectedCell = null;
    Font bigFont;
    Font smallFont;
    boolean drawTiles = false;
    private Object overlayToDraw = null;
    private Map<String, Integer> cardImages = new HashMap<>();

    BoardComponent() {
        setPreferredSize(250, 250);
    }

    ZGame getGame() {
        return ZombicideApplet.instance.game;
    }

    @Override
    protected void init(AWTGraphics g) {
//        GDimension cellDim = getGame().board.initCellRects(g, g.getViewportWidth()-5, g.getViewportHeight()-5);
        setMouseEnabled(true);
        //setMinimumSize(256, 256);
//        setPreferredSize( (int)cellDim.width * getGame().board.getColumns(), (int)cellDim.height * getGame().board.getRows());
        //setMaximumSize( (int)cellDim.width * getGame().board.getColumns(), (int)cellDim.height * getGame().board.getRows());
        smallFont = g.getFont();
        bigFont = g.getFont().deriveFont(24f).deriveFont(Font.BOLD).deriveFont(Font.ITALIC);
        new Thread() {
            @Override
            public void run() {
                loadImages(g);
            }
        }.start();
    }



    int numImagesLoaded=0;
    int totalImagesToLoad=1000;
    Map<GRectangle, List<ZMove>> rectMap = new HashMap<>();

    void collectRects(List options) {
        for (Object obj : options) {
            if (obj instanceof ZMove) {

            }
        }
    }

    void collectMoveRects(ZMove move) {

    }

    void pickMove(AGraphics g, int mx, int my) {
        for (Map.Entry<GRectangle, List<ZMove>> e : rectMap.entrySet()) {
            if (e.getKey().contains(mx, my)) {
                // show a context menu to the side of the rect with the move options

            }
        }
    }

    class ContextMenu {
        final GRectangle sourceRect;
        final List<ZMove> moves;

        public ContextMenu(GRectangle sourceRect, List<ZMove> moves) {
            this.sourceRect = sourceRect;
            this.moves = moves;
        }

        ZMove pick(AGraphics g, int mx, int my) {
            ZMove result = null;
            String [] items = new String[moves.size()];
            int index=0;
            float maxWidth=0;
            float border = 10;
            for (ZMove m : moves) {
                String label = m.getLabel();
                items[index++] = label;
                maxWidth = Math.max(maxWidth, g.getTextWidth(label));
            }
            // if there is space on the left of the sourceRect, then use that
            float x=0;
            float menuWidth = maxWidth + border*2;
            if (menuWidth < sourceRect.x) {
                x = sourceRect.x - menuWidth;
            } else {
                x = sourceRect.x + sourceRect.w;
            }

            float txtHeight = g.getTextHeight();
            float menuHeight = txtHeight*items.length + border*2;
            // try to top justify the menu
            float y = sourceRect.y;
            if (y + menuHeight > g.getViewportHeight()) {
                y = g.getViewportHeight() - menuHeight;
            }

            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledRoundedRect(x, y, menuWidth, menuHeight, 10);
            x += border;
            y += border;
            for (int i=0; i<items.length; i++) {
                if (Utils.isPointInsideRect(mx, my, x, y, maxWidth, txtHeight)) {
                    g.setColor(GColor.RED);
                    result = moves.get(i);
                } else {
                    g.setColor(GColor.YELLOW);
                }
            }

            return result;
        }
    }

    @Override
    protected float getInitProgress() {
        return (float)numImagesLoaded / totalImagesToLoad;
    }

    @Override
    protected void onDimensionChanged(AWTGraphics g, int width, int height) {
        GDimension cellDim = getGame().board.initCellRects(g, width-5, height-5);
        int newWidth = (int)cellDim.width * getGame().board.getColumns();
        int newHeight = (int)cellDim.height * getGame().board.getRows();
        setPreferredSize(newWidth, newHeight);
    }

    public ZActor drawActors(AWTGraphics2 g, ZBoard b, int mx, int my) {
        ZActor picked = null;
        boolean animating = false;
        for (ZCell cell : b.getCells()) {
            for (ZCellQuadrant q : ZCellQuadrant.values()) {
                ZActor a = cell.getOccupied(q);
                if (a == null)
                    continue;
                if (a.isAnimating() && overlayToDraw != null)
                    continue;
                AWTImage img = (AWTImage)g.getImage(a.getImageId());
                if (img != null) {
                    GRectangle rect = cell.getQuadrant(q).fit(img).scaledBy(a.getScale());
                    a.setRect(rect);
                    if (rect.contains(mx, my)) {
                        if (picked == null || !(picked instanceof ZCharacter))
                            picked = a;
                    }
                    if (a.isInvisible()) {
                        g.setTransparentcyFilter(.5f);
                    }
                    a.draw(g);
                    if (a.isAnimating()) {
                        animating = true;
                    }
                    g.removeComposite();
                }
            }
        }
        if (animating)
            repaint();
        return picked;
    }

    @Override
    protected synchronized void paint(AWTGraphics g, int mouseX, int mouseY) {
        if (getGame() == null || getGame().board == null)
            return;
        final ZBoard board = getGame().board;
        board.loadCells(g);
        highlightedActor = null;
        highlightedCell = null;
        highlightedResult = null;
        highlightedDoor = null;

        final int OUTLINE = 2;

        final List options = ZombicideApplet.instance.options;
        final Grid.Pos cellPos = board.drawDebug(g, getMouseX(), getMouseY());

        if (ZombicideApplet.instance.gameRunning) {
            int highlightedZone = board.drawZones(g, getMouseX(), getMouseY());
            if (drawTiles) {
                getGame().getQuest().drawTiles(g, board, this);
            }
            highlightedActor = //board.drawActors(g, getMouseX(), getMouseY());
                    drawActors((AWTGraphics2)g, getGame().board, getMouseX(), getMouseY());

            if (getGame().getCurrentCharacter() != null) {
//                if (highlightedActor == getGame().getCurrentCharacter())
  //                  highlightedActor = null; // prevent highlighting an already selected actor
                g.setColor(GColor.GREEN);
                g.drawRect(getGame().getCurrentCharacter().getRect().grownBy(4), OUTLINE);
            }

            g.setColor(GColor.BLACK);
            if (getGame().getQuest()!=null) {
                g.setFont(bigFont);
                g.drawJustifiedString(10, getHeight()-10-g.getTextHeight(), Justify.LEFT, Justify.BOTTOM, getGame().getQuest().getName());
            }
            g.setFont(smallFont);
            g.drawJustifiedString(10, getHeight()-10, Justify.LEFT, Justify.BOTTOM, message);
            switch (ZombicideApplet.instance.uiMode) {
                case PICK_ZOMBIE:
                case PICK_CHARACTER: {
                    g.setColor(GColor.YELLOW);
                    for (ZActor a : (List<ZActor>)options) {
                        a.getRect().drawOutlined(g, 1);
                    }
                    if (options.contains(highlightedActor)) {
                        highlightedResult = highlightedActor;
                    }
                    break;
                }
                case PICK_ZONE: {
                    if (highlightedZone >= 0 && options.contains(highlightedZone)) {
                        highlightedResult = highlightedZone;
                        g.setColor(GColor.YELLOW);
                        board.drawZoneOutline(g, highlightedZone);
                    } else if (cellPos != null) {
                        ZCell cell = board.getCell(cellPos);
                        for (int i = 0; i < options.size(); i++) {
                            if (cell.getZoneIndex() == (Integer)options.get(i)) {
                                highlightedCell = cellPos;
                                highlightedResult = cell.getZoneIndex();
                                break;
                            }
                        }
                    }
                    break;
                }
                case PICK_DOOR: {
                    highlightedResult = getGame().board.pickDoor(g, (List<ZDoor>)options, mouseX, mouseY);
                    break;
                }
            }
            if (highlightedCell != null) {
                ZCell cell = board.getCell(highlightedCell);
                g.setColor(GColor.RED.withAlpha(32));
                board.drawZoneOutline(g, cell.getZoneIndex());
            }
            if (highlightedActor != null) {
                g.setColor(GColor.RED);
                g.drawRect(highlightedActor.getRect().grownBy(2), OUTLINE);
            }
            ZombicideApplet.instance.charComp.repaint();

        } else {
            if (drawTiles) {
                getGame().getQuest().drawTiles(g, board, this);
            }

            if (cellPos != null) {
                highlightedCell = cellPos;
                ZCell cell = board.getCell(cellPos);
                g.setColor(GColor.RED.withAlpha(32));
                board.drawZoneOutline(g, cell.getZoneIndex());
                g.setColor(GColor.RED);
                g.drawRect(cell.getRect());

                List<ZDoor> doors = board.getZone(cell.getZoneIndex()).getDoors();
                highlightedDoor = getGame().board.pickDoor(g, doors, mouseX, mouseY);

                if (selectedCell != null) {
                    ZCell selected = board.getCell(selectedCell);
                    g.setColor(GColor.MAGENTA);
                    selected.getRect().drawOutlined(g, 4);
                    ZCell highlighted = board.getCell(highlightedCell);
                    //Collection<ZDir> dirs = board.getShortestPathOptions(selectedCell, highlighted.getZoneIndex());
                    List<List<ZDir>> paths = board.getShortestPathOptions(selectedCell, highlighted.getZoneIndex());
                    GColor [] colors = new GColor[] { GColor.CYAN, GColor.MAGENTA, GColor.PINK, GColor.ORANGE };
                    int colorIndex = 0;
                    for (List<ZDir> path : paths) {
                        g.setColor(colors[colorIndex]);
                        colorIndex = (colorIndex+1) & colors.length;
                        Grid.Pos cur = selectedCell;
                        g.begin();
                        g.vertex(board.getCell(cur).getRect().getCenter());
                        for (ZDir dir : path) {
                            cur = dir.getAdjacent(cur);
                            g.vertex(board.getCell(cur).getRect().getCenter());
                        }
                        g.drawLineStrip(3);
                    }


                    g.setColor(GColor.CYAN);
//                    g.drawJustifiedStringOnBackground(mouseX, mouseY, Justify.CENTER, Justify.BOTTOM, dirs.toString(), GColor.TRANSLUSCENT_BLACK, 10, 10);
                } else {
                    g.setColor(GColor.CYAN);
                    g.drawJustifiedStringOnBackground(mouseX, mouseY, Justify.CENTER, Justify.BOTTOM, cellPos.toString(), GColor.TRANSLUSCENT_BLACK, 10, 10);
                }
            }

        }

        if (getGame().isGameOver() && overlayToDraw == null) {
            setOverlay(getGame().getGameSummaryTable());
        }

        // overlay
        if (overlayToDraw != null) {
            if (overlayToDraw instanceof Integer) {
                int id = ((Integer) overlayToDraw);
                if (id >= 0) {
                    AImage img = g.getImage(id);
                    GRectangle rect = new GRectangle(0, 0, getWidth(), getHeight());
                    rect.scale(.9f, .9f);
                    rect = rect.fit(img, Justify.LEFT, Justify.CENTER);
                    g.drawImage(id, rect);
                }
            } else if (overlayToDraw instanceof Table) {
                Font font = g.getFont();
                Font fixedWidth = new Font("courier", Font.BOLD, 16);
                g.setFont(fixedWidth);
                g.setColor(GColor.YELLOW);
                GDimension dim = ((Table)overlayToDraw).getDimension(g);
                g.pushMatrix();
                g.translate(getWidth()/2, getHeight()/2);
                g.translate(-dim.width/2, -dim.height/2);
                ((Table)overlayToDraw).draw(g);
                g.popMatrix();
                //g.drawJustifiedStringOnBackground(getWidth() / 2, getHeight() / 2, Justify.CENTER, Justify.CENTER, overlayToDraw.toString(), GColor.TRANSLUSCENT_BLACK, 10, 10);
                g.setFont(font);
            }
        }
    }

    @Override
    protected void onFocusGained() {
        setOverlay(null);
    }

    @Override
    protected void onMousePressed(int mouseX, int mouseY) {
        if (ZombicideApplet.instance.gameRunning) {
            ZombicideApplet.instance.setResult(highlightedResult);
        } else {
            if (highlightedDoor != null) {
                highlightedDoor.toggle(getGame().board);
            } else if (highlightedCell != null) {
                if (highlightedCell.equals(selectedCell)) {
                    selectedCell = null;
                } else {
                    selectedCell = highlightedCell;
                }
            }
        }
    }

    boolean loaded = false;

    void loadImages(AWTGraphics g) {
        if (loaded)
            return;

        //Map<Object, String> fileMap = new HashMap<>();
        Object [][] files = {

            { ZZombieType.Abomination, "zabomination.gif" },
            { ZZombieType.Necromancer, "znecro.gif" },
            { ZZombieType.Walker, "zwalker1.gif" },
            { ZZombieType.Walker, "zwalker2.gif" },
            { ZZombieType.Walker, "zwalker3.gif" },
            { ZZombieType.Walker, "zwalker4.gif" },
            { ZZombieType.Walker, "zwalker5.gif" },
            { ZZombieType.Runner, "zrunner1.gif" },
            { ZZombieType.Runner, "zrunner1.gif" },
            { ZZombieType.Fatty, "zfatty1.gif" },
            { ZZombieType.Fatty, "zfatty2.gif" },
            { ZPlayerName.Clovis, "zchar_clovis.gif" },
            { ZPlayerName.Baldric, "zchar_baldric.gif" },
            { ZPlayerName.Ann, "zchar_ann.gif" },
            { ZPlayerName.Nelly, "zchar_nelly.gif" },
            { ZPlayerName.Samson, "zchar_samson.gif" },
            { ZPlayerName.Silas, "zchar_silas.gif" },
            { ZPlayerName.Tucker, "zchar_tucker.gif" },
            { ZPlayerName.Jain, "zchar_jain.gif" },

            { ZPlayerName.Ann.name(), "zcard_ann.gif" },
            { ZPlayerName.Baldric.name(), "zcard_baldric.gif" },
            { ZPlayerName.Clovis.name(), "zcard_clovis.gif" },
            { ZPlayerName.Nelly.name(), "zcard_nelly.gif" },
            { ZPlayerName.Samson.name(), "zcard_samson.gif" },
            { ZPlayerName.Silas.name(), "zcard_silas.gif" },
            { ZPlayerName.Tucker.name(), "zcard_tucker.gif" },
            { ZPlayerName.Jain.name(), "zcard_jain.gif" },

        };

        Map<Object, List<Integer>> objectToImageMap = new HashMap<>();

        totalImagesToLoad = files.length;
        for (Object [] entry : files) {
            Object key = entry[0];
            String file = (String)entry[1];
            int id = g.loadImage(file, null, 1);
            if (id >= 0) {
                if (!objectToImageMap.containsKey(key)) {
                    objectToImageMap.put(key, new ArrayList<>());
                }
                objectToImageMap.get(key).add(id);
            }
            numImagesLoaded++;
            repaint();
        }

        for (ZZombieType type : ZZombieType.values()) {
            List<Integer> list = objectToImageMap.get(type);
            int [] options = new int[list.size()];
            for (int i=0; i<options.length; i++)
                options[i] = list.get(i);
            type.imageOptions = options;
        }

        for (ZPlayerName pl : ZPlayerName.values()) {
            pl.imageId = objectToImageMap.get(pl).get(0);
            cardImages.put(pl.name(), objectToImageMap.get(pl.name()).get(0));
        }

        log.debug("Images: " + objectToImageMap);
        numImagesLoaded = totalImagesToLoad;
        ZombicideApplet.instance.onAllImagesLoaded();
        repaint();
        loaded = true;
    }

    int [] tiles = new int[0];

    @Override
    public int[] loadTiles(AGraphics _g, String[] names, int[] orientations) {
        AWTGraphics g = (AWTGraphics)_g;
        for (int t : tiles) {
            g.deleteImage(t);
        }

        tiles = new int[names.length];

        for (int i=0; i<names.length; i++) {
            tiles[i] = g.loadImage("ztile_" + names[i] + ".png", orientations[i]);
        }
        return tiles;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        ZGame game = getGame();
        ZCharacter cur = game.getCurrentCharacter();
        if (cur != null) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (game.board.canMove(cur, ZDir.WEST)) {
                        ZombicideApplet.instance.setResult(ZMove.newWalkDirMove(ZDir.WEST));
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (game.board.canMove(cur, ZDir.EAST)) {
                        ZombicideApplet.instance.setResult(ZMove.newWalkDirMove(ZDir.EAST));
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (game.board.canMove(cur, ZDir.NORTH)) {
                        ZombicideApplet.instance.setResult(ZMove.newWalkDirMove(ZDir.NORTH));
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (game.board.canMove(cur, ZDir.SOUTH)) {
                        ZombicideApplet.instance.setResult(ZMove.newWalkDirMove(ZDir.SOUTH));
                    }
                    break;
            }
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_T:
                drawTiles = !drawTiles;
                break;
        }
        repaint();
    }

    public void setOverlay(Object obj) {
        if (obj == null) {
            overlayToDraw = null;
        } else if (obj instanceof Table) {
            overlayToDraw = obj;
        } else if (obj instanceof String){
            overlayToDraw = cardImages.get((String)obj.toString());
        }
        repaint();
    }
}