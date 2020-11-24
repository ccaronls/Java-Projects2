package cc.lib.zombicide.quests;

import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZTiles;
import cc.lib.zombicide.ZZombieType;

public class ZQuestTheAbomination extends ZQuest {

    public ZQuestTheAbomination() {
        super("The Abomination");
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:de:ds", "z1",       "z2:abom", "z3", "z4:i:dw:ds" },
                { "z5",         "z6",       "z7", "z8", "z9" },
                { "z10:sp",     "z11",      "z12", "z13", "z14:sp" },
                { "z15",        "z16",      "z17", "z18", "z19" },
                { "z20:i:dn:de","z21",      "z22:st", "z23", "z24:i:dn:dw" }
        };

        return load(map);
    }

    @Override
    public void addMoves(ZGame game, ZCharacter cur, List<ZMove> options) {

    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {

    }

    @Override
    public boolean isQuestComplete(ZGame game) {
        return game.getNumKills(ZZombieType.Abomination) > 0;
    }

    @Override
    public boolean isQuestFailed(ZGame game) {
        return false;
    }

    @Override
    public void drawTiles(AGraphics g, ZBoard board, ZTiles tiles) {

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
