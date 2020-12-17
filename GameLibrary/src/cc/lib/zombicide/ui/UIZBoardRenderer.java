package cc.lib.zombicide.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.IRectangle;
import cc.lib.game.Justify;
import cc.lib.math.Vector2D;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZAnimation;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCellType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZZone;

public class UIZBoardRenderer extends UIRenderer {

    List<ZAnimation> preActor = new ArrayList<>();
    List<ZAnimation> postActor = new ArrayList<>();
    List<ZAnimation> overlayAnimations = new ArrayList<>();

    Grid.Pos highlightedCell = null;
    Object highlightedResult = null;
    ZActor highlightedActor = null;
    ZDoor highlightedDoor = null;
    Grid.Pos selectedCell = null;
    boolean actorsAnimating = false;
    private Object overlayToDraw = null;

    boolean drawTiles = false;

    public UIZBoardRenderer(UIComponent component) {
        super(component);
    }

    UIZombicide getGame() {
        return UIZombicide.getInstance();
    }

    ZBoard getBoard() {
        return getGame().getBoard();
    }

    public boolean isAnimating() {
        return actorsAnimating || postActor.size() > 0 || preActor.size() > 0 || overlayAnimations.size() > 0;
    }

    void addPreActor(ZAnimation a) {
        synchronized (preActor) {
            preActor.add(a.start());
        }
        redraw();
    }

    void addPostActor(ZAnimation a) {
        synchronized (postActor) {
            postActor.add(a.start());
        }
        redraw();
    }

    void addOverlay(ZAnimation a) {
        synchronized (overlayAnimations) {
            overlayAnimations.add(a.start());
        }
        redraw();
    }

    synchronized void drawAnimations(List<ZAnimation> anims, AGraphics g) {
        synchronized (anims) {
            Iterator<ZAnimation> it = anims.iterator();
            while (it.hasNext()) {
                ZAnimation a = it.next();
                if (a.isDone()) {
                    it.remove();
                } else {
                    a.update(g);
                }
            }
        }
    }

    public ZActor drawActors(AGraphics g, ZBoard b, float mx, float my, boolean drawAnimating) {
        ZActor picked = null;
        float distFromCenter = 0;
        actorsAnimating = false;
        for (ZActor a : b.getAllLiveActors()) {
            if (a.isAnimating() && !drawAnimating)
                continue;
            AImage img = g.getImage(a.getImageId());
            if (img != null) {
                GRectangle rect = a.getRect(b);
                if (rect.contains(mx, my)) {
                    float dist = rect.getCenter().sub(mx, my).magSquared();
                    if (picked == null || !(picked instanceof ZCharacter) || dist < distFromCenter) {
                        picked = a;
                        distFromCenter = dist;
                    }
                }
                if (a.isInvisible()) {
                    g.setTransparencyFilter(.5f);
                }
                a.draw(g);
                if (a.isAnimating()) {
                    actorsAnimating = true;
                }
                g.removeFilter();
            }
        }
        return picked;
    }

    public int pickZone(AGraphics g, int mouseX, int mouseY) {
        for (ZCell cell : getBoard().getCells()) {
            if (cell.contains(mouseX, mouseY)) {
                return cell.getZoneIndex();
            }
        }
        return -1;
    }

    public ZDoor pickDoor(AGraphics g, List<ZDoor> doors, float mouseX, float mouseY) {
        ZDoor picked = null;
        for (ZDoor door : doors) {
            GRectangle doorRect = door.getRect(getBoard()).grow(.1f);
            if (doorRect.contains(mouseX, mouseY)) {
                g.setColor(GColor.RED);
                picked = door;
                // highlight the other side if this is a vault
                g.drawRect(door.getOtherSide().getRect(getBoard()).grow(.1f), 2);
            } else {
                g.setColor(GColor.DARK_OLIVE);
            }
            g.drawRect(doorRect, 2);
        }
        return picked;
    }

    public void drawZoneOutline(AGraphics g, ZBoard board, int zoneIndex) {
        ZZone zone = board.getZone(zoneIndex);
        for (Grid.Pos cellPos : zone.getCells()) {
            g.drawRect(board.getCell(cellPos), 2);
        }
    }

