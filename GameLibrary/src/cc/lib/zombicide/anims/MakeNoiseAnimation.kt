package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.IVector2D;
import cc.lib.zombicide.ZAnimation;

public class MakeNoiseAnimation extends ZAnimation {

    final GRectangle rect;

    public MakeNoiseAnimation(IVector2D center) {
        super(1000);
        this.rect = new GRectangle(0, 0, 1, 1).withCenter(center);
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        final float RADIUS = rect.getRadius();
        final int numCircles = 3;
        float r = RADIUS * position;
        float steps = numCircles+1;
        float r2 = ((float)((int)(steps*position))) / steps;
        g.setColor(GColor.BLACK);
        g.drawCircle(rect.getCenter(), r, 3);
        if (r2 > 0) {
            float radius = r2*RADIUS;
            float delta = (r-radius)*steps / RADIUS;
            float alpha = 1 - delta;
            //log.debug("alpha = %d", Math.round(alpha*100));
            g.setColor(GColor.BLACK.withAlpha(alpha));
            g.drawCircle(rect.getCenter(), radius, 0);
        }
    }
}
