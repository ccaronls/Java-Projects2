package cc.lib.math

import cc.lib.game.IVector2D
import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored

@Mirror
interface IMirroredVector : IVector2D, Mirrored {
	override val x: Float
	override val y: Float
}

/**
 * Immutable.  Use MutableVector2D for mutator operations.
 *
 * Can be used as an interpolator that returns a fixed position
 *
 * @author chriscaron
 */
open class Vector2D(x: Float = 0f, y: Float = 0f) : MirroredVectorImpl() {

	init {
		this.x = x
		this.y = y
	}

	constructor(v: IVector2D) : this(v.x, v.y)
}
