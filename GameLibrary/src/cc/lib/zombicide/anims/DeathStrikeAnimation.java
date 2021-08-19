package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZIcon;

public class DeathStrikeAnimation extends ZActorAnimation {

    final GRectangle targetRect;
    final GRectangle startRect;
    final GRectangle endRect;

    static long [] phaseDur = {
            800,
            200,
            200,
            400
    };

    public DeathStrikeAnimation(ZActor actor, GRectangle targetRect) {
        super(actor, phaseDur);
        this.targetRect = targetRect;
        startRect = new GRectangle(targetRect).moveBy(0, -targetRect.h);
        endRect = new GRectangle(targetRect).moveBy(0, -targetRect.h/2);
    }

    void drawPhase0(AGraphics g, float position) {
        int id = Utils.randItem(ZIcon.SKULL.imageIds);
        AImage img = g.getImage(id);
        g.setTransparencyFilter(position);
        g.drawImage(id, startRect.fit(img));
        g.removeFilter();
    }

    void drawPhase1(AGraphics g, float position) {
        int id = Utils.randItem(ZIcon.SKULL.imageIds);
        AImage img = g.getImage(id);
        g.drawImage(id, startRect.getInterpolationTo(targetRect, position).fit(img));
    }

    void drawPhase2(AGraphics g, float position) {
        int id = Utils.randItem(ZIcon.SKULL.imageIds);
        AImage img = g.getImage(id);
        g.pushMatrix();
        GRectangle rect = targetRect.shaked(0.1f, 0f);
        g.drawImage(id, rect.fit(img));
        g.popMatrix();
    }

    void drawPhase3(AGraphics g, float position) {
        int id = Utils.randItem(ZIcon.SKULL.imageIds);
        AImage img = g.getImage(id);
        g.setTransparencyFilter(1f-position);
        g.drawImage(id, targetRect.getInterpolationTo(endRect, position).fit(img));
        g.removeFilter();
    }

    @Override
    protected void drawPhase(AGraphics g, float pos, int phase) {
        switch (phase) {
            case 0: drawPhase0(g, pos); break;
            case 1: drawPhase1(g, pos); break;
            case 2: drawPhase2(g, pos); break;
            case 3: drawPhase3(g, pos); break;
        }
    }

    /*
    @Override
    protected void draw(AGraphics g, float position, float dt) {
        // phase 1: make skull fade in over the actor
        long dur=0;
        for (int i=0; i<phaseDur.length; i++) {
            long d = phaseDur[i]+dur;
            if (getElapsedTime() < d) {
                float pos = (getElapsedTime()-dur) / (float)phaseDur[i];
                switch (i) {
                    case 0: drawPhase1(g, pos); break;
                    case 1: drawPhase2(g, pos); break;
                    case 2: drawPhase3(g, pos); break;
                    case 3: drawPhase4(g, pos); break;
                }
                break;
            }
            dur+=phaseDur[i];
        }
    }*/

    @Override
    protected boolean hidesActor() {
        return false;
    }
}