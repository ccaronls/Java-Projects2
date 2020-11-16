package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import cc.lib.game.AGraphics;
import cc.lib.utils.Grid;
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
    private int exitZone = -1;
    List<ZEquipmentType> vaultItems = new ArrayList<>();

    protected ZQuest(String name) {
        this.name = name;
    }

    public abstract ZBoard loadBoard();

    protected void loadCmd(Grid<ZCell> grid, int row, int col, String cmd) {
        ZCell cell = grid.get(row, col);
        switch (cmd) {
            case "i":
                cell.environment=ZCell.ENV_BUILDING;
                break;
            case "v":
                cell.environment=ZCell.ENV_VAULT;
                break;
            case "vd1":
                cell.cellType = ZCellType.VAULT_DOOR;
                cell.vaultFlag = 1;
                break;
            case "vd2":
                cell.cellType = ZCellType.VAULT_DOOR;
                cell.vaultFlag = 2;
                break;
            case "vd3":
                cell.cellType = ZCellType.VAULT_DOOR;
                cell.vaultFlag = 3;
                break;
            case "vd4":
                cell.cellType = ZCellType.VAULT_DOOR;
                cell.vaultFlag = 4;
                break;
            case "wn":
                setCellWall(grid, row, col, DIR_NORTH, ZWallFlag.WALL);
                break;
            case "ws":
                setCellWall(grid, row, col, DIR_SOUTH, ZWallFlag.WALL);
                break;
            case "we":
                setCellWall(grid, row, col, DIR_EAST, ZWallFlag.WALL);
                break;
            case "ww":
                setCellWall(grid, row, col, DIR_WEST, ZWallFlag.WALL);
                break;
            case "dn":
                setCellWall(grid, row, col, DIR_NORTH, ZWallFlag.CLOSED);
                break;
            case "ds":
                setCellWall(grid, row, col, DIR_SOUTH, ZWallFlag.CLOSED);
                break;
            case "de":
                setCellWall(grid, row, col, DIR_EAST, ZWallFlag.CLOSED);
                break;
            case "dw":
                setCellWall(grid, row, col, DIR_WEST, ZWallFlag.CLOSED);
                break;
            case "ldn":
                setCellWall(grid, row, col, DIR_NORTH, ZWallFlag.LOCKED);
                break;
            case "lds":
                setCellWall(grid, row, col, DIR_SOUTH, ZWallFlag.LOCKED);
                break;
            case "lde":
                setCellWall(grid, row, col, DIR_EAST, ZWallFlag.LOCKED);
                break;
            case "ldw":
                setCellWall(grid, row, col, DIR_WEST, ZWallFlag.LOCKED);
                break;
            case "odn":
                setCellWall(grid, row, col, DIR_NORTH, ZWallFlag.OPEN);
                break;
            case "ods":
                setCellWall(grid, row, col, DIR_SOUTH, ZWallFlag.OPEN);
                break;
            case "ode":
                setCellWall(grid, row, col, DIR_EAST, ZWallFlag.OPEN);
                break;
            case "odw":
                setCellWall(grid, row, col, DIR_WEST, ZWallFlag.OPEN);
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

    void setCellWall(Grid<ZCell> grid, int row, int col, int dir, ZWallFlag flag) {
        grid.get(row, col).walls[dir] = flag;
        switch (dir) {
            case DIR_NORTH:
                if (row > 0) {
                    grid.get(row-1, col).walls[DIR_SOUTH] = flag;
                }
                break;
            case DIR_SOUTH:
                if (row < grid.getRows()-1) {
                    grid.get(row+1, col).walls[DIR_NORTH] = flag;
                }
                break;
            case DIR_EAST:
                if (col < grid.getCols()-1) {
                    grid.get(row, col+1).walls[DIR_WEST] = flag;
                }
                break;
            case DIR_WEST:
                if (col > 0) {
                    grid.get(row, col-1).walls[DIR_EAST] = flag;
                }
                break;
        }
    }

    public ZBoard load(String [][] map) {
        int rows = map.length;
        int cols = map[0].length;
        Grid<ZCell> grid = new Grid<>(rows, cols);
        Map<Integer, ZZone> zoneMap = new HashMap<>();
        int maxZone = 0;
        for (int row=0; row<map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                grid.set(row, col, new ZCell());
            }
        }
        for (int row=0; row<map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                ZCell cell = grid.get(row, col);
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
                        zone.cells.add(new Grid.Pos(row, col));
                        cell.zoneIndex = index;
                        cell.cellType = ZCellType.NONE;
                        continue;
                    }
                    assert(zone != null);
                    loadCmd(grid, row, col, cmd);
                    switch (cell.cellType) {
                        case EXIT:
                            exitZone = cell.zoneIndex;
                            break;
                        case OBJECTIVE:
                            zone.objective = true;
                            break;
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

    /**
     *
     * @param zGame
     * @param cur
     * @param options
     */
    public abstract void addMoves(ZGame zGame, ZCharacter cur, List<ZMove> options);

    /**
     *
     * @param zGame
     * @param c
     * @param move
     */
    public abstract void processObjective(ZGame zGame, ZCharacter c, ZMove move);

    /**
     *
     * @param game
     * @return
     */
    public boolean isQuestComplete(ZGame game) {
        for (ZCharacter c : game.getAllCharacters()) {
            if (c.getOccupiedZone() != exitZone)
                return false;
        }
        return true;
    }

    /**
     *
     * @param g
     */
    public abstract void drawTiles(AGraphics g, ZBoard board, ZTiles tiles);

    /**
     * Determine if this quest requires all players must live
     * @return
     */
    public boolean isAllMustLive() {
        return true;
    }

    /**
     *
     * @return
     */
    public List<ZEquipmentType> getVaultItems() {
        return vaultItems;
    }

}
