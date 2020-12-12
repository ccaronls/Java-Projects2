package cc.lib.zombicide.quests;

import java.util.ArrayList;
import java.util.List;

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
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZEquipmentType;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZItemType;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZTiles;
import cc.lib.zombicide.ZWallFlag;
import cc.lib.zombicide.ZWeaponType;

public class ZQuestTutorial extends ZQuest {

    static {
        addAllFields(ZQuestTutorial.class);
    }


    public ZQuestTutorial() {
        super("Tutorial");
    }

    @Override
    public ZBoard loadBoard() {
        // 6x3
        final String [][] map = {
                { "z0:i",                   "z0:i", "z1:dw:fatty", "z2:i:ws:we", "z3:green:greende:ww", "z4:i:ws:exit" },
                { "z5:spw:wn:ws",            "z6:bluedn:we:walker", "z7:ds:we", "z8:red:wn:ws", "z9",               "z10:red:wn:ws" },
                { "z11:blue:i:wn:ws:ode",   "z12:start:ws:odw:we", "z13:i:ws:dn:runner", "z13:i:wn:we:ws:gvd1", "z14:ws:ww:de", "z15:i:dw:ws:we:wn:gvd2" },
                { "",                       "",                     "",                      "z16:v:wn:ww:gvd1", "z16:v:wn", "z16:v:wn:gvd2" },
        };

        return load(map);
    }

    ZDoor blueDoor=null, greenDoor=null;
    int numRedZones = 0;
    int greenSpawnZone=-1;
    int blueKeyZone=-1;
    int greenKeyZone=-1;

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
        int zoneIndex = cell.getZoneIndex();
        switch (cmd) {
            case "green":
                greenSpawnZone = zoneIndex;
                break;
            case "blue":
                blueKeyZone = zoneIndex;
                cell.setCellType(ZCellType.OBJECTIVE_BLUE, true);
                break;
            case "bluedn":
                blueDoor = new ZDoor(pos, ZDir.NORTH, GColor.BLUE);
                break;
            case "greende":
                greenDoor = new ZDoor(pos, ZDir.EAST, GColor.GREEN);
                break;
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        super.processObjective(game, c, move);
        if (move.integer == blueKeyZone) {
            game.getBoard().setDoor(blueDoor, ZWallFlag.CLOSED);
            game.getCurrentUser().showMessage(c.name() + " has unlocked the BLUE door");
            blueKeyZone = -1;
        } else if (move.integer == greenKeyZone) {
            game.getBoard().setDoor(greenDoor, ZWallFlag.CLOSED);
            game.getBoard().setSpawnZone(greenSpawnZone, true);
            game.spawnZombies(greenSpawnZone);
            game.getCurrentUser().showMessage(c.name() + " has unlocked the GREEN door");
            game.getCurrentUser().showMessage(c.name() + " has created a new spawn zone!");
            greenKeyZone = -1;
        } else {
            //throw new AssertionError("Invalid move for objective: " + move);
        }
    }

    @Omit
    int [] tileIds = null;

    @Override
    public void drawTiles(AGraphics g, ZBoard board, ZTiles tiles) {

        if (tileIds == null) {
            tileIds = tiles.loadTiles(g, new String [] { "4V", "9R" }, new int [] { 90, 90 });
        }

        if (tileIds.length == 0)
            return;

        GRectangle quadrant1 = new GRectangle(board.getCell(0, 0).getRect().getTopLeft(),
                board.getCell(2, 2).getRect().getBottomRight());
        GRectangle quadrant2 = new GRectangle(board.getCell(0, 3).getRect().getTopLeft(),
                board.getCell(2, 5).getRect().getBottomRight());
        g.drawImage(tileIds[0], quadrant1);
        g.drawImage(tileIds[1], quadrant2);
    }


    @Override
    public void init(ZGame game) {
        greenKeyZone = Utils.randItem(redObjectives);
        game.getBoard().setDoorLocked(blueDoor);
        game.getBoard().setDoorLocked(greenDoor);
        redObjectives.add(blueKeyZone); // call this after putting the greenKeyRandomly amongst the red objectives
        numRedZones = redObjectives.size();
    }

    @Override
    public List<ZEquipmentType> getAllVaultOptions() {
        return Utils.toList(ZItemType.DRAGON_BILE, ZItemType.TORCH, ZWeaponType.ORCISH_CROSSBOW, ZWeaponType.INFERNO);

    }

    @Override
    public List<ZEquipment> getInitVaultItems(int vaultZone) {
        List<ZEquipment> vaultItems = new ArrayList<>();
        if (vaultItems == null) {
            vaultItems = new ArrayList<>();
            for (ZEquipmentType et : getAllVaultOptions())
                vaultItems.add(et.create());
        }
        return vaultItems;
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Unlock the BLUE Door.", game.getBoard().getDoor(blueDoor) != ZWallFlag.LOCKED)
                    .addRow("2.", "Unlock the GREEN Door.", game.getBoard().getDoor(greenDoor) != ZWallFlag.LOCKED)
                    .addRow("3.", String.format("Collect all Objectives for %d EXP Each", OBJECTIVE_EXP), String.format("%d of %d", numRedZones- redObjectives.size(), numRedZones))
                    .addRow("4.", "Get all players into the EXIT zone.", isQuestComplete(game))
                    .addRow("5.", "Exit zone must be cleared of zombies.")
                    .addRow("6.", "All Players must survive.")
                );
    }

    @Override
    public boolean isQuestComplete(ZGame game) {
        if (game.getBoard().getZombiesInZone(exitZone).size() > 0)
            return false;
        for (ZCharacter c : game.getAllCharacters()) {
            if (c.getOccupiedZone() != exitZone)
                return false;
        }
        return true;
    }

    @Override
    public boolean isQuestFailed(ZGame game) {
        return Utils.filter(game.getAllCharacters(), object -> object.isDead()).size() > 0;
    }
}
