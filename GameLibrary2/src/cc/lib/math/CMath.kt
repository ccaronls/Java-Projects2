package cc.lib.math

import cc.lib.game.IVector2D
import cc.lib.utils.GException
import cc.lib.utils.swap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

const val M_E = 2.71828182845904523536
const val M_LOG2E = 1.44269504088896340736
const val M_LOG10E = 0.434294481903251827651
const val M_LN2 = 0.693147180559945309417
const val M_LN10 = 2.30258509299404568402
const val M_PI = 3.14159265358979323846
const val M_PI_2 = 1.57079632679489661923
const val M_PI_4 = 0.785398163397448309616
const val M_1_PI = 0.318309886183790671538
const val M_2_PI = 0.636619772367581343076
const val M_2_SQRTPI = 1.12837916709551257390
const val M_SQRT2 = 1.41421356237309504880
const val M_SQRT1_2 = 0.707106781186547524401
const val M_SQRT_2_PI = 2.506628274631000502415765284811

/**
 * define a small num
 */
var EPSILON = 0.00001f
const val DEG_TO_RAD = (PI / 180.0).toFloat() // converts from degrees to radians
const val RAD_TO_DEG = (180.0 / PI).toFloat() // converts form radians to degress

/**
 *
 * @param mat
 * @param vx
 * @param vy
 * @param result_v
 */
fun mult2x2MatrixVector(mat: FloatArray, vx: Float, vy: Float, result_v: FloatArray) {
	result_v[0] = mat[0] * vx + mat[1] * vy
	result_v[1] = mat[2] * vx + mat[3] * vy
}

/**
 *
 * @param a
 * @param b
 * @param c
 * @param d
 * @param vx
 * @param vy
 * @param result_v
 */
fun mult2x2MatrixVector(a: Float, b: Float, c: Float, d: Float, vx: Float, vy: Float, result_v: FloatArray) {
	result_v[0] = a * vx + b * vy
	result_v[1] = c * vx + d * vy
}

/**
 *
 * @param mat1
 * @param mat2
 * @param result
 */
fun mult2x2Matricies(mat1: FloatArray, mat2: FloatArray, result: FloatArray) {
	result[0] = mat1[0] * mat2[0] + mat1[1] * mat2[2]
	result[1] = mat1[0] * mat2[1] + mat1[1] * mat2[3]
	result[2] = mat1[2] * mat2[0] + mat1[3] * mat2[2]
	result[3] = mat1[2] * mat2[1] + mat1[3] * mat2[3]
}

/**
 *
 * @param vector
 * @param degrees
 */
fun rotateVector(vector: FloatArray, degrees: Float) {
	rotateVector(vector, vector, degrees)
}

/**
 *
 * @param vector
 * @param degrees
 */
fun rotateVector(vector: FloatArray, result: FloatArray, degrees: Float) {
	var degrees = degrees
	degrees *= DEG_TO_RAD
	val cosd = cos(degrees.toDouble()).toFloat()
	val sind = sin(degrees.toDouble()).toFloat()
	val x = vector[0] * cosd - vector[1] * sind
	val y = vector[0] * sind + vector[1] * cosd
	result[0] = x
	result[1] = y
}

/**
 * Return true if difference between to floats is less than EPSILON
 * @param a
 * @param b
 * @return
 */
fun isAlmostEqual(a: Float, b: Float): Boolean {
	return abs(a - b) < EPSILON
}

/**
 * Return determinant of 2x2 matrix
 *
 * @param mat
 * @return
 */
fun determinant2x2Matrix(mat: FloatArray): Float {
	return mat[0] * mat[3] - mat[1] * mat[2]
}

/**
 * Invert a matrix
 *
 * @param source
 * @param dest
 * @return
 */
fun invert2x2Matrix(source: FloatArray, dest: FloatArray): Boolean {
	val det = source[0] * source[3] - source[1] * source[2]
	if (abs(det) < EPSILON) return false
	dest[0] = source[3] / det
	dest[1] = -source[1] / det
	dest[2] = -source[2] / det
	dest[3] = source[0] / det
	return true
}