    /**
     * return zone highlighted by mouseX, mouseY
     *
     * @param g
     * @param mouseX
     * @param mouseY
     * @return
     */
    public int drawZones(AGraphics g, float mouseX, float mouseY) {
        final ZBoard board = getBoard();
        int result = -1;
        for (int i=0; i<getBoard().getNumZones(); i++) {
            ZZone zone = board.getZone(i);
            for (Grid.Pos pos : zone.getCells()) {
                ZCell cell = board.getCell(pos);
                if (cell.isCellTypeEmpty())
                    continue;
                switch (zone.type) {
                    case VAULT:
                        g.setColor(GColor.BROWN);
                        g.drawFilledRect(cell);
                        break;
                }
                drawCellWalls(g, pos, 1);
                if (cell.contains(mouseX, mouseY)) {
                    result = i;
                    g.setColor(GColor.RED.withAlpha(32));
                    g.drawFilledRect(cell);
                }
                String text = "";
                for (ZCellType type : ZCellType.values()) {
                    if (cell.isCellType(type)) {
                        switch (type) {
                            case OBJECTIVE_BLACK:
                            case OBJECTIVE_BLUE:
                            case OBJECTIVE_GREEN:
                            case OBJECTIVE_RED:
                                if (zone.objective) {
                                    // draw a big red X om the center of the cell
                                    GRectangle redX = new GRectangle(cell).scaledBy(.25f, .25f);
                                    g.setColor(type.getColor());
                                    g.drawLine(redX.getTopLeft(), redX.getBottomRight(), 10);
                                    g.drawLine(redX.getTopRight(), redX.getBottomLeft(), 10);
                                }
                                break;
                            case EXIT:
                                text += "EXIT";
                                break;
                            case SPAWN_NORTH:
                                if (zone.isSpawn) {
                                    drawSpawn(g, cell, ZDir.NORTH);
                                }
                                break;
                            case SPAWN_SOUTH:
                                if (zone.isSpawn) {
                                    drawSpawn(g, cell, ZDir.SOUTH);
                                }
                                break;
                            case SPAWN_EAST:
                                if (zone.isSpawn) {
                                    drawSpawn(g, cell, ZDir.EAST);
                                }
                                break;
                            case SPAWN_WEST:
                                if (zone.isSpawn) {
                                    drawSpawn(g, cell, ZDir.WEST);
                                }
                                break;
                        }
                    }
                }
                if (zone.isDragonBile()) {
                    g.drawImage(ZIcon.SLIME.imageIds[0], cell);
                }
                if (text.length() > 0) {
                    g.setColor(GColor.YELLOW);
                    g.drawJustifiedStringOnBackground(cell.getCenter(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10, 2);
                }
            }
            if (zone.noiseLevel > 0) {
                g.setColor(GColor.BLACK);
                g.drawJustifiedString(zone.getCenter(board), Justify.CENTER, Justify.CENTER, String.valueOf(zone.noiseLevel));
            }
        }
        ZZone maxNoise = board.getMaxNoiseLevelZone();
        if (maxNoise != null) {
            GColor color = new GColor(GColor.BLACK);
            float radius = 0.5f;
            float dr = radius / 5;
            radius = dr;
            for (int i=0; i<5; i++) {
                g.setColor(color);
                g.drawCircle(maxNoise.getCenter(board), radius, 0);
                color = color.lightened(.1f);
                radius += dr;
            }
        }
        return result;
    }

    void drawSpawn(AGraphics g, IRectangle rect, ZDir dir) {
        int id = ZIcon.SPAWN.imageIds[dir.ordinal()];
        AImage img = g.getImage(id);
        g.drawImage(id, new GRectangle(rect).scale(.8f).fit(img, dir.horz, dir.vert));
    }

    public void drawCellWalls(AGraphics g, Grid.Pos cellPos, float scale) {
        final ZBoard board = getBoard();
        ZCell cell = board.getCell(cellPos);
        g.pushMatrix();
        Vector2D center = cell.getCenter();
        g.translate(center);
        Vector2D v0 = cell.getTopLeft().subEq(center);
        Vector2D v1 = cell.getTopRight().subEq(center);
        g.scale(scale);

        for (ZDir dir : ZDir.getCompassValues()) {
            Vector2D dv = v1.sub(v0).scaleEq(.33f);
            Vector2D dv0 = v0.add(dv);
            Vector2D dv1 = dv0.add(dv);

            g.pushMatrix();
            g.rotate(dir.rotation);
            g.setColor(GColor.BLACK);
            switch (cell.getWallFlag(dir)) {
                case WALL:
                    g.drawLine(v0, v1, 3);
                    break;
                case OPEN: {
                    g.drawLine(v0, dv0, 3);
                    g.drawLine(dv1, v1, 3);
                    break;
                }
                case LOCKED:
                    g.drawLine(v0, v1, 3);
                    g.setColor(getBoard().findDoor(cellPos, dir).getLockedColor());
                    g.drawLine(dv0, dv1, 4);
                    break;
                case CLOSED:
                    g.drawLine(v0, v1, 3);
                    g.setColor(GColor.YELLOW);
                    g.drawLine(dv0, dv1, 4);
                    break;
            }
            g.popMatrix();
        }
        g.popMatrix();

        for (ZDir dir : ZDir.getElevationValues()) {
            switch (cell.getWallFlag(dir)) {
                case LOCKED: {
                    ZDoor door = board.findDoor(cellPos, dir);
                    g.setColor(GColor.BLACK);
                    GRectangle vaultRect = door.getRect(board);
                    g.setColor(GColor.ORANGE);
                    vaultRect.drawFilled(g);
                    g.setColor(GColor.RED);
                    vaultRect.drawOutlined(g, 2);
//                    g.drawJustifiedString(vaultRect.getCenter(), Justify.CENTER, Justify.CENTER, "LOCKED");
                    break;
                }
                case CLOSED: {
                    ZDoor door = board.findDoor(cellPos, dir);
                    g.setColor(GColor.BLACK);
                    GRectangle vaultRect = door.getRect(board);
                    g.setColor(door.lockedColor);
                    vaultRect.drawFilled(g);
                    g.setColor(GColor.YELLOW);
                    vaultRect.drawOutlined(g, 2);
//                    g.drawJustifiedString(vaultRect.getCenter(), Justify.CENTER, Justify.CENTER, "VAULT");
                    break;
                }
                case OPEN: {
                    ZDoor door = board.findDoor(cellPos, dir);
                    g.setColor(GColor.BLACK);
                    GRectangle vaultRect = door.getRect(board);
                    vaultRect.drawFilled(g);
                    // draw the 'lid' opened
                    g.begin();
                    g.vertex(vaultRect.getTopRight());
                    g.vertex(vaultRect.getTopLeft());
                    float dh = vaultRect.h/3;
                    float dw = vaultRect.w/5;
                    if (dir == ZDir.ASCEND) {
                        // open 'up'
                        g.moveTo(-dw, -dh);
                        g.moveTo(vaultRect.w+dw*2, 0);
                    } else {
                        // open 'down
                        g.moveTo(dw, dh);
                        g.moveTo(vaultRect.w-dw*2, 0);
                    }
                    g.setColor(door.lockedColor);
                    g.drawTriangleFan();
                    g.setColor(GColor.YELLOW);
                    g.end();
                    vaultRect.drawOutlined(g, 2);
                    g.drawLineLoop(2);
                }
            }
        }
    }

    public Grid.Pos pickCell(AGraphics g, float mouseX, float mouseY) {
        Grid.Iterator<ZCell> it = getBoard().getCellsIterator();
        while (it.hasNext()) {
            ZCell cell = it.next();
            if (cell.isCellTypeEmpty())
                continue;
            if (cell.contains(mouseX, mouseY)) {
                return it.getPos();
            }
        }
        return null;
    }


    public Grid.Pos drawDebug(AGraphics g, float mouseX, float mouseY) {
        g.clearScreen(GColor.LIGHT_GRAY);
        Grid.Pos returnCell = null;

        Grid.Iterator<ZCell> it = getBoard().getCellsIterator();
        while (it.hasNext()) {
            ZCell cell = it.next();
            if (cell.isCellTypeEmpty())
                continue;
            switch (cell.getEnvironment()) {
                case ZCell.ENV_BUILDING:
                    g.setColor(GColor.ORANGE); break;
                case ZCell.ENV_OUTDOORS:
                    g.setColor(GColor.LIGHT_GRAY); break;
                case ZCell.ENV_VAULT:
                    g.setColor(GColor.BROWN); break;
            }
            g.drawFilledRect(cell);
            drawCellWalls(g, it.getPos(), .97f);
            String text = "Zone " + cell.getZoneIndex();
            for (ZCellType type : ZCellType.values()) {
                if (cell.isCellType(type)) {
                    switch (type) {
                        case NONE:
                        case VAULT_DOOR_VIOLET:
                        case VAULT_DOOR_GOLD:
                            break;
                        case OBJECTIVE_RED:
                        case OBJECTIVE_BLUE:
                        case OBJECTIVE_GREEN:
                        case OBJECTIVE_BLACK:
                            text += "\n" + type.name().substring(10);
                            break;
                        case SPAWN_NORTH:
                        case SPAWN_SOUTH:
                        case SPAWN_EAST:
                        case SPAWN_WEST:
                            text += "\nSPAWN";
                            break;
                        default:
                            text += "\n" + type;
                    }
                }
            }
            /*
            if (cell.rect.contains(mouseX, mouseY)) {
                List<Integer> accessible = getAccessableZones(cell.zoneIndex, 1, ZActionType.MOVE);
                text = "1 Unit away:\n" + accessible;
                returnCell = it.getPos();//new int[] { col, row };
                List<Integer> accessible2 = getAccessableZones(cell.zoneIndex, 2, ZActionType.MAGIC);
                text += "\n2 Units away:\n" + accessible2;
            }*/
            g.setColor(GColor.CYAN);
            for (ZActor a : cell.getOccupant()) {
                if (a != null) {
                    text += "\n" + a.name();
                }
            }
            if (cell.getVaultFlag() > 0) {
                text += "\nvaultFlag " + cell.getVaultFlag();
            }
            g.drawJustifiedStringOnBackground(cell.getCenter(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10, 3);

        }
        return returnCell;
    }


    @Override
    public void draw(APGraphics g, int _mouseX, int _mouseY) {
        highlightedActor = null;
        highlightedCell = null;
        highlightedResult = null;
        highlightedDoor = null;

        UIZombicide game = getGame();
        ZBoard board = getBoard();
        g.setIdentity();
        g.ortho();
        g.pushMatrix();
        g.translate(getWidth()/2,0);// - dim.width/2, 0);

        GDimension dim = board.getDimension();
        g.scale(getHeight() / dim.height);
        g.translate(-dim.width/2, 0);

        Vector2D mouse = g.screenToViewport(_mouseX, _mouseY);
        //log.debug("mouse %d,%d -> %s", _mouseX, _mouseY, mouse);

        final int OUTLINE = 2;

        //final Grid.Pos cellPos = board.drawDebug(g, mouse.X(), mouse.Y());

        if (game.isGameRunning() || game.isGameOver()) {
            Grid.Pos cellPos = null;
            if (drawTiles) {
                game.getQuest().drawTiles(g, board, game);
                cellPos = pickCell(g, mouse.X(), mouse.Y());
            } else {
                cellPos = drawDebug(g, mouse.X(), mouse.Y());
            }
            int highlightedZone = drawZones(g, mouse.X(), mouse.Y());
            boolean drawAnimating = game.isGameOver();

            drawAnimations(preActor, g);

            highlightedActor = //board.drawActors(g, getMouseX(), getMouseY());
                    drawActors(g, board, mouse.X(), mouse.Y(), drawAnimating || overlayToDraw == null);

            drawAnimations(postActor, g);

            //if (drawZoneAnimations(g))
            //    repaint();

            if (game.getCurrentCharacter() != null) {
//                if (highlightedActor == getGame().getCurrentCharacter())
                //                  highlightedActor = null; // prevent highlighting an already selected actor
                g.setColor(GColor.GREEN);
                g.drawRect(game.getCurrentCharacter().getRect(board).scale(1.02f), OUTLINE);
            }

            g.setColor(GColor.BLACK);
            if (game.getQuest()!=null) {
                float height = g.getTextHeight();
                g.setTextHeight(24);//setFont(bigFont);
                g.setTextStyles(AGraphics.TextStyle.BOLD, AGraphics.TextStyle.ITALIC);
                g.drawJustifiedString(10, getHeight()-10-g.getTextHeight(), Justify.LEFT, Justify.BOTTOM, game.getQuest().getName());
                g.setTextHeight(height);
                g.setTextStyles(AGraphics.TextStyle.NORMAL);
            }
            //g.setFont(smallFont);
            g.drawJustifiedString(10, getHeight()-10, Justify.LEFT, Justify.BOTTOM, game.message);
            switch (game.uiMode) {
                case PICK_ZOMBIE:
                case PICK_CHARACTER: {
                    g.setColor(GColor.YELLOW);
                    for (ZActor a : (List<ZActor>)game.options) {
                        a.getRect(board).drawOutlined(g, 1);
                    }
                    if (game.options.contains(highlightedActor)) {
                        highlightedResult = highlightedActor;
                    }
                    break;
                }
                case PICK_ZONE: {
                    if (highlightedZone >= 0 && game.options.contains(highlightedZone)) {
                        highlightedResult = highlightedZone;
                        g.setColor(GColor.YELLOW);
                        drawZoneOutline(g, board, highlightedZone);
                    } else if (cellPos != null) {
                        ZCell cell = board.getCell(cellPos);
                        for (int i = 0; i < game.options.size(); i++) {
                            if (cell.getZoneIndex() == (Integer)game.options.get(i)) {
                                highlightedCell = cellPos;
                                highlightedResult = cell.getZoneIndex();
                                break;
                            }
                        }
                    }
                    break;
                }
                case PICK_DOOR: {
                    highlightedResult = pickDoor(g, (List<ZDoor>)game.options, mouse.X(), mouse.Y());
                    break;
                }
            }
            if (highlightedCell != null) {
                ZCell cell = board.getCell(highlightedCell);
                g.setColor(GColor.RED.withAlpha(32));
                drawZoneOutline(g, board, cell.getZoneIndex());
            }
            if (highlightedActor != null) {
                g.setColor(GColor.RED);
                g.drawRect(highlightedActor.getRect(board).scale(1.01f), OUTLINE);
            }

            game.characterRenderer.redraw();


        } else {

            Grid.Pos cellPos = drawDebug(g, mouse.X(), mouse.Y());
            if (drawTiles) {
                game.getQuest().drawTiles(g, board, game);
            }

            if (cellPos != null) {
                highlightedCell = cellPos;
                ZCell cell = board.getCell(cellPos);
                g.setColor(GColor.RED.withAlpha(32));
                drawZoneOutline(g, board, cell.getZoneIndex());
                g.setColor(GColor.RED);
                g.drawRect(cell);

                List<ZDoor> doors = board.getZone(cell.getZoneIndex()).getDoors();
                highlightedDoor = pickDoor(g, doors, mouse.X(), mouse.Y());

                if (selectedCell != null) {
                    ZCell selected = board.getCell(selectedCell);
                    g.setColor(GColor.MAGENTA);
                    selected.drawOutlined(g, 4);
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
                        g.vertex(board.getCell(cur).getCenter());
                        for (ZDir dir : path) {
                            cur = board.getAdjacent(cur, dir);///dir.getAdjacent(cur);
                            g.vertex(board.getCell(cur).getCenter());
                        }
                        g.drawLineStrip(3);
                    }


                    g.setColor(GColor.CYAN);
//                    g.drawJustifiedStringOnBackground(mouseX, mouseY, Justify.CENTER, Justify.BOTTOM, dirs.toString(), GColor.TRANSLUSCENT_BLACK, 10, 10);
                } else {
                    g.setColor(GColor.CYAN);
                    g.drawJustifiedStringOnBackground(mouse.X(), mouse.Y(), Justify.CENTER, Justify.BOTTOM, cellPos.toString(), GColor.TRANSLUSCENT_BLACK, 10, 10);
                }
            }

        }

        g.popMatrix();
        drawAnimations(overlayAnimations, g);

        if (game.isGameOver() && overlayToDraw == null && !isAnimating()) {
            setOverlay(game.getGameSummaryTable());
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
                g.setColor(GColor.YELLOW);
                dim = ((Table)overlayToDraw).getDimension(g);
                g.pushMatrix();
                g.translate(getWidth()/2, getHeight()/2);
                g.translate(-dim.width/2, -dim.height/2);
                ((Table)overlayToDraw).draw(g);
                g.popMatrix();
            }
        }

        if (isAnimating())
            redraw();
    }

    public void setOverlay(Object obj) {
        if (obj == null) {
            overlayToDraw = null;
        } else if (obj instanceof Table) {
            overlayToDraw = obj;
        } else if (obj instanceof ZPlayerName){
            overlayToDraw = ((ZPlayerName)obj).cardImageId;
        }
        redraw();
    }

    @Override
    public void onClick() {
        if (getGame().isGameRunning()) {
            overlayToDraw = null;
            getGame().setResult(highlightedResult);
        } else {
            if (highlightedDoor != null) {
                highlightedDoor.toggle(getBoard());
            } else if (highlightedCell != null) {
                if (highlightedCell.equals(selectedCell)) {
                    selectedCell = null;
                } else {
                    selectedCell = highlightedCell;
                }
            }
        }
    }

    public void setDrawTiles(boolean draw) {
        this.drawTiles = draw;
    }

    public void toggleDrawTiles() {
        this.drawTiles = !this.drawTiles;
    }

    public ZActor getHighlightedActor() {
        return highlightedActor;
    }

}
