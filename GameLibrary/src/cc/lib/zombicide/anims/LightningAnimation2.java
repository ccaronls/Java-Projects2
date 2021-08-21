package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.game.IInterpolator;
import cc.lib.game.InterpolatorUtils;
import cc.lib.game.LightningStrand;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;

public class LightningAnimation2 extends ZActorAnimation {

    final Vector2D start0, start1;
    // phase1 arcs between magicians hands to arc upward
    final LightningStrand [] arcs;
    final LightningStrand [] shots;

    final float minArc = 0f;
    final float maxArc = .5f;

    public LightningAnimation2(ZActor actor, GRectangle target, int numDice) {
        super(actor, 700L, 1000L);
        start0 = actor.getRect().getTopLeft();
        start1 = actor.getRect().getTopRight();

        arcs = new LightningStrand[Utils.randRange(3, 6)];
        for (int i=0; i<arcs.length; i++) {
            arcs[i] = new LightningStrand(start0, start1, InterpolatorUtils.linear(minArc, maxArc), 4, 7, .5f);
        }

        shots = new LightningStrand[numDice*2];
        Vector2D dv = start1.sub(start0).normEq().scaleEq(-maxArc);
        //Vector2D start = (start0.add(start1)).scaleEq(.5f).addEq(dv);
        for (int i=0; i<shots.length; i+=2) {
            IInterpolator<Vector2D> endInt = Vector2D.getLinearInterpolator(target.getRandomPointInside(), target.getRandomPointInside());
            shots[i] = new LightningStrand(start0, endInt, 10, 15, .4f);
            endInt = Vector2D.getLinearInterpolator(target.getRandomPointInside(), target.getRandomPointInside());
            shots[i+1] = new LightningStrand(start1, endInt, 10, 15, .4f);
        }
    }

    @Override
    protected void drawPhase(AGraphics g, float position, int phase) {
        switch (phase) {
            case 0: {
                // draw the arc build up
                for (LightningStrand l : arcs) {
                    l.draw(g, position);
                }
                break;
            }

            case 1: {
                // draw the arc build up
                //for (LightningStrand l : arcs) {
                 //   l.draw(g, 1);
                //}
                for (LightningStrand l : shots) {
                    l.draw(g, position);
                }
                break;
            }
        }
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}