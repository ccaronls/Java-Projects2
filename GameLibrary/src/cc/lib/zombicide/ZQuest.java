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

    private final ZQuests quest;
    protected int exitZone = -1;
    private Map<Integer, List<ZEquipment>> vaultMap = new HashMap<>();
    private List<ZEquipment> vaultItemsRemaining = null;
    private int numFoundVaultItems = 0;
    protected List<Integer> redObjectives = new ArrayList<>();
    private int numStartRedObjectives = 0;

    protected ZQuest(ZQuests quest) {
        this.quest = quest;
    }

    public abstract ZBoard loadBoard();

    public String getName() {
        return quest.getDisplayName();
    }

    public ZQuests getQuest() {
        return quest;
    }

    public List<ZEquipmentType> getAllVaultOptions() {
        return Utils.asList(ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW);
    }

    public int getNumFoundVaultItems() {
        return numFoundVaultItems;
    }

    ZDir getDirectionForEnvironment(int env) {
        switch (env) {
            case ZCell.ENV_VAULT:
                return ZDir.ASCEND;
        }
        return ZDir.DESCEND;
    }

    protected void setVaultDoor(ZCell cell, Grid<ZCell> grid, Grid.Pos pos, ZCellType type, int vaultFlag) {
        setVaultDoor(cell, grid, pos, type, vaultFlag, ZWallFlag.CLOSED);
    }

    protected void setVaultDoor(ZCell cell, Grid<ZCell> grid, Grid.Pos pos, ZCellType type, int vaultFlag, ZWallFlag wallFlag) {
        cell.setCellType(type, true);
        cell.vaultFlag = vaultFlag;
        setCellWall(grid, pos, getDirectionForEnvironment(cell.environment), wallFlag);
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
                setVaultDoor(cell, grid, pos,  ZCellType.VAULT_DOOR_VIOLET, 1);
                break;
            case "vd2":
                setVaultDoor(cell, grid, pos,  ZCellType.VAULT_DOOR_VIOLET, 2);
                break;
            case "vd3":
                setVaultDoor(cell, grid, pos,  ZCellType.VAULT_DOOR_VIOLET, 3);
                break;
            case "vd4":
                setVaultDoor(cell, grid, pos,  ZCellType.VAULT_DOOR_VIOLET, 4);
                break;
            case "gvd1":
                setVaultDoor(cell, grid, pos,  ZCellType.VAULT_DOOR_GOLD, 1);
                break;
            case "gvd2":
                setVaultDoor(cell, grid, pos,  ZCellType.VAULT_DOOR_GOLD, 2);
                break;
            case "gvd3":
                setVaultDoor(cell, grid, pos,  ZCellType.VAULT_DOOR_GOLD, 3);
                break;
            case "gvd4":
                setVaultDoor(cell, grid, pos,  ZCellType.VAULT_DOOR_GOLD, 4);
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
            case "sp":
            case "spn":
                cell.setCellType(ZCellType.SPAWN_NORTH, true);
                break;
            case "sps":
                cell.setCellType(ZCellType.SPAWN_SOUTH, true);
                break;
            case "spe":
                cell.setCellType(ZCellType.SPAWN_EAST, true);
                break;
            case "spw":
                cell.setCellType(ZCellType.SPAWN_WEST, true);
                break;
            case "st":
            case "start":
                cell.setCellType(ZCellType.START, true);
                break;
            case "exit":
                cell.setCellType(ZCellType.EXIT, true);
                break;
            case "walker":
                cell.setCellType(ZCellType.WALKER, true);
                break;
            case "runner":
                cell.setCellType(ZCellType.RUNNER, true);
                break;
            case "fatty":
                cell.setCellType(ZCellType.FATTY, true);
                break;
            case "necro":
                cell.setCellType(ZCellType.NECRO, true);
                break;
            case "abom":
            case "abomination":
                cell.setCellType(ZCellType.ABOMINATION, true);
                break;
            case "red":
                redObjectives.add(cell.getZoneIndex());
                cell.setCellType(ZCellType.OBJECTIVE_RED, true);
                break;

            default:
                throw new RuntimeException("Invalid command '" + cmd + "'");
        }
    }

    protected void setCellWall(Grid<ZCell> grid, Grid.Pos pos, ZDir dir, ZWallFlag flag) {
        grid.get(pos).setWallFlag(dir, flag);
        Grid.Pos adj = dir.getAdjacent(pos);
        if (adj != null && grid.isOnGrid(adj))
            grid.get(adj).setWallFlag(dir.getOpposite(), flag);
    }

    public final ZBoard load(String [][] map) {
        int rows = map.length;
        int cols = map[0].length;
        Grid<ZCell> grid = new Grid<>(rows, cols);
        Map<Integer, ZZone> zoneMap = new HashMap<>();
        int maxZone = 0;
        for (int row=0; row<map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                grid.set(row, col, new ZCell(col, row));
            }
        }
        for (int row=0; row<map.length; row++) {
            if (map[0].length != map[row].length)
                throw new IllegalArgumentException("Lenght of row " + row + " differs");
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
                            zone = new ZZone(index);
                            zoneMap.put(index, zone);
                        }
                        zone.cells.add(new Grid.Pos(row, col));
                        cell.zoneIndex = index;
                        cell.setCellType(ZCellType.NONE, true);
                        continue;
                    }
                    Utils.assertTrue(zone != null);
                    loadCmd(grid, pos, cmd);
                    // make sure outer perimeter has walls
                }
                if (cell.isCellType(ZCellType.EXIT))
                    exitZone = cell.zoneIndex;
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

        numStartRedObjectives = redObjectives.size();
        return new ZBoard(grid, zones);

    }

    /**
     *
     * @param game
     * @param cur
     * @param options
     */
    public void addMoves(ZGame game, ZCharacter cur, List<ZMove> options) {
        for (int red : redObjectives) {
            if (cur.getOccupiedZone() == red && game.getBoard().getZombiesInZone(red).size() == 0)
                options.add(ZMove.newObjectiveMove(red));
        }
    }

    /**
     *
     * @param game
     * @param c
     * @param move
     */
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        if (redObjectives.remove((Object)move.integer)) {
            game.addExperience(c, OBJECTIVE_EXP);
        }
    }

    /**
     * Return value between 0-100 for progress
     *
     * @param game
     * @return
     */
    public abstract int getPercentComplete(ZGame game);

    /**
     *
     * @return null if game not failed, otherwise a failed reason
     */
    public String getQuestFailedReason(ZGame game) {
        return null;
    }

    /**
     *
     * @return
     */
    public abstract ZTile [] getTiles(ZBoard board);

    public Collection<Integer> getVaultZones() {
        return vaultMap.keySet();
    }

    /**
     *
     * @param zone
     * @param equip
     */
    public final void pickupItem(int zone, ZEquipment equip) {
        if (equip.vaultItem) {
            numFoundVaultItems++;
            equip.vaultItem = false;
        }
        getVaultItems(zone).remove(equip);
    }

    /**
     *
     * @param zone
     * @param equip
     */
    public final void dropItem(int zone, ZEquipment equip) {
        getVaultItems(zone).add(equip);
    }

    /**
     *
     * @return
     */
    public final List<ZEquipment> getVaultItems(int vaultZone) {
        List<ZEquipment> list = vaultMap.get(vaultZone);
        if (list == null) {
            list = new ArrayList<>();
            List<ZEquipment> remainingItems = getVaultItemsRemaining();
            if (remainingItems.size() > 0) {
                int item = Utils.rand() % remainingItems.size();
                ZEquipment equip = remainingItems.remove(item);
                equip.vaultItem = true;
                list.add(equip);
            }
            vaultMap.put(vaultZone, list);
        }
        return list;
    }

    /**
     * By default inits a vault with 1 item from the remaining items list
     *
     * @param vaultZone
     * @return
     */
    protected List<ZEquipment> getInitVaultItems(int vaultZone) {
        List<ZEquipment> list = new ArrayList<>();
        List<ZEquipment> remainingItems = getVaultItemsRemaining();
        if (remainingItems.size() > 0) {
            int item = Utils.rand() % remainingItems.size();
            ZEquipment equip = remainingItems.remove(item);
            equip.vaultItem = true;
            list.add(equip);
        }
        return list;
    }

    protected List<ZEquipment> getVaultItemsRemaining() {
        if (vaultItemsRemaining == null) {
            vaultItemsRemaining = new ArrayList<>();
            for (ZEquipmentType et : getAllVaultOptions()) {
                vaultItemsRemaining.add(et.create());
            }
        }
        return vaultItemsRemaining;
    }

    /**
     * Called once during INIT stage of game
     */
    public abstract void init(ZGame game);

    public int getMaxNumZombiesOfType(ZZombieType type) {
        switch (type) {
            case Abomination:
                return 1;
            case Necromancer:
                return 2;
        }
        return Integer.MAX_VALUE;
    }

    public abstract Table getObjectivesOverlay(ZGame game);

    public void onEquipmentFound(ZGame game, ZEquipment equip) {
        //
    }

    protected boolean isAllPlayersInExit(ZGame game) {
        Utils.assertTrue(exitZone >= 0);
        return game.getBoard().getZombiesInZone(exitZone).size() == 0 && !(Utils.filter(game.getAllCharacters(), object -> object.getOccupiedZone() != exitZone).size() > 0);
    }

    /**
     * Perform any processing to the searchable. Called once on quest init
     * @param items
     */
    public void processSearchables(List<ZEquipment> items) {}

    protected int getNumStartRedObjectives() {
        return this.numStartRedObjectives;
    }

    public void onDragonBileExploded(ZCharacter c, int zoneIdx) {}

    public void drawQuest(ZGame game, AGraphics g) {}

    public void onNecromancerEscaped(ZGame game, ZZombie z) {
        game.gameLost("Necromancer Escaped");
    }
}
