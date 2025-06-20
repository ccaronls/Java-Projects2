package cc.lib.game

import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.randomFloat
import cc.lib.utils.randomFloatPlusOrMinus
import cc.lib.utils.squared
import kotlin.math.sqrt

interface IRectangle : IDimension, IShape {
	val left: Float
	val top: Float
	val right: Float
		get() = left + width
	val bottom: Float
		get() = top + height
	val isNan: Boolean
		get() = width === Float.NaN || height === Float.NaN || top == Float.NaN || left == Float.NaN

	val topLeft: MutableVector2D
		get() = MutableVector2D(left, top)
	val topRight: MutableVector2D
		get() = MutableVector2D(left + width, top)
	val bottomLeft: MutableVector2D
		get() = MutableVector2D(left, top + height)
	val bottomRight: MutableVector2D
		get() = MutableVector2D(left + width, top + height)
	val centerLeft: MutableVector2D
		get() = MutableVector2D(left, top + height / 2)
	val centerRight: MutableVector2D
		get() = MutableVector2D(left + width, top + height / 2)
	val centerTop: MutableVector2D
		get() = MutableVector2D(left + width / 2, top)
	val centerBottom: MutableVector2D
		get() = MutableVector2D(left + width / 2, top + height)

	override val center: IVector2D
		get() = MutableVector2D(left + width / 2, top + height / 2)

	override val area: Float
		get() = width * height

	/**
	 *
	 * @param other
	 * @return
	 */
	fun isIntersectingWidth(other: IRectangle): Boolean {
		return Utils.isBoxesOverlapping(left, top, width, height, other.left, other.top, other.width, other.height)
	}

	/**
	 * @param px
	 * @param py
	 * @return
	 */
	override fun contains(px: Float, py: Float): Boolean {
		return Utils.isPointInsideRect(px, py, left, top, width, height)
	}

	/**
	 * @param other
	 * @return
	 */
	operator fun contains(other: IRectangle): Boolean {
		return (Utils.isPointInsideRect(other.topLeft.x, other.topLeft.y, left, top, width, height)
			&& Utils.isPointInsideRect(other.bottomRight.x, other.bottomRight.y, left, top, width, height))
	}

	override operator fun component0(): Float = left
	override operator fun component1(): Float = top
	operator fun component2(): Float = right
	operator fun component3(): Float = bottom

	/**
	 * @param v
	 * @return
	 */
	override fun contains(v: IVector2D): Boolean {
		return contains(v.x, v.y)
	}

	/**
	 *
	 * @param g
	 */
	override fun drawFilled(g: AGraphics) {
		g.begin()
		g.vertex(topLeft)
		g.vertex(bottomLeft)
		g.vertex(topRight)
		g.vertex(bottomRight)
		g.drawQuadStrip()
		//        g.drawFilledRect(X(), .getY(), getWidth(), getHeight());
	}

	/**
	 *
	 * @param g
	 * @param radius
	 */
	fun drawRounded(g: AGraphics, radius: Float) {
		g.drawRoundedRect(left, top, width, height, radius)
	}

	/**
	 *
	 * @param g
	 * @param thickness
	 */
	fun drawOutlined(g: AGraphics, thickness: Int) {
		g.drawRect(left, top, width, height, thickness.toFloat())
	}

	override fun drawOutlined(g: AGraphics) {
		g.drawRect(left, top, width, height)
	}

	val dimension: GDimension
		get() = GDimension(width, height)

	/**
	 * Return half of min(W,H)
	 * @return
	 */
	override val radius: Float
		get() {
			return sqrt(width.squared() + height.squared()) * .5f
		}

	val randomPointInside: Vector2D
		/**
		 * @return
		 */
		get() = Vector2D(left + randomFloat(width), top + randomFloat(height))

	/**
	 * @param s
	 * @return
	 */
	fun scaledBy(s: Float): GRectangle {
		return scaledBy(s, s)
	}

	fun scaledBy(s: Float, horz: Justify, vert: Justify): GRectangle {
		return scaledBy(s, s, horz, vert)
	}

	fun scaledBy(xScale: Float, yScale: Float, horz: Justify, vert: Justify): GRectangle {

		val newWidth = width * xScale
		val newHeight = height * yScale
		var newX = left
		var newY = top
		when (horz) {
			Justify.LEFT -> newX += width - newWidth
			Justify.RIGHT -> {}
			Justify.CENTER -> newX += (width - newWidth) / 2
			else -> Unit
		}
		when (vert) {
			Justify.TOP -> {}
			Justify.BOTTOM -> newY += height - newHeight
			Justify.CENTER -> newY += (height - newHeight) / 2
			else -> Unit
		}
		return GRectangle(newX, newY, newWidth, newHeight)
	}

	/**
	 * @param sx
	 * @param sy
	 * @return
	 */
	fun scaledBy(sx: Float, sy: Float): GRectangle {
		val nw = width * sx
		val nh = height * sy
		val dw = nw - width
		val dh = nh - height
		return GRectangle(left - dw / 2, top - dh / 2, nw, nh)
	}

