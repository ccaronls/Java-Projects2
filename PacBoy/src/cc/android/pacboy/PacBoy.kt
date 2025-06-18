package cc.android.pacboy

import cc.lib.game.AGraphics
import cc.lib.game.IVector2D
import cc.lib.game.Utils
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D.Companion.newTemp

class PacBoy internal constructor() {
	val seperation = floatArrayOf(0f, 15f, 30f, 45f, 60f, 75f, 90f, 75f, 60f, 45f, 30f, 15f, 0f)
	val pos: MutableVector2D
	var degrees = 0f
	var targetDegrees = 0f
	var speed = 0f
	var frame = 0
	var radius = 0.5f
	var maxSpeed = 0.15f

	init {
		pos = MutableVector2D()
		reset()
	}

	fun reset() {
		speed = 0.01f
		frame = 0
		degrees = 0f
		radius = 0.5f
	}

	fun draw(g: AGraphics) {
		g.pushMatrix()
		g.translate(pos)
		g.rotate(degrees)
		val sep = seperation[frame++ % seperation.size]
		val v = MutableVector2D(radius, 0)
		var ang = sep / 2
		val step = 15f
		v.rotateEq(ang)
		g.begin()
		g.vertex(0f, 0f)
		while (ang <= 360 - sep / 2) {
			g.vertex(v)
			v.rotateEq(step)
			ang += step
		}
		g.drawTriangleFan()
		g.end()
		g.popMatrix()
	}

	fun moveTo(v: IVector2D?): Boolean {
		val dv = newTemp(v!!)
		dv.subEq(pos)
		degrees = dv.angleOf()
		/* TODO: work in progress
		float dd = targetDegrees - degrees;
		float angSpeed = 500 * speed;
		if (dd > 180) {
			degrees -= angSpeed;
		} else if (dd < -180) {
			degrees += angSpeed;
		} else if (dd < 0) {
			degrees -= angSpeed;
		} else {
			degrees += angSpeed;
		}
		if (degrees < 0) {
			degrees += 360;
		} else if (degrees > 360) {
			degrees -= 360;
		}*/
		val d = dv.mag()
		if (d < speed) {
			pos.assign(v)
			speed = Utils.clamp(speed + 0.01f, 0f, maxSpeed)
			return true
		}
		dv.scaleEq(speed).scaleEq(1.0f / d)
		pos.addEq(dv)
		return false
	}
}
