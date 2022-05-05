package cc.lib.zombicide.anims;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZIcon;

public class DeathStrikeAnimation extends ZActorAnimation {

    static long phaseFadeInDur = 800; // 0
    static long phaseDropDur = 200; // 1
    static long phaseShakeDur = 200; // 2
    static long phaseRiseDur = 300; // 3
    static long phaseFadeOutDur = 400; // 4

    static class Phase {
        final long dur;
        final int id;
        final GRectangle [] rects;

        public Phase(int id, long dur, GRectangle ... rects) {
            this.dur = dur;
            this.id = id;
            this.rects = rects;
        }
    }

    List<Phase> phases = new ArrayList<>();

    public DeathStrikeAnimation(ZActor actor, GRectangle targetRect, int numDice) {
        super(actor, 1L);
        GRectangle startRect = targetRect.movedBy(0, -targetRect.h);
        GRectangle endRect = targetRect.movedBy(0, -targetRect.h/2);
        phases.add(new Phase(0, phaseFadeInDur, startRect));
        GRectangle target = endRect, start = startRect;
        for (int i=0; i<numDice; i++) {
            target = targetRect.movedBy(Utils.randFloatX(targetRect.w/2), 0);
            phases.add(new Phase(1, phaseDropDur, start, target));
            start = endRect;
            phases.add(new Phase(2, phaseShakeDur, target));
            if (i < numDice-1) {
                phases.add(new Phase(3, phaseRiseDur, target, endRect));
            }
        }
        phases.add(new Phase(4, phaseFadeOutDur, target, endRect));

        setDurations(Utils.map(phases, (m)->m.dur));
    }

    void drawPhase0(AGraphics g, float position, GRectangle ... rects) {
        int id = Utils.randItem(ZIcon.SKULL.imageIds);
        AImage img = g.getImage(id);
        g.setTransparencyFilter(position);
        g.drawImage(id, rects[0].fit(img));
        g.removeFilter();
    }

    void drawPhase1(AGraphics g, float position, GRectangle ... rects) {
        int id = Utils.randItem(ZIcon.SKULL.imageIds);
        AImage img = g.getImage(id);
        g.drawImage(id, rects[0].getInterpolationTo(rects[1], position).fit(img));
    }

    void drawPhase2(AGraphics g, float position, GRectangle ... rects) {
        int id = Utils.randItem(ZIcon.SKULL.imageIds);
        AImage img = g.getImage(id);
        g.pushMatrix();
        GRectangle rect = rects[0].shaked(0.1f, 0f);
        g.drawImage(id, rect.fit(img));
        g.popMatrix();
    }

    void drawPhase3(AGraphics g, float position, GRectangle ... rects) {
        int id = Utils.randItem(ZIcon.SKULL.imageIds);
        AImage img = g.getImage(id);
        g.drawImage(id, rects[0].getInterpolationTo(rects[1], position).fit(img));
        g.removeFilter();
    }

    void drawPhase4(AGraphics g, float position, GRectangle ... rects) {
        int id = Utils.randItem(ZIcon.SKULL.imageIds);
        AImage img = g.getImage(id);
        g.setTransparencyFilter(1f-position);
        g.drawImage(id, rects[0].getInterpolationTo(rects[1], position).fit(img));
        g.removeFilter();
    }

    @Override
    protected void drawPhase(AGraphics g, float pos, int phase) {
        Phase entry = phases.get(phase);
        switch (entry.id) {
            case 0: drawPhase0(g, pos, entry.rects); break;
            case 1: drawPhase1(g, pos, entry.rects); break;
            case 2: drawPhase2(g, pos, entry.rects); break;
            case 3: drawPhase3(g, pos, entry.rects); break;
            case 4: drawPhase4(g, pos, entry.rects); break;
        }
    }

    @Override
    public boolean hidesActor() {
        return false;
    }
}