/**
 *
 * @param degrees
 * @return
 */
fun sine(degrees: Float): Float {
	return sin((degrees * DEG_TO_RAD).toDouble()).toFloat()
}

/**
 *
 * @param degrees
 * @return
 */
fun cosine(degrees: Float): Float {
	return cos((degrees * DEG_TO_RAD).toDouble()).toFloat()
}

/**
 * Return the anle of a vector
 * @param x
 * @param y
 * @return
 */
fun angle(x: Float, y: Float): Int {
	if (abs(x) < EPSILON) return if (y > 0) 90 else 270
	val r = (atan((y / x).toDouble()) * RAD_TO_DEG).roundToInt()
	return if (x < 0) 180 + r else if (r < 0) 360 + r else r
}

/**
 *
 * @param n
 * @return
 */
fun factorial(n: Int): Int {
	var r = 1
	for (i in 1..n) {
		r *= i
	}
	return r
}

/**
 * Return the angle in degrees between 2 vectors
 *
 * @param dx
 * @param dy
 * @param vx
 * @param vy
 * @return
 */
fun computeDegrees(dx: Float, dy: Float, vx: Float, vy: Float): Float {
	val magA = sqrt((dx * dx + dy * dy).toDouble())
	val magB = sqrt((vx * vx + vy * vy).toDouble())
	val AdotB = (dx * vx + dy * vy).toDouble()
	val acos = acos(AdotB / (magA * magB))
	return (acos * RAD_TO_DEG).toFloat()
}

/**
 * n!
 * N choose R means --------
 * (n-r)!r!
 * | n |
 * | r |
 * @param n
 * @param r
 * @return
 */
fun n_choose_r(n: Int, r: Int): Int {
	return factorial(n) / (factorial(n - r) * factorial(r))
}

fun DoubleArray.sum(n: Int): Double {
	var sum = 0.0
	for (i in 0 until n)
		sum += get(i)
	return sum
}

/**
 *
 * @param items
 * @param num
 * @return
 */
fun stdDev(items: DoubleArray, num: Int): Double {
	assert(num > 1)
	val ave = items.sum(num) * 1.0f / num
	return stdDev(items, num, ave)
}

/**
 *
 * @param items
 * @param num
 * @param ave
 * @return
 */
fun stdDev(items: DoubleArray, num: Int, ave: Double): Double {
	assert(num > 1)
	var sum = 0.0
	for (i in 0 until num) {
		val ds = items[i] - ave
		sum += ds * ds
	}
	return sqrt(sum * 1.0f / (num - 1))
}

/**
 *
 * @param items
 * @param ave
 * @return
 */
fun stdDev(items: DoubleArray, ave: Double): Double {
	return stdDev(items, items.size, ave)
}

/**
 *
 * @param values
 * @return
 */
fun sum(values: Collection<Double>): Double {
	var sum = 0.0
	for (d in values) sum += d
	return sum
}

/**
 *
 * @param values
 * @return
 */
fun stdDev(values: Collection<Double>): Double {
	assert(values.size > 1)
	val ave = sum(values) * 1.0f / values.size
	return stdDev(values, ave)
}

/**
 *
 * @param values
 * @param ave
 * @return
 */
fun stdDev(values: Collection<Double>, ave: Double): Double {
	assert(values.isNotEmpty())
	var sum = 0.0
	for (d in values) {
		val ds = d - ave
		sum += ds * ds
	}
	return sqrt(sum * 1.0f / (values.size - 1))
}

/**
 * 'Bell' Curve
 *
 * f(x) = e^(-0.5x^2)
 * -----------
 * sqrt(2*PI)
 *
 * @param x
 * @param mean pivot value.  Curve is at its highest point when x == mean
 * @return value between (0-1] of x where nd(mean, mean) == 1
 */
fun normalDistribution(x: Double, mean: Double): Double {
	return M_E.pow(-0.5 * (x - mean).pow(2.0)) / M_SQRT_2_PI
}

