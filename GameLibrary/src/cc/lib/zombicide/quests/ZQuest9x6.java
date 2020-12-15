package cc.lib.zombicide.quests;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZTiles;

abstract class ZQuest9x6 extends ZQuest {

    public ZQuest9x6(String name) {
        super(name);
    }

    @Omit
    private int [] tileIds = null;

    protected abstract String [] getTileIds();

    protected abstract int [] getTileRotations();

    @Override
    public final void drawTiles(AGraphics g, ZBoard board, ZTiles tiles) {
        if (tileIds == null) {
            tileIds = tiles.loadTiles(g, getTileIds(), getTileRotations());
        }

        if (tileIds.length == 0)
            return;

        GRectangle quadrantTL = new GRectangle(
                board.getCell(0, 0).getTopLeft(),
                board.getCell(2, 2).getBottomRight());
        GRectangle quadrantTM = new GRectangle(
                board.getCell(0, 3).getTopLeft(),
                board.getCell(2, 5).getBottomRight());
        GRectangle quadrantTR = new GRectangle(
                board.getCell(0, 6).getTopLeft(),
                board.getCell(2, 8).getBottomRight());

        GRectangle quadrantBL = new GRectangle(
                board.getCell(3, 0).getTopLeft(),
                board.getCell(5, 2).getBottomRight());
        GRectangle quadrantBM = new GRectangle(
                board.getCell(3, 3).getTopLeft(),
                board.getCell(5, 5).getBottomRight());
        GRectangle quadrantBR = new GRectangle(
                board.getCell(3, 6).getTopLeft(),
                board.getCell(5, 8).getBottomRight());

        g.drawImage(tileIds[0], quadrantTL);
        g.drawImage(tileIds[1], quadrantTM);
        g.drawImage(tileIds[2], quadrantTR);
        g.drawImage(tileIds[3], quadrantBL);
        g.drawImage(tileIds[4], quadrantBM);
        g.drawImage(tileIds[5], quadrantBR);


    }
}