package cc.applets.zombicide;

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
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
    Map<Object, Integer> objectToImageMap = new HashMap<>();
    private Integer overlayToDraw = null;
    private Table overlayTable = null;

    BoardComponent() {
        setPreferredSize(250, 250);
    }

    ZGame getGame() {
        return ZombicideApplet.instance.game;
    }

    @Override
    protected void init(AWTGraphics g) {
        GDimension cellDim = getGame().board.initCellRects(g, g.getViewportWidth()-5, g.getViewportHeight()-5);
        setMouseEnabled(true);
        //setMinimumSize(256, 256);
        setPreferredSize( (int)cellDim.width * getGame().board.getColumns(), (int)cellDim.height * getGame().board.getRows());
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

    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        if (getGame() == null || getGame().board == null)
            return;
        final ZBoard board = getGame().board;
        board.loadCells(g);
        highlightedActor = null;
        highlightedCell = null;
        highlightedResult = null;
        highlightedDoor = null;

        final List options = ZombicideApplet.instance.options;
        final Grid.Pos cellPos = board.drawDebug(g, getMouseX(), getMouseY());

        if (drawTiles) {
            getGame().getQuest().drawTiles(g, board, this);
        }

        if (ZombicideApplet.instance.gameRunning) {
            int highlightedZone = board.drawZones(g, getMouseX(), getMouseY());
            highlightedActor = board.drawActors(g, this, getMouseX(), getMouseY());

            if (getGame().getCurrentCharacter() != null) {
                g.setColor(GColor.GREEN);
                g.drawRect(getGame().getCurrentCharacter().getRect(), 1);
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
                    highlightedResult = pickDoor(g, (List<ZDoor>)options, mouseX, mouseY);
                    break;
                }
            }
            if (highlightedCell != null) {
                ZCell cell = board.getCell(highlightedCell);
                g.setColor(GColor.RED.withAlpha(32));
                board.drawZoneOutline(g, cell.getZoneIndex());
            }
            if (highlightedActor != null) {
                g.setColor(GColor.YELLOW);
                g.drawRect(highlightedActor.getRect());
            }
            ZombicideApplet.instance.charComp.repaint();

        } else {
            if (cellPos != null) {
                highlightedCell = cellPos;
                ZCell cell = board.getCell(cellPos);
                g.setColor(GColor.RED.withAlpha(32));
                board.drawZoneOutline(g, cell.getZoneIndex());
                g.setColor(GColor.RED);
                g.drawRect(cell.getRect());

                List<ZDoor> doors = board.getZone(cell.getZoneIndex()).getDoors();
                highlightedDoor = pickDoor(g, doors, mouseX, mouseY);
                if (highlightedDoor != null) {
                    highlightedDoor.draw(g, board);
                }

                if (selectedCell != null) {
                    ZCell selected = board.getCell(selectedCell);
                    g.setColor(GColor.MAGENTA);
                    selected.getRect().drawOutlined(g, 4);
                    ZCell highlighted = board.getCell(highlightedCell);
                    Collection<ZDir> dirs = board.getShortestPathOptions(selectedCell, highlighted.getZoneIndex());
                    g.setColor(GColor.CYAN);
                    g.drawJustifiedStringOnBackground(mouseX, mouseY, Justify.CENTER, Justify.BOTTOM, dirs.toString(), GColor.TRANSLUSCENT_BLACK, 10, 10);
                } else {
                    g.setColor(GColor.CYAN);
                    g.drawJustifiedStringOnBackground(mouseX, mouseY, Justify.CENTER, Justify.BOTTOM, cellPos.toString(), GColor.TRANSLUSCENT_BLACK, 10, 10);
                }
            }

        }

        // overlay
        if (overlayToDraw != null && overlayToDraw >= 0) {
            AImage img = g.getImage(overlayToDraw);
            GRectangle rect = new GRectangle(0, 0, getWidth(), getHeight());
            rect.scale(.9f, .9f);
            rect = rect.fit(img, Justify.LEFT, Justify.CENTER);
            g.drawImage(overlayToDraw, rect);
        }

        if (overlayTable != null) {
            Font font = g.getFont();
            Font fixedWidth = new Font("monospaced", Font.PLAIN, 16);
            g.setFont(fixedWidth);
            g.setColor(GColor.YELLOW);
            g.drawJustifiedStringOnBackground(getWidth()/2, getHeight()/2, Justify.CENTER, Justify.CENTER, overlayTable.toString(), GColor.TRANSLUSCENT_BLACK, 10, 10);
            g.setFont(font);
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

    ZDoor pickDoor(AWTGraphics g, List<ZDoor> doors, int mouseX, int mouseY) {
        ZDoor picked = null;
        for (ZDoor door : doors) {
            ZCell cell = getGame().board.getCell(door.getCellPos());
            GRectangle doorRect = door.getRect(getGame().board).grownBy(10);
            if (doorRect.contains(mouseX, mouseY)) {
                g.setColor(GColor.RED);
                picked = door;
            } else {
                g.setColor(GColor.CYAN);
            }
            g.drawRect(doorRect, 1);
        }
        return picked;
    }

    boolean loaded = false;

    void loadImages(AWTGraphics g) {
        if (loaded)
            return;

        Map<Object, String> fileMap = new HashMap<>();
        fileMap.put(ZZombieType.Abomination,"zabomination.png");
        fileMap.put(ZZombieType.Necromancer,"znecro.png");
        fileMap.put(ZZombieType.Walker1,"zwalker1.png");
        fileMap.put(ZZombieType.Walker2,"zwalker2.png");
        fileMap.put(ZZombieType.Walker3,"zwalker3.png");
        fileMap.put(ZZombieType.Walker4,"zwalker4.png");
        fileMap.put(ZZombieType.Walker5,"zwalker5.png");
        fileMap.put(ZZombieType.Runner1,"zrunner1.png");
        fileMap.put(ZZombieType.Runner2,"zrunner1.png");
        fileMap.put(ZZombieType.Fatty1,"zfatty1.png");
        fileMap.put(ZZombieType.Fatty2,"zfatty2.png");
        fileMap.put(ZPlayerName.Clovis,"zchar_clovis.png");
        fileMap.put(ZPlayerName.Baldric,"zchar_baldric.png");
        fileMap.put(ZPlayerName.Ann,"zchar_ann.png");
        fileMap.put(ZPlayerName.Nelly,"zchar_nelly.png");
        fileMap.put(ZPlayerName.Samson,"zchar_samson.png");
        fileMap.put(ZPlayerName.Silas,"zchar_silas.png");
        fileMap.put(ZPlayerName.Ann.name(), "zcard_ann.png");
        fileMap.put(ZPlayerName.Baldric.name(), "zcard_baldric.png");
        fileMap.put(ZPlayerName.Clovis.name(), "zcard_clovis.png");
        fileMap.put(ZPlayerName.Nelly.name(), "zcard_nelly.png");
        fileMap.put(ZPlayerName.Samson.name(), "zcard_samson.png");
        fileMap.put(ZPlayerName.Silas.name(), "zcard_silas.png");

        totalImagesToLoad = fileMap.size();
        for (Map.Entry<Object, String> e : fileMap.entrySet()) {
            int id = g.loadImage(e.getValue());
            if (id >= 0)
                objectToImageMap.put(e.getKey(), id);
            numImagesLoaded++;
            repaint();
        }

        log.debug("Images: " + objectToImageMap);

        numImagesLoaded = totalImagesToLoad;
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
            switch (names[i]) {
                case "4V":
                    tiles[i] = g.loadImage("ztile1.png", orientations[i]);
                    break;
                case "9R":
                    tiles[i] = g.loadImage("ztile8.png", orientations[i]);
                    break;
                case "2R":
                    tiles[i] = g.loadImage("ztile9.png", orientations[i]);
                    break;
                case "8V":
                    tiles[i] = g.loadImage("ztile7.png", orientations[i]);
                    break;
                case "9V":
                    tiles[i] = g.loadImage("ztile6.png", orientations[i]);
                    break;
                case "1V":
                    tiles[i] = g.loadImage("ztile3.png", orientations[i]);
                    break;


            }

        }
        return tiles;
    }

    @Override
    public int getImage(Object obj) {
        log.debug("getImage: " + obj);
        Integer id = objectToImageMap.get(obj);
        if (id == null)
            return -1;
        return id;
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
            overlayTable = (Table)obj;
        } else {
            overlayToDraw = objectToImageMap.get(obj);
        }
        repaint();
    }
}