/**
 * Return -1 if n < 0, 1 otherwise
 * @param n
 * @return
 */
fun Float.signOf(): Int {
	return if (this < 0) -1 else 1
}

/**
 *
 * @param pt
 * @param l0
 * @param l1
 * @return
 */
fun distSqPointLine(pt: IVector2D, l0: IVector2D, l1: IVector2D): Float {
	return distSqPointLine(pt.x, pt.y, l0.x, l0.y, l1.x, l1.y)
}

/**
 * return length of x,y with 8% error
 *
 * @param x
 * @param y
 * @return
 */
fun fastLen(x: Int, y: Int): Int {
	var x = x
	var y = y
	x = abs(x)
	y = abs(y)
	val mn = if (x > y) y else x
	return x + y - mn / 2 - mn / 4 + mn / 16
}

/**
 * return approx len of x, y
 *
 * @param x
 * @param y
 * @return
 */
fun fastLen(x: Float, y: Float): Float {
	var x = x
	var y = y
	x = abs(x)
	y = abs(y)
	val mn = if (x > y) y else x
	return x + y - mn / 2 - mn / 4 + mn / 16
}

/**
 * @param x0
 * @param y0
 * @param x1
 * @param y1
 * @return
 */
fun distSqPointPoint(x0: Float, y0: Float, x1: Float, y1: Float): Float {
	val dx = x0 - x1
	val dy = y0 - y1
	return dx * dx + dy * dy
}

fun distSqPointPoint(v0: IVector2D, v1: IVector2D): Float {
	return distSqPointPoint(v0.x, v0.y, v1.x, v1.y)
}

/**
 * @param point_x
 * @param point_y
 * @param x0
 * @param y0
 * @param x1
 * @param y1
 * @return
 */
fun distSqPointLine(point_x: Float, point_y: Float, x0: Float, y0: Float, x1: Float, y1: Float): Float {
	// get the normal (N) to the line
	val nx = -(y1 - y0)
	val ny = x1 - x0
	if (abs(nx) < EPSILON && abs(ny) < EPSILON) {
		throw GException("Degenerate Vector")
		// TODO: treat this is a point?
	}
	// get the vector (L) from point to line
	val lx = point_x - x0
	val ly = point_y - y0

	// compute N dot N
	val ndotn = nx * nx + ny * ny
	// compute N dot L
	val ndotl = nx * lx + ny * ly
	// get magnitude squared of vector of L projected onto N
	val px = nx * ndotl / ndotn
	val py = ny * ndotl / ndotn
	return px * px + py * py
}

/**
 * Convenience mehtod
 *
 * @param p_x
 * @param p_y
 * @param pts
 * @return
 */
fun distSqPointLine(p_x: Float, p_y: Float, vararg pts: Float): Float {
	return distSqPointLine(p_x, p_y, pts[0], pts[1], pts[2], pts[3])
}

/**
 * Convenience method
 *
 * @param p_x
 * @param p_y
 * @param pts
 * @return
 */
fun distSqPointLine(p_x: Float, p_y: Float, pts: IntArray): Float {
	return distSqPointLine(p_x, p_y, pts[0].toFloat(), pts[1].toFloat(), pts[2].toFloat(), pts[3].toFloat())
}

/**
 * @param px
 * @param py
 * @param x0
 * @param y0
 * @param x1
 * @param y1
 * @return
 */
fun distSqPointSegment(px: Float, py: Float, x0: Float, y0: Float, x1: Float, y1: Float): Float {
	// compute vector rep of line
	val lx = x1 - x0
	val ly = y1 - y0

	// compute vector from p0 too unicodePattern
	var dx = px - x0
	var dy = py - y0

	// dot product of d l
	var dot = lx * dx + ly * dy
	if (dot <= 0) {
		return dx * dx + dy * dy
	}
	dx = px - x1
	dy = py - y1
	dot = dx * lx + dy * ly
	return if (dot >= 0) {
		dx * dx + dy * dy
	} else distSqPointLine(px, py, x0, y0, x1, y1)
}

