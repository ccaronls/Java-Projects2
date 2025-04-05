package cc.lib.game

import cc.lib.math.Vector2D

interface IDimension {
	val width: Float
	val height: Float
	val aspect: Float
		get() = width / height

	val center: IVector2D
		get() = Vector2D(width / 2, height / 2)

	val area: Float
		get() = width * height

	/**
	 * Return a rectangle with the aspect ratio of 'this' that contains t
	 * entire rect of target grown and 'filled' top keep aspect ratio
	 *
	 * @param target
	 * @return
	 */
	fun fillFit(target: GRectangle): GRectangle {
		val x0: Float
		val y0: Float
		val w0: Float
		val h0: Float
		val A = aspect
		require(!(A <= 0)) { "Cannot fit empty rect" }
		if (A < target.aspect) {
			w0 = target.width
			h0 = w0 / A
			x0 = 0f
			y0 = target.height / 2 - height / 2
		} else {
			h0 = target.height
			w0 = h0 * A
			y0 = 0f
			x0 = target.width / 2 - width / 2
		}
		return GRectangle(x0, y0, w0, h0)
	}

	/**
	 * Return a rectangle with same aspect as 'this' with maximum amount of rect
	 * cropped to keep aspect
	 *
	 * @param target
	 * @return
	 */
	fun cropFit(target: GRectangle): GRectangle {
		val x0: Float
		val y0: Float
		val w0: Float
		val h0: Float
		val A = aspect
		if (A <= 0) throw IllegalArgumentException("Cannot fit empty rect")
		if (A < target.aspect) {
			h0 = target.height
			w0 = h0 * A
			y0 = 0f
			x0 = target.width / 2 - w0 / 2
		} else {
			w0 = target.width
			h0 = w0 / A
			x0 = 0f
			y0 = target.height / 2 - h0 / 2
		}
		return GRectangle(x0, y0, w0, h0)
	}

	/**
	 * Return a rectangle with aspect ratio of 'this' and entire contents
	 * of target grown to meet aspect ratio and adjusted to be in bounds
	 *
	 * @param target
	 * @return
	 */
	fun fitInner(target: GRectangle): GRectangle {
		val A = aspect
		if (A <= 0) throw IllegalArgumentException("Cannot fit empty rect")
		val rect = GRectangle(target)
		rect.setAspect(A)
		if (rect.width > width || rect.height > height) {
			rect.width = width
			rect.height = height
		}
		if (rect.left < 0) {
			rect.left = 0f
		}
		if (rect.top < 0) {
			rect.top = 0f
		}
		if (rect.left + rect.width > width) {
			rect.left = width - rect.width
		}
		if (rect.top + rect.height > height) {
			rect.top = height - rect.height
		}
		return rect
	}

	val isEmpty: Boolean
		get() = width <= 0 || height <= 0

	/**
	 * Return the rectangular region that encompasses this rectangle if it were to be rotated.
	 * For instance, if a 4x2 rect were rotated 45 degrees, then the resulting rectangle would be approx 4.2x4.2
	 *
	 * @param degrees
	 * @return
	 */
	fun rotated(degrees: Float): GDimension {
		val tl: Vector2D = Vector2D(-width / 2, -height / 2).rotate(degrees)
		val tr: Vector2D = Vector2D(width / 2, -height / 2).rotate(degrees)
		val newWidth = Math.max(Math.abs(tl.x), Math.abs(tr.x)) * 2
		val newHeight = Math.max(Math.abs(tl.y), Math.abs(tr.y)) * 2
		return GDimension(newWidth, newHeight)
	}

	fun adjustedBy(dw: Float, dh: Float): GDimension {
		return GDimension(width + dw, height + dh)
	}

	fun interpolateTo(other: GDimension, factor: Float): GDimension {
		val w = width + (other.width - width) * factor
		val h = height + (other.height - height) * factor
		return GDimension(w, h)
	}

	fun addVert(d: GDimension): GDimension {
		return GDimension(Math.max(width, d.width), height + d.height)
	}

	fun addHorz(d: GDimension): GDimension {
		return GDimension(width + d.width, Math.max(height, d.height))
	}

	fun minLength(): Float {
		return Math.min(width, height)
	}

	fun scaledTo(sx: Float, sy: Float): GDimension {
		return GDimension(width * sx, height * sy)
	}

	fun scaledTo(s: Float): GDimension {
		return scaledTo(s, s)
	}

	fun withAspect(newAspect: Float): GDimension {
		val aspect = aspect
		if (newAspect < aspect && newAspect > 0.001f) {
			// grow height
			return GDimension(width, width / newAspect)
		} else if (newAspect > aspect) {
			// grow width
			return GDimension(height * newAspect, height)
		}
		return GDimension(this)
	}
}
