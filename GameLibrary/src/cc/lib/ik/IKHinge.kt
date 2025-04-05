package cc.lib.ik

import cc.lib.game.IVector2D
import cc.lib.math.MutableVector2D
import cc.lib.reflector.Reflector

/**
 * Created by chriscaron on 10/20/17.
 */
class IKHinge(vararg cons: IKConstraint) : Reflector<IKHinge>(), IVector2D {
	val v = MutableVector2D()

	/**
	 * Cached distance to the previous hinge
	 * @return
	 */
	var prevMag = 0f

	/**
	 * Cached distance to the next hinge
	 * @return
	 */
	var nextMag = 0f // cache magnitudes to the next and previous sections
	val constraints = mutableListOf<IKConstraint>()

	init {
		constraints.addAll(cons)
	}

	override val x: Float
		get() = v.x
	override val y: Float
		get() = v.y

	companion object {
		init {
			addAllFields(IKHinge::class.java)
		}
	}
}