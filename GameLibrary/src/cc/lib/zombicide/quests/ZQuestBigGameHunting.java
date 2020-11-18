package cc.lib.zombicide.quests;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.utils.Grid;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCellType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZTiles;

public class ZQuestBigGameHunting extends ZQuest {

    String [][] map = {
            { "z0:i:wn:ww:ds:vd1", "z0:i:wn:de:ws:red:odw", "z1:sp:wn:de",      "z2:i:wn:ode:ws",   "z3:i:wn:ode:ws",       "z4:i:red:wn:we", "z26:v:wn:we:vd2" },
            { "z5:ww",              "z6",                   "z7",               "z8:we",            "z9:i:ods",             "z9:i:ods:we",        "z26:v:we" },
            { "z10:ww:we:start",    "z11:i:ww:wn::ws:red:ode", "z12:i:wn:ds:ode",   "z13:i:wn:red:ds:we", "z14:i:ws:we:odn",    "z15:i:ds:we:odn",    "z26:v:we:ws:vd3" },
            { "z16:ww:ds",          "z17",                  "z18",              "z19",              "z20",                  "z21:we:sp:dn:ds",    "z27:v:we:vd1" },
            { "z22:i:ww:we:vd3",    "z23",                  "z24:i:wn:ww:we",   "z25:i:wn",         "z25:i:wn",             "z25:i:dn:we",        "z27:v:we" },
            { "z22:i:ww:red:ws:de", "z26:ws:sp:de",         "z24:i:red:ww:we:ws:dw", "z25:i:ws",    "z25:i:red:ws",         "z25:i:ws:vd4:we", "z27:v:we:ws:vd4" }

    };

    public ZQuestBigGameHunting() {
        super("Big Game Hunting");
    }

    @Override
    public ZBoard loadBoard() {
        return load(map);
    }

    @Override
    public void addMoves(ZGame zGame, ZCharacter cur, List<ZMove> options) {
        for (int red : redObjectives) {
            if (cur.getOccupiedZone() == red)
                options.add(ZMove.newObjectiveMove(red));
        }
    }

    @Override
    public void processObjective(ZGame zGame, ZCharacter c, ZMove move) {
        zGame.addExperience(c, 5);
        // check for necro / abom in special spawn places

    }

    List<Integer> redObjectives = new ArrayList<>();

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
        switch (cmd) {
            case "red":
                redObjectives.add(cell.getZoneIndex());
                cell.cellType = ZCellType.OBJECTIVE;
                break;
            default:
                super.loadCmd(grid, pos, cmd);

        }
    }

    @Override
    public void init(ZGame game) {

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
}
