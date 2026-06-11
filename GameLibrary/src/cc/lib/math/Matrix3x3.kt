package cc.lib.math

import cc.lib.game.IVector2D
import cc.lib.game.Utils
import cc.lib.reflector.Reflector
import kotlin.math.cos
import kotlin.math.sin

/**
 * A 3x3 matrix implementation
 */
class Matrix3x3(
	a11: Number = 0.0,
	a12: Number = 0.0,
	a13: Number = 0.0,
	a21: Number = 0.0,
	a22: Number = 0.0,
	a23: Number = 0.0,
	a31: Number = 0.0,
	a32: Number = 0.0,
	a33: Number = 0.0

) : Reflector<Matrix3x3>() {

	private var a11 = a11.toDouble()
	private var a12 = a12.toDouble()
	private var a13 = a13.toDouble()
	private var a21 = a21.toDouble()
	private var a22 = a22.toDouble()
	private var a23 = a23.toDouble()
	private var a31 = a31.toDouble()
	private var a32 = a32.toDouble()
	private var a33 = a33.toDouble()

	/**
	 * Assign the zero matrix to this matrix
	 * @return `this`
	 */
	fun zeroEq(): Matrix3x3 {
		a11 = 0.0
		a12 = 0.0
		a13 = 0.0
		a21 = 0.0
		a22 = 0.0
		a23 = 0.0
		a31 = 0.0
		a32 = 0.0
		a33 = 0.0
		return this
	}


	/**
	 * Construct a new 3x3 matrix as a copy of the given matrix B
	 * @param B
	 * @throws NullPointerException
	 */
	constructor(B: Matrix3x3) : this() {
		assign(B)
	}

	fun get(): Array<DoubleArray> {
		return arrayOf(doubleArrayOf(a11, a12, a13), doubleArrayOf(a21, a22, a23), doubleArrayOf(a31, a32, a33))
	}

	/**
	 * assign the value of B to this Matrix
	 * @param B
	 */
	fun assign(B: Matrix3x3): Matrix3x3 {
		a11 = B.a11
		a12 = B.a12
		a13 = B.a13
		a21 = B.a21
		a22 = B.a22
		a23 = B.a23
		a31 = B.a31
		a32 = B.a32
		a33 = B.a33
		return this
	}

	/**
	 * Assign the scale matrix given by s, to this matrix
	 */
	operator fun times(s: Number): Matrix3x3 {
		val ss = s.toDouble()
		return Matrix3x3(
			a11 * ss, a12 * ss, a13 * ss,
			a21 * ss, a22 * ss, a23 * ss,
			a31 * ss, a32 * ss, a33 * ss,
		)
	}

	operator fun timesAssign(s: Number) {
		val ss = s.toDouble()
		assign(
			a11 * ss, a12 * ss, a13 * ss,
			a21 * ss, a22 * ss, a23 * ss,
			a31 * ss, a32 * ss, a33 * ss,
		)
	}

	/**
	 * Assign the identity matrix to this matrix
	 */
	fun identityEq(): Matrix3x3 {
		a11 = 1.0
		a12 = 0.0
		a13 = 0.0
		a21 = 0.0
		a22 = 1.0
		a23 = 0.0
		a31 = 0.0
		a32 = 0.0
		a33 = 1.0
		return this
	}

	fun assign(
		a11: Number, a12: Number, a13: Number,
		a21: Number, a22: Number, a23: Number,
		a31: Number, a32: Number, a33: Number
	): Matrix3x3 {
		this.a11 = a11.toDouble()
		this.a12 = a12.toDouble()
		this.a13 = a13.toDouble()
		this.a21 = a21.toDouble()
		this.a22 = a22.toDouble()
		this.a23 = a23.toDouble()
		this.a31 = a31.toDouble()
		this.a32 = a32.toDouble()
		this.a33 = a33.toDouble()
		return this
	}

	/**
	 * Multiply a vector by this matrix, return the resulting vector
	 */
	operator fun times(v: IVector2D): Vector2D {
		val r = MutableVector2D()
		multiply(this, v, r)
		return r
	}

	operator fun times(m: Matrix3x3): Matrix3x3 {
		return Matrix3x3().also {
			multiply(this, m, it)
		}
	}

	operator fun timesAssign(m: Matrix3x3) {
		multiply(this, m, this)
	}

	//A = A^T 
	fun transposeEq(): Matrix3x3 {
		var t: Double = a12
		a12 = a21
		a21 = t
		t = a13
		a13 = a31
		a31 = t
		t = a23
		a23 = a32
		a32 = t
		return this
	}

	/**
	 * Functional method. Transpose this matrix and return the result
	 * @return
	 */
	fun transpose(): Matrix3x3 {
		return Matrix3x3(this).transposeEq()
	}

	/**
	 * Substract to this matrix the matrix B, return result in a new matrix instance
	 * @param B
	 * @return
	 */
	operator fun minus(B: Matrix3x3): Matrix3x3 {
		return subtract(this, B, Matrix3x3())
	}

	operator fun minusAssign(B: Matrix3x3) {
		subtract(this, B, this)
	}

	/**
	 * Add to this matrix the matrix B, return result in a new matrix instance
	 * @param B
	 * @return
	 */
	operator fun plus(B: Matrix3x3): Matrix3x3 {
		return add(this, B, Matrix3x3())
	}

	operator fun plusAssign(B: Matrix3x3) {
		add(this, B, this)
	}

	fun setTranslate(v: IVector2D): Matrix3x3 {
		return assign(1, 0, v.x,
			0, 1, v.y,
			0, 0, 1)
	}

	fun setTranslate(x: Number, y: Number): Matrix3x3 {
		return assign(1, 0, x,
			0, 1, y,
			0, 0, 1)
	}

	fun setRotate(degrees: Number): Matrix3x3 {
		val rads = (degrees.toFloat() * CMath.DEG_TO_RAD).toDouble()
		val cos = cos(rads)
		val sin = sin(rads)
		return assign(cos, -sin, 0.0, sin, cos, 0.0, 0.0, 0.0, 1.0)
	}

	/**
	 * Compute the determinant of Matrix A
	 * @return
	 */
	fun determinant(): Double {
		return a31 * a12 * a23 - a31 * a13 * a22 - a21 * a12 * a33 + a21 * a13 * a32 + a11 * a22 * a33 - a11 * a23 * a32
	}

	operator fun unaryMinus(): Matrix3x3 {
		return Matrix3x3(
			-a11, -a12, -a13,
			-a21, -a22, -a23,
			-a31, -a32, -a33
		)
	}

	fun inverted(): Matrix3x3 {
		return inverse(this, Matrix3x3())
	}

	fun inverseEq(): Matrix3x3 {
		return inverse(this, this)
	}

	override fun toString(): String {
		return """
	    	[$a11, $a12, $a13]
	    	[$a21, $a22, $a23]
	    	[$a31, $a32, $a33]
	    	""".trimIndent()
	}

	/**
	 * Check matrix for NaN values
	 */
	val isNaN: Boolean
		get() = toArray().any { java.lang.Double.isNaN(it) }

	fun toArray(): DoubleArray {
		return doubleArrayOf(
			a11, a21, a31,
			a12, a22, a32,
			a13, a23, a33)
	}

	fun toFloatArray(): FloatArray {
		return floatArrayOf(a11.toFloat(), a21.toFloat(), a31.toFloat(), a12.toFloat(), a22.toFloat(), a32.toFloat(), a13.toFloat(), a23.toFloat(), a33.toFloat())
	}

	fun copyInto(arr: FloatArray) {
		System.arraycopy(
			Utils.toFloatArray(a11.toFloat(), a21.toFloat(), a31.toFloat(), a12.toFloat(), a22.toFloat(), a32.toFloat(), a13.toFloat(), a23.toFloat(), a33.toFloat()),
			0, arr, 0, arr.size)
	}

	/**
	 * Return the Frobenius norm of this Matrix
	 * @return
	 */
	fun fnorm(): Double {
		return Math.sqrt(a11 * a11 + a12 * a12 + a13 * a13 + a21 * a21 + a22 * a22 + a23 * a23 + a31 * a31 + a32 * a32 + a33 * a33)
	}

	fun transform(v: MutableVector2D) {
		v.assign(times(v))
	}

	fun translated(v: IVector2D): Matrix3x3 {
		return multiply(this, newTranslate(v), this)
	}

	fun translateEq(v: IVector2D): Matrix3x3 {
		return multiply(this, newTranslate(v), this)
	}

	fun translateEq(x: Number, y: Number): Matrix3x3 {
		return multiply(this, newTranslate(x, y), this)
	}

	fun translated(x: Number, y: Number): Matrix3x3 {
		return times(newTranslate(x, y))
	}

	fun scaled(sx: Number, sy: Number): Matrix3x3 {
		return Matrix3x3(a11 * sx.toDouble(), a12, a13,
			a21, a22 * sy.toDouble(), a23,
			a31, a32, a33)
	}

	fun scaleEq(sx: Number, sy: Number): Matrix3x3 {
		return assign(a11 * sx.toDouble(), a12, a13,
			a21, a22 * sy.toDouble(), a23,
			a31, a32, a33)
	}

	fun rotateEq(degrees: Number): Matrix3x3 {
		return multiply(this, newRotation(degrees), this)
	}

	companion object {
		init {
			addAllFields(Matrix3x3::class.java)
		}

		fun newIdentity(): Matrix3x3 {
			return Matrix3x3(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
		}

		//C = AxB 
		fun multiply(A: Matrix3x3, B: Matrix3x3, C: Matrix3x3): Matrix3x3 {
			//               B | b11 b12 b13
			//                 | b21 b22 b23
			//                 | b31 b32 b33
			//     -------------------------
			//  A  a11 a12 a13 | c11 c12 c13
			//     a21 a22 a23 | c21 c22 c23
			//     a31 a32 a33 | c31 c32 c33  C
			val t11 = A.a11 * B.a11 + A.a12 * B.a21 + A.a13 * B.a31
			val t12 = A.a11 * B.a12 + A.a12 * B.a22 + A.a13 * B.a32
			val t13 = A.a11 * B.a13 + A.a12 * B.a23 + A.a13 * B.a33
			val t21 = A.a21 * B.a11 + A.a22 * B.a21 + A.a23 * B.a31
			val t22 = A.a21 * B.a12 + A.a22 * B.a22 + A.a23 * B.a32
			val t23 = A.a21 * B.a13 + A.a22 * B.a23 + A.a23 * B.a33
			val t31 = A.a31 * B.a11 + A.a32 * B.a21 + A.a33 * B.a31
			val t32 = A.a31 * B.a12 + A.a32 * B.a22 + A.a33 * B.a32
			val t33 = A.a31 * B.a13 + A.a32 * B.a23 + A.a33 * B.a33

			return C.assign(t11, t12, t13, t21, t22, t23, t31, t32, t33)
		}

		//C = A-B
		fun subtract(A: Matrix3x3, B: Matrix3x3, C: Matrix3x3): Matrix3x3 {
			C.a11 = A.a11 - B.a11
			C.a12 = A.a12 - B.a12
			C.a13 = A.a13 - B.a13
			C.a21 = A.a21 - B.a21
			C.a22 = A.a22 - B.a22
			C.a23 = A.a23 - B.a23
			C.a31 = A.a31 - B.a31
			C.a32 = A.a32 - B.a32
			C.a33 = A.a33 - B.a33
			return C
		}

		//C = A+B
		fun add(A: Matrix3x3, B: Matrix3x3, C: Matrix3x3): Matrix3x3 {
			C.a11 = A.a11 + B.a11
			C.a12 = A.a12 + B.a12
			C.a13 = A.a13 + B.a13
			C.a21 = A.a21 + B.a21
			C.a22 = A.a22 + B.a22
			C.a23 = A.a23 + B.a23
			C.a31 = A.a31 + B.a31
			C.a32 = A.a32 + B.a32
			C.a33 = A.a33 + B.a33
			return C
		}

		/**
		 * Multiply v by A, and place result in r, so r = Av
		 * @param A 3 by 3 matrix
		 * @param v Vector to be multiplied
		 * @param r Vector to hold result of multiplication
		 * @return Reference to the given IVector2D r instance
		 */
		fun multiply(A: Matrix3x3, v: IVector2D, r: MutableVector2D): MutableVector2D {
			//
			//               V | v1
			//                 | v2
			//                 | 1
			//     -----------------
			//  A  a11 a12 a13 | c1
			//     a21 a22 a23 | c2
			//     a31 a32 a33 | c3
			val t1 = v.x * A.a11 + v.y * A.a12 + A.a13
			val t2 = v.x * A.a21 + v.y * A.a22 + A.a23
			val t3 = v.x * A.a31 + v.y * A.a32 + A.a33
			r.assign((t1 / t3).toFloat(), (t2 / t3).toFloat())
			return r
		}

		/**
		 * Compute the inverse of the matrix A, place the result in C
		 */
		fun inverse(A: Matrix3x3, C: Matrix3x3 = Matrix3x3()): Matrix3x3 {
			val d = A.determinant()
			val t11 = (A.a22 * A.a33 - A.a23 * A.a32) / d
			val t12 = -(A.a12 * A.a33 - A.a13 * A.a32) / d
			val t13 = (A.a12 * A.a23 - A.a13 * A.a22) / d
			val t21 = -(-A.a31 * A.a23 + A.a21 * A.a33) / d
			val t22 = (-A.a31 * A.a13 + A.a11 * A.a33) / d
			val t23 = -(-A.a21 * A.a13 + A.a11 * A.a23) / d
			val t31 = (-A.a31 * A.a22 + A.a21 * A.a32) / d
			val t32 = -(-A.a31 * A.a12 + A.a11 * A.a32) / d
			val t33 = (-A.a21 * A.a12 + A.a11 * A.a22) / d
			return C.assign(t11, t12, t13, t21, t22, t23, t31, t32, t33)
		}

		fun newTranslate(v: IVector2D): Matrix3x3 {
			return newTranslate(v.x, v.y)
		}

		fun newTranslate(tx: Number, ty: Number): Matrix3x3 {
			return Matrix3x3(1.0, 0.0, tx.toDouble(), 0.0, 1.0, ty.toDouble(), 0.0, 0.0, 1.0)
		}

		fun newRotation(degrees: Number): Matrix3x3 {
			val rads = (degrees.toFloat() * CMath.DEG_TO_RAD).toDouble()
			val cos = cos(rads)
			val sin = sin(rads)
			return Matrix3x3(cos, -sin, 0.0, sin, cos, 0.0, 0.0, 0.0, 1.0)
		}

		fun newScale(sx: Number, sy: Number): Matrix3x3 {
			return Matrix3x3(sx.toDouble(), 0.0, 0.0, 0.0, sy.toDouble(), 0.0, 0.0, 0.0, 1.0)
		}

		fun newHorzSkew(n: Number): Matrix3x3 {
			return Matrix3x3((1 - 2 * n.toDouble()), n.toDouble(), 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
		}

	}
}
