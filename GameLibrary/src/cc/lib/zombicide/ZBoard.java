package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTGraphics;
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

    ZCell [][] grid; // TODO: Use Grid Container Type
    List<ZZone> zones;

    public ZBoard() {

    }

    public ZBoard(ZCell[][] grid, List<ZZone> zones) {
        this.grid = grid;
        this.zones = zones;
        //removeEmptyZones();
    }

    public int getRows() {
        return grid.length;
    }

    public int getColumns() {
        return grid[0].length;
    }

    ZZone getZone(int index) {
        return zones.get(index);
    }


    Iterable<ZZone> getZones() {
        return zones;
    }

    public List<Integer> getAccessableZones(int zoneIndex, int distance) {
        if (distance == 0)
            return Collections.singletonList(zoneIndex);

        List<Integer> result = new ArrayList<>();
        for (int [] cellPos : zones.get(zoneIndex).cells) {
            // fan out in all direction to given distance
            for (int dir = 0; dir <4; dir++) {
                int x = cellPos[0];
                int y = cellPos[1];
                boolean open = true;
                for (int dist = 0; dist <distance; dist++) {
                    if (!grid[y][x].walls[dir].isOpen()) {
                        open = false;
                        break;
                    }
                    x += DIR_DX[dir];
                    y += DIR_DY[dir];
                }
                if (open && grid[y][x].zoneIndex != zoneIndex) {
                    result.add(grid[y][x].zoneIndex);
                }
            }
        }
        return result;
    }

    public boolean canSee(int fromZone, int toZone) {
        for (int [] pos0: zones.get(fromZone).cells) {
            for (int [] pos1: zones.get(toZone).cells) {
                if (canSeeCell(pos0, pos1))
                    return true;
            }
        }
        return false;
    }

    public boolean canSeeCell(int [] fromCell, int [] toCell) {
        int fromCellX = fromCell[0];
        int fromCellY = fromCell[1];
        int toCellX = toCell[0];
        int toCellY = toCell[1];

        if (fromCellX != toCellX && fromCellY != toCellY)
            return false; // can only see horz and vertically

        if (fromCellX == toCellX && fromCellY == toCellY)
            return true; // TODO: Should we be able to see inside our own?

        int dir = getDirFrom(fromCell, toCell);

        int zoneChanges = 0;
        int curZoneId = grid[fromCellY][fromCellX].zoneIndex;

        int tx = fromCellX;
        int ty = fromCellY;

        while (tx != toCellX || ty != toCellY) {
            ZCell cell = grid[ty][tx];
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

    private int getDirFrom(int [] fromCell, int [] toCell) {
        int fromCellX = fromCell[0];
        int fromCellY = fromCell[1];
        int toCellX = toCell[0];
        int toCellY = toCell[1];

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

    public List<Integer> getShortestPathOptions(int fromZone, int toZone) {
        ZZone a = zones.get(fromZone);
        ZZone b = zones.get(toZone);

        List<List<Integer>> allPaths = new ArrayList<>();
        int maxDist = grid.length +  grid[0].length;

        for (int [] aCellPos : a.cells) {
            for (int [] bCellPos : b.cells) {
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

    private List<List<Integer>> getShortestPathOptions(int [] fromCell, int [] toCell, int maxDist) {
        List<List<Integer>> paths = new ArrayList<>();
        searchPathsR(fromCell, toCell, new int[] { maxDist }, new LinkedList<>(), paths);
        return paths;
    }

    private void searchPathsR(int [] start, int [] end, int [] maxDist, LinkedList<Integer> curPath, List<List<Integer>> paths) {
        if (start[0] == end[0] && start[1] == end[1]) {
            if (curPath.size() > 0) {
                paths.add(new ArrayList<>(curPath));
                maxDist[0] = Math.min(maxDist[0], curPath.size());
            }
            return;
        }
        if (curPath.size() >= maxDist[0])
            return;
        ZCell fromCell = grid[start[0]][start[1]];
        for (int dir = 0; dir <4; dir++) { // todo: order the directions we search
            if (!fromCell.walls[dir].isOpen()) {
                continue;
            }
            int x = start[1] + DIR_DX[dir];
            int y = start[0] + DIR_DY[dir];
            curPath.addLast(dir);
            searchPathsR(new int [] { x, y }, end, maxDist, curPath, paths);
            curPath.removeLast();
        }
    }

    public ZCell getCell(int[] pos) {
        return grid[pos[1]][pos[0]];
    }

    public void toggleDorOpen(int [] cellPos, int dir) {
        ZCell cell = getCell(cellPos);
        switch (cell.walls[dir]) {
            case CLOSED:
                cell.walls[dir] = ZWallFlag.OPEN; break;
            case OPEN:
                cell.walls[dir] = ZWallFlag.CLOSED; break;
            default:
                return;
        }
        cellPos[0] += DIR_DX[dir];
        cellPos[1] += DIR_DY[dir];
        cell = getCell(cellPos);
        dir = DIR_OPPOSITE[dir];
        switch (cell.walls[dir]) {
            case CLOSED:
                cell.walls[dir] = ZWallFlag.OPEN; break;
            case OPEN:
                cell.walls[dir] = ZWallFlag.CLOSED; break;
        }
    }

    public int pickZone(AGraphics g, int mouseX, int mouseY) {
        for (int row=0; row<getRows(); row++) {
            for (int col=0; col<getColumns(); col++) {
                if (grid[row][col].getRect().contains(mouseX, mouseY)) {
                    return grid[row][col].zoneIndex;
                }
            }
        }
        return -1;
    }

    public ZActor pickActor(AGraphics g, int mouseX, int mouseY) {
        return null;
    }

    public int [] drawDebug(AGraphics g, int mouseX, int mouseY) {
        g.clearScreen(GColor.LIGHT_GRAY);
        int [] returnCell = null;
        int rows = getRows();
        int cols = getColumns();

        for (int row=0; row<rows; row++) {
            for (int col=0; col<cols; col++) {
                ZCell cell = grid[row][col];
                if (cell.isInside) {
                    g.setColor(GColor.YELLOW);
                    g.drawFilledRect(cell.rect);
                } else {
                    g.setColor(GColor.WHITE);
                    g.drawFilledRect(cell.rect);
                }
                g.pushMatrix();
                Vector2D center = cell.rect.getCenter();
                g.translate(center);
                Vector2D v0 = cell.rect.getTopLeft().subEq(center);
                Vector2D v1 = cell.rect.getTopRight().subEq(center);
                g.scale(.97f);

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
                            g.drawLine(dv0, dv1, 1);
                            break;
                        case CLOSED:
                            g.drawLine(v0, v1, 3);
                            g.setColor(GColor.GREEN);
                            g.drawLine(dv0, dv1, 1);
                            break;
                    }
                    g.popMatrix();
                }
                g.popMatrix();
                String text = "Zone " + cell.zoneIndex;
                if (cell.cellType != ZCellType.NONE) {
                    text += "\n" + cell.cellType;
                }
                if (cell.rect.contains(mouseX, mouseY)) {
                    List<Integer> accessible = getAccessableZones(cell.zoneIndex, 1);
                    text = "1 Unit away:\n" + accessible;
                    returnCell = new int[] { col, row };
                    List<Integer> accessible2 = getAccessableZones(cell.zoneIndex, 2);
                    text += "\n2 Units away:\n" + accessible2;
                }
                g.setColor(GColor.CYAN);
                for (ZActor a : cell.occupied) {
                    if (a != null) {
                        text += "\n" + a.name();
                    }
                }
                g.drawJustifiedStringOnBackground(center.getX(), center.getY(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 3);
            }
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
            for (int [] cellPos : zones.get(i).cells) {
                getCell(cellPos).zoneIndex = i;
            }
        }
    }

    public GDimension initCellRects(AGraphics g, int width, int height) {
        int rows = getRows();
        int cols = getColumns();

        int dim = Math.min(width/cols, height/rows);

        for (int row=0; row<getRows(); row++) {
            for (int col=0; col<getColumns(); col++) {
                ZCell cell = grid[row][col];
                cell.rect = new GRectangle(col*dim, row*dim, dim, dim);
                zones.get(cell.zoneIndex).center.addEq(cell.rect.getCenter());
            }
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
            int [] cellPos = zone.cells.get(zone.nextCell);
            ZCell cell = getCell(cellPos);
            zone.nextCell = (zone.nextCell + 1) % zone.cells.size();
            for (int i = 0; i < cell.occupied.length; i++) {
                if (cell.occupied[i] == null) {
                    cell.occupied[i] = actor;
                    actor.occupiedZone = zoneIndex;
                    actor.occupiedCell = cellPos;
                    actor.occupiedQuadrant = i;
                    added = true;
                    break;
                }
            }
        }
        if (!added) {
            throw new AssertionError("Failed to add Actor");
        }
    }

    public void removeActor(ZActor actor) {
        ZCell cell = getCell(actor.occupiedCell);
        cell.occupied[actor.occupiedQuadrant] = null;
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
        return () -> ZBoard.this.iterator();
    }

    public Iterator<ZCell> iterator() {
        return new Iterator<ZCell>() {

            int row=0;
            int col=0;

            @Override
            public boolean hasNext() {
                return row<getRows();
            }

            @Override
            public ZCell next() {
                ZCell cell = grid[row][col];
                if (++col == getColumns()) {
                    col=0;
                    row++;
                }
                return cell;
            }
        };

    }

    public void drawZoneOutline(AWTGraphics g, int zoneIndex) {
        ZZone zone = getZone(zoneIndex);
        for (int [] cellPos : zone.cells) {
            g.drawFilledRect(getCell(cellPos).rect);
        }
    }

    public List<ZZombie> getZombiesInZone(int occupiedZone) {
        List<ZZombie> zombies = new ArrayList<>();
        for (int [] cellPos : zones.get(occupiedZone).cells) {
            for (ZActor a : getCell(cellPos).occupied) {
                if (a != null && a instanceof ZZombie) {
                    zombies.add((ZZombie)a);
                }
            }
        }
        return zombies;
    }

    public List<ZDoor> getDoorsForZone(int occupiedZone, ZWallFlag ... flags) {
        List<ZDoor> doors = new ArrayList<>();
        for (int [] cellPos : zones.get(occupiedZone).cells) {
            ZCell cell = getCell(cellPos);
            for (int i=0; i<4; i++) {
                if (Utils.linearSearch(flags, cell.walls[i]) >= 0) {
                    doors.add(new ZDoor(cellPos, i));
                }
            }
        }
        return doors;
    }
}
