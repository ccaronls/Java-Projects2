package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.math.Vector2D;

public class ZBoard {

    public static final int DIR_NORTH = 0;
    public static final int DIR_SOUTH = 1;
    public static final int DIR_EAST  = 2;
    public static final int DIR_WEST  = 3;

    public static final int [] DIR_DX = {  0, 0, 1,-1 };
    public static final int [] DIR_DY = { -1, 1, 0, 0 };

    public static final int [] DIR_ROTATION = { 0, 180, 90, 270 };
    public static final int [] DIR_OPPOSITE = { DIR_SOUTH, DIR_NORTH, DIR_WEST, DIR_EAST };

    ZCell [][] grid;
    Vector<ZZone> zones;

    public int getRows() {
        return grid.length;
    }

    public int getColumns() {
        return grid[0].length;
    }

    public void load(String [][] map) {
        int rows = map.length;
        int cols = map[0].length;
        grid = new ZCell[rows][cols];
        Map<Integer, ZZone> zoneMap = new HashMap<>();
        int maxZone = 0;
        for (int row=0; row<map.length; row++) {
            for (int col=0; col<map[row].length; col++) {
                ZCell cell = grid[row][col] = new ZCell();
                String [] parts = map[row][col].split("[:]");
                ZZone zone = null;
                for (String cmd:parts) {
                    if (cmd.length() == 0)
                        continue;
                    if (cmd.startsWith("z")) {
                        int index = Integer.parseInt(cmd.substring(1));
                        maxZone = Math.max(maxZone, index);
                        zone = zoneMap.get(index);
                        if (zone == null) {
                            zone = new ZZone();
                            zoneMap.put(index, zone);
                        }
                        zone.cells.add(new int[] { row, col });
                        cell.zoneIndex = index;
                        continue;
                    }

                    switch (cmd) {
                        case "i":
                            cell.isInside = true;
                            break;
                        case "v":
                            cell.cellType = ZCellType.VAULT;
                            break;
                        case "wn":
                            cell.walls[DIR_NORTH] = ZWallFlag.WALL;
                            break;
                        case "ws":
                            cell.walls[DIR_SOUTH] = ZWallFlag.WALL;
                            break;
                        case "we":
                            cell.walls[DIR_EAST] = ZWallFlag.WALL;
                            break;
                        case "ww":
                            cell.walls[DIR_WEST] = ZWallFlag.WALL;
                            break;
                        case "dn":
                            cell.walls[DIR_NORTH] = ZWallFlag.CLOSED;
                            break;
                        case "ds":
                            cell.walls[DIR_SOUTH] = ZWallFlag.CLOSED;
                            break;
                        case "de":
                            cell.walls[DIR_EAST] = ZWallFlag.CLOSED;
                            break;
                        case "dw":
                            cell.walls[DIR_WEST] = ZWallFlag.CLOSED;
                            break;
                        case "ldn0":
                            cell.walls[DIR_NORTH] = ZWallFlag.LOCKED0;
                            break;
                        case "lds0":
                            cell.walls[DIR_SOUTH] = ZWallFlag.LOCKED0;
                            break;
                        case "lde0":
                            cell.walls[DIR_EAST] = ZWallFlag.LOCKED0;
                            break;
                        case "ldw0":
                            cell.walls[DIR_WEST] = ZWallFlag.LOCKED0;
                            break;
                        case "ldn1":
                            cell.walls[DIR_NORTH] = ZWallFlag.LOCKED1;
                            break;
                        case "lds1":
                            cell.walls[DIR_SOUTH] = ZWallFlag.LOCKED1;
                            break;
                        case "lde1":
                            cell.walls[DIR_EAST] = ZWallFlag.LOCKED1;
                            break;
                        case "ldw1":
                            cell.walls[DIR_WEST] = ZWallFlag.LOCKED1;
                            break;
                        case "ldn2":
                            cell.walls[DIR_NORTH] = ZWallFlag.LOCKED2;
                            break;
                        case "lds2":
                            cell.walls[DIR_SOUTH] = ZWallFlag.LOCKED2;
                            break;
                        case "lde2":
                            cell.walls[DIR_EAST] = ZWallFlag.LOCKED2;
                            break;
                        case "ldw2":
                            cell.walls[DIR_WEST] = ZWallFlag.LOCKED2;
                            break;
                        case "odn":
                            cell.walls[DIR_NORTH] = ZWallFlag.OPEN;
                            break;
                        case "ods":
                            cell.walls[DIR_SOUTH] = ZWallFlag.OPEN;
                            break;
                        case "ode":
                            cell.walls[DIR_EAST] = ZWallFlag.OPEN;
                            break;
                        case "odw":
                            cell.walls[DIR_WEST] = ZWallFlag.OPEN;
                            break;
                        case "obj":
                            cell.cellType = ZCellType.OBJECTIVE;
                            break;
                        case "key0":
                            cell.cellType = ZCellType.KEY0;
                            break;
                        case "key1":
                            cell.cellType = ZCellType.KEY1;
                            break;
                        case "key2":
                            cell.cellType = ZCellType.KEY2;
                            break;
                        case "sp":
                            cell.cellType = ZCellType.SPAWN;
                            break;
                        case "start":
                            cell.cellType = ZCellType.START;
                            break;
                        case "exit":
                            cell.cellType = ZCellType.EXIT;
                            break;
                        case "walker":
                            cell.cellType = ZCellType.WALKER;
                            break;
                        case "runner":
                            cell.cellType = ZCellType.RUNNER;
                            break;
                        case "fatty":
                            cell.cellType = ZCellType.FATTY;
                            break;
                        case "necro":
                            cell.cellType = ZCellType.NECRO;
                            break;
                        default:
                            throw new RuntimeException("Invalid command '" + cmd + "'");
                    }
                }
            }
        }
        zones = new Vector<>();
        zones.setSize(maxZone+1);
        for (Map.Entry<Integer, ZZone> e : zoneMap.entrySet()) {
            zones.set(e.getKey(), e.getValue());
        }
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
                int x = cellPos[1];
                int y = cellPos[0];
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

            switch (cell.walls[dir]) {
                case CLOSED:
                case WALL:
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
            switch (fromCell.walls[dir]) {
                case WALL:
                case CLOSED:
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

    public void initCellRects(AGraphics g, int width, int height) {
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
    }
}
