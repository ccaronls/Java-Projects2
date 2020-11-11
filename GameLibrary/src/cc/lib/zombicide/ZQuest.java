package cc.lib.zombicide;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import cc.lib.utils.Reflector;

import static cc.lib.zombicide.ZBoard.DIR_EAST;
import static cc.lib.zombicide.ZBoard.DIR_NORTH;
import static cc.lib.zombicide.ZBoard.DIR_SOUTH;
import static cc.lib.zombicide.ZBoard.DIR_WEST;

public abstract class ZQuest extends Reflector<ZQuest> {

    static {
        addAllFields(ZQuest.class);
    }

    public final String name;



    protected ZQuest(String name) {
        this.name = name;
    }

    public abstract ZBoard loadBoard();

    public ZBoard load(String [][] map) {
        int rows = map.length;
        int cols = map[0].length;
        ZCell [][] grid = new ZCell[rows][cols];
        final ZCell DUMMY = new ZCell();
        Map<Integer, ZZone> zoneMap = new HashMap<>();
        int maxZone = 0;
        for (int row=0; row<map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                grid[row][col] = new ZCell();
            }
        }
        for (int row=0; row<map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                ZCell cell = grid[row][col];
                ZCell north = DUMMY, south=DUMMY, east=DUMMY, west=DUMMY;
                if (row>0)
                    north = grid[row-1][col];
                if (row<rows-1)
                    south = grid[row+1][col];
                if (col>0)
                    west = grid[row][col-1];
                if (col<cols-1)
                    east = grid[row][col+1];
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
                        zone.cells.add(new int[] { col, row });
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
                            north.walls[DIR_SOUTH] = ZWallFlag.WALL;
                            break;
                        case "ws":
                            cell.walls[DIR_SOUTH] = ZWallFlag.WALL;
                            south.walls[DIR_NORTH] = ZWallFlag.WALL;
                            break;
                        case "we":
                            cell.walls[DIR_EAST] = ZWallFlag.WALL;
                            east.walls[DIR_WEST] = ZWallFlag.WALL;
                            break;
                        case "ww":
                            cell.walls[DIR_WEST] = ZWallFlag.WALL;
                            west.walls[DIR_EAST] = ZWallFlag.WALL;
                            break;
                        case "dn":
                            cell.walls[DIR_NORTH] = ZWallFlag.CLOSED;
                            north.walls[DIR_SOUTH] = ZWallFlag.CLOSED;
                            break;
                        case "ds":
                            cell.walls[DIR_SOUTH] = ZWallFlag.CLOSED;
                            south.walls[DIR_NORTH] = ZWallFlag.CLOSED;
                            break;
                        case "de":
                            cell.walls[DIR_EAST] = ZWallFlag.CLOSED;
                            east.walls[DIR_WEST] = ZWallFlag.CLOSED;
                            break;
                        case "dw":
                            cell.walls[DIR_WEST] = ZWallFlag.CLOSED;
                            west.walls[DIR_EAST] = ZWallFlag.CLOSED;
                            break;
                        case "ldn":
                            cell.walls[DIR_NORTH] = ZWallFlag.LOCKED;
                            north.walls[DIR_SOUTH] = ZWallFlag.LOCKED;
                            break;
                        case "lds":
                            cell.walls[DIR_SOUTH] = ZWallFlag.LOCKED;
                            south.walls[DIR_NORTH] = ZWallFlag.LOCKED;
                            break;
                        case "lde":
                            cell.walls[DIR_EAST] = ZWallFlag.LOCKED;
                            east.walls[DIR_WEST] = ZWallFlag.LOCKED;
                            break;
                        case "ldw":
                            cell.walls[DIR_WEST] = ZWallFlag.LOCKED;
                            west.walls[DIR_EAST] = ZWallFlag.LOCKED;
                            break;
                        case "odn":
                            cell.walls[DIR_NORTH] = ZWallFlag.OPEN;
                            north.walls[DIR_SOUTH] = ZWallFlag.OPEN;
                            break;
                        case "ods":
                            cell.walls[DIR_SOUTH] = ZWallFlag.OPEN;
                            south.walls[DIR_NORTH] = ZWallFlag.OPEN;
                            break;
                        case "ode":
                            cell.walls[DIR_EAST] = ZWallFlag.OPEN;
                            east.walls[DIR_WEST] = ZWallFlag.OPEN;
                            break;
                        case "odw":
                            cell.walls[DIR_WEST] = ZWallFlag.OPEN;
                            west.walls[DIR_EAST] = ZWallFlag.OPEN;
                            break;
                        case "obj":
                            cell.cellType = ZCellType.OBJECTIVE;
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
        Vector<ZZone> zones = new Vector<>();
        zones.setSize(maxZone+1);
        for (Map.Entry<Integer, ZZone> e : zoneMap.entrySet()) {
            zones.set(e.getKey(), e.getValue());
        }

        return new ZBoard(grid, zones);

    }
}
