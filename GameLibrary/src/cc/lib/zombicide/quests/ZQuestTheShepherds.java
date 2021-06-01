package cc.lib.zombicide.quests;

import cc.lib.game.Utils;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;

public class ZQuestTheShepherds extends ZQuest {

    static {
        addAllFields(ZQuestTheShepherds.class);
    }

    public ZQuestTheShepherds() {
        super(ZQuests.The_Shepherds);
    }

    int greenSpawnZone=-1;
    int blueSpawnZone=-1;
    int numTotal=0;

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:gvd1:ws:ode", "z49:i:red:ws:de", "z1", "z2", "z3:spn", "z4:i:ww:ods:red:ode", "z5:i:we", "z6", "z7:i:ww:vd4" },
                { "z8", "z9", "z10", "z11:i:ww:wn:we:ods", "z12",           "z13:i:ww:ds:we", "z5:i:ods:we", "z14", "z7:i:ww:ds" },
                { "z15", "z16:i:wn:ww:ws", "z16:i:wn:ode:ods", "z17:i:de:ods:red", "z18", "z19", "z20", "z21", "z22" },
                { "z23", "z24:i:dw:ws:ode", "z25:i:ws:ode", "z26:i:ws:we", "z27", "z28:i:ww:ws:dn:ode", "z28:i:ws:we:dn:red", "z29", "z30:i:ww:ws:dn" },
                { "z31:spw", "z32", "z33", "z34", "z35", "z36", "z37", "z38", "z39:spe" },
                { "z40:i:red:wn:ode:ws", "z50:i:wn:ws:de", "z41:ws", "z42:i:dw:wn:gvd3:we", "z43:st", "z44:i:dw:wn:vd2:ode", "z45:i:wn:ode", "z46:i:wn", "z46:i:red:wn"},
                { "", "", "", "z47:v:gvd3:ww:wn", "z47:v:wn", "z47:v:wn:we:gvd1", "z48:v:vd2:wn", "z48:v:wn", "z48:v:vd4:wn" }
        };

        return load(map);
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        super.processObjective(game, c, move);
        if (move.integer == blueSpawnZone) {
            game.spawnZombies(blueSpawnZone);
            blueSpawnZone = -1;
        } else if (move.integer == greenSpawnZone) {
            game.spawnZombies(greenSpawnZone);
            greenSpawnZone = -1;
        }
    }

    @Override
    public int getPercentComplete(ZGame game) {
        return (getNumStartRedObjectives() - redObjectives.size()) * 100 / getNumStartRedObjectives();
    }

    @Override
    public boolean isQuestFailed(ZGame game) {
        return false;
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[] {
                new ZTile("1R", 90, ZTile.getQuadrant(0, 0, board)),
                new ZTile("2R", 180, ZTile.getQuadrant(0, 3, board)),
                new ZTile("9V", 270, ZTile.getQuadrant(0, 6, board)),
                new ZTile("3V", 0, ZTile.getQuadrant(3, 0, board)),
                new ZTile("4V", 270, ZTile.getQuadrant(3, 3, board)),
                new ZTile("5R", 180, ZTile.getQuadrant(3, 6, board)),
        };
    }

    @Override
    public void init(ZGame game) {
        numTotal = redObjectives.size();
        while (blueSpawnZone == greenSpawnZone) {
            blueSpawnZone = Utils.randItem(redObjectives);
            greenSpawnZone = Utils.randItem(redObjectives);
        }
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        int numTaken = numTotal - redObjectives.size();
        return new Table(getName()).addRow(
                new Table().setNoBorder()
                    .addRow("Rescue the townsfolk by claiming\nall of the objectives.\nSome townsfolk are infected.", String.format("%d of %d", numTaken, numTotal))
        );
    }
}
