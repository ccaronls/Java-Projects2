package cc.lib.swing

import java.awt.BasicStroke
import java.awt.Component
import java.awt.Composite
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import kotlin.math.roundToInt

/**
 * Created by chriscaron on 3/15/18.
 */
class _AWTGraphics2(var graphics2D: Graphics2D, comp: Component) : AWTGraphics(graphics2D, comp) {
	private var stroke = BasicStroke(2f)
	private var dashedStroke: BasicStroke? = null

	override fun setLineWidth(newWidth: Float): Float {
		val old = stroke.lineWidth
		stroke = BasicStroke(newWidth)
		graphics2D.stroke = stroke
		return old
	}

	override fun drawLineStrip() {
		graphics2D.drawPolyline(x, y, polyPts)
	}

	override fun drawLineLoop() {
		graphics2D.drawPolygon(x, y, polyPts)
	}

	var old: Composite? = null

	init {
		graphics2D.stroke = stroke
	}

	override fun setComposite(comp: Composite) {
		if (old == null) old = graphics2D.composite
		graphics2D.composite = comp
	}

	override fun removeFilter() {
		super.removeFilter()
		if (old != null) {
			graphics2D.composite = old
			old = null
		}
	}

	override fun drawImage(imageKey: Int, x: Int, y: Int, w: Int, h: Int) {
		//super.drawImage(imageKey, x, y, w, h);
		imageMgr.getImage(imageKey)?.let { img ->
			val xScale = w.toFloat() / img.getWidth(comp)
			val yScale = h.toFloat() / img.getHeight(comp)
			val t = AffineTransform()
			t.translate(x.toDouble(), y.toDouble())
			t.scale(xScale.toDouble(), yScale.toDouble())
			//        t.translate(-w/2, -h/2);
			graphics2D.drawImage(img, t, comp)
		}
	}

	override fun drawImage(imageKey: Int) {
		val M = R.currentTransform.get()
		val t = AffineTransform(M[0][0], M[1][0], M[0][1], M[1][1], M[0][2], M[1][2])
		val img = imageMgr.getImage(imageKey)
		graphics2D.drawImage(img, t, comp)
	}

	/*
    @Override
    public void setClipRect(float x, float y, float w, float h) {
        Rectangle rect = new Rectangle(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
        G2.setClip(rect);
        G2.clip(rect);
    }

    @Override
    public void clearClip() {
        G2.clip(null);
    }*/
	override fun drawDashedLine(
		x0: Float,
		y0: Float,
		x1: Float,
		y1: Float,
		thickness: Float,
		dashLength: Float
	) {
		require(dashLength >= 1) { "Invalid dashLength: $dashLength" }
		if (dashedStroke == null || dashedStroke!!.dashArray[0] != dashLength || dashedStroke!!.lineWidth != thickness) {
			dashedStroke = BasicStroke(
				thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
				0f, floatArrayOf(dashLength, dashLength * 2), 0f
			)
		}
		val prev = graphics2D.stroke
		graphics2D.stroke = dashedStroke
		try {
			val v = floatArrayOf(0f, 0f)
			transform(x0, y0, v)
			val xi0 = v[0].roundToInt()
			val yi0 = v[1].roundToInt()
			transform(x1, y1, v)
			val xi1 = v[0].roundToInt()
			val yi1 = v[1].roundToInt()
			graphics2D.drawLine(xi0, yi0, xi1, yi1)
		} finally {
			graphics2D.stroke = prev
		}
	}
}