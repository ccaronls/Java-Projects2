package cc.lib.zombicide.quests;

import java.util.ArrayList;
import java.util.List;

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
import cc.lib.zombicide.ZZombieName;
import cc.lib.zombicide.ZZombieType;

public class ZQuestBigGameHunting extends ZQuest {

    static {
        addAllFields(ZQuestBigGameHunting.class);
    }

    @Omit
    String [][] map = {
            { "z0:i:wn:ww:ds:vd1", "z0:i:wn:de:ws:red:odw", "z1:sp:wn:de",      "z2:i:wn:ode:ws",   "z3:i:red:wn:ode:ws",   "z9:i:vd2:wn:we",  "z28:v:wn:we:vd2" },
            { "z5:ww",              "z6",                   "z7",               "z8:we",            "z9:i:ods",             "z9:i:ods:we",     "z28:v:we" },
            { "z10:ww:we:start",    "z11:i:ww:wn::ws:red:ode", "z12:i:wn:ds:ode","z13:i:wn:red:ds:we","z14:i:ws:we:odn",    "z15:i:ds:we:odn", "z28:v:we:ws:vd3" },
            { "z16:ww:ds",          "z17",                  "z18",              "z19",              "z20",                  "z21:we:sp:dn:ds", "z27:v:we:vd1" },
            { "z22:i:ww:we:vd3",    "z23",                  "z24:i:wn:ww:we",   "z25:i:wn",         "z25:i:wn",             "z25:i:dn:we",     "z27:v:we" },
            { "z22:i:ww:red:ws:de", "z26:ws:sp:de",         "z24:i:red:ww:we:ws:dw", "z25:i:ws",    "z25:i:blue:ws",        "z25:i:ws:vd4:we", "z27:v:we:ws:vd4" }

    };

    List<Integer> redObjectives = new ArrayList<>();
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
    public void addMoves(ZGame game, ZCharacter cur, List<ZMove> options) {
        for (int red : redObjectives) {
            if (cur.getOccupiedZone() == red)
                options.add(ZMove.newObjectiveMove(red));
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        game.addExperience(c, OBJECTIVE_EXP);
        // check for necro / abom in special spawn places
        game.board.getZone(c.getOccupiedZone()).objective = false;
        redObjectives.remove((Object)c.getOccupiedZone());
        if (move.integer == blueRevealZone) {
            redObjectives.add(blueObjZone);
            game.getCurrentUser().showMessage("The Labratory objective is revealed!");
            game.board.getZone(blueObjZone).objective = true;
            game.spawnZombie(ZZombieType.NECROMANCERS, blueObjZone);
            blueRevealZone = -1;
        }
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
        switch (cmd) {
            case "red":
                redObjectives.add(cell.getZoneIndex());
                cell.cellType = ZCellType.OBJECTIVE;
                break;

            case "blue":
                blueObjZone = cell.getZoneIndex();
                cell.cellType = ZCellType.OBJECTIVE;
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
                board.getCell(5, 3).getRect().getBottomRight());
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
        boolean necroKilled = game.getNumKills(ZZombieName.Necromancer) > 0;
        boolean abimKilled = game.getNumKills(ZZombieName.Abomination) > 0;


        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Collect all objectives. One of the objectives\nexposes the laboratory objective.", allObjCollected ? "(x)" : "")
                    .addRow("2.", "Kill at least 1 Abomination and 1 Necromancer.", necroKilled ? "(x)" : "")
                    .addRow("3.", "Not all players need to survive.", abimKilled ? "(x)" : ""));
    }

    @Override
    public boolean isQuestComplete(ZGame game) {
        return redObjectives.size() == 0 && blueRevealZone < 0 && game.getNumKills(ZZombieName.Abomination) > 0 && game.getNumKills(ZZombieName.Necromancer) > 0;
    }

    @Override
    public boolean isQuestFailed(ZGame game) {
        return game.getAllLivingCharacters().size() == 0;
    }
}
