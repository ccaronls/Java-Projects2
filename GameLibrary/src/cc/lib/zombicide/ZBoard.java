package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public class ZBoard extends Reflector<ZBoard> {

    static {
        addAllFields(ZBoard.class);
    }

    public static final int DIR_NORTH = 0;
    public static final int DIR_SOUTH = 1;
    public static final int DIR_EAST  = 2;
    public static final int DIR_WEST  = 3;

    public static final int [] DIR_DX = {  0, 0, 1,-1 };
    public static final int [] DIR_DY = { -1, 1, 0, 0 };

    public static final int [] DIR_ROTATION = { 0, 180, 90, 270 };
    public static final int [] DIR_OPPOSITE = { DIR_SOUTH, DIR_NORTH, DIR_WEST, DIR_EAST };

    Grid<ZCell> grid; // TODO: Use Grid Container Type
    List<ZZone> zones;

    public ZBoard() {

    }

    public ZBoard(Grid<ZCell> grid, List<ZZone> zones) {
        this.grid = grid;
        this.zones = zones;
        //removeEmptyZones();
    }

    public int getRows() {
        return grid.getRows();
    }

    public int getColumns() {
        return grid.getCols();
    }

    public ZZone getZone(int index) {
        return zones.get(index);
    }


    Iterable<ZZone> getZones() {
        return zones;
    }

    public List<Integer> getAccessableZones(int zoneIndex, int distance) {
        if (distance == 0)
            return Collections.singletonList(zoneIndex);

        List<Integer> result = new ArrayList<>();
        for (Grid.Pos cellPos : zones.get(zoneIndex).cells) {
            // fan out in all direction to given distance
            for (int dir = 0; dir <4; dir++) {
                int x = cellPos.getColumn();
                int y = cellPos.getRow();
                boolean open = true;
                for (int dist = 0; dist <distance; dist++) {
                    if (!grid.get(y,x).walls[dir].isOpen()) {
                        open = false;
                        break;
                    }
                    x += DIR_DX[dir];
                    y += DIR_DY[dir];
                }
                if (open && grid.get(y,x).zoneIndex != zoneIndex) {
                    result.add(grid.get(y,x).zoneIndex);
                }
            }
        }
        return result;
    }

    public boolean canSee(int fromZone, int toZone) {
        for (Grid.Pos pos0: zones.get(fromZone).cells) {
            for (Grid.Pos pos1: zones.get(toZone).cells) {
                if (canSeeCell(pos0, pos1))
                    return true;
            }
        }
        return false;
    }

    public boolean canSeeCell(Grid.Pos fromCell, Grid.Pos toCell) {
        int fromCellX = fromCell.getColumn();
        int fromCellY = fromCell.getRow();
        int toCellX = toCell.getColumn();
        int toCellY = toCell.getRow();

        if (fromCellX != toCellX && fromCellY != toCellY)
            return false; // can only see horz and vertically

        if (fromCellX == toCellX && fromCellY == toCellY)
            return true; // TODO: Should we be able to see inside our own?

        int dir = getDirFrom(fromCell, toCell);

        int zoneChanges = 0;
        int curZoneId = grid.get(fromCellY, fromCellX).zoneIndex;

        int tx = fromCellX;
        int ty = fromCellY;

        while (tx != toCellX || ty != toCellY) {
            ZCell cell = grid.get(ty, tx);
            // can only see 1 one zone difference
            if (cell.isInside && cell.zoneIndex != curZoneId) {
                if (++zoneChanges > 1)
                    return false;
                curZoneId = cell.zoneIndex;
            }

            if (!cell.walls[dir].isOpen()) {
                return false;
            }

            tx += DIR_DX[dir];
            ty += DIR_DY[dir];
        }

        return true;
    }

    private int getDirFrom(Grid.Pos fromCell, Grid.Pos toCell) {
        int fromCellX = fromCell.getColumn();
        int fromCellY = fromCell.getRow();
        int toCellX = toCell.getColumn();
        int toCellY = toCell.getRow();

        int dx = toCellX > fromCellX ? 1 : (fromCellX > toCellX ? -1 : 0);
        int dy = toCellY > fromCellY ? 1 : (fromCellY > toCellY ? -1 : 0);

        if (dx != 0 && dy != 0) {
            throw new AssertionError("No direction for diagonals");
        }

        if (dx < 0)
            return DIR_WEST;
        else if (dx > 0)
            return DIR_EAST;
        else if (dy < 0)
            return DIR_NORTH;
        return DIR_SOUTH;
    }

    /**
     * Returns a list of directions the zombie can move
     * @See DIR_NORTH, DIR_SOUTH, DIR_EAST, DIR_WEST
     *
     * @param fromZone
     * @param toZone
     * @return
     */
    public List<Integer> getShortestPathOptions(int fromZone, int toZone) {
        ZZone a = zones.get(fromZone);
        ZZone b = zones.get(toZone);

        List<List<Integer>> allPaths = new ArrayList<>();
        int maxDist = grid.getRows() +  grid.getCols();

        for (Grid.Pos aCellPos : a.cells) {
            for (Grid.Pos bCellPos : b.cells) {
                List<List<Integer>> paths = getShortestPathOptions(aCellPos, bCellPos, maxDist);
                for (List l : paths) {
                    maxDist = Math.min(maxDist, l.size());
                }
                allPaths.addAll(paths);
            }
        }

        Iterator<List<Integer>> it = allPaths.iterator();
        while (it.hasNext()) {
            if (it.next().size() > maxDist)
                it.remove();
        }

        List<Integer> dirs = new ArrayList<>();
        for (List<Integer> p : allPaths) {
            dirs.add(p.get(0));
        }

        return dirs;
    }

    private List<List<Integer>> getShortestPathOptions(Grid.Pos fromCell, Grid.Pos toCell, int maxDist) {
        List<List<Integer>> paths = new ArrayList<>();
        searchPathsR(fromCell, toCell, new int[] { maxDist }, new LinkedList<>(), paths);
        return paths;
    }

    private void searchPathsR(Grid.Pos start, Grid.Pos end, int [] maxDist, LinkedList<Integer> curPath, List<List<Integer>> paths) {
        if (start.equals(end)) {
            if (curPath.size() > 0) {
                paths.add(new ArrayList<>(curPath));
                maxDist[0] = Math.min(maxDist[0], curPath.size());
            }
            return;
        }
        if (curPath.size() >= maxDist[0])
            return;
        ZCell fromCell = grid.get(start);
        for (int dir = 0; dir <4; dir++) { // todo: order the directions we search
            if (!fromCell.walls[dir].isOpen()) {
                continue;
            }
            int x = start.getColumn() + DIR_DX[dir];
            int y = start.getRow() + DIR_DY[dir];
            curPath.addLast(dir);
            searchPathsR(new Grid.Pos(y, x), end, maxDist, curPath, paths);
            curPath.removeLast();
        }
    }

    public ZCell getCell(Grid.Pos pos) {
        return grid.get(pos);
    }

    public ZCell getCell(int row, int col) {
        return grid.get(row, col);
    }

    ZDoor getOtherSide(ZDoor door) {
        return new ZDoor(door.cellPos.getRow() + DIR_DY[door.dir],
                door.cellPos.getColumn() + DIR_DX[door.dir],
                DIR_OPPOSITE[door.dir]);
    }

    public ZWallFlag getDoor(ZDoor door) {
        return getCell(door.cellPos).walls[door.dir];
    }

    public void setDoor(ZDoor door, ZWallFlag flag) {
        getCell(door.cellPos).walls[door.dir] = flag;
        door = getOtherSide(door);
        getCell(door.cellPos).walls[door.dir] = flag;
    }

    public void toggleDoor(ZDoor door) {
        switch (getDoor(door)) {
            case OPEN:
                setDoor(door, ZWallFlag.CLOSED);
                break;
            case CLOSED:
                setDoor(door, ZWallFlag.OPEN);
                break;
        }
    }

    public int pickZone(AGraphics g, int mouseX, int mouseY) {
        for (ZCell cell : grid.getCells()) {
            if (cell.getRect().contains(mouseX, mouseY)) {
                return cell.zoneIndex;
            }
        }
        return -1;
    }

    public ZActor pickActor(AGraphics g, int mouseX, int mouseY) {
        return null;
    }

    /**
     * return zone highlighted by mouseX, mouseY
     *
     * @param g
     * @param mouseX
     * @param mouseY
     * @return
     */
    public int drawZones(AGraphics g, int mouseX, int mouseY) {
        int result = -1;
        for (int i=0; i<zones.size(); i++) {
            ZZone zone = zones.get(i);
            for (Grid.Pos pos : zone.cells) {
                ZCell cell = getCell(pos);
                if (zone.searchable) {
                    g.setColor(GColor.ORANGE);
                } else {
                    g.setColor(GColor.LIGHT_GRAY);
                }
                g.drawFilledRect(cell.rect);
                drawCellWalls(g, cell, 1);
                if (cell.rect.contains(mouseX, mouseY)) {
                    result = i;
                    g.setColor(GColor.RED.withAlpha(32));
                    g.drawFilledRect(cell.rect);
                }
                String text = "";
                switch (cell.cellType) {
                    case OBJECTIVE:
                        if (zone.objective) {
                            // draw a big red X om the center of the cell
                            GRectangle redX = cell.rect.scaledBy(.25f, .25f);
                            g.setColor(GColor.RED);
                            g.drawLine(redX.getTopLeft(), redX.getBottomRight(), 10);
                            g.drawLine(redX.getTopRight(), redX.getBottomLeft(), 10);
                        }
                        break;
                    case EXIT:
                        text += "EXIT";
                        break;
                    case VAULT:
                        if (zone.vault) {
                            text += "VAULT";
                        }
                        break;
                    case SPAWN:
                        if (zone.isSpawn) {
                            text += "SPAWN";
                        }
                        break;
                }
                if (text.length() > 0) {
                    g.setColor(GColor.YELLOW);
                    g.drawJustifiedStringOnBackground(cell.rect.getCenter(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10, 2);
                }
            }
            if (zone.noiseLevel > 0) {
                g.setColor(GColor.CYAN);
                g.drawJustifiedStringOnBackground(getCell(zone.cells.get(0)).rect.getTopLeft().addEq(10, 10), Justify.LEFT, Justify.TOP, String.valueOf(zone.noiseLevel), GColor.TRANSLUSCENT_BLACK, 10, 10);
            }

        }
        return result;
    }

    public void drawCellWalls(AGraphics g, ZCell cell, float scale) {
        g.pushMatrix();
        Vector2D center = cell.rect.getCenter();
        g.translate(center);
        Vector2D v0 = cell.rect.getTopLeft().subEq(center);
        Vector2D v1 = cell.rect.getTopRight().subEq(center);
        g.scale(scale);

        for (int d=0; d<4; d++) {
            Vector2D dv = v1.sub(v0).scaleEq(.33f);
            Vector2D dv0 = v0.add(dv);
            Vector2D dv1 = dv0.add(dv);

            g.pushMatrix();
            g.rotate(DIR_ROTATION[d]);
            g.setColor(GColor.BLACK);
            switch (cell.walls[d]) {
                case WALL:
                    g.drawLine(v0, v1, 3);
                    break;
                case OPEN:
                    g.drawLine(v0, dv0, 3);
                    g.drawLine(dv1, v1, 3);
                    break;
                case LOCKED:
                    g.drawLine(v0, v1, 3);
                    g.setColor(GColor.RED);
                    g.drawLine(dv0, dv1, 4);
                    break;
                case CLOSED:
                    g.drawLine(v0, v1, 3);
                    g.setColor(GColor.GREEN);
                    g.drawLine(dv0, dv1, 4);
                    break;
            }
            g.popMatrix();
        }
        g.popMatrix();
    }

    public Grid.Pos drawDebug(AGraphics g, int mouseX, int mouseY) {
        g.clearScreen(GColor.LIGHT_GRAY);
        Grid.Pos returnCell = null;

        Grid.Iterator<ZCell> it = grid.iterator();
        while (it.hasNext()) {
            ZCell cell = it.next();
            if (cell.isInside) {
                g.setColor(GColor.YELLOW);
                g.drawFilledRect(cell.rect);
            } else {
                g.setColor(GColor.WHITE);
                g.drawFilledRect(cell.rect);
            }
            drawCellWalls(g, cell, .97f);
            String text = "Zone " + cell.zoneIndex;
            if (cell.cellType != ZCellType.NONE) {
                text += "\n" + cell.cellType;
            }
            if (cell.rect.contains(mouseX, mouseY)) {
                List<Integer> accessible = getAccessableZones(cell.zoneIndex, 1);
                text = "1 Unit away:\n" + accessible;
                returnCell = it.getPos();//new int[] { col, row };
                List<Integer> accessible2 = getAccessableZones(cell.zoneIndex, 2);
                text += "\n2 Units away:\n" + accessible2;
            }
            g.setColor(GColor.CYAN);
            for (ZActor a : cell.occupied) {
                if (a != null) {
                    text += "\n" + a.name();
                }
            }
            g.drawJustifiedStringOnBackground(cell.rect.getCenter(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10, 3);

        }
        return returnCell;
    }

    private void removeEmptyZones() {
        Iterator<ZZone> it = zones.iterator();
        while (it.hasNext()) {
            ZZone z = it.next();
            if (z == null)
                it.remove();
            if (z.cells.isEmpty()) {
                it.remove();
            }
        }
        // renumber the zones for the cells
        for (int i=0; i<zones.size(); i++) {
            for (Grid.Pos cellPos : zones.get(i).cells) {
                getCell(cellPos).zoneIndex = i;
            }
        }
    }

    public GDimension initCellRects(AGraphics g, int width, int height) {
        int rows = getRows();
        int cols = getColumns();

        int dim = Math.min(width/cols, height/rows);

        for (Grid.Iterator<ZCell> it = grid.iterator(); it.hasNext(); ) {
            ZCell cell = it.next();
            cell.rect = new GRectangle(it.getPos().getColumn()*dim, it.getPos().getRow()*dim, dim, dim);
            zones.get(cell.zoneIndex).center.addEq(cell.rect.getCenter());
        }

        for (ZZone zone : zones) {
            zone.center.scaleEq(1.0f / zone.cells.size());
        }

        return new GDimension(dim, dim);
    }

    /**
     *
     * @param actor
     * @param zoneIndex
     */
    public void addActor(ZActor actor, int zoneIndex) {
        ZZone zone = zones.get(zoneIndex);
        boolean added = false;
        for (int c=0;!added && c < zone.cells.size(); c++) {
            Grid.Pos cellPos = zone.cells.get(zone.nextCell);
            zone.nextCell = (zone.nextCell + 1) % zone.cells.size();
            if (addActorToCell(actor, cellPos)) {
                added = true;
                break;
            }
        }
        if (!added) {
            throw new AssertionError("Failed to add Actor");
        }
    }

    public void removeActor(ZActor actor) {
        ZCell cell = getCell(actor.occupiedCell);
        cell.occupied[actor.occupiedQuadrant] = null;
        zones.get(cell.zoneIndex).noiseLevel -= actor.getNoise();
        //actor.occupiedZone = -1;
        //actor.occupiedQuadrant = -1;
        //actor.occupiedCell = null;
    }

    /**
     * To be called after load game - will make sure actors asyncronized with cell quadrants
     * @param actors
     */
    void syncActors(List<ZActor> actors) {
        for (ZActor actor : actors) {
            ZZone zone = zones.get(actor.occupiedZone);
            ZCell cell = getCell(actor.occupiedCell);
            cell.occupied[actor.occupiedQuadrant] = actor;
        }
    }

    public ZActor drawActors(AGraphics g, int mx, int my) {
        ZActor picked = null;
        for (ZCell cell : getCells()) {
            for (int i=0; i<cell.occupied.length; i++) {
                ZActor a = cell.occupied[i];
                if (a == null)
                    continue;
                AImage img = g.getImage(a.getImageId());
                if (img != null) {
                    GRectangle rect = cell.getQuadrant(i).fit(img);
                    if (rect.contains(mx, my))
                        picked = a;
                    g.drawImage(a.getImageId(), rect);
                    a.rect = rect;
                }
            }
        }
        return picked;
    }

    // TODO - Move this to the renderer so we can do better highlighting in android canvas
    public GRectangle drawActor(AGraphics g, ZActor actor, GColor outline) {
        AImage img = g.getImage(actor.getImageId());
        if (img != null) {
            if (actor.rect == null)
                actor.rect = getCell(actor.occupiedCell).getQuadrant(actor.occupiedQuadrant).fit(img);
            if (outline != null) {
                GColor oldColor = g.getColor();
                g.setColor(outline);
                g.drawRect(actor.rect, 2);
                g.setColor(oldColor);
            }
            g.drawImage(actor.getImageId(), actor.rect);
            return actor.rect;
        }
        return null;
    }

    /**
     * Iterate over all cells
     * @return
     */
    public Iterable<ZCell> getCells() {
        return grid.getCells();
    }

    public void drawZoneOutline(AGraphics g, int zoneIndex) {
        ZZone zone = getZone(zoneIndex);
        for (Grid.Pos cellPos : zone.cells) {
            g.drawRect(getCell(cellPos).rect, 1);
        }
    }

    public List<ZZombie> getZombiesInZone(int zoneIdx) {
        return Utils.filter(getActorsInZone(zoneIdx), (Utils.Filter<ZActor>) object -> object instanceof ZZombie);
    }

    public List<ZCharacter> getCharactersInZone(int zoneIdx) {
        return Utils.filter(getActorsInZone(zoneIdx), (Utils.Filter<ZActor>) object -> object instanceof ZCharacter);
    }

    public List getActorsInZone(int zoneIndex) {
        List<ZActor> actors = new ArrayList<>();
        for (Grid.Pos cellPos : zones.get(zoneIndex).cells) {
            for (ZActor a : getCell(cellPos).occupied) {
                if (a != null) {
                    actors.add(a);
                }
            }
        }
        return actors;
    }

    public List getAllActors() {
        List<ZActor> actors = new ArrayList<>();
        for (ZCell cell : grid.getCells()) {
            for (ZActor a : cell.occupied) {
                if (a != null)
                    actors.add(a);
            }
        }
        return actors;
    }

    public List<ZZombie> getAllZombies() {
        return Utils.filter(getAllActors(), (Utils.Filter<ZActor>) object -> object instanceof ZZombie);
    }

    public List<ZCharacter> getAllCharacters() {
        return Utils.filter(getAllActors(), (Utils.Filter<ZActor>) object -> object instanceof ZCharacter);
    }

    public List<ZDoor> getDoorsForZone(int occupiedZone, ZWallFlag ... flags) {
        List<ZDoor> doors = new ArrayList<>();
        for (Grid.Pos cellPos : zones.get(occupiedZone).cells) {
            ZCell cell = getCell(cellPos);
            for (int i=0; i<4; i++) {
                if (Utils.linearSearch(flags, cell.walls[i]) >= 0) {
                    doors.add(new ZDoor(cellPos, i));
                }
            }
        }
        return doors;
    }

    Grid.Pos getAdjacentPos(Grid.Pos pos, int direction) {
        int col = pos.getColumn();
        int row = pos.getRow();
        switch (direction) {
            case DIR_NORTH:
                if (row <= 0)
                    return null;
                row--;
                break;
            case DIR_SOUTH:
                if (row >= getRows())
                    return null;
                row++;
                break;
            case DIR_EAST:
                if (col >= getColumns())
                    return null;
                col++;
                break;
            case DIR_WEST:
                if (col <= 0)
                    return null;
                col--;
                break;
        }
        return new Grid.Pos(row, col);
    }

    public void moveActorInDirection(ZActor actor, int direction) {
        Grid.Pos pos = actor.occupiedCell;
        Grid.Pos adj = getAdjacentPos(pos, direction);
        if (adj != null) {
            removeActor(actor);
            addActorToCell(actor, adj);
        }
    }

    private boolean addActorToCell(ZActor actor, Grid.Pos pos) {
        ZCell cell = getCell(pos);
        for (int i = 0; i < cell.occupied.length; i++) {
            if (cell.occupied[i] == null) {
                cell.occupied[i] = actor;
                actor.occupiedZone = cell.zoneIndex;
                actor.occupiedCell = pos;
                actor.occupiedQuadrant = i;
                zones.get(cell.zoneIndex).noiseLevel += actor.getNoise();
                return true;
            }
        }
        return false;
    }

    public void getUndiscoveredIndoorZones(Grid.Pos startPos, Set<Integer> undiscovered) {
        ZCell cell = getCell(startPos);
        if (cell.discovered)
            return;
        cell.discovered = true;
        ZZone zone = zones.get(cell.zoneIndex);
        if (!zone.searchable)
            return;
        undiscovered.add(cell.zoneIndex);
        for (int i=0; i<4; i++) {
            switch (cell.walls[i]) {
                case NONE:
                case OPEN:
                    getUndiscoveredIndoorZones(getAdjacentPos(startPos, i), undiscovered);
                    break;
            }
        }
    }

    void resetNoise() {
        for (ZZone zone : zones) {
            zone.noiseLevel = 0;
            for (Grid.Pos pos: zone.cells) {
                for (ZActor a : getCell(pos).occupied) {
                    if (a != null && a instanceof ZCharacter) {
                        zone.noiseLevel ++;
                    }
                }
            }
        }
    }
}
