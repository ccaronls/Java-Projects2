package cc.lib.zombicide.quests;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZWallFlag;

public class ZQuestDeadTrail extends ZQuest9x6 {

    static {
        addAllFields(ZQuestDeadTrail.class);
    }

    final static int NUM_VAULT_ITEMS = 2;

    int numObjectives = 0;
    int blueKeyZone = -1;
    int greenKeyZone = -1;
    ZDoor violetVault1, violetVault2, goldVault;

    public ZQuestDeadTrail() {
        super("Dead Trail");
    }

    @Override
    protected String[] getTileIds() {
        return new String[] { "5V", "1R", "2R", "3V", "6V", "4R" };
    }

    @Override
    protected int[] getTileRotations() {
        return new int[] { 0, 90, 0, 90, 0, 90 };
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
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        super.processObjective(game, c, move);
        if (move.integer == blueKeyZone) {
            game.getCurrentUser().showMessage(c.name() + " has found the BLUE key. Violet Vault doos UNLOCKED!");
            game.getBoard().setDoor(violetVault1, ZWallFlag.CLOSED);
            game.getBoard().setDoor(violetVault2, ZWallFlag.CLOSED);
            blueKeyZone = -1;
        } else if (move.integer == greenKeyZone) {
            game.getCurrentUser().showMessage(c.name() + " has found the GREEN key.");
            greenKeyZone = -1;
        }
        if (blueKeyZone < 0 && greenKeyZone < 0) {
            game.getCurrentUser().showMessage("Gold Vault door UNLOCKED");
            game.getBoard().setDoor(goldVault, ZWallFlag.CLOSED);
        }
    }

    @Override
    public boolean isQuestComplete(ZGame game) {
        return redObjectives.size() == 0 && getNumFoundVaultItems() == NUM_VAULT_ITEMS && isAllPlayersInExit(game);
    }

    @Override
    public boolean isQuestFailed(ZGame game) {
        return Utils.filter(game.getAllCharacters(), object -> object.isDead()).size() > 0;
    }

    @Override
    public void init(ZGame game) {
        numObjectives = redObjectives.size();
        while (greenKeyZone == blueKeyZone) {
            greenKeyZone = Utils.randItem(redObjectives);
            blueKeyZone = Utils.randItem(redObjectives);
        }
        game.getBoard().setDoor(violetVault1, ZWallFlag.LOCKED);
        game.getBoard().setDoor(violetVault2, ZWallFlag.LOCKED);
        game.getBoard().setDoor(goldVault, ZWallFlag.LOCKED);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Take all Objectives", String.format("%d of %d", numObjectives-redObjectives.size(), numObjectives))
                    .addRow("2.", "Find the BLUE key", blueKeyZone == -1)
                    .addRow("3.", "Find the GREEN key", greenKeyZone == -1)
                    .addRow("4.", "Take all vault artifacts", String.format("%d of %d", getNumFoundVaultItems(), NUM_VAULT_ITEMS))
                    .addRow("5.", "Get all survivors to the exit zone")
                );
    }
}
