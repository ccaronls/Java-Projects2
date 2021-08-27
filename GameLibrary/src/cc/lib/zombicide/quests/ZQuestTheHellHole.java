package cc.lib.zombicide.quests;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZItemType;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZZombieType;
import cc.lib.zombicide.ZZone;

public class ZQuestTheHellHole extends ZQuest {

    static {
        addAllFields(ZQuestTheHellHole.class);
    }

    int hellHoleZone = -1;
    final List<Integer> objSpawns = new ArrayList<>();
    int numStartObjSpawns = -1;
    boolean hellholeBurnt = false;

    public ZQuestTheHellHole() {
        super(ZQuests.The_Hell_Hole);
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[] {
                new ZTile("7R", 180, ZTile.getQuadrant(0, 0, board)),
                new ZTile("9R", 180, ZTile.getQuadrant(0, 3, board)),
                new ZTile("8V", 90, ZTile.getQuadrant(0, 6, board)),
                new ZTile("2R", 0, ZTile.getQuadrant(3, 0, board)),
                new ZTile("1V", 270, ZTile.getQuadrant(3, 3, board)),
                new ZTile("6V", 270, ZTile.getQuadrant(3, 6, board)),
        };
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:red:necro", "z0:i:ws:ode", "z1:i:ws:ode", "z2:i:ws:de", "z3:st", "z4:i:dw:ode", "z5:i:ods:ode", "z6:i:ode", "z7:i:red:ws:necro" },
                { "z0:i:we:ods", "z8", "z9", "z10", "z11", "z4:i:ds:we:ww", "z12:i:ds:ode", "z6:i:ws:ode", "z13:i:ods" },
                { "z14:i:ds:we", "z15", "z16:i:wn:ww:ode:ods", "z17:i:wn:ws:ode", "z18:i:wn:ws", "z18:i:ds:ode", "z19:ods:i:we", "z20", "z21:i:ww:ds" },
                { "z22:objspawnw", "z23", "z24:i:dw:ods:we",              "z25:i:spn",    "z25:i",        "z25:i:spn:we", "z26:i:ods:de", "z27", "z28:objspawne" },
                { "z29:i:wn:de:ods:red", "z30", "z32:i:ww:ws:we:necro",   "z25:i:ws",     "z25:i:hh:ds", "z25:i:we:ws", "z33:i:ws:we:necro", "z34", "z35:i:red:wn:dw:ods" },
                { "z36:i:we", "z37", "z38", "z39", "z40", "z41", "z42", "z43", "z44:i:ww" }
        };

        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        switch (cmd) {
            case "hh":
                hellHoleZone = grid.get(pos).getZoneIndex();
                break;
            case "objspawnw":
                super.loadCmd(grid, pos, "spw");
                objSpawns.add(grid.get(pos).getZoneIndex());
                break;
            case "objspawne":
                super.loadCmd(grid, pos, "spe");
                objSpawns.add(grid.get(pos).getZoneIndex());
                break;
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        super.processObjective(game, c, move);
        game.giftEquipment(c, ZItemType.DRAGON_BILE.create());
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int numTasks = numStartObjSpawns + 1;
        int numCompleted = numStartObjSpawns - objSpawns.size();
        if (hellholeBurnt)
            numCompleted++;
        return numCompleted * 100 / numTasks;
    }

    @Override
    public void init(ZGame game) {
        numStartObjSpawns = objSpawns.size();
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                        .addRow("1.", "Destroy all spawn zones OUTSIDE the HELLHOLE (Red area). This requires waiting for Necromancers to spawn, killing them, then removing the spawns outside of the hellhole.", String.format("%d of %d", numStartObjSpawns-objSpawns.size(), numStartObjSpawns))
                        .addRow("2.", "Set the Hellhole ablaze using dragon bile AFTER the spawn objectives completed.", hellholeBurnt)
                        .addRow("3.", "The RED Objectives give EXP and a Dragon Bile to the player that takes it.", String.format("%d Left", getRedObjectives().size()))
                );
    }

    @Override
    public void onDragonBileExploded(ZCharacter c, int zoneIdx) {
        if (zoneIdx == hellHoleZone && objSpawns.size() == 0) {
            hellholeBurnt = true;
        }
    }

    @Override
    public void drawQuest(ZGame game, AGraphics g) {
        ZZone hellhole = game.getBoard().getZone(hellHoleZone);
        g.setColor(GColor.RED.withAlpha(.2f));
        hellhole.drawFilled(g);
    }

    @Override
    public int getMaxNumZombiesOfType(ZZombieType type) {
        switch (type) {
            case Necromancer:
                return 6;
        }
        return super.getMaxNumZombiesOfType(type);
    }
}
