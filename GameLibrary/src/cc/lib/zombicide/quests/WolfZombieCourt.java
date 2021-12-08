package cc.lib.zombicide.quests;

import cc.lib.game.Utils;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;

/**
 * Created by Chris Caron on 9/1/21.
 */
public class WolfZombieCourt extends ZQuest {

    static {
        addAllFields(WolfZombieCourt.class);
    }

    int blueObjZone = -1;
    int greenObjZone = -1;

    public WolfZombieCourt() {
        super(ZQuests.Zombie_Court);
    }

    @Override
    public ZBoard loadBoard() {
        String [][] map = {
                { "z0:i:we:ws","z1:t3","z1:t3:re:gvd1",                "z10:spn","z11:i:ww:ws:ode","z12:i:ds:de:red",           "z20","z21:i:ds:ode:ww","z22:i:red:ws"   },
                { "z2:t2","z1:t3:rs","z1:t3:rs:re",                 "z13","z14","z15",                                      "z23","z24","z25:spe"   },
                { "z3:t2:rs","z4:t1:rs","z5",                       "z16:i:dn:dw:ode:ods","z17:i:wn:de:ws","z18",           "z26:i:dw:wn:ode","z27:i:wn:we:red","z28"     },

                { "z30:i:vd1:ds:ode","z31:i:red:we:ws","z32",       "z40:i:dw:ws:red","z40:i:ws:we","z41",                  "z50:t3:rn:rw:vd2","z50:t3:rn:re","z51" },
                { "z33","z34","z35:st",                             "z42","z43","z44",                                      "z50:t3:rw","z50:t3:rs:re","z52"     },
                { "z36:spw:ws","z37:i:ww:wn:red:ws","z37:i:wn:ode:ws",        "z45:i:gvd2:dn","z45:i:wn:we","z46:sps",        "z53:t2:rw","z54:t1","z55:red"    },

                { "","","",                                         "z60:v:gvd1:ww:wn","z60:v:wn","z60:v:gvd2:ww:wn",       "z61:v:vd1:wn","z61:v:wn","z61:v:vd2:wn" }
        };

        return load(map);
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[] {
                new ZTile("11R", 0, ZTile.getQuadrant(0, 0, board)),
                new ZTile("6V", 0, ZTile.getQuadrant(0, 3, board)),
                new ZTile("3V", 0, ZTile.getQuadrant(0, 6, board)),

                new ZTile("1R", 90, ZTile.getQuadrant(3, 0, board)),
                new ZTile("9V", 180, ZTile.getQuadrant(3, 3, board)),
                new ZTile("10V", 90, ZTile.getQuadrant(3, 6, board))
        };
    }

    @Override
    public void init(ZGame game) {
        while (blueObjZone == greenObjZone) {
            blueObjZone = Utils.randItem(getRedObjectives());
            greenObjZone = Utils.randItem(getRedObjectives());
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        if (c.getOccupiedZone() == greenObjZone) {
            game.addLogMessage(c.name() + " has found the PENDANT");
            greenObjZone = -1;
        } else if (c.getOccupiedZone() == blueObjZone) {
            game.addLogMessage(c.name() + " has found the CROWN");
            blueObjZone = -1;
        } else {
            game.spawnZombies(c.getOccupiedZone());
        }
    }


    @Override
    public int getPercentComplete(ZGame game) {
        return getNumFoundObjectives()*100 / getNumStartRedObjectives();
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Collect all objectives", String.format("%d of %d", getNumFoundObjectives(), getNumStartRedObjectives()))
                    .addRow("2.", "Find the Crown", blueObjZone < 0)
                    .addRow("3.", "Find the Pendant", greenObjZone < 0)
                    .addRow("4.", "Find Vault Artifacts", getNumFoundVaultItems())
                );
    }
}
