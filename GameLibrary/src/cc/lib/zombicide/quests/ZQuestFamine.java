package cc.lib.zombicide.quests;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
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
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZItemType;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZTiles;
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

    List<Integer> objectives = new ArrayList<>();

    public ZQuestFamine() {
        super("Famine");
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:ws", "z0:i:ws:ode", "z1:i:red:ds:ode", "z2:i:ws:ode", "z3:i:ws:we", "z4", "z5", "z6:sp", "z7:i:ww:ods" },
                { "z8:sp", "z9", "z10", "z11", "z12", "z13", "z14:i:ww:wn:we", "z15", "z16:i:dw:ws:red:" },
                { "z17:i:lvd1:wn:ode:ods", "z18:i:wn:ws", "z18:i:wn:de:ods", "z19", "z20:i:ww:wn:ws:ode", "z21:i:wn:ds:ode", "z14:i:de:ods", "z22", "z23" },
                { "z24:i:ws:we", "z25", "z26:i:ws:ww:de", "z27", "z28", "z29", "z30:i:dw:ws:we", "z31", "z32:i:dn:ww:" },
                { "z33:sp", "z34", "z35", "z36", "z37:i:lvd2:wn:we:ws:dw", "z38", "z39", "z40", "z32:i:red:ods:ww" },
                { "z41:i:wn:ws:ode", "z42:i:red:wn:ws", "z42:i:wn:ws:de", "z43:ws", "z44:st:ws", "z45:ws", "z46:i:wn:we:dw:ws", "z47:sp:ws", "z48:i:ww:ws" },
                { "", "", "", "z49:v:vd1:ww", "z49:v", "z49:v:vd2:we", "", "", "" }
        };

        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
        switch (cmd) {
            case "red":
                objectives.add(cell.getZoneIndex());
                cell.setCellType(ZCellType.OBJECTIVE_RED, true);
                break;
            case "lvd1":
            case "lvd2":
                super.loadCmd(grid, pos, cmd.substring(1));
                setCellWall(grid, pos, ZDir.DESCEND, ZWallFlag.LOCKED);
                break;
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public void addMoves(ZGame game, ZCharacter cur, List<ZMove> options) {
        for (int obj : objectives) {
            if (cur.getOccupiedZone() == obj) {
                options.add(ZMove.newObjectiveMove(obj));
            }
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        game.addExperience(c, OBJECTIVE_EXP);
        objectives.remove((Object)move.integer);
        if (move.integer == blueKeyZone) {
            game.getCurrentUser().showMessage("Blue key found. Vault unlocked");
            blueKeyZone = -1;
        }
    }

    @Override
    public boolean isQuestComplete(ZGame game) {
        return numSaltedMeatFound >= 2 && numWaterFound >= 2 && numApplesFound >= 2 && isAllLockedInVault(game);
    }

    boolean isAllLockedInVault(ZGame game) {
        int vaultZone = -1;
        for (ZCharacter c : game.getAllLivingCharacters()) {
            ZZone zone = game.board.getZone(c.getOccupiedZone());
            if (zone.type != ZZoneType.VAULT) {
                return false;
            } else {
                vaultZone = c.getOccupiedZone();
            }
        }

        if (vaultZone >= 0) {
            if (game.board.getZombiesInZone(vaultZone).size() > 0)
                return false;
            ZZone zone = game.board.getZone(vaultZone);
            for (ZDoor door : zone.getDoors()) {
                if (!door.isClosed(game.board))
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

    @Omit
    int [] tileIds = null;

    @Override
    public void drawTiles(AGraphics g, ZBoard board, ZTiles tiles) {

        // 3R 2R 1R
        // 8R 6R 5R

        if (tileIds == null) {
            tileIds = tiles.loadTiles(g, new String [] { "3R", "2R", "1R", "8R", "6R", "5R" }, new int [] { 270, 270, 180, 0, 270, 90 });
        }

        if (tileIds.length == 0)
            return;

        GRectangle quadrant3R = new GRectangle(
                board.getCell(0, 0).getRect().getTopLeft(),
                board.getCell(2, 2).getRect().getBottomRight());
        GRectangle quadrant2R = new GRectangle(
                board.getCell(0, 3).getRect().getTopLeft(),
                board.getCell(2, 5).getRect().getBottomRight());
        GRectangle quadrant1R = new GRectangle(
                board.getCell(0, 6).getRect().getTopLeft(),
                board.getCell(2, 8).getRect().getBottomRight());

        GRectangle quadrant8R = new GRectangle(
                board.getCell(3, 0).getRect().getTopLeft(),
                board.getCell(5, 2).getRect().getBottomRight());
        GRectangle quadrant6R = new GRectangle(
                board.getCell(3, 3).getRect().getTopLeft(),
                board.getCell(5, 5).getRect().getBottomRight());
        GRectangle quadrant5R = new GRectangle(
                board.getCell(3, 6).getRect().getTopLeft(),
                board.getCell(5, 8).getRect().getBottomRight());

        g.drawImage(tileIds[0], quadrant3R);
        g.drawImage(tileIds[1], quadrant2R);
        g.drawImage(tileIds[2], quadrant1R);
        g.drawImage(tileIds[3], quadrant8R);
        g.drawImage(tileIds[4], quadrant6R);
        g.drawImage(tileIds[5], quadrant5R);
    }

    @Override
    public void init(ZGame game) {
        blueKeyZone = Utils.randItem(objectives);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
            .addRow(new Table().setNoBorder()
                .addRow("1.", "Find BLUE key to unlock the Vault", blueKeyZone >= 0 ? "no" : "yes")
                .addRow("2.", "Find 2 Apples", String.format("%d of %d", numApplesFound, 2))
                .addRow("3.", "Find 2 Water", String.format("%d of %d", numWaterFound, 2))
                .addRow("4.", "Find 2 Salted Meat", String.format("%d of %d", numSaltedMeatFound, 2))
                .addRow("5.", "Lock youselves in the Vault with no zombies.", isAllLockedInVault(game) ? "yes" : "no")
            );

    }
}
