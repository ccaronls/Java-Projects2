package cc.lib.zombicide.anims;

import java.util.LinkedList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZBoard;

public class LightningAnimation extends ZActorAnimation {

    Vector2D dv;
    final float mag;
    final Vector2D start, end;
    final LinkedList<Vector2D>[] sections;// = new LinkedList<>();
    final float sectionLen;
    final int numSections;

    public LightningAnimation(ZActor actor, ZBoard board, int targetZone, int strands) {
        this(actor, actor.getRect(board).getCenter(), board.getZone(targetZone).getCenter().add(Vector2D.newRandom(.3f)), 4, strands);
    }

    public LightningAnimation(ZActor actor, Vector2D start, Vector2D end, int sections, int strands) {
        super(actor, 150, 3);
        dv = end.sub(start);
        mag = dv.mag();
        dv = dv.scaledBy(1.0f / mag);
        this.start = start;
        this.end = end;
        numSections = sections;
        sectionLen = mag / (numSections+1);
        this.sections = new LinkedList[strands];
        for (int i=0; i<strands; i++) {
            this.sections[i] = new LinkedList<>();
        }
        onRepeat(0);
    }

    @Override
    protected void onRepeat(int n) {
        for (List l : sections) {
            l.clear();
            l.add(start);
        }
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {

        float randLenFactor = .8f;
        float randAngFactor = 30;

        if (position <= .52f) {

            int sec = Utils.clamp(Math.round(position * 2 * (numSections + 1))+1, 1, numSections+1);

            for (LinkedList<Vector2D> l : sections) {
                while (sec > l.size()) {
                    float m = sectionLen * (l.size() + 1);
                    MutableVector2D n = l.getFirst().add(dv.scaledBy(randLenFactor * m));
                    //n.addEq(Vector2D.newRandom(sectionLen / (maxRandomFactor/sec)));
                    n.addEq(dv.rotate(Utils.randFloatX(randAngFactor)).scaledBy((1f-randLenFactor) * m));
                    l.add(n);
                }
            }
        } else {
            for (LinkedList<Vector2D> l : sections) {
                int sec = (numSections + 1) - Math.round((position - .5f) * 2 * (numSections + 1));
                if (sec < 1)
                    sec = 1;
                while (sec < l.size()) {
                    l.removeFirst();
                }
            }
        }

        g.setColor(GColor.WHITE);
        g.setLineWidth(2);
        for (LinkedList<Vector2D> l : sections) {
            g.begin();
            for (Vector2D v : l) {
                g.vertex(v);
            }
            g.drawLineStrip();
            g.end();
        }
    }
}