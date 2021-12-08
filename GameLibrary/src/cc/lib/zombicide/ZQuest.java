package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.utils.GException;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;
import cc.lib.utils.Table;
import cc.lib.zombicide.ui.UIZombicide;

public abstract class ZQuest extends Reflector<ZQuest> {

    static {
        addAllFields(ZQuest.class);
    }

    private final ZQuests quest;
    private int exitZone = -1;
    private Map<Integer, List<ZEquipment>> vaultMap = new HashMap<>();
    private List<ZEquipment> vaultItemsRemaining = null;
    private int numFoundVaultItems = 0;
    // TODO: Make this just 'objectives' and capture blue objective from Tutorial
    private List<Integer> redObjectives = new ArrayList<>();
    private int numStartRedObjectives = 0;

    protected ZQuest(ZQuests quest) {
        this.quest = quest;
    }

    public abstract ZBoard loadBoard();

    /**
     *
     * @return
     */
    public abstract ZTile [] getTiles(ZBoard board);

    /**
     * Called once during INIT stage of game
     */
    public abstract void init(ZGame game);

    /**
     * Return value between 0-100 for progress
     * 100 is assumed to be a game over win
     *
     * @param game
     * @return
     */
    public abstract int getPercentComplete(ZGame game);

    /**
     * Return a table to be displayed when used want to view the objectives
     *
     * @param game
     * @return
     */
    public abstract Table getObjectivesOverlay(ZGame game);

    /**
     *
     * @return
     */
    public String getName() {
        return quest.getDisplayName();
    }

    /**
     *
     * @return
     */
    public ZQuests getQuest() {
        return quest;
    }

    /**
     *
     * @return
     */
    public int getExitZone() {
        return exitZone;
    }

    /**
     *
     * @return
     */
    public int getNumUnfoundObjectives() {
        return redObjectives.size();
    }

    /**
     *
     * @return
     */
    public int getNumFoundObjectives() {
        return getNumStartRedObjectives() - redObjectives.size();
    }

    /**
     *
     * @return
     */
    public List<Integer> getRedObjectives() {
        return redObjectives;
    }

    /**
     *
     * @return
     */
    public List<ZEquipmentType> getAllVaultOptions() {
        if (isWolfBurg()) {
            return Utils.toList(ZWeaponType.CHAOS_LONGBOW, ZWeaponType.VAMPIRE_CROSSBOW);
        }
        return Utils.asList(ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW);
    }

    /**
     *
     * @return
     */
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

    protected void setSpawnArea(ZCell cell, ZSpawnArea area) {
        Utils.assertTrue(cell.numSpawns == 0);
        cell.spawns[cell.numSpawns++] = area;
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
            case "t1":
            case "t2":
            case "t3":
                switch (Integer.parseInt(cmd.substring(1))) {
                    case 1:
                        cell.scale = 1.05f;
                        break;
                    case 2:
                        cell.scale = 1.1f;
                        break;
                    case 3:
                        cell.scale = 1.15f;
                        break;
                    default:
                        throw new GException("Unhandled case");
                }
                cell.environment=ZCell.ENV_TOWER;
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
            case "spn":
                setSpawnArea(cell, new ZSpawnArea(pos, ZDir.NORTH));
                break;
            case "sps":
                setSpawnArea(cell, new ZSpawnArea(pos, ZDir.SOUTH));
                break;
            case "spe":
                setSpawnArea(cell, new ZSpawnArea(pos, ZDir.EAST));
                break;
            case "spw":
                setSpawnArea(cell, new ZSpawnArea(pos, ZDir.WEST));
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
                // ramparts (wulfsburg) cannot be walked past but can be seen through for ranhed attacks
            case "rn":
                setCellWall(grid, pos, ZDir.NORTH, ZWallFlag.RAMPART);
                break;
            case "rs":
                setCellWall(grid, pos, ZDir.SOUTH, ZWallFlag.RAMPART);
                break;
            case "re":
                setCellWall(grid, pos, ZDir.EAST, ZWallFlag.RAMPART);
                break;
            case "rw":
                setCellWall(grid, pos, ZDir.WEST, ZWallFlag.RAMPART);
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
        // make sure all cols the same length
        for (int i=1; i<rows; i++) {
            if (map[i].length != cols) {
                throw new IllegalArgumentException("Row " +i + " is not same length as rest: " + cols);
            }
        }
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
                throw new IllegalArgumentException("Length of row " + row + " differs");
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
                    if (zone == null) {
                        throw new GException("Problem with cmd: " + map[row][col]);
                    }
                    loadCmd(grid, pos, cmd);
                    // make sure outer perimeter has walls
                }
                if (cell.isCellType(ZCellType.EXIT)) {
                    Utils.assertTrue(exitZone < 0, "Multiple EXIT zones not supported");
                    exitZone = cell.zoneIndex;
                } if (row == 0) {
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

        // do another pass ans make sure all the zone cells are adjacent
        Vector<ZZone> zones = new Vector<>();
        zones.setSize(maxZone+1);
        for (Map.Entry<Integer, ZZone> e : zoneMap.entrySet()) {
            zones.set(e.getKey(), e.getValue());
        }
        // fill in null zones with empty ones
        for (int i=0; i<zones.size(); i++) {
            if (zones.get(i) == null)
                zones.set(i, new ZZone());
            else {
                zones.get(i).checkSanity();
            }
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
            if (cur.getOccupiedZone() == red && game.getBoard().getNumZombiesInZone(red) == 0)
                options.add(ZMove.newObjectiveMove(red));
        }
    }

