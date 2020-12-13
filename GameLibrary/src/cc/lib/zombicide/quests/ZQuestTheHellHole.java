package cc.lib.zombicide.quests;

import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZItemType;
import cc.lib.zombicide.ZMove;

public class ZQuestTheHellHole extends ZQuest9x6 {

    static {
        addAllFields(ZQuestTheHellHole.class);
    }

    int hellHoleZone = -1;

    public ZQuestTheHellHole() {
        super("The Hellhole");
    }

    @Override
    protected String[] getTileIds() {
        return new String[] { "7R", "9R", "8V", "2R", "1V", "6V" };
    }

    @Override
    protected int[] getTileRotations() {
        return new int[] { 180,180,90,0,270,270 };
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:red", "z0:i:ws:ode", "z1:i:ws:ode", "z2:i:ws:de", "z3:st", "z4:i:dw:ode", "z5:i:ods:ode", "z6:i:ode", "z7:i:red:ws" },
                { "z0:i:we:ods", "z8", "z9", "z10", "z11", "z4:i:ds:we:ww", "z12:i:ds:ode", "z6:i:ws:ode", "z13:i:ods" },
                { "z14:i:ds:we", "z15", "z16:i:wn:ww:ode:ods", "z17:i:wn:ws:ode", "z18:i:wn:ws", "z18:i:ds:ode", "z19:ods:i:we", "z20", "z21:i:ww:ds" },
                { "z22:spw", "z23", "z24:i:dw:ods:we",              "z25:i:spn",    "z25:i",        "z25:i:spn:we", "z26:i:ods:de", "z27", "z28:spe" },
                { "z29:i:wn:de:ods:red", "z30", "z32:i:ww:ws:we",   "z25:i:ws",     "z25:i:hh:ds", "z25:i:we:ws", "z33:i:ws:we", "z34", "z35:i:red:wn:dw:ods" },
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
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        super.processObjective(game, c, move);
        c.attachEquipment(ZItemType.DRAGON_BILE.create());
    }

    @Override
    public boolean isQuestComplete(ZGame game) {
        return false;
    }

    @Override
    public boolean isQuestFailed(ZGame game) {
        return false;
    }

    @Override
    public void init(ZGame game) {

    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return null;
    }
}
