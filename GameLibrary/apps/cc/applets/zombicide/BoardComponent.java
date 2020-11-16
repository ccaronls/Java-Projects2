package cc.applets.zombicide;

import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;
import cc.lib.utils.Grid;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZTiles;
import cc.lib.zombicide.ZZombieType;

class BoardComponent extends AWTComponent implements ZTiles {

    String message = "";
    Grid.Pos highlightedCell = null;
    Object highlightedResult = null;
    ZActor highlightedActor = null;
    ZDoor highlightedDoor = null;
    Grid.Pos selectedCell = null;

    BoardComponent() {
        setPreferredSize(250, 250);
    }

    ZGame getGame() {
        return ZombicideApplet.instance.game;
    }

    @Override
    protected void init(AWTGraphics g) {
        GDimension cellDim = getGame().board.initCellRects(g, g.getViewportWidth()-5, g.getViewportHeight()-5);
        loadImages(g);
        setMouseEnabled(true);
        //setMinimumSize(256, 256);
        setPreferredSize( (int)cellDim.width * getGame().board.getColumns(), (int)cellDim.height * getGame().board.getRows());
        //setMaximumSize( (int)cellDim.width * getGame().board.getColumns(), (int)cellDim.height * getGame().board.getRows());
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
        ZBoard board = getGame().board;
        highlightedActor = null;
        highlightedCell = null;
        highlightedResult = null;
        highlightedDoor = null;

        List options = ZombicideApplet.instance.options;

        Grid.Pos cellPos = board.drawDebug(g, getMouseX(), getMouseY());

        if (ZombicideApplet.instance.gameRunning) {
            //game.getQuest().drawTiles(g, ZombicideApplet.this);

            int highlightedZone = board.drawZones(g, getMouseX(), getMouseY());
            highlightedActor = board.drawActors(g, getMouseX(), getMouseY());

            g.setColor(GColor.BLACK);
            g.drawJustifiedString(getWidth()-10, getHeight()-10, Justify.RIGHT, Justify.BOTTOM, message);
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
                ZombicideApplet.instance.charComp.repaint();
            }

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

                if (selectedCell != null) {
                    ZCell selected = board.getCell(selectedCell);
                    g.setColor(GColor.MAGENTA);
                    selected.getRect().drawOutlined(g, 5);
                    ZCell highlighted = board.getCell(highlightedCell);
                    List<Integer> dirs = board.getShortestPathOptions(selected.getZoneIndex(), highlighted.getZoneIndex());
                    g.setColor(GColor.CYAN);
                    g.drawJustifiedStringOnBackground(mouseX, mouseY, Justify.CENTER, Justify.CENTER, dirs.toString(), GColor.TRANSLUSCENT_BLACK, 10, 10);
                }
            }

        }
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
        ZZombieType.Abomination.imageId = g.loadImage("zabomination.png");
        ZZombieType.Necromancer.imageId = g.loadImage("znecro.png");
        ZZombieType.Walker1.imageId = g.loadImage("zwalker1.png");
        ZZombieType.Walker2.imageId = g.loadImage("zwalker2.png");
        ZZombieType.Walker3.imageId = g.loadImage("zwalker3.png");
        ZZombieType.Walker4.imageId = g.loadImage("zwalker4.png");
        ZZombieType.Walker5.imageId = g.loadImage("zwalker5.png");
        ZZombieType.Runner1.imageId = g.loadImage("zrunner1.png");
        ZZombieType.Runner2.imageId = g.loadImage("zrunner1.png");
        ZZombieType.Fatty1.imageId = g.loadImage("zfatty1.png");
        ZZombieType.Fatty2.imageId = g.loadImage("zfatty2.png");
        ZPlayerName.Clovis.imageId = g.loadImage("zchar_clovis.png");
        ZPlayerName.Baldric.imageId = g.loadImage("zchar_baldric.png");
        ZPlayerName.Ann.imageId = g.loadImage("zchar_ann.png");
        ZPlayerName.Nelly.imageId = g.loadImage("zchar_nelly.png");
        ZPlayerName.Samson.imageId = g.loadImage("zchar_samson.png");
        ZPlayerName.Silas.imageId = g.loadImage("zchar_silas.png");
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
            }

        }
        return tiles;
    }

}