    /**
     *
     * @param game
     * @param c
     */
    public void processObjective(ZGame game, ZCharacter c) {
        if (redObjectives.remove((Object)c.getOccupiedZone())) {
            game.addExperience(c, getObjectiveExperience(c.getOccupiedZone(), getNumFoundObjectives()));
        }
    }

    protected int getObjectiveExperience(int zoneIdx, int nthFound) {
        return 5;
    }

    /**
     *
     * @return null if game not failed, otherwise a failed reason
     */
    public String getQuestFailedReason(ZGame game) {
        return null;
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
            Utils.shuffle(vaultItemsRemaining);
        }
        return vaultItemsRemaining;
    }

    protected ZEquipment getRandomVaultArtifact() {
        List<ZEquipment> remaining = getVaultItemsRemaining();
        if (remaining.size() > 0) {
            ZEquipment e = Utils.randItem(remaining);
            remaining.remove(e);
            return e;
        }
        Utils.assertTrue(false);
        return null;
    }

    public int getMaxNumZombiesOfType(ZZombieType type) {
        switch (type) {
            case Abomination:
            case Wolfbomination:
                return 1;
            case Necromancer:
                return 2;
            case Wolfz:
                return 22;
            case Walker:
                return 35;
            case Fatty:
            case Runner:
                return 14;
        }
        return Integer.MAX_VALUE;
    }

    public void onEquipmentFound(ZGame game, ZEquipment equip) {
        //
    }

    protected boolean isAllPlayersInExit(ZGame game) {
        Utils.assertTrue(exitZone >= 0);
        return game.getBoard().getNumZombiesInZone(exitZone) == 0 && !(Utils.count(game.board.getAllCharacters(), object -> object.getOccupiedZone() != exitZone) > 0);
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

    public void drawQuest(UIZombicide game, AGraphics g) {}

    public void onNecromancerEscaped(ZGame game, ZZombie z) {
        game.gameLost("Necromancer Escaped");
    }

    public final boolean isWolfBurg() {
        return quest.isWolfburg();
    }

    public void onZombieSpawned(ZGame game, ZZombie zombie, int zone) {
        switch (zombie.type) {
            case Necromancer: {
                game.getBoard().setSpawnZone(zone, ZIcon.SPAWN_GREEN, false, false, true);
                game.spawnZombies(zone);
                break;
            }
        }
    }

    /**
     * Return a spawn card or null if none left. Default behavior is infinite spawn cards
     *
     * @param game
     * @param targetZone
     * @param dangerLevel
     * @return
     */
    public ZSpawnCard drawSpawnCard(ZGame game, int targetZone, ZSkillLevel dangerLevel) {
        return ZSpawnCard.drawSpawnCard(isWolfBurg(), game.canZoneSpawnNecromancers(targetZone), game.getDifficulty());
    }
}
