package cc.lib.game;

import cc.lib.math.Bezier;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

/**
 * Created by Chris Caron on 8/18/21.
 */
public class LightningStrand {

    final IInterpolator<Vector2D> startInt, endInt;
    final int minSections, maxSections;
    final IInterpolator<Float> arcInt;
    final float excitability;

    public LightningStrand(IInterpolator<Vector2D> startInt, IInterpolator<Vector2D> endInt, int minSections, int maxSections, float excitability) {
        this(startInt, endInt, InterpolatorUtils.FLOAT_ZERO, minSections, maxSections, excitability);
    }

    public LightningStrand(IInterpolator<Vector2D> startInt, IInterpolator<Vector2D> endInt, IInterpolator<Float> arcInt, int minSections, int maxSections, float excitability) {

        this.startInt = startInt;
        this.endInt = endInt;
        this.minSections = minSections;
        this.maxSections = maxSections;
        this.arcInt = arcInt;
        this.excitability = excitability;
    }

    public void draw(AGraphics g, float position) {

        MutableVector2D start = new MutableVector2D(startInt.getAtPosition(position));
        IVector2D end = endInt.getAtPosition(position);
        drawLightning(g, start, end, minSections, maxSections, excitability, arcInt.getAtPosition(position));
/*
        //MutableVector2D dv = end.sub(start);
        //float mag = dv.mag();
        //float secLen = mag / sec;
        //dv.scaleEq(secLen / mag);
        g.setColor(GColor.WHITE.withAlpha(.5f + Utils.randFloat(.5f)));
        g.setLineWidth(Utils.randRange(1,5));
        g.begin();
        g.vertex(start);
        float sign = 1;
        for (int ii=1; ii<=sec; ii++) {
            float pos = (float)ii / (float)sec;
            //Vector2D v = start.addEq(dv.rotate(Utils.randFloatX(30*excitability)));
            Vector2D v = vInt.getAtPosition(pos);
            Vector2D dv = v.sub(start).rotate(sign * Utils.randFloat(30*excitability));
            sign *= -1;
            g.vertex(start.addEq(dv));
            //start.set(v);
        }
        g.vertex(end);
        g.drawLineStrip();
*/
    }

    public static void drawLightning(AGraphics g, IVector2D _start, IVector2D end, int minSections, int maxSections, float excitability, float arcBend) {
        int sec = Utils.randRange(minSections, maxSections);
        if (sec < 1)
            return;

        MutableVector2D start = _start.toMutable();
        IInterpolator<Vector2D> vInt = Bezier.build(start, end.toMutable(), arcBend);

        g.setColor(GColor.WHITE.withAlpha(.5f + Utils.randFloat(.5f)));
        g.setLineWidth(Utils.randRange(1,5));
        g.begin();
        g.vertex(start);
        float sign = 1;
        for (int ii=1; ii<=sec; ii++) {
            float pos = (float)ii / (float)sec;
            //Vector2D v = start.addEq(dv.rotate(Utils.randFloatX(30*excitability)));
            Vector2D v = vInt.getAtPosition(pos).toMutable();
            Vector2D dv = v.sub(start).rotate(sign * Utils.randFloat(30*excitability));
            sign *= -1;
            g.vertex(start.addEq(dv));
            //start.set(v);
        }
        g.vertex(end);
        g.drawLineStrip();

    }

}
