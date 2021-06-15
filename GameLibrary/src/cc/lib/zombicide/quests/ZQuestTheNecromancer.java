package cc.lib.zombicide.quests;

import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZZombieType;

public class ZQuestTheNecromancer extends ZQuest {

    public ZQuestTheNecromancer() {
        super(ZQuests.The_Necromancer);
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:de:ds", "z1:walker",        "z2",         "z3:walker",    "z4:i:dw:ds" },
                { "z5",         "z6:ws",            "z7:wn:ww:we",      "z8:ws",        "z9" },
                { "z10",    "z11",              "z12:v:necro:abom",              "z13",          "z14" },
                { "z15",        "z16:wn",           "z17:we:ww",     "z18:wn",       "z19" },
                { "z20:i:dn:de","z21",              "z22:st:sps",       "z23",          "z24:i:dn:dw" }
        };

        return load(map);
    }

    @Override
    public int getPercentComplete(ZGame game) {
        return game.getNumKills(ZZombieType.Necromancer) > 0 ? 100 : 0;
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[0];
    }

    @Override
    public void init(ZGame game) {

    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                        .addRow("Kill the Necromancer")
                );
    }

}
