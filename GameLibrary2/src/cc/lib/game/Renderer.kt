package cc.lib.game

import cc.lib.math.Matrix3x3
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.GException

/**
 * Provides mechanism to manipulate vectors in a OpenGL type way
 *
 * @author chriscaron
 */
class Renderer(private var window: Renderable) {
	/**
	 *
	 * @param m
	 */
	fun multiply(m: Matrix3x3) {
		cur_mat.timesAssign(m)
	}

	/**
	 * multiply a translation matrix to the transform
	 * @param tx
	 * @param ty
	 */
	fun translate(tx: Float, ty: Float) {
		t_mat.setTranslationMatrix(tx, ty)
		cur_mat.timesAssign(t_mat)
	}

	/**
	 *
	 * @param v
	 */
	fun translate(v: IVector2D) {
		translate(v.x, v.y)
	}

	/**
	 * multiply a rotation matrix to the transform
	 * @param degrees
	 */
	fun rotate(degrees: Float) {
		t_mat.setRotationMatrix(degrees)
		cur_mat.timesAssign(t_mat)
	}

	/**
	 * Scale all components of current transform by a scalar
	 * @param s
	 */
	fun scale(s: Float) {
		scale(s, s)
	}

	/**
	 * multiply a scale matrix to the transform
	 * @param sx
	 * @param sy
	 */
	fun scale(sx: Float, sy: Float) {
		t_mat.setScaleMatrix(sx, sy)
		cur_mat.timesAssign(t_mat)
	}

	/**
	 * set the top matrix to the identity matrix
	 */
	fun makeIdentity() {
		cur_mat.setIdentityMatrix()
	}

	/**
	 * Add a transformed vertex to the list
	 * @param x
	 * @param y
	 */
	fun addVertex(x: Float, y: Float) {
		transformXY(x, y, s_vec)
		pts[numVerts].assign(s_vec.x, s_vec.y)
		names[numVerts++] = cur_name
	}

	fun getVerticesForName(name: Int): List<IVector2D> {
		val pts: MutableList<IVector2D> = ArrayList()
		for (i in 0 until numVerts) {
			if (names[i] == name) {
				pts.add(this.pts[i])
			}
		}
		return pts
	}

	/**
	 *
	 * @param v
	 */
	fun transformXY(v: MutableVector2D) {
		transformXY(v.x, v.y, v)
	}

