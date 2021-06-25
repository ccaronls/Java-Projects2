package cc.lib.zombicide.quests;

import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZZombieType;

public class ZQuestTheAbomination extends ZQuest {

    static {
        addAllFields(ZQuestTheAbomination.class);
    }

    public ZQuestTheAbomination() {
        super(ZQuests.The_Abomination);
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:de:ds", "z1",       "z2:spn", "z3", "z4:i:dw:ds" },
                { "z5",         "z6",       "z7:abom:i:ws:wn", "z8", "z9" },
                { "z10:spw",     "z11",      "z12:st", "z13", "z14:spe" },
                { "z15:i:ode:wn",        "z16",      "z17:t:st:rn:re:rw", "z18:i:wn:ode:ods", "z19" },
                { "z20:i:dn:ode","z21:i:wn:ode",      "z22", "z23", "z24:i:dn:dw" }
        };

        return load(map);
    }

    @Override
    public int getPercentComplete(ZGame game) {
        return game.getNumKills(ZZombieType.Abomination) > 0 ? 100 : 0;
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
                .addRow("Kill the Abomination")
            );
    }
}
