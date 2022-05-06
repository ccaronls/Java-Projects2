package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.IInterpolator;
import cc.lib.game.LightningStrand;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;

/**
 * Created by Chris Caron on 8/18/21.
 */
public class MagicOrbAnimation extends ZActorAnimation {

    final Vector2D path;
    final Vector2D start;
    final GRectangle rect;
    final LightningStrand [] strands;

    final float startAlpha = .9f;
    final float endAlpha = .3f;
    final float startRadius = .05f;
    final float endRadius = .25f;
    final float padding = .02f; // padding between the lightning strands and the outer edge of orb

    public MagicOrbAnimation(ZActor actor, Vector2D end) {
        super(actor, 600L, 800L);
        this.rect = actor.getRect().scaledBy(.5f);
        this.start = rect.getCenterTop();
        path = end.sub(start);
        strands = new LightningStrand[Utils.randRange(7, 9)];
        for (int i=0; i<strands.length; i++) {
            IInterpolator<Vector2D> i0 = Vector2D.getPolarInterpolator(Vector2D.ZERO, startRadius+padding, endRadius+padding, Utils.randFloat(360), Utils.randFloat(360));
            IInterpolator<Vector2D> i1 = Vector2D.getPolarInterpolator(Vector2D.ZERO, startRadius+padding, endRadius+padding, Utils.randFloat(360), Utils.randFloat(360));
            strands[i] = new LightningStrand(i0, i1, 4, 7, .4f);
        }
        setDuration(1, Math.round(path.mag()* 900));
    }

    @Override
    protected void drawPhase(AGraphics g, float position, int phase) {
        GColor orbColor = GColor.MAGENTA;

        switch (phase) {
            case 0:
                // orb expands over our actors head
                g.setColor(orbColor.withAlpha(startAlpha));
                g.drawFilledCircle(start, startRadius * position);
                break;
            case 1: {
                // draw purple orb that fades but also grows at it travels
                float alpha = startAlpha + (endAlpha - startAlpha) * position;
                float radius = startRadius + (endRadius - startRadius) * position;

                Vector2D center = start.add(path.scaledBy(position));
                g.setColor(orbColor.withAlpha(alpha));
                g.pushMatrix();
                g.translate(center);
                g.drawFilledCircle(Vector2D.ZERO, radius);

                // draw strands of electrocution
                for (LightningStrand l : strands) {
                    l.draw(g, position);
                }
                g.popMatrix();
            }
        }
    }

    @Override
    public boolean hidesActor() {
        return false;
    }
}
