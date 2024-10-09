package cc.lib.math

import cc.lib.game.IInterpolator
import cc.lib.game.IVector2D
import cc.lib.utils.GException
import kotlin.math.abs

/**
 * Generate a bezier curve
 *
 * @author chriscaron
 */
class Bezier : IInterpolator<Vector2D> {
	private val ctrl: Array<IVector2D>
	private var numCtrl = 0

	constructor() {
		ctrl = Array(4) { MutableVector2D() }
	}

	constructor(v: Array<IVector2D>) {
		ctrl = v
		numCtrl = v.size
	}

	fun addPoint(x: Float, y: Float): Bezier {
		ctrl[numCtrl++] = Vector2D(x, y)
		return this
	}

	fun addPoint(v: IVector2D) {
		ctrl[numCtrl++] = Vector2D(v)
	}

	fun reset() {
		numCtrl = 0
	}

	override fun getAtPosition(t: Float): Vector2D {
		if (numCtrl < 4) throw GException()
		val fW = 1 - t
		val fA = fW * fW * fW
		val fB = 3 * t * fW * fW
		val fC = 3 * t * t * fW
		val fD = t * t * t
		val fX = fA * ctrl[0].x + fB * ctrl[1].x + fC * ctrl[2].x + fD * ctrl[3].x
		val fY = fA * ctrl[0].y + fB * ctrl[1].y + fC * ctrl[2].y + fD * ctrl[3].y
		return Vector2D(fX, fY)
	}

	companion object {
		/**
		 * Construct a curve
		 *
		 * @param r0  start of curve
		 * @param r1  end of curve
		 * @param arc fraction of distance between r0 and r1 to arc. 0 is same as no arc and .5 would be a perfect half-circle
		 * @return
		 */
		fun build(r0: Vector2D, r1: Vector2D, arc: Float): IInterpolator<Vector2D> {
			if (abs(arc) < 0.001) return IVector2D.getLinearInterpolator(r0, r1)
			val curve = Bezier()
			curve.addPoint(r0)
			val dv: Vector2D = r1.minus(r0)
			val N = dv.norm().scaledBy(arc)
			if (N.y > 0) {
				N.y = -N.y
			}
			curve.addPoint(r0.plus(dv.times(.33f)).plus(N))
			curve.addPoint(r0.plus(dv.times(.66f)).plus(N))
			curve.addPoint(r1)
			return curve
		}
	}
}
