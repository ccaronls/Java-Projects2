package cc.lib.zombicide.anims;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;

public class ElectrocutionAnimation extends ZActorAnimation {

    private final GRectangle rect;
    private int minStrands=5, maxStrands=7;
    private int minSections=6, maxSections=10;

    private List<float[]> strands = new ArrayList<>();

    public ElectrocutionAnimation(ZActor actor) {
        this(actor, actor.getRect());
    }

    public ElectrocutionAnimation(ZActor actor, GRectangle rect) {
        super(actor, 1000);
        this.rect = rect;
        int n = Utils.randRange(minStrands, maxStrands);
        float t = 0;
        final float h = rect.getHeight()/n;
        for (int i=0; i<n; i++) {
            float y0 = t + Utils.randFloat(h);
            float y1 = t + Utils.randFloat(h);
            t += h;
            float dy0 = Utils.randFloatX(rect.getHeight()/n);
            float dy1 = Utils.randFloatX(rect.getHeight()/n);
            if (y0+dy0 < 0 || y0+dy0>rect.getHeight())
                dy0 = -dy0;
            if (y1+dy1 < 0 || y1+dy1>rect.getHeight())
                dy1 = -dy1;
            strands.add(new float[] { y0, y1, dy0, dy1 });
        }
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        g.pushMatrix();
        g.translate(rect.getTopLeft());
        for (int i=0; i<strands.size(); i++) {
            int t = Utils.rand()%100;

            switch (t) {
                case 0:
                    // move something on the left side
                case 1:
                    // move something on the right side
                case 2:
                    // add a strand
                case 3:
                    // remove a strand
            }
            float [] y = strands.get(i);
            float y0 = y[0] + position*y[2];
            float y1 = y[1] + position*y[3];
            float sec = Utils.randRange(minSections, maxSections);
            MutableVector2D start = new MutableVector2D(0, y0);
            Vector2D end   = new Vector2D(rect.getWidth(), y1);
            MutableVector2D dv = end.sub(start);
            float mag = dv.mag();
            float secLen = mag / sec;
            dv.scaleEq(secLen / mag);
            g.setColor(GColor.WHITE.withAlpha(.5f + Utils.randFloat(.5f)));
            g.setLineWidth(Utils.randRange(1,5));
            g.begin();
            g.vertex(start);
            for (int ii=0; ii<sec-1; ii++) {
                Vector2D v = start.addEq(dv.rotate(Utils.randFloatX(10)));
                g.vertex(v);
            }
            g.vertex(end);
            g.drawLineStrip();
        }
        g.popMatrix();
    }

    protected void draw2(AGraphics g, float position, float dt) {
        int n = Utils.randRange(minStrands, maxStrands);
        float t = 0;
        final float h = rect.getHeight()/(n+1);
        for (int i=0; i<n; i++) {
            float y0 = t + Utils.randFloat(h);
            float y1 = t + Utils.randFloat(h);
            t += h;
            float sec = Utils.randRange(minSections, maxSections);
            MutableVector2D start = new MutableVector2D(0, y0);
            Vector2D end   = new Vector2D(rect.getWidth(), y1);
            MutableVector2D dv = end.sub(start);
            float mag = dv.mag();
            float secLen = mag / sec;
            dv.scaleEq(secLen / mag);
            g.setColor(GColor.WHITE.withAlpha(.5f + Utils.randFloat(.5f)));
            g.setLineWidth(Utils.randRange(1,3));
            g.pushMatrix();
            g.translate(rect.getTopLeft());
            g.begin();
            g.vertex(start);
            for (int ii=0; ii<sec-1; ii++) {
                Vector2D v = start.addEq(dv.rotate(Utils.randFloatX(30)));
                g.vertex(v);
            }
            g.vertex(end);
            g.drawLineStrip();
            g.popMatrix();
        }
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}