/**
 * @param x0
 * @param y0
 * @param x1
 * @param y1
 * @param px
 * @param py
 * @param radius
 * @return
 */
fun isCircleIntersectingLineSeg(x0: Int, y0: Int, x1: Int, y1: Int, px: Int, py: Int, radius: Int): Boolean {
	val d2: Float = distSqPointSegment(px.toFloat(), py.toFloat(), x0.toFloat(), y0.toFloat(), x1.toFloat(), y1.toFloat())
	val r2 = (radius * radius).toFloat()
	return if (d2 < r2) true else false
}

/**
 *
 * @param x0
 * @param y0
 * @param x1
 * @param y1
 * @param x2
 * @param y2
 * @param x3
 * @param y3
 * @return
 */
fun isLineSegsIntersecting(
	x0: Int, y0: Int, x1: Int, y1: Int,  // line segment 1
	x2: Int, y2: Int, x3: Int, y3: Int
): Boolean // line segment 2
{
	return when (getLineSegsIntersection(x0, y0, x1, y1, x2, y2, x3, y3)) {
		0 -> false
		else -> true
	}
}

// CONSTANTS
private val m_matrix_2x2 = FloatArray(4)
private val r_matrix_2x2 = FloatArray(4)

/**
 * @param x0
 * @param y0
 * @param x1
 * @param y1
 * @param x2
 * @param y2
 * @param x3
 * @param y3
 * @return 1 if segs are intersecting at a single point.  2 is segs are coincident.  0 if not intersecting
 */
fun getLineSegsIntersection(
	x0: Int, y0: Int, x1: Int, y1: Int,  // line segment 1
	x2: Int, y2: Int, x3: Int, y3: Int
): Int // line segment 2
{
	floatArrayOf(
		(x1 - x0).toFloat(),
		(x2 - x3).toFloat(),
		(y1 - y0).toFloat(),
		(y2 - y3).toFloat()
	).copyInto(m_matrix_2x2)
	if (!invert2x2Matrix(m_matrix_2x2, r_matrix_2x2)) {
		val cx0 = ((x0 + x1) / 2).toFloat() // center x of seg0
		val cy0 = ((y0 + y1) / 2).toFloat()
		val cx1 = ((x2 + x3) / 2).toFloat() // center x of seg1
		val cy1 = ((y2 + y3) / 2).toFloat()
		val dx = cx0 - cx1 // distance between centers
		val dy = cy0 - cy1

		// dot product of normal to delta and one of the line segs
		val dot = -dy * (x1 - x0) + dx * (y1 - y0)
		if (abs(dot) > EPSILON) return 0 // if the delta is not near parallel, then this is not intersecting
		val d = abs(dx * dx + dy * dy) // len^2 of delta
		val dx0 = x1 - cx0
		val dy0 = y1 - cy0
		val dx1 = x3 - cx1
		val dy1 = y3 - cy1

		// len^2 of 1/2 of the sum of the 2 segs
		val maxd = abs(dx0 * dx0 + dy0 * dy0 + dx1 * dx1 + dy1 * dy1)
		return if (d <= maxd) 2 else 0
		// lines are parallel and not coincident
	}

	// is it neccessary to cache these?
	val vx = (x2 - x0).toFloat()
	val vy = (y2 - y0).toFloat()

	// tx,ty are the t value of L = p0 + v0*t for each line
	val t0: Float = r_matrix_2x2.get(0) * vx + r_matrix_2x2.get(1) * vy
	val t1: Float = r_matrix_2x2.get(2) * vx + r_matrix_2x2.get(3) * vy
	return if (t0 < 0 || t0 > 1 || t1 < 0 || t1 > 1) 0 else 1
}

/**
 *
 * @param p0
 * @param p1
 * @param p2
 * @param p3
 * @return
 */
fun isLineSegsIntersecting(p0: IVector2D, p1: IVector2D, p2: IVector2D, p3: IVector2D): Int {
	return isLineSegsIntersecting(
		p0.x, p0.y,
		p1.x, p1.y,
		p2.x, p2.y,
		p3.x, p3.y
	)
}

