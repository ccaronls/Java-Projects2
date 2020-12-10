package cc.lib.zombicide.quests;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCellType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZTiles;
import cc.lib.zombicide.ZZombieType;

public class ZQuestBigGameHunting extends ZQuest {

    static {
        addAllFields(ZQuestBigGameHunting.class);
    }

    @Omit
    String [][] map = {
            { "z0:i:wn:ww:ds:gvd1",  "z29:i:wn:de:ws:red:odw", "z1:sp:wn:de",      "z2:i:wn:ode:ws",   "z3:i:red:wn:ode:ws",   "z9:i:vd2:wn:we",  "z28:v:wn:we:vd2" },
            { "z5:ww",              "z6",                     "z7",                     "z8:we",            "z9:i:ods",           "z9:i:ods:we",     "z28:v:we" },
            { "z10:ww:we:start",    "z11:i:ww:wn::ws:red:ode","z12:i:wn:ds:ode",        "z13:i:wn:red:ds:we","z14:i:ws:we:odn",   "z15:i:ds:we:odn", "z28:v:we:ws:vd3" },
            { "z16:ww:ds",          "z17",                    "z18",                    "z19",              "z20",                "z21:we:spe:dn:ds", "z27:v:we:gvd1" },
            { "z22:i:ww:we:vd3",    "z23",                    "z24:i:wn:ww:we",         "z25:i:wn",         "z25:i:wn",           "z25:i:dn:we",     "z27:v:we" },
            { "z22:i:ww:red:ws:de", "z26:ws:sps:de",           "z24:i:red:ww:we:ws:dw",  "z25:i:ws",         "z25:i:blue:ws",      "z25:i:ws:gvd4:we", "z27:v:we:ws:gvd4" }

    };

    int blueObjZone = -1;
    int blueRevealZone = -1;

    public ZQuestBigGameHunting() {
        super("Big Game Hunting");
    }

    @Override
    public ZBoard loadBoard() {
        return load(map);
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        super.processObjective(game, c, move);
        // check for necro / abom in special spawn places
        game.board.getZone(c.getOccupiedZone()).objective = false;
        if (move.integer == blueRevealZone) {
            redObjectives.add(blueObjZone);
            game.getCurrentUser().showMessage("The Labratory objective is revealed!");
            game.board.getZone(blueObjZone).objective = true;
            game.spawnZombies(1, ZZombieType.Necromancer, blueObjZone);
            blueRevealZone = -1;
        }
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
        switch (cmd) {
            case "blue":
                blueObjZone = cell.getZoneIndex();
                cell.setCellType(ZCellType.OBJECTIVE_BLUE, true);
                break;

            default:
                super.loadCmd(grid, pos, cmd);

        }
    }

    @Override
    public void init(ZGame game) {
        blueRevealZone = Utils.randItem(redObjectives);
        game.board.getZone(blueObjZone).objective = false; // this does not get revealed until the blueRevealZone found
    }

    @Omit
    int [] tileIds = null;

    @Override
    public void drawTiles(AGraphics g, ZBoard board, ZTiles tiles) {
        if (tileIds == null) {
            tileIds = tiles.loadTiles(g, new String [] { "2R", "8V", "9V", "1V"  }, new int [] { 90, 180, 90, 90 });
        }

        if (tileIds.length == 0)
            return;

        GRectangle quadrant2R = new GRectangle(
                board.getCell(0, 0).getRect().getTopLeft(),
                board.getCell(2, 2).getRect().getBottomRight());
        GRectangle quadrant8V = new GRectangle(
                board.getCell(0, 3).getRect().getTopLeft(),
                board.getCell(2, 5).getRect().getBottomRight());
        GRectangle quadrant9V = new GRectangle(
                board.getCell(3, 0).getRect().getTopLeft(),
                board.getCell(5, 2).getRect().getBottomRight());
        GRectangle quadrant1V = new GRectangle(
                board.getCell(3, 3).getRect().getTopLeft(),
                board.getCell(5, 5).getRect().getBottomRight());
        g.drawImage(tileIds[0], quadrant2R);
        g.drawImage(tileIds[1], quadrant8V);
        g.drawImage(tileIds[2], quadrant9V);
        g.drawImage(tileIds[3], quadrant1V);

    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        boolean allObjCollected = redObjectives.size() == 0 && blueRevealZone < 0;
        boolean exposeLaboratory = blueRevealZone < 0;
        boolean necroKilled = game.getNumKills(ZZombieType.Necromancer) > 0;
        boolean abimKilled = game.getNumKills(ZZombieType.Abomination) > 0;

        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Collect all objectives. One of the objectives\nexposes the laboratory objective.", allObjCollected)
                    .addRow("2.", "Find the Laboratory Objective", exposeLaboratory)
                    .addRow("3.", "Kill at least 1 Necromancer.", necroKilled)
                    .addRow("4.", "Kill at least 1 Abomination.", abimKilled)
                    .addRow("5.", "Not all players need to survive.")
                );
    }

    @Override
    public boolean isQuestComplete(ZGame game) {
        return redObjectives.size() == 0 && blueRevealZone < 0 && game.getNumKills(ZZombieType.Abomination) > 0 && game.getNumKills(ZZombieType.Necromancer) > 0;
    }

    @Override
    public boolean isQuestFailed(ZGame game) {
        return game.getAllLivingCharacters().size() == 0;
    }
}
