package cc.lib.game

import cc.lib.math.Bezier
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D

/**
 * Created by Chris Caron on 8/18/21.
 */
class LightningStrand(val startInt: IInterpolator<Vector2D>, val endInt: IInterpolator<Vector2D>, val arcInt: IInterpolator<Float>, val minSections: Int, val maxSections: Int, val excitability: Float) {
	constructor(startInt: IInterpolator<Vector2D>, endInt: IInterpolator<Vector2D>, minSections: Int, maxSections: Int, excitability: Float) : this(startInt, endInt, InterpolatorUtils.FLOAT_ZERO, minSections, maxSections, excitability)

	fun draw(g: AGraphics, position: Float) {
		val start = MutableVector2D(startInt.getAtPosition(position)!!)
		val end: IVector2D = endInt.getAtPosition(position)
		drawLightning(g, start, end, minSections, maxSections, excitability, arcInt.getAtPosition(position))
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
            //Vector2D v = start.addEq(dv.rotate(Utils.randFloatPlusOrMinus(30*excitability)));
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

	companion object {
		fun drawLightning(g: AGraphics, _start: IVector2D, end: IVector2D, minSections: Int, maxSections: Int, excitability: Float, arcBend: Float) {
			val sec = Utils.randRange(minSections, maxSections)
			if (sec < 1) return
			val start = _start.toMutable()
			val vInt = Bezier.build(start, end.toMutable(), arcBend)
			g.color = GColor.WHITE.withAlpha(.5f + Utils.randFloat(.5f))
			g.setLineWidth(Utils.randRange(1, 5).toFloat())
			g.begin()
			g.vertex(start)
			var sign = 1f
			for (ii in 1..sec) {
				val pos = ii.toFloat() / sec.toFloat()
				//Vector2D v = start.addEq(dv.rotate(Utils.randFloatPlusOrMinus(30*excitability)));
				val v: Vector2D = vInt.getAtPosition(pos).toMutable()
				val dv: Vector2D = v.sub(start).rotate(sign * Utils.randFloat(30 * excitability))
				sign *= -1f
				g.vertex(start.addEq(dv))
				//start.set(v);
			}
			g.vertex(end)
			g.drawLineStrip()
		}
	}
}