	/**
	 * Return a rectangle that fits inside this rect and with same aspect.
	 * How to position inside this determined by horz/vert justifys.
	 *
	 * @param rectToFit
	 * @return
	 */
	/**
	 * Return a rectangle of dimension not to exceed this dimension and
	 * whose aspect ratio is that of rectToFit and is centered inside this.
	 *
	 * @param rectToFit
	 * @return
	 */
	fun fit(rectToFit: IDimension, horz: Justify = Justify.CENTER, vert: Justify = Justify.CENTER): GRectangle {
		val targetAspect = rectToFit.aspect
		val rectAspect = aspect
		var tx = 0f
		var ty = 0f
		var tw = 0f
		var th = 0f
		if (targetAspect > rectAspect) {
			// target is wider than rect, so fit lengthwise
			tw = width
			th = width / targetAspect
			tx = left
			when (vert) {
				Justify.CENTER -> ty = top + height / 2 - th / 2
				Justify.TOP -> ty = top
				Justify.BOTTOM -> ty = top + height - th
				else -> Unit
			}
		} else {
			th = height
			tw = height * targetAspect
			ty = top
			when (horz) {
				Justify.CENTER -> tx = left + width / 2 - tw / 2
				Justify.LEFT -> tx = left
				Justify.RIGHT -> tx = left + width - tw
				else -> Unit
			}
		}
		return GRectangle(tx, ty, tw, th)
	}

	fun canContain(other: IRectangle): Boolean {
		return width >= other.width && height >= other.height
	}

	fun getDeltaToContain(other: IRectangle): Vector2D {
		if (width < other.width || height < other.height) return Vector2D.ZERO
		val t0 = MutableVector2D()
		val t1 = MutableVector2D()
		val delta = other.center.sub(center, t0)
		val x =
			if (other.topLeft.x < topLeft.x) topLeft.x else if (other.bottomRight.x > bottomRight.x) bottomRight.x - other.width else other.topLeft.x
		val y =
			if (other.topLeft.y < topLeft.y) topLeft.y else if (other.bottomRight.y > bottomRight.y) bottomRight.y - other.height else other.topLeft.y
		val contained = GRectangle(x, y, other.width, other.height)
		val delta2: Vector2D = contained.center.sub(center, t1)
		return delta.subEq(delta2)
	}

	fun withCenter(cntr: IVector2D): GRectangle {
		return GRectangle(cntr.x - width / 2, cntr.y - height / 2, width, height)
	}

	fun withPosition(topLeft: IVector2D): GRectangle {
		return GRectangle(topLeft, this)
	}

	fun withDimension(dim: IDimension): GRectangle {
		return GRectangle(left, top, dim.width, dim.height)
	}

	fun withDimension(w: Number, h: Number): GRectangle {
		return GRectangle(left, top, w, h)
	}

	fun movedBy(dx: Float, dy: Float): GRectangle {
		return GRectangle(left + dx, top + dy, width, height)
	}

	fun movedBy(dv: IVector2D): GRectangle {
		return movedBy(dv.x, dv.y)
	}

	fun add(other: IRectangle): GRectangle {
		return GRectangle(
			Math.min(left, other.left),
			Math.min(top, other.top),
			Math.max(width, other.width),
			Math.max(height, other.height)
		)
	}

	val randomInterpolator: IInterpolator<Vector2D>
		/**
		 * @return
		 */
		get() = object : IInterpolator<Vector2D> {
			override fun getAtPosition(position: Float): Vector2D {
				return randomPointInside
			}
		}

	fun shaked(factor: Float): GRectangle {
		return shaked(factor, factor)
	}

	fun shaked(xfactor: Float, yfactor: Float): GRectangle {
		val nx = left + width * randomFloatPlusOrMinus(xfactor)
		val ny = top + height * randomFloatPlusOrMinus(yfactor)
		return GRectangle(nx, ny, width, height)
	}

	fun subDivide(rows: Int, cols: Int): Array<GRectangle> {
		val divisions = arrayOfNulls<GRectangle>(rows * cols)
		val wid = width / cols
		val hgt = height / rows
		var idx = 0
		for (i in 0 until cols) {
			val tl = topLeft.addEq(wid * i, 0)
			for (ii in 0 until rows) {
				divisions[idx++] = GRectangle(tl, wid, hgt)
				tl.addEq(0, hgt)
			}
		}
		return divisions.filterNotNull().toTypedArray()
	}

	override val isEmpty: Boolean
		get() = this === GRectangle.EMPTY || width <= 0 && height <= 0

	fun isInside(other: IRectangle): Boolean {
		return topLeft.x >= other.topLeft.x && bottomRight.x <= other.bottomRight.x && topLeft.y >= other.topLeft.y && bottomRight.y <= other.bottomRight.y
	}

	override fun enclosingRect(): IRectangle {
		return this
	}
}
