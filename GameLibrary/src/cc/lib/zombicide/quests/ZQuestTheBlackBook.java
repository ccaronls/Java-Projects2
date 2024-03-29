package cc.lib.zombicide.quests;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCellType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZColor;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZSkillLevel;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZWallFlag;

public class ZQuestTheBlackBook extends ZQuest {

    static {
        addAllFields(ZQuestTheBlackBook.class);
    }

    // these are random amongst the red
    int blackBookZone=-1; // TODO: Draw as a black book
    int blueObjZone=-1;
    int greenObjZone=-1;
    int greenSpawnZone=-1;
    ZDoor blueDoor, greenDoor;

    public ZQuestTheBlackBook() {
        super(ZQuests.The_Black_Book);
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:we",        "z1:green:de",  "z2:i:ws:ode",                  "z3:i:red:we:ws",       "z4:spn",    "z5:i:ww:we:vd1",       "z6:v:vd1" },
                { "z0:i:we",        "z7",           "z8",                           "z9",                   "z10",      "z5:i:ww:we:ods",       "z6:v"     },
                { "z0:i:we:ods",    "z11:st",       "z12:i:wn:ww:blackbook:ods:ode","z13:i:wn:we:ods:vd2",  "z14",      "z15:i:ww:we:ods",       "z6:v:vd2" },
                { "z16:i:grds:we:gvd3", "z17",        "z18:i:ww:ws:gvd4:ode",          "z19:i:ws:we",          "z20",      "z21:i:blds:we:ww",      "z22:wn:v:gvd3" },
                { "z23:spw",         "z24",          "z25",                          "z26",                  "z27",      "z28:spe:we",            "z22:v" },
                { "z29:i:red:wn:de","z30",          "z31:i:dw:wn:ode",              "z32:i:red:wn:we",      "z33",      "z34:i:dw:red:wn:we",   "z22:v:gvd4" }
        };
        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
        switch (cmd) {
            case "blackbook":
                blackBookZone = cell.getZoneIndex();
                cell.setCellType(ZCellType.OBJECTIVE_BLACK, true);
                break;

            case "green":
                greenSpawnZone = cell.getZoneIndex();
                break;

            // these are locked until the theor respective objectives found
            case "blds":
                blueDoor = new ZDoor(pos, ZDir.SOUTH, GColor.BLUE);
                break;
            case "grds":
                greenDoor = new ZDoor(pos, ZDir.SOUTH, GColor.GREEN);
                break;

            default:
                super.loadCmd(grid, pos, cmd);

        }
    }


    @Override
    public void init(ZGame game) {
        while (blueObjZone == greenObjZone) {
            blueObjZone = Utils.randItem(getRedObjectives());
            greenObjZone = Utils.randItem(getRedObjectives());
        }
        // do this after the above so it does not get mixed in with other objectives. Effect would be player could never access
        getRedObjectives().add(blackBookZone);
        game.getBoard().setDoorLocked(blueDoor);
        game.getBoard().setDoorLocked(greenDoor);
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        if (c.getOccupiedZone() == blueObjZone) {
            game.unlockDoor(blueDoor);
        }

        if (c.getOccupiedZone() == blackBookZone) {
            game.addLogMessage(c.name() + " has found the Black Book");
            blackBookZone = -1;
        }

        if (c.getOccupiedZone() == greenObjZone) {
            game.addLogMessage(c.name() + " has unlocked the Green Door. A New Spawn zone has appeared!");
            game.unlockDoor(greenDoor);
            game.getBoard().setSpawnZone(greenSpawnZone, ZIcon.SPAWN_GREEN, false, false, true);
            game.spawnZombies(greenSpawnZone);
        }
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int numTasks = getAllVaultOptions().size() + 1 + ZColor.RED.ordinal();
        int numCompleted = blackBookZone < 0 ? 1 : 0;
        numCompleted += getNumFoundVaultItems();
        numCompleted += game.getHighestSkillLevel().getDifficultyColor().ordinal();

        return numCompleted * 100 / numTasks;
    }

    @Override
    public ZTile[] getTiles() {
        return new ZTile[] {
                new ZTile("8R", 90, ZTile.getQuadrant(0, 0)),
                new ZTile("5R", 90, ZTile.getQuadrant(0, 3)),
                new ZTile("4V", 90, ZTile.getQuadrant(3, 0)),
                new ZTile("7V", 90, ZTile.getQuadrant(3, 3)),
        };
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        boolean blackBookTaken = blackBookZone < 0;
        int allVaultItems = getAllVaultOptions().size();
        int numVaultItemsTaken = getNumFoundVaultItems();
        ZSkillLevel lvl = game.getHighestSkillLevel();

        return new Table(getName())
                .addRow(new Table().setNoBorder()
                        .addRow("1.", "Unlock the GREEN Door. GREEN Key hidden among the RED objectives.", "", game.getBoard().getDoor(greenDoor) != ZWallFlag.LOCKED)
                        .addRow("2.", "Unlock the BLUE Door. BLUE Key hidden among the RED objectives.", "", game.getBoard().getDoor(blueDoor) != ZWallFlag.LOCKED)
                        .addRow("3.", "Steal the Black Book in central building.", "", blackBookTaken)
                        .addRow("4.", "Claim all Vault artifacts.", String.format("%d of %d", numVaultItemsTaken, allVaultItems), numVaultItemsTaken == allVaultItems)
                        .addRow("5.", "Get to RED Danger level with at least one survivor.", lvl, lvl.getDifficultyColor() == ZColor.RED)
                );
    }
}
