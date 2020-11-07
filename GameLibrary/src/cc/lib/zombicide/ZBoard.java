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

    // 6x3
    public final static String [][] quest1 = {
            { "z0:i:wn:ww:ws", "z0:i::wn:ws:de:ds", "z1:wn:dw:fatty", "z2:i:wn:ws:we", "z3:sp:ww:wn:de", "z4:dw:wn:we:ws:exit" },
            { "z5:sp:wn:ww:ws", "z6:dn:we:walker", "z7:ww:ds:we", "z8:obj:wn:ww:ws", "z9",               "z10:obj:wn:we:ws" },
            { "z11:obj:i:wn:ww:ws:ode", "z12:start:ws:odw:we", "z13:i:ww:ws:dn:walker", "z13:i:wn:we:ws:v", "z14:ws:ww:de", "z15:i:dw:ws:we:wn:v" },
    };

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
                            cell.cellType = ZCell.CellType.VAULT;
                            break;
                        case "wn":
                            cell.walls[DIR_NORTH] = ZCell.WallFlag.WALL;
                            break;
                        case "ws":
                            cell.walls[DIR_SOUTH] = ZCell.WallFlag.WALL;
                            break;
                        case "we":
                            cell.walls[DIR_EAST] = ZCell.WallFlag.WALL;
                            break;
                        case "ww":
                            cell.walls[DIR_WEST] = ZCell.WallFlag.WALL;
                            break;
                        case "dn":
                            cell.walls[DIR_NORTH] = ZCell.WallFlag.CLOSED;
                            break;
                        case "ds":
                            cell.walls[DIR_SOUTH] = ZCell.WallFlag.CLOSED;
                            break;
                        case "de":
                            cell.walls[DIR_EAST] = ZCell.WallFlag.CLOSED;
                            break;
                        case "dw":
                            cell.walls[DIR_WEST] = ZCell.WallFlag.CLOSED;
                            break;
                        case "odn":
                            cell.walls[DIR_NORTH] = ZCell.WallFlag.OPEN;
                            break;
                        case "ods":
                            cell.walls[DIR_SOUTH] = ZCell.WallFlag.OPEN;
                            break;
                        case "ode":
                            cell.walls[DIR_EAST] = ZCell.WallFlag.OPEN;
                            break;
                        case "odw":
                            cell.walls[DIR_WEST] = ZCell.WallFlag.OPEN;
                            break;
                        case "obj":
                            cell.cellType = ZCell.CellType.OBJECTIVE;
                            break;
                        case "sp":
                            cell.cellType = ZCell.CellType.SPAWN;
                            break;
                        case "start":
                            cell.cellType = ZCell.CellType.START;
                            break;
                        case "exit":
                            cell.cellType = ZCell.CellType.EXIT;
                            break;
                        case "walker":
                            cell.cellType = ZCell.CellType.WALKER;
                            break;
                        case "fatty":
                            cell.cellType = ZCell.CellType.FATTY;
                            break;
                        case "necro":
                            cell.cellType = ZCell.CellType.NECRO;
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

    /*

    EXAMPLE BOARD
       0   1   2   3   4   5
     + - + - + - + - + - + - +
0    | o     = b |   | b   b |
     + - +   + = + - + - + - +
1    | b |   | b |         s |
     +   +   + - +   + - + - +
2    | b |           | b   b |
     + = +           + - + = +
3    | b =           | bv| b |
     + = +   + - + - + - + - +
4    | bv|   |       | b   b |
     + - +   +       + - + - +
5    | s     |       \ b   b |
     + - + - + - + - + - + - +

| - walls

=   doors

b   building

t   treasure

s   spawn

o   player origin (starting position)

v   vault

blacks are outside streets




     */

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
                cell.walls[dir] = ZCell.WallFlag.OPEN; break;
            case OPEN:
                cell.walls[dir] = ZCell.WallFlag.CLOSED; break;
            default:
                return;
        }
        cellPos[0] += DIR_DX[dir];
        cellPos[1] += DIR_DY[dir];
        cell = getCell(cellPos);
        dir = DIR_OPPOSITE[dir];
        switch (cell.walls[dir]) {
            case CLOSED:
                cell.walls[dir] = ZCell.WallFlag.OPEN; break;
            case OPEN:
                cell.walls[dir] = ZCell.WallFlag.CLOSED; break;
        }
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
                if (cell.cellType != ZCell.CellType.NONE) {
                    text += "\n" + cell.cellType;
                }
                if (cell.rect.contains(mouseX, mouseY)) {
                    List<Integer> accessible = getAccessableZones(cell.zoneIndex, 1);
                    text = "Can Walk to:\n" + accessible;
                    returnCell = new int[] { col, row };
                }
                g.setColor(GColor.CYAN);
                g.drawJustifiedStringOnBackground(center.getX(), center.getY(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 3);
            }
        }
        return returnCell;
    }

    public void initCellRects(AGraphics g, int width, int height) {
        int rows = getRows();
        int cols = getColumns();

        int dim = Math.max(width/cols, height/rows);

        for (int row=0; row<getRows(); row++) {
            for (int col=0; col<getColumns(); col++) {
                grid[row][col].rect = new GRectangle(col*dim, row*dim, dim, dim);
            }
        }
    }
}
