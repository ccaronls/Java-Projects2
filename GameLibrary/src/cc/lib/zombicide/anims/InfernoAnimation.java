package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.IRectangle;
import cc.lib.utils.Grid;
import cc.lib.zombicide.ZAnimation;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZIcon;

public class InfernoAnimation extends ZAnimation {

    final int zone;
    float index = 0;
    ZBoard board;

    public InfernoAnimation(ZBoard board, int zoneIndex) {
        super(2000);
        this.zone = zoneIndex;
        this.board = board;
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        for (Grid.Pos pos : board.getZone(zone).getCells()) {
            IRectangle rect = board.getCell(pos);
            int idx = ((int)index) % ZIcon.FIRE.imageIds.length;
            g.drawImage(ZIcon.FIRE.imageIds[idx], rect);
            index += .2f;
        }
    }
}
