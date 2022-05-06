package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.Utils
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import java.util.*

open class ElectrocutionAnimation(actor: ZActor<*>) : ZActorAnimation(actor, 1000) {
	private val r: GRectangle = actor.rect
    private val minStrands = 5
    private val maxStrands = 7
    private val minSections = 6
    private val maxSections = 10
    private val strands: MutableList<FloatArray> = ArrayList()
    override fun draw(g: AGraphics, position: Float, dt: Float) {
        g.pushMatrix()
        g.translate(r.topLeft)
        for (i in strands.indices) {
            val t = Utils.rand() % 100
            when (t) {
                0, 1, 2, 3 -> {
                }
            }
            val y = strands[i]
            val y0 = y[0] + position * y[2]
            val y1 = y[1] + position * y[3]
            val sec = Utils.randRange(minSections, maxSections).toFloat()
            val start = MutableVector2D(0f, y0)
            val end = Vector2D(r.width, y1)
            val dv = end.sub(start)
            val mag = dv.mag()
            val secLen = mag / sec
            dv.scaleEq(secLen / mag)
            g.color = GColor.WHITE.withAlpha(.5f + Utils.randFloat(.5f))
            g.setLineWidth(Utils.randRange(1, 5).toFloat())
            g.begin()
            g.vertex(start)
            var ii = 0
            while (ii < sec - 1) {
                val v: Vector2D = start.addEq(dv.rotate(Utils.randFloatX(10f)))
                g.vertex(v)
                ii++
            }
            g.vertex(end)
            g.drawLineStrip()
        }
        g.popMatrix()
    }

    override fun hidesActor(): Boolean {
        return false
    }

    init {
        val n = Utils.randRange(minStrands, maxStrands)
        var t = 0f
        val h = r.height / n
        for (i in 0 until n) {
            val y0 = t + Utils.randFloat(h)
            val y1 = t + Utils.randFloat(h)
            t += h
            var dy0 = Utils.randFloatX(r.height / n)
            var dy1 = Utils.randFloatX(r.height / n)
            if (y0 + dy0 < 0 || y0 + dy0 > r.height) dy0 = -dy0
            if (y1 + dy1 < 0 || y1 + dy1 > r.height) dy1 = -dy1
            strands.add(floatArrayOf(y0, y1, dy0, dy1))
        }
    }
}