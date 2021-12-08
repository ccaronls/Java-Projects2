package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZBoard;

/**
 * Animate concentric arcs that have a start and end point
 */
public class SonarAnimation extends ZActorAnimation {

    Vector2D start, end, dv;
    final int numArcs;
    final float startAngle;
    final float sweepAngle;
    final float radius;

    public SonarAnimation(ZActor actor, ZBoard board, int targetZone) {
        this(actor, actor.getRect().getCenter(), board.getZone(targetZone).getCenter(), 5, 20);
    }

    public SonarAnimation(ZActor actor, Vector2D start, Vector2D end, int numArcs, float sweepAngle) {
        super(actor, 2000);
        this.start = start;
        this.end = end;
        this.numArcs = numArcs;
        this.sweepAngle = sweepAngle;
        this.radius = end.sub(start).mag();
        this.startAngle = end.sub(start).angleOf() - sweepAngle/2;
        this.dv = end.sub(start).scaledBy(1f / numArcs);
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        g.setColor(GColor.WHITE);
        g.setLineWidth(3);
        float radiusStep = radius / numArcs;
        if (position <= .5f) {
            // draw the arcs emanating from the start
            int numArcsToDraw = Math.round(position * 2 * numArcs);
            //g.drawFilledCircle(start, radius/10);
            g.drawArc(start, radius/10, startAngle, sweepAngle);
            float r = radiusStep;
            for (int i=0; i<numArcsToDraw; i++) {
                g.drawArc(start, r, startAngle, sweepAngle);
                r += radiusStep;
            }
            g.drawArc(start, position*2*radius, startAngle, sweepAngle);
        } else {
            // draw the arcs backward from end
            int numArcsToDraw = Math.round(2 * (1f - position) * numArcs);
            float r = numArcs*radiusStep;
            for (int i=0; i<numArcsToDraw; i++) {
                g.drawArc(start, r, startAngle, sweepAngle);
                r -= radiusStep;
            }
            g.drawArc(start, (position-.5f)*2*radius, startAngle, sweepAngle);
        }
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}