/**
 * @param x0
 * @param y0
 * @param x1
 * @param y1
 * @param x2
 * @param y2
 * @param x3
 * @param y3
 * @return 1 if segs are intersecting at a single point.  2 is segs are coincident.  0 if not intersecting
 */
fun isLineSegsIntersecting(
	x0: Float, y0: Float, x1: Float, y1: Float,  // line segment 1
	x2: Float, y2: Float, x3: Float, y3: Float
): Int // line segment 2
{
	floatArrayOf(x1 - x0, x2 - x3, y1 - y0, y2 - y3).copyInto(m_matrix_2x2)
	if (!invert2x2Matrix(m_matrix_2x2, r_matrix_2x2)) {
		val cx0 = (x0 + x1) / 2 // center x of seg0
		val cy0 = (y0 + y1) / 2
		val cx1 = (x2 + x3) / 2 // center x of seg1
		val cy1 = (y2 + y3) / 2
		val dx = cx0 - cx1 // distance between centers
		val dy = cy0 - cy1

		// dot product of normal to delta and one of the line segs
		val dot = dx * (y1 - y0) - dy * (x1 - x0)
		if (abs(dot) > EPSILON) return 0 // if the delta is not near parallel, then this is not intersecting
		val d = abs(dx * dx + dy * dy) // len^2 of delta
		val dx0 = x1 - cx0
		val dy0 = y1 - cy0
		val dx1 = x3 - cx1
		val dy1 = y3 - cy1

		// len^2 of 1/2 of the sum of the 2 segs
		val maxd = abs(dx0 * dx0 + dy0 * dy0 + dx1 * dx1 + dy1 * dy1)
		return if (d <= maxd) 2 else 0
		// lines are parallel and not coincident
	}

	// is it neccessary to cache these?
	val vx = x2 - x0
	val vy = y2 - y0

	// tx,ty are the t value of L = p0 + v0*t for each line
	val t0: Float = r_matrix_2x2.get(0) * vx + r_matrix_2x2.get(1) * vy
	val t1: Float = r_matrix_2x2.get(2) * vx + r_matrix_2x2.get(3) * vy
	return if (t0 < 0 || t0 > 1 || t1 < 0 || t1 > 1) 0 else 1
}

/**
 * @param px
 * @param py
 * @param rx
 * @param ry
 * @param rw
 * @param rh
 * @return
 */
fun isPointInsideRect(px: Float, py: Float, rx: Float, ry: Float, rw: Float, rh: Float): Boolean {
	return if (px >= rx && py >= ry && px <= rx + rw && py <= ry + rh) true else false
}

/**
 * @param px
 * @param py
 * @param xpts
 * @param ypts
 * @return
 */
fun isPointInsidePolygon(px: Int, py: Int, xpts: IntArray, ypts: IntArray, numPts: Int): Boolean {
	var orient = 0
	for (i in 0 until numPts) {
		val dx = px - xpts[i]
		val dy = py - ypts[i]
		val ii = (i + 1) % numPts
		val dx2 = xpts[ii] - xpts[i]
		val dy2 = ypts[ii] - ypts[i]
		val nx = -dy2
		val dot = nx * dx + dx2 * dy
		if (dot < 0) {
			// return false if orientation changes
			if (orient > 0) return false else if (orient == 0) orient = -1
		} else {
			// return false if orientation changes
			if (orient < 0) return false else if (orient == 0) orient = 1
		}
	}
	return true
}

/**
 * @param px
 * @param py
 * @param xpts
 * @param ypts
 * @return
 */
fun isPointInsidePolygonf(px: Float, py: Float, xpts: FloatArray, ypts: FloatArray, numPts: Int): Boolean {
	var orient = 0
	for (i in 0 until numPts) {
		val dx = px - xpts[i]
		val dy = py - ypts[i]
		val ii = (i + 1) % numPts
		val dx2 = xpts[ii] - xpts[i]
		val dy2 = ypts[ii] - ypts[i]
		val nx = -dy2
		val dot = nx * dx + dx2 * dy
		if (abs(dot) < EPSILON) {
			// ignore since this is 'on' the segment
		} else if (dot < 0) {
			// return false if orientation changes
			if (orient > 0) return false else if (orient == 0) orient = -1
		} else {
			// return false if orientation changes
			if (orient < 0) return false else if (orient == 0) orient = 1
		}
	}
	return true
}

