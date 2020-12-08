package cc.lib.zombicide.quests;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCellType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZSkillLevel;
import cc.lib.zombicide.ZTiles;
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
        super("The Black Book");
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:we",        "z1:green:de",  "z2:i:ws:ode",                  "z3:i:red:we:ws",       "z4:sp",    "z5:i:ww:we:vd1",       "z6:v:vd1" },
                { "z0:i:we",        "z7",           "z8",                           "z9",                   "z10",      "z5:i:ww:we:ods",       "z6:v"     },
                { "z0:i:we:ods",    "z11:st",       "z12:i:wn:ww:blackbook:ods:ode","z13:i:wn:we:ods:vd2",  "z14",      "z15:i:ww:we:ods",       "z6:v:vd2" },
                { "z16:i:grds:we:vd3", "z17",        "z18:i:ww:ws:vd4:ode",          "z19:i:ws:we",          "z20",      "z21:i:blds:we:ww",      "z22:wn:v:vd3" },
                { "z23:sp",         "z24",          "z25",                          "z26",                  "z27",      "z28:sp:we",            "z22:v" },
                { "z29:i:red:wn:de","z30",          "z31:i:dw:wn:ode",              "z32:i:red:wn:we",      "z33",      "z34:i:dw:red:wn:we",   "z22:v:vd4" }
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
            blueObjZone = Utils.randItem(redObjectives);
            greenObjZone = Utils.randItem(redObjectives);
        }
        // do this after the above so it does not get mixed in with other objectives. Effect would be player could never access
        redObjectives.add(blackBookZone);
        game.board.setDoorLocked(blueDoor);
        game.board.setDoorLocked(greenDoor);
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        super.processObjective(game, c, move);
        if (move.integer == blueObjZone) {
            game.getCurrentUser().showMessage(c.name() + " has unlocked the Blue Door");
            game.board.setDoor(blueDoor, ZWallFlag.CLOSED);
        }

        if (move.integer == blackBookZone) {
            game.getCurrentUser().showMessage(c.name() + " has found the Black Book");
            blackBookZone = -1;
        }

        if (move.integer == greenObjZone) {
            game.getCurrentUser().showMessage(c.name() + " has unlocked the Green Door. A New Spwn zone has appeared!");
            game.board.setDoor(greenDoor, ZWallFlag.CLOSED);
            game.board.setSpawnZone(greenSpawnZone, true);
            game.spawnZombies(greenSpawnZone);
        }
    }

    @Override
    public boolean isQuestComplete(ZGame game) {
        boolean blackBookTaken = blackBookZone < 0;
        int allVaultItems = getAllVaultOptions().size();
        ZSkillLevel lvl = game.getHighestSkillLevel();

        return blackBookTaken && getNumFoundVaultItems() == allVaultItems && lvl == ZSkillLevel.RED;
    }

    @Override
    public boolean isQuestFailed(ZGame game) {
        return false;
    }

    @Omit
    int [] tileIds = null;


    @Override
    public void drawTiles(AGraphics g, ZBoard board, ZTiles tiles) {
        if (tileIds == null) {
            tileIds = tiles.loadTiles(g, new String [] { "8R", "5R", "4V", "7V"  }, new int [] { 90, 90, 90, 90 });
        }

        if (tileIds.length == 0)
            return;

        GRectangle quadrant2R = new GRectangle(
                board.getCell(0, 0).getRect().getTopLeft(),
                board.getCell(2, 2).getRect().getBottomRight());
        GRectangle quadrant8V = new GRectangle(
                board.getCell(0, 3).getRect().getTopLeft(),
                board.getCell(2, 5).getRect().getBottomRight());
        GRectangle quadrant9V = new GRectangle(
                board.getCell(3, 0).getRect().getTopLeft(),
                board.getCell(5, 2).getRect().getBottomRight());
        GRectangle quadrant1V = new GRectangle(
                board.getCell(3, 3).getRect().getTopLeft(),
                board.getCell(5, 5).getRect().getBottomRight());
        g.drawImage(tileIds[0], quadrant2R);
        g.drawImage(tileIds[1], quadrant8V);
        g.drawImage(tileIds[2], quadrant9V);
        g.drawImage(tileIds[3], quadrant1V);

    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        boolean blackBookTaken = blackBookZone < 0;
        int allVaultItems = getAllVaultOptions().size();
        int numVaultItemsTaken = getNumFoundVaultItems();
        ZSkillLevel lvl = game.getHighestSkillLevel();

        return new Table(getName())
                .addRow(new Table().setNoBorder()
                        .addRow("1.", "Unlock the GREEN Door. GREEN Key hidden among the RED objectives.", "", game.board.getDoor(greenDoor) != ZWallFlag.LOCKED)
                        .addRow("2.", "Unlock the BLUE Door. BLUE Key hidden among the RED objectives.", "", game.board.getDoor(blueDoor) != ZWallFlag.LOCKED)
                        .addRow("3.", "Steal the Black Book in central building.", "", blackBookTaken)
                        .addRow("4.", "Claim all Vault artifacts.", String.format("%d of %d", numVaultItemsTaken, allVaultItems), numVaultItemsTaken == allVaultItems)
                        .addRow("5.", "Get to RED Danger level with at least one survivor.", lvl, lvl == ZSkillLevel.RED)
                );
    }
}