	/**
	 *
	 * @param v
	 */
	fun transformXY(v: FloatArray) {
		transformXY(v[0], v[1], v)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param v
	 */
	fun transformXY(x: Float, y: Float, v: MutableVector2D) {
		v.assign(x, y)
		cur_mat.transform(v)
		min.minEq(v)
		max.maxEq(v)
		val W = window.viewportWidth.toFloat()
		val H = window.viewportHeight.toFloat()
		if (USE_PROJECTION_MATRIX) {
			proj_mat.transform(v)
			v.addEq(1f, 1f).scaleEq(W / 2, H / 2)
		} else {
			v.x = (v.x - left) * (W / (right - left))
			v.y = H - (v.y - bottom) * H / (top - bottom)
		}
	}

	/**
	 * Convert screen coods to viewport coords using projection only
	 * @param screenX
	 * @param screenY
	 * @return
	 */
	fun untransform(screenX: Float, screenY: Float): MutableVector2D {
		val W = window.viewportWidth.toFloat()
		val H = window.viewportHeight.toFloat()
		if (USE_PROJECTION_MATRIX) {
			val Xd = 2f * screenX / W - 1
			val Yd = 2f * screenY / H - 1
			val v = MutableVector2D(Xd, Yd)
			proj_mat.inverted().transform(v)
			cur_mat.inverted().transform(v)
			return v
		}
		val x = left + screenX / (W / (right - left))
		val y = bottom + (H - screenY) / (H / (top - bottom))
		val v = MutableVector2D(x, y)
		cur_mat.inverted().transform(v)
		return v
	}

	/**
	 *
	 * @param v
	 * @return
	 */
	fun transformXY(v: IVector2D): MutableVector2D {
		transformXY(v.x, v.y, s_vec)
		return MutableVector2D(s_vec)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	fun transformXY(x: Float, y: Float): MutableVector2D {
		transformXY(x, y, s_vec)
		return MutableVector2D(s_vec)
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param v
	 */
	fun transformXY(x: Float, y: Float, v: FloatArray) {
		transformXY(x, y, s_vec)
		v[0] = s_vec.x
		v[1] = s_vec.y
	}

	fun setOrtho(rect: IRectangle) {
		setOrtho(rect.left, rect.left + rect.width, rect.top, rect.top + rect.height)
	}

	/**
	 * @param left
	 * @param right
	 * @param top
	 * @param bottom
	 */
	fun setOrtho(left: Float, right: Float, top: Float, bottom: Float) {
		val xo = 2 / (right - left)
		val yo = 2 / (bottom - top)
		val tx = -(right + left) / (right - left)
		val ty = -(top + bottom) / (bottom - top)
		proj_mat.assign(
			xo.toDouble(), 0.0, tx.toDouble(),
			0.0, yo.toDouble(), ty.toDouble(),
			0.0, 0.0, 1.0
		)
		this.left = left
		this.right = right
		this.top = top
		this.bottom = bottom
	}

	/**
	 *
	 * @param t
	 */
	fun addVertex(t: IVector2D) {
		addVertex(t.x, t.y)
	}

	/**
	 *
	 * @param list a list of Transformable objects
	 */
	fun addVertices(list: Iterable<IVector2D>) {
		for (v in list) {
			addVertex(v)
		}
	}

	/**
	 *
	 * @param list
	 */
	fun addVertices(list: Collection<IVector2D>) {
		for (v in list) {
			addVertex(v)
		}
	}

	/**
	 *
	 * @param list
	 */
	fun addVertices(vararg list: IVector2D) {
		for (v in list) {
			addVertex(v)
		}
	}

	/**
	 * Push the matrix stack
	 */
	fun pushMatrix() {
		if (stackSize < m_stack.size) {
			m_stack[stackSize].assign(m_stack[stackSize - 1])
			m_stack_proj[stackSize].assign(m_stack_proj[stackSize - 1])
			cur_mat = m_stack[stackSize]
			proj_mat = m_stack_proj[stackSize]
			stackSize++
		} else throw GException("Fixed stack size of [" + m_stack.size + "] for pushMatrix")
	}

	/**
	 * pop off the matrix stack
	 */
	fun popMatrix() {
		if (stackSize > 1) {
			stackSize -= 1
			cur_mat = m_stack[stackSize - 1]
			proj_mat = m_stack_proj[stackSize - 1]
		} else throw GException("too many pops")
	}

	/**
	 *
	 * @param name
	 */
	fun setName(name: Int) {
		cur_name = name
	}

	/**
	 * Return the top level matrix
	 * @return
	 */
	val currentTransform: Matrix3x3
		get() = Matrix3x3(cur_mat)

	/**
	 *
	 * @param index
	 * @return
	 */
	fun getX(index: Int): Float {
		return pts[index].x
	}

	/**
	 *
	 * @param index
	 * @return
	 */
	fun getY(index: Int): Float {
		return pts[index].y
	}

	/**
	 *
	 * @param index
	 * @return
	 */
	fun getVertex(index: Int): Vector2D {
		return pts[index]
	}

	/**
	 *
	 * @param index
	 * @return
	 */
	fun getName(index: Int): Int {
		return names[index]
	}

	/**
	 *
	 */
	fun clearVerts() {
		numVerts = 0
	}

	/**
	 *
	 * @param r
	 */
	fun setWindow(r: Renderable) {
		window = r
	}

	/**
	 * Return the transformed vector that is minimum bounding rect point
	 * @return
	 */
	fun getMin(): Vector2D {
		return min
	}

	/**
	 * Return the transformed vector that is the maximum bounding rect point
	 * @return
	 */
	fun getMax(): Vector2D {
		return max
	}

	/**
	 *
	 */
	fun clearBoundingRect() {
		min.assign(Float.MAX_VALUE, Float.MAX_VALUE)
		max.assign(-Float.MAX_VALUE, -Float.MAX_VALUE)
	}

	/**
	 *
	 * @return
	 */
	val MATRIX_STACK_SIZE = 16
	var numVerts = 0
		private set
	private var cur_name = 0
	private val m_stack: Array<Matrix3x3> = Array(MATRIX_STACK_SIZE) { Matrix3x3() }
	private var cur_mat: Matrix3x3 = m_stack[0]
	var stackSize: Int = 1
		private set
	private val m_stack_proj: Array<Matrix3x3> = Array(MATRIX_STACK_SIZE) { Matrix3x3() }
	private var proj_mat: Matrix3x3 = m_stack_proj[0]
	private val s_mat = Matrix3x3()
	private val t_mat = Matrix3x3()
	private var left = 0f
	private var right = 0f
	private var top = 0f
	private var bottom = 0f
	private val MAX_VERTS = 1024

	private val s_vec = MutableVector2D()
	val min = MutableVector2D()
	val max = MutableVector2D()
	private val pts: Array<MutableVector2D> = Array(MAX_VERTS) { MutableVector2D() }
	private val names: IntArray = IntArray(MAX_VERTS) { 0 }

	init {
		cur_mat = m_stack[0]
		proj_mat = m_stack_proj[0]
		makeIdentity()
	}

	companion object {
		var USE_PROJECTION_MATRIX = true
	}
}
