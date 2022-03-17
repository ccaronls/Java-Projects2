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
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZZone;
import cc.lib.zombicide.ui.UIZombicide;

public class ZQuestDeadTrail extends ZQuest {

    static {
        addAllFields(ZQuestDeadTrail.class);
    }

    final static int NUM_VAULT_ITEMS = 2;

    int blueKeyZone = -1;
    int greenKeyZone = -1;
    ZDoor violetVault1, violetVault2, goldVault;

    public ZQuestDeadTrail() {
        super(ZQuests.Dead_Trail);
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[] {
                new ZTile("5V", 0, ZTile.getQuadrant(0, 0, board)),
                new ZTile("1R", 90, ZTile.getQuadrant(0, 3, board)),
                new ZTile("2R", 0, ZTile.getQuadrant(0, 6, board)),
                new ZTile("3V", 90, ZTile.getQuadrant(3, 0, board)),
                new ZTile("6V", 0, ZTile.getQuadrant(3, 3, board)),
                new ZTile("4R", 90, ZTile.getQuadrant(3, 6, board)),
        };
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:de:ods", "z1:violet_vd1", "z2:i:dw:ods:ode", "z3:i:red:ws:ode", "z4:i:de:ws", "z5", "z6:start", "z7", "z8:i:dw:ods" },
                { "z9:i:ods", "z9:i:wn:ws", "z9:i:we:ods", "z10", "z11", "z12", "z13:i:wn:ww:we:ods", "z14", "z15:i:red:ww:ws" },
                { "z16:i:we:ods", "z17", "z18:i:ww:ds:de", "z19", "z20:i:dw:wn:ws", "z20:i:wn:ode:ods", "z21:i:we:ods", "z22", "z23:spe" },
                { "z24:i:we:ods", "z25", "z26", "z27", "z28:i:ww:ws:ode", "z29:i:gold_vd2:red:ws:ode", "z30:i:ds", "z30:i:wn:ws:ode", "z31:i:dn:ws" },
                { "z32:i:ws:we:red", "z33", "z34:i:dn:ww:we:ods", "z35", "z36", "z37", "z38", "z39", "z40:violet_vd3:spe" },
                { "z41:ws:exit", "z42:sps:ws", "z43:ww:i:ws:ode", "z44:i:red:wn:ws:ode", "z45:i:ws:de:wn", "z46:sps:ws", "z47:i:dw:wn:ws:ode", "z48:i:wn:ws", "z48:i:red:wn:ws" },
                { "", "", "", "", "z49:v:vd1:ww", "z49:v", "z49:v:vd3:we", "z50:v:gvd2", "z50:v" }
        };

        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        switch (cmd) {
            case "violet_vd1":
                super.loadCmd(grid, pos, "vd1");
                violetVault1 = new ZDoor(pos, ZDir.DESCEND, GColor.MAGENTA);
                break;
            case "violet_vd3":
                super.loadCmd(grid, pos, "vd3");
                violetVault2 = new ZDoor(pos, ZDir.DESCEND, GColor.MAGENTA);
                break;
            case "gold_vd2":
                super.loadCmd(grid, pos, "gvd2");
                goldVault = new ZDoor(pos, ZDir.DESCEND, GColor.GOLD);
                break;
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        if (c.getOccupiedZone() == blueKeyZone) {
            game.addLogMessage(c.name() + " has found the BLUE key. Violet Vault doos UNLOCKED!");
            game.unlockDoor(violetVault1);
            game.unlockDoor(violetVault2);
            blueKeyZone = -1;
        } else if (c.getOccupiedZone() == greenKeyZone) {
            game.addLogMessage(c.name() + " has found the GREEN key.");
            greenKeyZone = -1;
        }
        if (game.isDoorLocked(goldVault) && blueKeyZone < 0 && greenKeyZone < 0) {
            game.addLogMessage("Gold Vault door UNLOCKED");
            game.unlockDoor(goldVault);
        }
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int numTasks = getNumStartObjectives() + NUM_VAULT_ITEMS + 1;
        int numComplete = getNumFoundObjectives();
        numComplete += getNumFoundVaultItems();
        if (isAllPlayersInExit(game))
            numComplete ++;
        return numComplete * 100 / numTasks;
    }

    @Override
    public String getQuestFailedReason(ZGame game) {
        if (Utils.count(game.getBoard().getAllCharacters(), object -> object.isDead()) > 0) {
            return "Not all players survived";
        }
        return null;
    }

    @Override
    public void init(ZGame game) {
        while (greenKeyZone == blueKeyZone) {
            greenKeyZone = Utils.randItem(getRedObjectives());
            blueKeyZone = Utils.randItem(getRedObjectives());
        }
        game.lockDoor(violetVault1);
        game.lockDoor(violetVault2);
        game.lockDoor(goldVault);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Take all Objectives", String.format("%d of %d", getNumFoundObjectives(), getNumStartObjectives()))
                    .addRow("2.", "Key to Violet Vault is hidden among the RED objectives", blueKeyZone == -1 ? "Found" : "Not Found")
                    .addRow("3.", "Key to Gold Vault is hidden among the RED objectives", greenKeyZone == -1 ? "Found" : "Not Found")
                    .addRow("4.", "Take all vault artifacts", String.format("%d of %d", getNumFoundVaultItems(), NUM_VAULT_ITEMS))
                    .addRow("5.", "Get all survivors to the exit zone")
                );
    }

    @Override
    public void drawQuest(UIZombicide game, AGraphics g) {
        if (true) {
            return;
        }

        if (blueKeyZone >= 0) {
            ZZone z = game.getBoard().getZone(blueKeyZone);
            for (Grid.Pos p : z.getCells()) {
                ZCell cell = game.getBoard().getCell(p);
                if (cell.isCellType(ZCellType.OBJECTIVE_RED)) {
                    GRectangle redX = new GRectangle(cell).scaledBy(.2f, .2f);
                    g.setColor(GColor.BLUE);
                    g.drawLine(redX.getTopLeft(), redX.getBottomRight(), 5);
                    g.drawLine(redX.getTopRight(), redX.getBottomLeft(), 5);
                }
            }
        }

        if (greenKeyZone >= 0) {
            ZZone z = game.getBoard().getZone(greenKeyZone);
            for (Grid.Pos p : z.getCells()) {
                ZCell cell = game.getBoard().getCell(p);
                if (cell.isCellType(ZCellType.OBJECTIVE_RED)) {
                    GRectangle redX = new GRectangle(cell).scaledBy(.2f, .2f);
                    g.setColor(GColor.GREEN);
                    g.drawLine(redX.getTopLeft(), redX.getBottomRight(), 5);
                    g.drawLine(redX.getTopRight(), redX.getBottomLeft(), 5);
                }
            }
        }

    }
}
