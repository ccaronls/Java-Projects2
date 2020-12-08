package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.Vector2D;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public class ZBoard extends Reflector<ZBoard> {

    private final static Logger log = LoggerFactory.getLogger(ZBoard.class);

    static {
        addAllFields(ZBoard.class);
    }

    Grid<ZCell> grid; // TODO: Use Grid Container Type
    List<ZZone> zones;
    @Omit
    boolean loaded = false;

    public ZBoard() {

    }

    public ZBoard(Grid<ZCell> grid, List<ZZone> zones) {
        this.grid = grid;
        this.zones = zones;
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

    /**
     * Get list of accessable zones
     *
     * @param zoneIndex
     * @param distance
     * @return
     */
    public List<Integer> getAccessableZones(int zoneIndex, int distance, ZActionType action) {
        if (distance == 0)
            return Collections.singletonList(zoneIndex);

        List<Integer> result = new ArrayList<>();

        ZDir [] options = null;
        switch (action) {
            case MOVE:
                options = ZDir.values();
                break;
            default:
                options = ZDir.getCompassValues();
        }

        for (Grid.Pos cellPos : zones.get(zoneIndex).cells) {
            // fan out in all direction to given distance
            //for (int dir = 0; dir <4; dir++) {
            for (ZDir dir : options) {
                Grid.Pos pos = cellPos;
                boolean open = true;
                for (int dist = 0; dist <distance; dist++) {
                    if (!grid.get(pos).getWallFlag(dir).isOpen()) {
                        open = false;
                        break;
                    }
                    pos = getAdjacent(pos, dir);
                }
                if (open && grid.get(pos).zoneIndex != zoneIndex) {
                    result.add(grid.get(pos).zoneIndex);
                }
            }
        }
        return result;
    }

    public Grid.Pos getAdjacent(Grid.Pos from, ZDir dir) {
        switch (dir) {
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                return new Grid.Pos(from.getRow() + dir.dy, from.getColumn() + dir.dx);

            case ASCEND:
            case DESCEND:
                return findDoor(from, dir).getCellPosEnd();
        }
        assert(false);
        return null;
    }

    public ZDoor findDoor(Grid.Pos pos, ZDir dir) {
        for (ZDoor door : zones.get(getCell(pos).zoneIndex).doors) {
            if (door.getCellPosStart().equals(pos) && door.getMoveDirection()==dir) {
                return door;
            }
        }
        assert(false);
        return null;
    }

    public boolean canSee(int fromZone, int toZone) {
        if (fromZone == toZone)
            return true;
        for (Grid.Pos pos0: zones.get(fromZone).cells) {
            for (Grid.Pos pos1: zones.get(toZone).cells) {
                if (canSeeCell(pos0, pos1))
                    return true;
            }
        }
        return false;
    }

    public boolean canSeeCell(Grid.Pos fromCell, Grid.Pos toCell) {
        ZDir dir = ZDir.getDirFrom(fromCell, toCell);
        if (dir == null)
            return false;

        int zoneChanges = 0;
        int curZoneId = grid.get(fromCell).zoneIndex;

        while (!fromCell.equals(toCell)) {
            ZCell cell = grid.get(fromCell);
            // can only see 1 one zone difference
            if (cell.isInside() && cell.zoneIndex != curZoneId) {
                if (++zoneChanges > 1)
                    return false;
                curZoneId = cell.zoneIndex;
            }

            if (!cell.getWallFlag(dir).isOpen()) {
                return false;
            }

            fromCell = getAdjacent(fromCell, dir);
        }

        return true;
    }
/*
    @Omit
    Map<Grid.Pos, List<ZDir> []> lookup = null;

    void buildLookup() {
        lookup = new HashMap<>();
        int numCells = grid.getRows() * grid.getCols();
        int [][] distance = new int[numCells][numCells];
        int [][] path     = new int[numCells][numCells];
        Utils.fill(distance, Integer.MAX_VALUE);
        Utils.fill(path, -1);
        for (int i=0; i<numCells; i++) {
            Grid.Pos pos = Grid.Pos.fromIndex(i);
            ZCell cell = getCell(pos);
            if (cell.cellType == ZCellType.NONE)
                continue;
            for (ZDir dir : ZDir.getCompassValues()) {
                if (cell.getWallFlag(dir).isOpen()) {
                    Grid.Pos next = dir.getAdjacent(pos);
                    distance[i][next.getIndex()] = 1;
                }
            }
            distance[i][i] = 0;
            path[i][i] = i;
        }
        for (int k=0; k<numCells; k++) {
            for (int i=0; i<numCells; i++) {
                for (int j=0; j<numCells; j++) {
                    if (distance[i][j] > distance[i][k] + distance[k][j]) {
                        distance[i][j] = distance[i][k] + distance[k][j];
                        path[i][j] = path[i][k];
                    }
                }
            }
        }
        // find all paths from each cellpos to each zome index
        lookup = new HashMap<>();

    }*/

    /**
     * Returns a list of directions the zombie can move
     * @See DIR_NORTH, DIR_SOUTH, DIR_EAST, DIR_WEST
     *
     * @param fromPos
     * @param toZoneIndex
     * @return
     */
    public List<List<ZDir>> getShortestPathOptions(Grid.Pos fromPos, int toZoneIndex) {
        if (grid.get(fromPos).zoneIndex == toZoneIndex)
            return Collections.emptyList();

        ZZone toZone = zones.get(toZoneIndex);

        List<List<ZDir>> allPaths = new ArrayList<>();
        int maxDist = (grid.getRows() + grid.getCols()) * 2;

        Set<Grid.Pos> visited = new HashSet<>();
        for (Grid.Pos bCellPos : toZone.cells) {
            List<List<ZDir>> paths = getShortestPathOptions(fromPos, bCellPos, visited, maxDist);
            for (List l : paths) {
                maxDist = Math.min(maxDist, l.size());
            }
            allPaths.addAll(paths);
        }

        Iterator<List<ZDir>> it = allPaths.iterator();
        while (it.hasNext()) {
            if (it.next().size() > maxDist)
                it.remove();
        }

        return allPaths;
    }
/*
    public List<List<ZDir>> getShortestPathOptions(Grid.Pos fromCell, Grid.Pos toCell) {
        int maxDist = (grid.getRows() + grid.getCols()) * 2;
        Set<Grid.Pos> visited = new HashSet<>();
        List<List<ZDir>> paths = new ArrayList<>();
        searchPathsR(fromCell, toCell, new int[] { maxDist }, new LinkedList<>(), paths, visited);
        return paths;
    }
*/
    private List<List<ZDir>> getShortestPathOptions(Grid.Pos fromCell, Grid.Pos toCell, Set<Grid.Pos> visited, int maxDist) {
        List<List<ZDir>> paths = new ArrayList<>();
        searchPathsR(fromCell, toCell, new int[] { maxDist }, new LinkedList<>(), paths, visited);
        return paths;
    }

    private void searchPathsR(Grid.Pos fromPos, Grid.Pos toPos, int [] maxDist, LinkedList<ZDir> curPath, List<List<ZDir>> paths, Set<Grid.Pos> visited) {
        if (fromPos.equals(toPos)) {
            if (curPath.size() > 0) {
                paths.add(new ArrayList<>(curPath));
                maxDist[0] = Math.min(maxDist[0], curPath.size());
            }
            return;
        }
        if (curPath.size() >= maxDist[0])
            return;
        if (visited.contains(fromPos))
            return;
        visited.add(fromPos);
        ZCell fromCell = grid.get(fromPos);

        for (ZDir dir : ZDir.valuesSorted(fromPos, toPos)) {
            if (fromCell.getWallFlag(dir).isOpen()) {
                Grid.Pos nextPos = getAdjacent(fromPos, dir);
                if (visited.contains(nextPos))
                    continue;

                // is the cell full?
                if (getCell(nextPos).isFull())
                    continue;
                curPath.addLast(dir);
                searchPathsR(nextPos, toPos, maxDist, curPath, paths, visited);
                curPath.removeLast();
            }
        }


        ZZone fromZone = zones.get(fromCell.getZoneIndex());
        for (ZDoor door : fromZone.getDoors()) {
            if (door.getCellPosStart().equals(fromPos) && !door.isClosed(this)) {
                curPath.addLast(door.getMoveDirection());
                searchPathsR(door.getCellPosEnd(), toPos, maxDist, curPath, paths, visited);
                curPath.removeLast();
            }
        }
    }

    public ZCell getCell(Grid.Pos pos) {
        return grid.get(pos);
    }

    public ZCell getCell(int row, int col) {
        return grid.get(row, col);
    }

    public ZWallFlag getDoor(ZDoor door) {
        return getCell(door.getCellPosStart()).getWallFlag(door.getMoveDirection());
    }

    public void setDoor(ZDoor door, ZWallFlag flag) {
        getCell(door.getCellPosStart()).setWallFlag(door.getMoveDirection(), flag);
        door = door.getOtherSide(this);
        getCell(door.getCellPosStart()).setWallFlag(door.getMoveDirection(), flag);
    }

    public void setDoorLocked(ZDoor door) {
        addLockedDoor(door);
        addLockedDoor(door.getOtherSide(this));
    }

    private void addLockedDoor(ZDoor door) {
        ZCell cell = getCell(door.getCellPosStart());
        ZZone zone = zones.get(cell.zoneIndex);
        if (zone.doors.contains(door))
            throw new AssertionError("Already have a door like that.");
        cell.setWallFlag(door.getMoveDirection(), ZWallFlag.LOCKED);
        zone.doors.add(door);
    }

    public int pickZone(AGraphics g, int mouseX, int mouseY) {
        for (ZCell cell : grid.getCells()) {
            if (cell.getRect().contains(mouseX, mouseY)) {
                return cell.zoneIndex;
            }
        }
        return -1;
    }

    public ZDoor pickDoor(AGraphics g, List<ZDoor> doors, float mouseX, float mouseY) {
        ZDoor picked = null;
        for (ZDoor door : doors) {
            GRectangle doorRect = door.getRect(this).grownBy(10);
            if (doorRect.contains(mouseX, mouseY)) {
                g.setColor(GColor.RED);
                picked = door;
                // highlight the other side if this is a vault
                g.drawRect(door.getOtherSide(this).getRect(this).grownBy(10), 2);
            } else {
                g.setColor(GColor.DARK_OLIVE);
            }
            g.drawRect(doorRect, 2);
        }
        return picked;
    }


    public ZActor pickActor(AGraphics g, int mouseX, int mouseY) {
        return null;
    }

    // TODO: This is stoopid
    public synchronized void loadCells(AGraphics g) {
        if (!loaded)
            initCellRects(g, g.getViewportWidth(), g.getViewportHeight());
        loaded = true;
    }

    public void setSpawnZone(int zoneIdx, boolean isSpawn) {
        ZZone zone = zones.get(zoneIdx);
        zone.isSpawn = isSpawn;
        getCell(zone.cells.get(0)).setCellType(ZCellType.SPAWN, true);
    }

    public int getMaxNoiseLevel() {
        int maxNoise = 0;
        for (ZZone z : zones) {
            if (z.noiseLevel > maxNoise) {
                maxNoise = z.noiseLevel;
            }
        }
        return maxNoise;
    }

    public ZZone getMaxNoiseLevelZone() {
        int maxNoise = 0;
        ZZone maxZone = null;
        for (ZZone z : zones) {
            if (z.noiseLevel > maxNoise) {
                maxZone = z;
                maxNoise = z.noiseLevel;
            } else if (z.noiseLevel == maxNoise) {
                maxZone = null; // when multiple zones share same noise level, then neither are the max
            }
        }
        return maxZone;
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
        loadCells(g);
        int result = -1;
        for (int i=0; i<zones.size(); i++) {
            ZZone zone = zones.get(i);
            for (Grid.Pos pos : zone.cells) {
                ZCell cell = getCell(pos);
                if (cell.isCellTypeEmpty())
                    continue;
                /*
                switch (zone.type) {
                    case VAULT:
                        g.setColor(GColor.BROWN);
                        break;
                    case BUILDING:
                        g.setColor(GColor.ORANGE);
                        break;
                    case OUTDOORS:
                        g.setColor(GColor.LIGHT_GRAY);
                        break;
                }
                g.drawFilledRect(cell.rect);

                 */
                drawCellWalls(g, pos, 1);
                if (cell.rect.contains(mouseX, mouseY)) {
                    result = i;
                    g.setColor(GColor.RED.withAlpha(32));
                    g.drawFilledRect(cell.rect);
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
                                    GRectangle redX = cell.rect.scaledBy(.25f, .25f);
                                    g.setColor(type.getColor());
                                    g.drawLine(redX.getTopLeft(), redX.getBottomRight(), 10);
                                    g.drawLine(redX.getTopRight(), redX.getBottomLeft(), 10);
                                }
                                break;
                            case EXIT:
                                text += "EXIT";
                                break;
                            case SPAWN:
                                if (zone.isSpawn) {
                                    text += "SPAWN";
                                }
                                break;
                        }
                    }
                }
                if (zone.isDragonBile()) {
                    g.drawImage(ZIcon.SLIME.imageIds[0], cell.rect);
                }
                if (text.length() > 0) {
                    g.setColor(GColor.YELLOW);
                    g.drawJustifiedStringOnBackground(cell.rect.getCenter(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10, 2);
                }
            }
            if (zone.noiseLevel > 0) {
                g.setColor(GColor.BLACK);
                g.drawJustifiedString(zone.center, Justify.CENTER, Justify.CENTER, String.valueOf(zone.noiseLevel));
            }
        }
        ZZone maxNoise = getMaxNoiseLevelZone();
        if (maxNoise != null) {
            GColor color = new GColor(GColor.BLACK);
            float radius = getCell(maxNoise.cells.get(0)).rect.w / 2;
            float dr = radius / 5;
            radius = dr;
            for (int i=0; i<5; i++) {
                g.setColor(color);
                g.drawCircle(maxNoise.center, radius);
                color = color.lightened(.1f);
                radius += dr;
            }
        }
        return result;
    }

    public void drawCellWalls(AGraphics g, Grid.Pos cellPos, float scale) {
        loadCells(g);
        ZCell cell = getCell(cellPos);
        g.pushMatrix();
        Vector2D center = cell.rect.getCenter();
        g.translate(center);
        Vector2D v0 = cell.rect.getTopLeft().subEq(center);
        Vector2D v1 = cell.rect.getTopRight().subEq(center);
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
                    g.setColor(findDoor(cellPos, dir).getLockedColor());
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
                    ZDoor door = findDoor(cellPos, dir);
                    g.setColor(GColor.BLACK);
                    GRectangle vaultRect = door.getRect(this);
                    g.setColor(GColor.ORANGE);
                    vaultRect.drawFilled(g);
                    g.setColor(GColor.RED);
                    vaultRect.drawOutlined(g, 2);
                    g.drawJustifiedString(vaultRect.getCenter(), Justify.CENTER, Justify.CENTER, "LOCKED");
                    break;
                }
                case CLOSED: {
                    ZDoor door = findDoor(cellPos, dir);
                    g.setColor(GColor.BLACK);
                    GRectangle vaultRect = door.getRect(this);
                    g.setColor(GColor.ORANGE);
                    vaultRect.drawFilled(g);
                    g.setColor(GColor.YELLOW);
                    vaultRect.drawOutlined(g, 2);
                    g.drawJustifiedString(vaultRect.getCenter(), Justify.CENTER, Justify.CENTER, "VAULT");
                    break;
                }
                case OPEN: {
                    ZDoor door = findDoor(cellPos, dir);
                    g.setColor(GColor.BLACK);
                    GRectangle vaultRect = door.getRect(this);
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
                    g.setColor(GColor.ORANGE);
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
        Grid.Iterator<ZCell> it = grid.iterator();
        while (it.hasNext()) {
            ZCell cell = it.next();
            if (cell.isCellTypeEmpty())
                continue;
            if (cell.rect.contains(mouseX, mouseY)) {
                return it.getPos();
            }
        }
        return null;
    }


    public Grid.Pos drawDebug(AGraphics g, float mouseX, float mouseY) {
        loadCells(g);
        g.clearScreen(GColor.LIGHT_GRAY);
        Grid.Pos returnCell = null;

        Grid.Iterator<ZCell> it = grid.iterator();
        while (it.hasNext()) {
            ZCell cell = it.next();
            if (cell.isCellTypeEmpty())
                continue;
            switch (cell.environment) {
                case ZCell.ENV_BUILDING:
                    g.setColor(GColor.ORANGE); break;
                case ZCell.ENV_OUTDOORS:
                    g.setColor(GColor.LIGHT_GRAY); break;
                case ZCell.ENV_VAULT:
                    g.setColor(GColor.BROWN); break;
            }
            g.drawFilledRect(cell.rect);
            drawCellWalls(g, it.getPos(), .97f);
            String text = "Zone " + cell.zoneIndex;
            for (ZCellType type : ZCellType.values()) {
                if (type == ZCellType.NONE)
                    continue;
                if (cell.isCellType(type)) {
                    text += "\n" + type;
                }
            }
            if (cell.rect.contains(mouseX, mouseY)) {
                List<Integer> accessible = getAccessableZones(cell.zoneIndex, 1, ZActionType.MOVE);
                text = "1 Unit away:\n" + accessible;
                returnCell = it.getPos();//new int[] { col, row };
                List<Integer> accessible2 = getAccessableZones(cell.zoneIndex, 2, ZActionType.MAGIC);
                text += "\n2 Units away:\n" + accessible2;
            }
            g.setColor(GColor.CYAN);
            for (ZActor a : cell.getOccupant()) {
                if (a != null) {
                    text += "\n" + a.name();
                }
            }
            if (cell.vaultFlag > 0) {
                text += "\nvaultFlag " + cell.vaultFlag;
            }
            g.drawJustifiedStringOnBackground(cell.rect.getCenter(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10, 3);

        }
        return returnCell;
    }

    public GDimension initCellRects(AGraphics g, int width, int height) {
        int rows = getRows();
        int cols = getColumns();

        int dim = Math.min(width/cols, height/rows);

        for (Grid.Iterator<ZCell> it = grid.iterator(); it.hasNext(); ) {
            ZCell cell = it.next();
            cell.rect = new GRectangle(it.getPos().getColumn()*dim, it.getPos().getRow()*dim, dim, dim);
        }

        for (ZZone zone : zones) {
            if (zone.cells.size() > 0) {
                zone.center.zero();
                for (Grid.Pos pos: zone.cells) {
                    zone.center.addEq(getCell(pos).rect.getCenter());
                }
                zone.center.scaleEq(1.0f / zone.cells.size());
            }
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
            if (getCell(cellPos).isFull())
                continue;
            addActorToCell(actor, cellPos);
            added = true;
            break;
        }
        if (!added && actor.getPriority() > 2) {
            //throw new AssertionError("Failed to add Actor");
            log.warn("Zone " + zoneIndex + " is full!");

            int minPriority = 100;
            Grid.Pos minPos = null;
            for (Grid.Pos pos : zone.cells) {
                ZCell cell = getCell(pos);
                ZCellQuadrant q = cell.findLowestPriorityOccupant();
                int priority = cell.getOccupant(q).getPriority();
                if (priority < minPriority || minPos == null) {
                    minPriority = priority;
                    minPos = pos;
                }
            }
            addActorToCell(actor, minPos);
        }
    }

    public void moveActor(ZActor actor, int toZone) {
        ZCell cell = getCell(actor.occupiedCell);
        cell.setQuadrant(null, actor.occupiedQuadrant);
        zones.get(cell.zoneIndex).noiseLevel -= actor.getNoise();
        addActor(actor, toZone);
    }

    public void moveActor(ZActor actor, Grid.Pos cellPos) {
        ZCell cell = getCell(actor.occupiedCell);
        cell.setQuadrant(null, actor.occupiedQuadrant);
        zones.get(cell.zoneIndex).noiseLevel -= actor.getNoise();
        addActorToCell(actor, cellPos);
    }

    public void removeActor(ZActor actor) {
        ZCell cell = getCell(actor.occupiedCell);
        cell.setQuadrant(null, actor.occupiedQuadrant);
        zones.get(cell.zoneIndex).noiseLevel -= actor.getNoise();
        //actor.occupiedZone = -1;
        //actor.occupiedQuadrant = -1;
        //actor.occupiedCell = null;
    }

    public ZActor drawActors(AGraphics g, int mx, int my) {
        ZActor picked = null;
        for (ZCell cell : getCells()) {
            for (ZCellQuadrant q : ZCellQuadrant.values()) {
                ZActor a = cell.getOccupant(q);
                if (a == null)
                    continue;
                AImage img = g.getImage(a.getImageId());
                if (img != null) {
                    GRectangle rect = cell.getQuadrant(q).fit(img).scaledBy(a.getScale());
                    if (rect.contains(mx, my))
                        picked = a;
                    g.drawImage(a.getImageId(), rect);
                }
            }
        }
        return picked;
    }
/*
    // TODO - Move this to the renderer so we can do better highlighting in android canvas
    public GRectangle drawActor(AGraphics g, ZTiles tiles, ZActor actor, GColor outline) {
        int id=actor.getImageId();
        AImage img = g.getImage(id);
        if (img != null) {
            if (actor.rect == null)
                actor.rect = getCell(actor.occupiedCell).getQuadrant(actor.occupiedQuadrant).fit(img).scaledBy(actor.getScale());
            if (outline != null) {
                GColor oldColor = g.getColor();
                g.setColor(outline);
                g.drawRect(actor.rect, 2);
                g.setColor(oldColor);
            }
            g.drawImage(id, actor.rect);
            return actor.rect;
        }
        return null;
    }*/

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
            g.drawRect(getCell(cellPos).rect, 2);
        }
    }

    public List<ZZombie> getZombiesInZone(int zoneIdx) {
        return Utils.filter((List)getActorsInZone(zoneIdx), (Utils.Filter<ZActor>) object -> object instanceof ZZombie);
    }

    public List<ZCharacter> getCharactersInZone(int zoneIdx) {
        return Utils.filter((List)getActorsInZone(zoneIdx), (Utils.Filter<ZActor>) object -> object instanceof ZCharacter);
    }

    public List<ZActor> getActorsInZone(int zoneIndex) {
        List<ZActor> actors = new ArrayList<>();
        for (Grid.Pos cellPos : zones.get(zoneIndex).cells) {
            for (ZActor a : getCell(cellPos).getOccupant()) {
                if (a != null) {
                    actors.add(a);
                }
            }
        }
        return actors;
    }

    public List<ZActor> getAllLiveActors() {
        List<ZActor> actors = new ArrayList<>();
        for (ZCell cell : grid.getCells()) {
            for (ZActor a : cell.getOccupant()) {
                if (a != null)
                    actors.add(a);
            }
        }
        return actors;
    }

    public List<ZZombie> getAllZombies() {
        return Utils.filter((List)getAllLiveActors(), (Utils.Filter<ZActor>) object -> object instanceof ZZombie);
    }

    public List<ZCharacter> getAllCharacters() {
        return Utils.filter((List)getAllLiveActors(), (Utils.Filter<ZActor>) object -> object instanceof ZCharacter);
    }

    public void addActorToCell(ZActor actor, Grid.Pos pos) {
        ZCell cell = getCell(pos);
        ZCellQuadrant current = actor.occupiedQuadrant;
        if (current == null) {
            current = actor.getSpawnQuadrant();
        }
        if (current == null || cell.getOccupant(current) != null) {
            current = cell.findLowestPriorityOccupant();
        }
        assert(current != null);
        if (cell.getOccupant(current) != null && cell.getOccupant(current).getPriority() >= actor.getPriority())
            return;
        cell.setQuadrant(actor, current);
        actor.occupiedZone = cell.zoneIndex;
        actor.occupiedCell = pos;
        actor.occupiedQuadrant = current;
        zones.get(cell.zoneIndex).noiseLevel += actor.getNoise();
    }

    public void getUndiscoveredIndoorZones(Grid.Pos startPos, Set<Integer> undiscovered) {
        ZCell cell = getCell(startPos);
        if (cell.discovered)
            return;
        cell.discovered = true;
        ZZone zone = zones.get(cell.zoneIndex);
        if (!zone.isSearchable())
            return;
        undiscovered.add(cell.zoneIndex);
        for (ZDir dir : ZDir.values()) {
            if (cell.getWallFlag(dir).isOpen())
                getUndiscoveredIndoorZones(getAdjacent(startPos, dir), undiscovered);
        }
    }

    void resetNoise() {
        for (ZZone zone : zones) {
            zone.noiseLevel = 0;
            for (Grid.Pos pos: zone.cells) {
                for (ZActor a : getCell(pos).getOccupant()) {
                    if (a != null && a instanceof ZCharacter) {
                        zone.noiseLevel ++;
                    }
                }
            }
        }
    }

    /**
     *
     * @param actor
     * @param dir
     * @return
     */
    public boolean canMove(ZActor actor, ZDir dir) {
        return getCell(actor.occupiedCell).getWallFlag(dir).isOpen();
    }

    /**
     *
     * @return
     */
    public GDimension getDimension() {
        if (grid == null || grid.getCols() == 0 || grid.getRows() == 0)
            return new GDimension(0,0);

        Vector2D br = grid.get(grid.getRows()-1, grid.getCols()-1).getRect().getBottomRight();
        return new GDimension(br.getX(), br.getY());
    }
}
