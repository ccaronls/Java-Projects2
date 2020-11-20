package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;
import cc.lib.utils.Table;

public abstract class ZQuest extends Reflector<ZQuest> {

    public final static int OBJECTIVE_EXP = 5;

    static {
        addAllFields(ZQuest.class);
    }

    private final String name;
    protected int exitZone = -1;
    private Map<Integer, List<ZEquipment>> vaultMap = new HashMap<>();
    private List<ZEquipmentType> allItems = null;

    protected ZQuest(String name) {
        this.name = name;
    }

    public abstract ZBoard loadBoard();

    public String getName() {
        return name;
    }

    public List<ZEquipmentType> getAllVaultOptions() {
        if (allItems == null)
            allItems = Utils.asList(ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW);
        return allItems;
    }

    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
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
                setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.WALL);
                break;
            case "ws":
                setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.WALL);
                break;
            case "we":
                setCellWall(grid, pos, ZDir.EAST, ZWallFlag.WALL);
                break;
            case "ww":
                setCellWall(grid, pos, ZDir.WEST, ZWallFlag.WALL);
                break;
            case "dn":
                setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.CLOSED);
                break;
            case "ds":
                setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.CLOSED);
                break;
            case "de":
                setCellWall(grid, pos, ZDir.EAST, ZWallFlag.CLOSED);
                break;
            case "dw":
                setCellWall(grid, pos, ZDir.WEST, ZWallFlag.CLOSED);
                break;
            case "ldn":
                setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.LOCKED);
                break;
            case "lds":
                setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.LOCKED);
                break;
            case "lde":
                setCellWall(grid, pos, ZDir.EAST, ZWallFlag.LOCKED);
                break;
            case "ldw":
                setCellWall(grid, pos, ZDir.WEST, ZWallFlag.LOCKED);
                break;
            case "odn":
                setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.OPEN);
                break;
            case "ods":
                setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.OPEN);
                break;
            case "ode":
                setCellWall(grid, pos, ZDir.EAST, ZWallFlag.OPEN);
                break;
            case "odw":
                setCellWall(grid, pos, ZDir.WEST, ZWallFlag.OPEN);
                break;
            case "obj":
                cell.cellType = ZCellType.OBJECTIVE;
                break;
            case "sp":
                cell.cellType = ZCellType.SPAWN;
                break;
            case "st":
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

    void setCellWall(Grid<ZCell> grid, Grid.Pos pos, ZDir dir, ZWallFlag flag) {
        grid.get(pos).setWallFlag(dir, flag);
        Grid.Pos adj = dir.getAdjacent(pos);
        if (grid.isOnGrid(adj))
            grid.get(adj).setWallFlag(dir.getOpposite(), flag);
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
                Grid.Pos pos = new Grid.Pos(row, col);
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
                    loadCmd(grid, pos, cmd);
                    // make sure outer perimeter has walls
                    switch (cell.cellType) {
                        case EXIT:
                            exitZone = cell.zoneIndex;
                            break;
                        case OBJECTIVE:
                            zone.objective = true;
                            break;
                    }
                }
                if (row == 0) {
                    loadCmd(grid, pos, "wn");
                } else if (row == map.length-1) {
                    loadCmd(grid, pos, "ws");
                }
                if (col == 0) {
                    loadCmd(grid, pos, "ww");
                } else if (col == map[0].length-1) {
                    loadCmd(grid, pos, "we");
                }

            }
        }
        Vector<ZZone> zones = new Vector<>();
        zones.setSize(maxZone+1);
        for (Map.Entry<Integer, ZZone> e : zoneMap.entrySet()) {
            zones.set(e.getKey(), e.getValue());
        }
        // fill in null zones with empty ones
        for (int i=0; i<zones.size(); i++) {
            if (zones.get(i) == null)
                zones.set(i, new ZZone());
        }

        return new ZBoard(grid, zones);

    }

    /**
     *
     * @param game
     * @param cur
     * @param options
     */
    public abstract void addMoves(ZGame game, ZCharacter cur, List<ZMove> options);

    /**
     *
     * @param game
     * @param c
     * @param move
     */
    public abstract void processObjective(ZGame game, ZCharacter c, ZMove move);

    /**
     *
     * @param game
     * @return
     */
    public abstract boolean isQuestComplete(ZGame game);

    /**
     *
     * @param game
     * @return
     */
    public abstract boolean isQuestFailed(ZGame game);

    /**
     *
     * @param g
     */
    public abstract void drawTiles(AGraphics g, ZBoard board, ZTiles tiles);

    public Collection<Integer> getVaultZones() {
        return vaultMap.keySet();
    }

    public List<ZEquipment> getRemainingVaultItems() {
        List<ZEquipment> all = new ArrayList<>();
        for (List l : vaultMap.values()) {
            all.addAll(l);
        }
        return all;
    }

    /**
     *
     * @return
     */
    public List<ZEquipment> getVaultItems(int vaultZone) {
        List<ZEquipment> list = vaultMap.get(vaultZone);
        if (list == null) {
            list = new ArrayList<>();
            List<ZEquipmentType> allItems = getAllVaultOptions();
            if (allItems.size() > 0) {
                int item = Utils.rand() % allItems.size();
                ZEquipment equip = allItems.remove(item).create();
                list.add(equip);
            }
            vaultMap.put(vaultZone, list);
        }
        return list;
    }

    /**
     * Called once during INIT stage of game
     */
    public abstract void init(ZGame game);

    public int getMaxNumZombiesOfType(ZZombieType type) {
        switch (type) {
            case Abomination:
            case Necromancer:
                return 1;
        }
        return Integer.MAX_VALUE;
    }

    public abstract Table getObjectivesOverlay(ZGame game);
}
