package cc.lib.zombicide.quests;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZItemType;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZWallFlag;
import cc.lib.zombicide.ZZone;
import cc.lib.zombicide.ZZoneType;

public class ZQuestFamine extends ZQuest {

    static {
        addAllFields(ZQuestFamine.class);
    }

    int numApplesFound = 0;
    int numSaltedMeatFound = 0;
    int numWaterFound = 0;
    int blueKeyZone = -1;
    List<ZDoor> lockedVaults = new ArrayList<>();

    public ZQuestFamine() {
        super(ZQuests.Famine);
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:ws", "z0:i:ws:ode", "z1:i:red:ds:ode", "z2:i:ws:ode", "z3:i:ws:we", "z4", "z5", "z6:spn", "z7:i:ww:ods" },
                { "z8:spw", "z9", "z10", "z11", "z12", "z13", "z14:i:ww:wn:we", "z15", "z16:i:dw:ws:red:" },
                { "z17:i:lvd1:wn:ode:ods", "z18:i:wn:ws", "z18:i:wn:de:ods", "z19", "z20:i:ww:wn:ws:ode", "z21:i:wn:ds:ode", "z14:i:de:ods", "z22", "z23" },
                { "z24:i:ws:we", "z25", "z26:i:ws:ww:de", "z27", "z28", "z29", "z30:i:dw:ws:we", "z31", "z32:i:dn:ww:" },
                { "z33:spw", "z34", "z35", "z36", "z37:i:lvd2:wn:we:ws:dw", "z38", "z39", "z40", "z32:i:red:ods:ww" },
                { "z41:i:wn:ws:ode", "z42:i:red:wn:ws", "z42:i:wn:ws:de", "z43:ws", "z44:st:ws", "z45:ws", "z46:i:wn:we:dw:ws", "z47:spe:ws", "z48:i:ww:ws" },
                { "", "", "", "z49:v:gvd1:ww", "z49:v", "z49:v:gvd2:we", "", "", "" }
        };

        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
        switch (cmd) {
            case "lvd1":
                super.loadCmd(grid, pos, "gvd1");
                setCellWall(grid, pos, ZDir.DESCEND, ZWallFlag.LOCKED);
                lockedVaults.add(new ZDoor(pos, ZDir.DESCEND, GColor.BLUE));
                break;
            case "lvd2":
                super.loadCmd(grid, pos, "gvd2");
                setCellWall(grid, pos, ZDir.DESCEND, ZWallFlag.LOCKED);
                lockedVaults.add(new ZDoor(pos, ZDir.DESCEND, GColor.BLUE));
                break;
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        super.processObjective(game, c, move);
        if (move.integer == blueKeyZone) {
            game.getCurrentUser().showMessage("Blue key found. Vault unlocked");
            for (ZDoor door : lockedVaults) {
                game.getBoard().setDoor(door, ZWallFlag.CLOSED);
            }
            blueKeyZone = -1;
        }
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int numTasks = 7;
        int numCompleted = Math.min(numApplesFound, 2)
                + Math.min(numSaltedMeatFound, 2)
                + Math.min(numWaterFound, 2);
        if (isAllLockedInVault(game)) {
            numCompleted++;
        }
        return numTasks * 100 / numCompleted;
    }

    boolean isAllLockedInVault(ZGame game) {
        int vaultZone = -1;
        for (ZCharacter c : game.getAllLivingCharacters()) {
            ZZone zone = game.getBoard().getZone(c.getOccupiedZone());
            if (zone.getType() != ZZoneType.VAULT) {
                return false;
            } else {
                vaultZone = c.getOccupiedZone();
            }
        }

        if (vaultZone >= 0) {
            if (game.getBoard().getZombiesInZone(vaultZone).size() > 0)
                return false;
            ZZone zone = game.getBoard().getZone(vaultZone);
            for (ZDoor door : zone.getDoors()) {
                if (!door.isClosed(game.getBoard()))
                    return false;
            }
        }

        return true;
    }

    @Override
    public boolean isQuestFailed(ZGame game) {
        return false;
    }

    @Override
    public void onEquipmentFound(ZGame game, ZEquipment equip) {
        if (equip.getType() == ZItemType.APPLES) {
            numApplesFound++;
        } else if (equip.getType() == ZItemType.SALTED_MEAT) {
            numSaltedMeatFound++;
        } else if (equip.getType() == ZItemType.WATER) {
            numWaterFound++;
        }
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[] {
                new ZTile("3R", 270, ZTile.getQuadrant(0, 0, board)),
                new ZTile("2R", 270, ZTile.getQuadrant(0, 3, board)),
                new ZTile("1R", 180, ZTile.getQuadrant(0, 6, board)),
                new ZTile("8R", 0, ZTile.getQuadrant(3, 0, board)),
                new ZTile("6R", 270, ZTile.getQuadrant(3, 3, board)),
                new ZTile("5R", 90, ZTile.getQuadrant(3, 6, board)),
        };
    }

    @Override
    public void init(ZGame game) {
        blueKeyZone = Utils.randItem(redObjectives);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
            .addRow(new Table().setNoBorder()
                .addRow("1.", "Find BLUE key to unlock the Vault", blueKeyZone >= 0)
                .addRow("2.", "Find 2 Apples", String.format("%d of %d", numApplesFound, 2))
                .addRow("3.", "Find 2 Water", String.format("%d of %d", numWaterFound, 2))
                .addRow("4.", "Find 2 Salted Meat", String.format("%d of %d", numSaltedMeatFound, 2))
                .addRow("5.", "Lock youselves in the Vault with no zombies.", isAllLockedInVault(game))
            );

    }
}
