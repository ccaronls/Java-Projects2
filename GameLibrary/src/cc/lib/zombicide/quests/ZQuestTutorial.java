package cc.lib.zombicide.quests;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.GColor;
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
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZItemType;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZWallFlag;
import cc.lib.zombicide.ZWeaponType;

public class ZQuestTutorial extends ZQuest {

    static {
        addAllFields(ZQuestTutorial.class);
    }

    public ZQuestTutorial() {
        super(ZQuests.Tutorial);
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
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        if (c.getOccupiedZone() == blueKeyZone) {
            game.unlockDoor(blueDoor);
            game.addLogMessage(c.name() + " has unlocked the BLUE door");
            blueKeyZone = -1;
        } else if (c.getOccupiedZone() == greenKeyZone) {
            game.unlockDoor(greenDoor);
            game.getBoard().setSpawnZone(greenSpawnZone, ZIcon.SPAWN_GREEN, false, true, true);
            game.spawnZombies(greenSpawnZone);
            game.addLogMessage(c.name() + " has unlocked the GREEN door");
            game.addLogMessage(c.name() + " has created a new spawn zone!");
            greenKeyZone = -1;
        }
    }

    @Override
    public ZTile [] getTiles() {
        return new ZTile[] {
                new ZTile("9R", 90, ZTile.getQuadrant(0, 0)),
                new ZTile("4V", 90, ZTile.getQuadrant(0, 3))
        };
    }

    @Override
    public void init(ZGame game) {
        greenKeyZone = Utils.randItem(getRedObjectives());
        game.getBoard().setDoorLocked(blueDoor);
        game.getBoard().setDoorLocked(greenDoor);
        getRedObjectives().add(blueKeyZone); // call this after putting the greenKeyRandomly amongst the red objectives
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
                    .addRow("2.", "Unlock the GREEN Door. GREEN key hidden among RED objectives.", game.getBoard().getDoor(greenDoor) != ZWallFlag.LOCKED)
                    .addRow("3.", String.format("Collect all Objectives for %d EXP Each", getObjectiveExperience(0,0)), String.format("%d of %d", getNumFoundObjectives(), getNumStartObjectives()))
                    .addRow("4.", "Get all players into the EXIT zone.", isAllPlayersInExit(game))
                    .addRow("5.", "Exit zone must be cleared of zombies.", isExitClearedOfZombies(game))
                    .addRow("6.", "All Players must survive.")
                );
    }


    @Override
    public int getPercentComplete(ZGame game) {
        int numTasks = getNumStartObjectives() + game.getAllCharacters().size();
        int numCompleted = getNumFoundObjectives();
        for (ZCharacter c : game.getBoard().getAllCharacters()) {
            if (c.getOccupiedZone() == getExitZone())
                numCompleted++;
        }
        int percentCompleted = numCompleted*100 / numTasks;
        if (game.getBoard().getZombiesInZone(getExitZone()).size() > 0)
            percentCompleted --;
        return percentCompleted;
    }

    @Override
    public String getQuestFailedReason(ZGame game) {
        if (Utils.count(game.getBoard().getAllCharacters(), object -> object.isDead()) > 0) {
            return "Not all players survived.";
        }
        return super.getQuestFailedReason(game);
    }

}
