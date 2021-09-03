package cc.lib.zombicide.anims;

import java.util.Arrays;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.IRectangle;
import cc.lib.zombicide.ZAnimation;
import cc.lib.zombicide.ZIcon;

/**
 * Engulf some number of rectangles in flames
 */
public class InfernoAnimation extends ZAnimation {

    float index = 0;
    final List<IRectangle> rects;

    public InfernoAnimation(IRectangle ... rect) {
        this(Arrays.asList(rect));
    }

    public InfernoAnimation(List<IRectangle> rects) {
        super(2000);
        this.rects = rects;
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        for (IRectangle rect : rects) {
            int idx = ((int)index) % ZIcon.FIRE.imageIds.length;
            g.drawImage(ZIcon.FIRE.imageIds[idx], rect);
            index += .2f;
        }
    }
}