/**
 * Return whether pos is contained by the convex polygon represented by the list.
 * List must be convex and ordered. A Result from computeGiftwrapVertices is compatible.
 *
 * @param pos
 * @param polygon
 * @return
 */
fun isPointInsidePolygon(pos: IVector2D, polygon: List<IVector2D>): Boolean {
	if (polygon.size < 3) return false
	val side: MutableVector2D = (polygon[0] - polygon[polygon.size - 1]).normEq()
	val dv: MutableVector2D = pos - polygon[polygon.size - 1]
	val sign = side.dot(dv).signOf()
	for (i in 1 until polygon.size) {
		side.assign(polygon[i]).subEq(polygon[i - 1]).normEq()
		dv.assign(pos).subEq(polygon[i - 1])
		if (sign != side.dot(dv).signOf()) {
			return false
		}
	}
	return true
}

/**
 * @param px
 * @param py
 * @param pts
 * @return
 */
fun isPointInsidePolygon(px: Float, py: Float, pts: Array<IVector2D>, numPts: Int = pts.size): Boolean {
	if (numPts < 3) return false
	val l: MutableList<IVector2D> = java.util.ArrayList()
	for (i in 0 until numPts) {
		l.add(pts[i])
	}
	return isPointInsidePolygon(Vector2D(px, py), l)
}

/**
 * @param px
 * @param py
 * @param cx
 * @param cy
 * @param radius
 * @return
 */
fun isPointInsideCircle(px: Int, py: Int, cx: Int, cy: Int, radius: Int): Boolean {
	val dx = px - cx
	val dy = py - cy
	val dist2 = dx * dx + dy * dy
	return dist2 <= radius * radius
}

/**
 * @param px0
 * @param py0
 * @param r0
 * @param px1
 * @param py1
 * @param r1
 * @return
 */
fun isCirclesOverlapping(px0: Int, py0: Int, r0: Int, px1: Int, py1: Int, r1: Int): Boolean {
	val d: Float = distSqPointPoint(px0.toFloat(), py0.toFloat(), px1.toFloat(), py1.toFloat())
	var d2 = (r0 + r1).toFloat()
	d2 *= d2
	return d < d2
}

fun isBoxesOverlapping(
	x0: Int, y0: Int, w0: Int, h0: Int,
	x1: Int, y1: Int, w1: Int, h1: Int
): Boolean = isBoxesOverlapping(
	x0.toFloat(),
	y0.toFloat(),
	w0.toFloat(),
	h0.toFloat(),
	x1.toFloat(),
	y1.toFloat(),
	w1.toFloat(),
	h1.toFloat()
)

/**
 * Primary version: takesd input 2 VALID RECTANGLES!
 *
 * @param x0
 * @param y0
 * @param w0
 * @param h0
 * @param x1
 * @param y1
 * @param w1
 * @param h1
 * @return true when rectangles overlapp
 */
fun isBoxesOverlapping(
	x0: Float, y0: Float, w0: Float, h0: Float,
	x1: Float, y1: Float, w1: Float, h1: Float
): Boolean {
	val cx0 = x0 + w0 / 2
	val cy0 = y0 + h0 / 2
	val cx1 = x1 + w1 / 2
	val cy1 = y1 + h1 / 2
	val dx = abs(cx0 - cx1)
	val dy = abs(cy0 - cy1)
	val minx = w0 / 2 + w1 / 2
	val miny = h0 / 2 + h1 / 2
	return dx < minx && dy < miny
}

