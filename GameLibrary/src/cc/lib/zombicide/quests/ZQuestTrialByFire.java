package cc.lib.zombicide.quests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
import cc.lib.zombicide.ZItemType;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZWallFlag;
import cc.lib.zombicide.ZWeapon;
import cc.lib.zombicide.ZWeaponType;
import cc.lib.zombicide.ZZombieType;

public class ZQuestTrialByFire extends ZQuest {

    static {
        addAllFields(ZQuestTrialByFire.class);
    }

    int numTotal = 0;
    int blueObjZone = 0;
    ZWeapon blueObjTreasure;
    ZDoor lockedVault;

    public ZQuestTrialByFire() {
        super(ZQuests.Trial_by_Fire);
    }

    @Override
    public ZBoard loadBoard() {

        final String [][] map = {
            { "z0:i:red:we:ods", "z1:i:ods:ode", "z2:i:ws:ode", "z3:i:ws", "z3:i:ws:ode", "z4:i:ds:ode", "z5:i:ws:we", "z6:sp", "z7:v:vd1:ww:ws" },
            { "z8:i:ods",        "z8:i:ws:we",   "z9:ws",       "z10:ws",  "z11:ws",      "z12:ws",      "z13:ws",     "z14",   "z15:ws" },
            { "z16:i:ode:ods", "z17:i:ws:ode",   "z18:i:ods:ode", "z19:i:ode:ds", "z20:i:ws", "z20:i:ds:ode", "z21:i:we:ods", "z22", "z23:i:ww:ods" },
            { "z24:i",         "z24:i:ode:ws",   "z68:i:de:ws",   "z25",     "z26",     "z27",    "z28:i:dw:ws:we",    "z29",    "z30:i:red:ww" },
            { "z24:i:ods:we",  "z31",            "z32",           "z33:st",  "z34:i:wn:ws:we:ww",  "z35", "z36", "z37", "z30:i:ww:ods" },
            { "z38:i:ds:we",   "z39",            "z40:i:de:wn:ods:ww", "z41", "z42", "z43", "z44:i:dw:wn:lvd1:we:ods", "z45", "z46:i:odn:ww:ods" },
            { "z47:spw",        "z48",            "z49:i:ww:ode:ods", "z50:i:dn", "z50:i:wn", "z50:i:dn", "z51:i:odn:we:ods", "z52", "z53:i:ww" },
            { "z54:i:wn:de:ods", "z55",          "z56:i:we:ws:ww",      "z50:i:ws", "z50:i:ws:abom", "z50:i:ws:we", "z57:i:ws:we", "z58", "z53:i:dw:ws" },
            { "z59:i:we:red",  "z60", "z61", "z62", "z63", "z64", "z65", "z66", "z67:spe" }
        };

        return load(map);
    }

    @Override
    public int getPercentComplete(ZGame game) {
        return game.getNumKills(ZZombieType.Abomination) > 0 ? 100 : 0;
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[] {
                //9V
                new ZTile("8V",  0, ZTile.getQuadrant(0, 0, board)),
                new ZTile("3R", 270, ZTile.getQuadrant(0, 3, board)),
                new ZTile("4V", 90, ZTile.getQuadrant(0, 6, board)),

                new ZTile("7R", 180, ZTile.getQuadrant(3, 0, board)),
                new ZTile("6R", 270, ZTile.getQuadrant(3, 3, board)),
                new ZTile("5R", 90, ZTile.getQuadrant(3, 6, board)),

                new ZTile("2R",  0, ZTile.getQuadrant(6, 0, board)),
                new ZTile("1V", 270, ZTile.getQuadrant(6, 3, board)),
                new ZTile("9V", 270, ZTile.getQuadrant(6, 6, board)),
        };
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
        switch (cmd) {
            case "lvd1":
                lockedVault = new ZDoor(pos, ZDir.DESCEND, GColor.MAGENTA);
                setVaultDoor(cell, grid, pos,  ZCellType.VAULT_DOOR_VIOLET, 1, ZWallFlag.LOCKED);
                break;

            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public void init(ZGame game) {
        numTotal = redObjectives.size();
        blueObjZone = Utils.randItem(redObjectives);
    }

    @Override
    public List<ZEquipmentType> getAllVaultOptions() {
        return new ArrayList<>(Arrays.asList(ZItemType.DRAGON_BILE, ZItemType.DRAGON_BILE, ZItemType.DRAGON_BILE, ZItemType.DRAGON_BILE));
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        if (c.getOccupiedZone() == blueObjZone) {
            if (blueObjTreasure == null)
                blueObjTreasure = Utils.randItem(Arrays.asList(ZWeaponType.ORCISH_CROSSBOW, ZWeaponType.INFERNO)).create();

            if (game.tryGiftEquipment(c, blueObjTreasure)) {
                blueObjZone = -1;
            } else {
                // if the character is too full to accept the gift then set a temp objective to hold
                // the gift until they or some other character can accept it. The new objective will not give exp.
                game.getBoard().setObjective(c.getOccupiedCell(), ZCellType.OBJECTIVE_BLUE);
            }
        }
        super.processObjective(game, c, move);
        if (redObjectives.size() == 0 && game.getBoard().getDoor(lockedVault) == ZWallFlag.LOCKED) {
            game.getCurrentUser().showMessage(c.name() + " has unlocked the Violet Door");
            game.unlockDoor(lockedVault);
        }
    }

    @Override
    public void addMoves(ZGame game, ZCharacter cur, List<ZMove> options) {
        super.addMoves(game, cur, options);
        if (cur.getOccupiedZone() == blueObjZone && !redObjectives.contains(blueObjZone)) {
            options.add(ZMove.newObjectiveMove(cur.getOccupiedZone()));
        }
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        boolean blueObjFound = blueObjZone < 0;
        int numTaken = numTotal - redObjectives.size();

        return new Table(getName())
                .addRow(new Table().setNoBorder()
                        .addRow("1.", "Kill the Abomination.", getPercentComplete(game) == 100)
                        .addRow("2.", "Blue objective hidden among the red objectives gives a random artifact", blueObjFound)
                        .addRow("3.", "All Dragon Bile hidden in the vault. Vault cannot be opened until all objectives taken.", String.format("%d of %d", numTaken, numTotal))
                );
    }

    @Override
    public void processSearchables(List<ZEquipment> items) {
        Iterator<ZEquipment> it = items.iterator();
        while (it.hasNext()) {
            if (it.next().getType() == ZItemType.DRAGON_BILE)
                it.remove();
        }
    }
}
