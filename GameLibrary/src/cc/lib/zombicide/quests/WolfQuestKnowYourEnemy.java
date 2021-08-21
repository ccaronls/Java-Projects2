package cc.lib.zombicide.quests;

import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;

public class WolfQuestKnowYourEnemy extends ZQuest {

    public WolfQuestKnowYourEnemy() {
        super(ZQuests.Know_Your_Enemy);
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:we",        "z1:spn", "z3:i:ww", "z4:t1:rw", "z5:t2", "z5:red:v:vvd1:ww:ws" },
                { "z0:i:red:de:ws", "z6",     "z3:we:ds", "z7"     , "z8:t3:rw", "z8:t3" },
                { "z9:exit",        "z10",    "z11",      "z12",     "z8:t3:rs", "z13:i:ww:wn:ws" },

                { "z14:i:dn:red:we:ods", "z15", "z16:i:dn:de:ww:red", "z17", "z18", "z19:spe" },
                { "z20:i:we",       "z21",    "z16:i:ww:we:ods",     "z22",  "z23:i:dn:ww:we:ws:red", "z24" },
                { "z20:i:ds:we",    "z25",    "z26:i:ww:de:ods",     "z27",  "z28",   "z29" },

                { "z30:spw",        "z31:st", "z32:i:ww",            "z33:t2:rw:rn", "z34:t3:rn", "z34:t3:rn" },
                { "z35:i:wn:de:ods","z36",    "z32:iww:ws:we:vvd1",  "z37:t1",       "z34:t3:rw:rs", "z34:t3:rs" },
                { "z38:i:odn:we",   "z39",    "z40",                 "z41",          "z42",         "z43" }

        };

        return load(map);
    }

    @Override
    public int getPercentComplete(ZGame game) {
        return 0;
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[] {
                new ZTile("9V", 0, ZTile.getQuadrant(0, 0, board)),
                new ZTile("11R", 0, ZTile.getQuadrant(0, 3, board)),

                new ZTile("4R", 0, ZTile.getQuadrant(3, 0, board)),
                new ZTile("6R", 0, ZTile.getQuadrant(3, 3, board)),

                new ZTile("1R", 0, ZTile.getQuadrant(6, 0, board)),
                new ZTile("10V", 0, ZTile.getQuadrant(6, 3, board)),

        };
    }

    @Override
    public void init(ZGame game) {

    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return null;
    }
}