fun getNthPrime(n: Int): Int {
	val primes = intArrayOf(
		2,
		3,
		5,
		7,
		11,
		13,
		17,
		19,
		23,
		29,
		31,
		37,
		41,
		43,
		47,
		53,
		59,
		61,
		67,
		71,
		73,
		79,
		83,
		89,
		97,
		101,
		103,
		107,
		109,
		113,
		127,
		131,
		137,
		139,
		149,
		151,
		157,
		163,
		167,
		173,
		179,
		181,
		191,
		193,
		197,
		199
	)
	//                   1  2  2   4   2   4   2   2   4   2   6   4   2   4   6   6   2   6   4   2   6   4   6   8    2    2    4    2    4    4    4    6    2   10    2    6    6    4    6    6    2   10    2    4    2
	//                      1  0   2  -2   2  -2   0   2  -2   4  -2  -2   2   2   0  -4   4  -2  -2   4  -2   2   2   -6    0    2   -2    2    0    0    2   -4    8   -8    4    0   -2    2    0   -4    8   -8    2   -2
	if (n >= primes.size) System.err.println("NOT ENOUGH PRIMES!!!! value $n is larger than number of known primes")
	return primes[n.coerceIn(primes.indices)]
}

fun <T> permute(array: Array<T>, callback: PermuteCallback<T>) {
	permuteR(array, 0, callback)
}

private fun <T> permuteR(array: Array<T>, index: Int, callback: PermuteCallback<T>) {
	val r = array.size - 1
	if (r == index) {
		callback.onPermutation(array)
		return
	}
	for (i in index..r) {
		array.swap(i, index)
		permuteR(array, index + 1, callback)
		array.swap(i, index)
	}
}

fun <T> combinations(array: Array<T>, data: Array<T>, callback: PermuteCallback<T>) {
	//combinationsR(array, data, 0, 0, callback);
	combinationUtil(array, data, 0, 0, callback)
}

private fun <T> combinationUtil(arr: Array<T>, data: Array<T>, start: Int, index: Int, callback: PermuteCallback<T>) {

	// Current combination is ready
	// to be printed, print it
	if (index == data.size) {
		callback.onPermutation(data)
		return
	}

	// replace index with all possible
	// elements. The condition "end-i+1 >= r-index"
	// makes sure that including one element
	// at index will make a combination with
	// remaining elements at remaining positions
	for (i in start until arr.size) {
		data[index] = arr[i]
		combinationUtil(arr, data, i + 1, index + 1, callback)
	}
}

private fun <T> combinationsR(array: Array<T>, data: Array<T>, start: Int, index: Int, callback: PermuteCallback<T>) {
	if (index == data.size) {
		callback.onPermutation(data)
		return
	}
	for (i in start until array.size) {
		data[index] = array[i]
		combinationsR(array, data, i + 1, index + 1, callback)
	}
}

/**
 *
 * c = sqrt(a^2 + b^2 - 2ab cos(g))
 *
 * For any triangle with sides of length a,b,c and angle g being the angle between a and b, the length of c is derived
 *
 * @return
 */
fun lawOfCosines(a: Float, b: Float, gamma: Float): Float {
	return sqrt(a * a + b * b - 2 * a * b * cos(gamma.toDouble())).toFloat()
}

/**
 *
 * c = sqrt(a^2 + b^2 - 2ab cos(g))
 *
 * For any triangle with sides of length a,b,c and angle g being the angle between a and b, the length of c is derived
 *
 * @return
 */
fun lawOfCosines2(a: Float, b: Float, degrees: Float): Float {
	return sqrt(a * a + b * b - 2 * a * b * cos((DEG_TO_RAD * degrees).toDouble())).toFloat()
}

/**
 * a = b * sin (x) / sin(y)
 *
 * For any triangle with sides of length a, b and angles x, y to be those angles not between a and b then length of a is derived
 *
 * @return
 */
fun lawOfSines(b: Float, xDegrees: Float, degrees: Float): Float {
	return (b * sin((DEG_TO_RAD * xDegrees).toDouble()) / sin((DEG_TO_RAD * degrees).toDouble())).toFloat()
}

interface PermuteCallback<T> {
	fun onPermutation(array: Array<T>?)
}

