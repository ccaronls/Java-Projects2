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

public class ZQuestTheShepherds extends ZQuest {

    static {
        addAllFields(ZQuestTheShepherds.class);
    }

    public ZQuestTheShepherds() {
        super("The Shepherds");
    }

    int greenSpawnZone=-1;
    int blueSpawnZone=-1;
    int numTotal=0;

    List<Integer> objectives = new ArrayList<>();

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:vd1:ws:ode", "z0:i:red:ws:de", "z1", "z2", "z3:sp", "z4:i:ww:ods:sp:ode", "z5:i:we", "z6", "z7:i:ww:vd2" },
                { "z8", "z9", "z10", "z11:i:ww:wn:we:ods", "z12",           "z13:i:ww:ds:we", "z5:i:ods:we", "z14", "z7:i:ww:ds" },
                { "z15", "z16:i:wn:ww:ws", "z16:i:wn:ode:ods", "z17:i:de:ods:red", "z18", "z19", "z20", "z21", "z22" },
                { "z23", "z24:i:dw:ws:ode", "z25:i:ws:ode", "z26:i:ws:we", "z27", "z28:i:ww:ws:dn:ode", "z28:i:ws:we:dn:red", "z29", "z30:i:ww:ws:dn" },
                { "z31:sp", "z32", "z33", "z34", "z35", "z36", "z37", "z38", "z39:sp" },
                { "z40:i:red:wn:ode:ws", "z40:i:wn:ws:de", "z41:ws", "z42:i:dw:wn:vd3:we", "z43:st", "z44:i:dw:wn:vd4:ode", "z45:i:wn:ode", "z46:i:wn", "z46:i:red:wn"},
                { "", "", "", "z47:v:vd1:ww:wn", "z47:v:wn", "z47:v:wn:we:vd3", "z48:v:vd2:wn", "z48:v:wn", "z48:v:vd4:wn" }
        };

        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
        switch (cmd) {
            case "red":
                objectives.add(cell.getZoneIndex());
                cell.cellType = ZCellType.OBJECTIVE_RED;
                break;
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public void addMoves(ZGame game, ZCharacter cur, List<ZMove> options) {
        for (int obj : objectives) {
            if (cur.getOccupiedZone() == obj) {
                options.add(ZMove.newObjectiveMove(obj));
            }
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        game.addExperience(c, OBJECTIVE_EXP);
        objectives.remove((Object)move.integer);
        if (move.integer == blueSpawnZone) {
            game.board.setSpawnZone(blueSpawnZone, true);
            blueSpawnZone = -1;
        } else if (move.integer == greenSpawnZone) {
            game.board.setSpawnZone(greenSpawnZone, true);
            greenSpawnZone = -1;
        }
    }

    @Override
    public boolean isQuestComplete(ZGame game) {
        return objectives.size() == 0;
    }

    @Override
    public boolean isQuestFailed(ZGame game) {
        return false;
    }

    @Omit
    int [] tileIds = null;

    @Override
    public void drawTiles(AGraphics g, ZBoard board, ZTiles tiles) {

        // 1R 2R 9V
        // 3V 4V 5R

        if (tileIds == null) {
            tileIds = tiles.loadTiles(g, new String [] { "1R", "2R", "9V", "3V", "4V", "5R" }, new int [] { 90, 180, 270, 0, 270, 180 });
        }

        if (tileIds.length == 0)
            return;

        GRectangle quadrant1R = new GRectangle(
                board.getCell(0, 0).getRect().getTopLeft(),
                board.getCell(2, 2).getRect().getBottomRight());
        GRectangle quadrant2V = new GRectangle(
                board.getCell(0, 3).getRect().getTopLeft(),
                board.getCell(2, 5).getRect().getBottomRight());
        GRectangle quadrant9V = new GRectangle(
                board.getCell(0, 6).getRect().getTopLeft(),
                board.getCell(2, 8).getRect().getBottomRight());

        GRectangle quadrant3V = new GRectangle(
                board.getCell(3, 0).getRect().getTopLeft(),
                board.getCell(5, 2).getRect().getBottomRight());
        GRectangle quadrant4V = new GRectangle(
                board.getCell(3, 3).getRect().getTopLeft(),
                board.getCell(5, 5).getRect().getBottomRight());
        GRectangle quadrant5R = new GRectangle(
                board.getCell(3, 6).getRect().getTopLeft(),
                board.getCell(5, 8).getRect().getBottomRight());

        g.drawImage(tileIds[0], quadrant1R);
        g.drawImage(tileIds[1], quadrant2V);
        g.drawImage(tileIds[2], quadrant9V);
        g.drawImage(tileIds[3], quadrant3V);
        g.drawImage(tileIds[4], quadrant4V);
        g.drawImage(tileIds[5], quadrant5R);

    }

    @Override
    public void init(ZGame game) {
        numTotal = objectives.size();
        while (blueSpawnZone == greenSpawnZone) {
            blueSpawnZone = Utils.randItem(objectives);
            greenSpawnZone = Utils.randItem(objectives);
        }
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        int numTaken = numTotal - objectives.size();
        return new Table(getName()).addRow(
                new Table().setNoBorder()
                    .addRow("Rescue the townsfolk.\nClaim all objectives.\nSome townsfolk are infected.", String.format("%d of %d", numTaken, numTotal))
        );
    }
}
