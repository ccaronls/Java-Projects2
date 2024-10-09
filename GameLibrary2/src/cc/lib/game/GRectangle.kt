package cc.lib.game

import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import java.util.Objects
import kotlin.math.abs

data class GRectangle(
	override var left: Float = 0f,
	override var top: Float = 0f,
	override var width: Float = 0f,
	override var height: Float = 0f
) : IRectangle {

	constructor(dim: IDimension) : this(0f, 0f, dim)
	constructor(toCopy: IRectangle) : this(toCopy.left, toCopy.top, toCopy.width, toCopy.height)
	constructor(x: Float, y: Float, dim: IDimension) : this(x, y, dim.width, dim.height)
	constructor(topLeft: IVector2D, w: Float, h: Float) : this(topLeft.x, topLeft.y, w, h)
	constructor(v: IVector2D, d: IDimension) : this(v.x, v.y, d)
	constructor(v0: IVector2D, v1: IVector2D) : this() {
		assign(v0, v1)
	}

	/**
	 * Create a rect that bounds 2 vectors
	 *
	 * @param v0
	 * @param v1
	 */
	fun assign(v0: IVector2D, v1: IVector2D): GRectangle {
		left = v0.x.coerceAtMost(v1.x)
		top = v0.y.coerceAtMost(v1.y)
		width = abs(v0.x - v1.x)
		height = abs(v0.y - v1.y)
		return this
	}

	fun assign(r: IRectangle): GRectangle {
		left = r.left
		top = r.top
		width = r.width
		height = r.height
		return this
	}

	/**
	 * Useful for mouse dragging rectangular bounds
	 *
	 * @param v
	 */
	fun setEnd(v: IVector2D): GRectangle {
		left = left.coerceAtMost(v.x)
		top = top.coerceAtMost(v.y)
		width = abs(left - v.x)
		height = abs(top - v.y)
		return this
	}

	fun assign(left: Float, top: Float, right: Float, bottom: Float) {
		this.left = left
		this.top = top
		width = right - left
		height = bottom - top
	}

	/**
	 *
	 * @param r
	 * @param position
	 * @return
	 */
	fun getInterpolationTo(r: GRectangle, position: Float): GRectangle {
		if (position < 0.01f) return this
		if (position > 0.99f) return r
		val v0 = MutableVector2D(left, top)
		val v1 = MutableVector2D(left + width, top + height)
		val r0 = MutableVector2D(r.left, r.top)
		val r1 = MutableVector2D(r.left + r.width, r.top + r.height)
		v0.addEq(r0.subEq(v0).scaleEq(position))
		v1.addEq(r1.subEq(v1).scaleEq(position))
		return GRectangle(v0, v1)
	}

	/**
	 * Adjust bounds by some number of pixels
	 *
	 * @param pixels
	 */
	fun grow(pixels: Float): GRectangle {
		left -= pixels
		top -= pixels
		width += pixels * 2
		height += pixels * 2
		return this
	}

	/**
	 * Scale the dimension of this rect by some amout. s < 1 reduces size. s > 1 increases size.
	 * @param sx
	 * @param sy
	 */
	fun scale(sx: Float, sy: Float): GRectangle {
		val nw = width * sx
		val nh = height * sy
		val dw = nw - width
		val dh = nh - height
		left -= dw / 2
		top -= dh / 2
		width = nw
		height = nh
		return this
	}

	fun scale(s: Float): GRectangle {
		return scale(s, s)
	}

	fun scaleDimension(s: Float): GRectangle {
		width *= s
		height *= s
		return this
	}

	override fun equals(o: Any?): Boolean {
		if (this === o) return true
		if (o == null || javaClass != o.javaClass) return false
		//if (!super.equals(o)) return false;
		val that = o as GRectangle
		return java.lang.Float.compare(that.left, left) == 0 && java.lang.Float.compare(
			that.top,
			top
		) == 0 && java.lang.Float.compare(
			that.width,
			width
		) == 0 && java.lang.Float.compare(that.height, height) == 0
	}

	override fun hashCode(): Int {
		return Objects.hash(left, top, width, height)
	}

	fun setCenter(cntr: IVector2D): GRectangle {
		left = cntr.x - width / 2
		top = cntr.y - height / 2
		return this
	}

	fun setPosition(topLeft: IVector2D): GRectangle {
		left = topLeft.x
		top = topLeft.y
		return this
	}

	fun setTopRightPosition(topRight: IVector2D): GRectangle {
		left = topRight.x - width
		top = topRight.y
		return this
	}

	fun setDimension(dim: IDimension): GRectangle {
		val cntr = center
		width = dim.width
		height = dim.height
		return setCenter(cntr)
	}

	fun setDimension(width: Float, height: Float): GRectangle {
		this.width = width
		this.height = height
		return this
	}

	fun setDimensionJustified(width: Float, height: Float, horz: Justify?, vert: Justify?): GRectangle {
		this.width = width
		this.height = height
		when (horz) {
			Justify.LEFT -> {}
			Justify.RIGHT -> moveBy(-width, 0f)
			Justify.CENTER -> moveBy(-width / 2, 0f)
			else -> Unit
		}
		when (vert) {
			Justify.TOP -> {}
			Justify.BOTTOM -> moveBy(0f, -height)
			Justify.CENTER -> moveBy(0f, -height / 2)
			else -> Unit
		}
		return this
	}

	fun moveBy(dx: Float, dy: Float): GRectangle {
		left += dx
		top += dy
		return this
	}

	fun moveBy(dv: IVector2D): GRectangle {
		return moveBy(dv.x, dv.y)
	}

	/**
	 * Add a rectangle to this such that the bound of this grow to contain all of input.
	 * This rect will never be reduces in size.
	 *
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return
	 */
	fun addEq(x: Float, y: Float, w: Float, h: Float): GRectangle {
		return addEq(GRectangle(x, y, w, h))
	}

	/**
	 * @param dv
	 * @param w
	 * @param h
	 * @return
	 */
	fun addEq(dv: IVector2D, w: Float, h: Float): GRectangle {
		return addEq(dv.x, dv.y, w, h)
	}

	fun addEq(r: IRectangle): GRectangle {
		plusAssign(r)
		return this
	}

	/**
	 * @param g
	 * @return
	 */
	infix operator fun plusAssign(g: IRectangle) {
		val tl: Vector2D = topLeft.minEq(g.topLeft)
		val br: Vector2D = bottomRight.maxEq(g.bottomRight)
		assign(tl, br)
	}

	fun setAspect(aspect: Float): GRectangle {
		val a = aspect
		val cntr = center
		if (a > aspect) {
			// grow the height to meet the target aspect
			height = width / aspect
		} else {
			// grow the width to meet the target aspect
			width = height * aspect
		}
		setCenter(cntr)
		return this
	}

	fun setAspectReduce(aspect: Float): GRectangle {
		val a = aspect
		val cntr = center
		if (a < aspect) {
			// grow the height to meet the target aspect
			height = width / aspect
		} else {
			// grow the width to meet the target aspect
			width = height * aspect
		}
		setCenter(cntr)
		return this
	}

	companion object {

		@JvmField
		val EMPTY: GRectangle = GRectangle()

		fun getInterpolator(start: GRectangle, end: GRectangle): IInterpolator<GRectangle> {
			return IInterpolator { position -> start.getInterpolationTo(end, position) }
		}
	}
}
