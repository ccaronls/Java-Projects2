package cc.lib.math

import cc.lib.game.IVector2D
import java.io.Serializable

/**
 * Immutable.  Use MutableVector2D for mutator operations.
 *
 * Can be used as an interpolator that returns a fixed position
 *
 * @author chriscaron
 */
open class Vector2D(x: Float = 0f, y: Float = 0f) : IVector2D, Serializable {

	override val x: Float = x
	override val y: Float = y

	constructor(v: IVector2D) : this(v.x, v.y)
}
