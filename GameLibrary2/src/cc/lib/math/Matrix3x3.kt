package cc.lib.math

import cc.lib.game.IVector2D
import kotlin.math.cos
import kotlin.math.sin

/**
 * A 3x3 matrix implementation
 */
data class Matrix3x3(
	private var a11: Double = 0.0,
	private var a12: Double = 0.0,
	private var a13: Double = 0.0,
	private var a21: Double = 0.0,
	private var a22: Double = 0.0,
	private var a23: Double = 0.0,
	private var a31: Double = 0.0,
	private var a32: Double = 0.0,
	private var a33: Double = 0.0
) {


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
		return arrayOf(
			doubleArrayOf(a11, a12, a13),
			doubleArrayOf(a21, a22, a23),
			doubleArrayOf(a31, a32, a33)
		)
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
	fun setScaleMatrix(s: Double): Matrix3x3 = setScaleMatrix(s, s, s)

	/**
	 * Assign the non-uniform scale matrix given by s1, s2 and s3, to this matrix
	 */
	fun setScaleMatrix(sx: Double, sy: Double, sz: Double): Matrix3x3 {
		a11 = sx
		a12 = 0.0
		a13 = 0.0
		a21 = 0.0
		a22 = sy
		a23 = 0.0
		a31 = 0.0
		a32 = 0.0
		a33 = sz
		return this
	}

	/**
	 * Assign the identity matrix to this matrix
	 */
	fun setIdentityMatrix(): Matrix3x3 {
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

	fun setTranslationMatrix(v: IVector2D): Matrix3x3 {
		return setTranslationMatrix(v.x, v.y)
	}

	fun setTranslationMatrix(tx: Float, ty: Float): Matrix3x3 {
		return assign(1.0, 0.0, tx.toDouble(), 0.0, 1.0, ty.toDouble(), 0.0, 0.0, 1.0)
	}

	fun setRotationMatrix(degrees: Float): Matrix3x3 {
		val rads = (degrees * DEG_TO_RAD).toDouble()
		val cos = cos(rads)
		val sin = sin(rads)
		return assign(cos, -sin, 0.0, sin, cos, 0.0, 0.0, 0.0, 1.0)
	}

	fun setScaleMatrix(sx: Float, sy: Float): Matrix3x3 {
		return assign(sx.toDouble(), 0.0, 0.0, 0.0, sy.toDouble(), 0.0, 0.0, 0.0, 1.0)
	}

	fun setHorizontalSkew(n: Float): Matrix3x3 {
		return assign((1 - 2 * n).toDouble(), n.toDouble(), 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
	}


	fun assign(
		a11: Double, a12: Double, a13: Double,
		a21: Double, a22: Double, a23: Double,
		a31: Double, a32: Double, a33: Double
	): Matrix3x3 {
		this.a11 = a11
		this.a12 = a12
		this.a13 = a13
		this.a21 = a21
		this.a22 = a22
		this.a23 = a23
		this.a31 = a31
		this.a32 = a32
		this.a33 = a33
		return this
	}

	/**
	 * Multiply this matrix by a scalar, return the resulting matrix
	 * @param s
	 * @return
	 */
	infix operator fun times(s: Double): Matrix3x3 = Matrix3x3(
		a11 * s, a12 * s, a13 * s,
		a21 * s, a22 * s, a23 * s,
		a31 * s, a32 * s, a33 * s
	)

	infix operator fun timesAssign(s: Double) {
		assign(
			a11 * s, a12 * s, a13 * s,
			a21 * s, a22 * s, a23 * s,
			a31 * s, a32 * s, a33 * s
		)
	}

	/**
	 * Multiply this matrix by the matrix A and return the result
	 * @param A
	 * @return
	 */
	infix operator fun times(A: Matrix3x3): Matrix3x3 {
		return multiply(this, A, Matrix3x3())
	}

	/**
	 * Multiply this matrix by the matrix A and return the result
	 * @param A
	 * @return
	 */
	infix operator fun timesAssign(A: Matrix3x3) {
		multiply(this, A, this)
	}

	/**
	 * Return M * v (not the same as v * M)
	 *
	 * M * v == v * transpose(M)
	 *
	 */
	infix operator fun times(v: IVector2D): MutableVector2D {
		return MutableVector2D(v).also {
			transform(it)
		}
	}

	fun transform(v: MutableVector2D) {
		val t1 = v.x * a11 + v.y * a12 + a13
		val t2 = v.x * a21 + v.y * a22 + a23
		val t3 = v.x * a31 + v.y * a32 + a33
		v.assign((t1 / t3).toFloat(), (t2 / t3).toFloat())
	}

	//A = A^T 
	fun transposeEq(): Matrix3x3 {
		var t = a12
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
	infix operator fun minus(B: Matrix3x3): Matrix3x3 {
		return subtract(this, B, Matrix3x3())
	}

	infix operator fun minusAssign(B: Matrix3x3) {
		subtract(this, B, this)
	}

	/**
	 * Add to this matrix the matrix B, return result in a new matrix instance
	 * @param B
	 * @return
	 */
	infix operator fun plus(B: Matrix3x3): Matrix3x3 {
		return add(this, B, Matrix3x3())
	}

	infix operator fun plusAssign(B: Matrix3x3) {
		add(this, B, this)
	}

	/**
	 * Compute the determinant of Matrix A
	 * @return
	 */
	fun determinant(): Double {
		return a31 * a12 * a23 - a31 * a13 * a22 - a21 * a12 * a33 + a21 * a13 * a32 + a11 * a22 * a33 - a11 * a23 * a32
		//		return a11*a22*a33 - a11*a23*a32 + a21*a32*a13 - a21*a12*a33 + a31*a12*a23 - a31*a22*a13;
	}

	fun inverseEq(): Matrix3x3 {
		return inverse(this, this)
	}

	fun inverted(): Matrix3x3 {
		return inverse(this, Matrix3x3())
	}

	override fun toString(): String {
		return """
	    	[$a11, $a12, $a13]
	    	[$a21, $a22, $a23]
	    	[$a31, $a32, $a33]
	    	""".trimIndent()
	}

	val isNaN: Boolean
		/**
		 * Check matrix for NaN values
		 */
		get() = (java.lang.Double.isNaN(a11) || java.lang.Double.isNaN(a12) || java.lang.Double.isNaN(a13)
			|| java.lang.Double.isNaN(a21) || java.lang.Double.isNaN(a22) || java.lang.Double.isNaN(a23)
			|| java.lang.Double.isNaN(a31) || java.lang.Double.isNaN(a32) || java.lang.Double.isNaN(a33))

	fun toArray(): DoubleArray {
		return doubleArrayOf(
			a11, a21, a31,
			a12, a22, a32,
			a13, a23, a33
		)
	}

	fun toFloatArray(): FloatArray {
		return floatArrayOf(
			a11.toFloat(),
			a21.toFloat(),
			a31.toFloat(),
			a12.toFloat(),
			a22.toFloat(),
			a32.toFloat(),
			a13.toFloat(),
			a23.toFloat(),
			a33.toFloat()
		)
	}

	fun copyInto(arr: FloatArray) {
		floatArrayOf(
			a11.toFloat(),
			a21.toFloat(),
			a31.toFloat(),
			a12.toFloat(),
			a22.toFloat(),
			a32.toFloat(),
			a13.toFloat(),
			a23.toFloat(),
			a33.toFloat()
		).copyInto(arr)
	}

	/**
	 * Return the Frobenius norm of this Matrix
	 * @return
	 */
	fun toFrobeniusNormal(): Double {
		return Math.sqrt(a11 * a11 + a12 * a12 + a13 * a13 + a21 * a21 + a22 * a22 + a23 * a23 + a31 * a31 + a32 * a32 + a33 * a33)
	}

	fun translateEq(v: Vector2D): Matrix3x3 {
		return times(newTranslate(v))
	}

	fun translateEq(x: Float, y: Float): Matrix3x3 {
		return multiply(this, newTranslate(x.toDouble(), y.toDouble()), this)
	}

	fun scaleEq(sx: Float, sy: Float): Matrix3x3 {
		return multiply(this, newScale(sx.toDouble(), sy.toDouble(), 1.0), this)
	}

	fun rotateEq(degrees: Float): Matrix3x3 {
		return multiply(this, newRotation(degrees), this)
	}

	companion object {
		fun newIdentity(): Matrix3x3 {
			return Matrix3x3(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
		}

		fun newScale(scaleX: Double, scaleY: Double, scaleZ: Double): Matrix3x3 {
			return Matrix3x3(
				scaleX, 0.0, 0.0,
				0.0, scaleY, 0.0,
				0.0, 0.0, scaleZ
			)
		}

		fun newScale(scale: Double): Matrix3x3 = newScale(scale, scale, scale)

		fun newTranslate(tx: Double, ty: Double, tz: Double = 1.0): Matrix3x3 = Matrix3x3(
			1.0, 0.0, tx,
			0.0, 1.0, ty,
			0.0, 0.0, tz
		)

		fun newTranslate(v: IVector2D): Matrix3x3 = newTranslate(v.x.toDouble(), v.y.toDouble())

		fun newRotation(degrees: Float): Matrix3x3 {
			val rads = (degrees * DEG_TO_RAD).toDouble()
			val cos = cos(rads)
			val sin = sin(rads)
			return Matrix3x3(cos, -sin, 0.0, sin, cos, 0.0, 0.0, 0.0, 1.0)
		}

		//C = AxB 
		@JvmStatic
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

			//copy to C
			C.a11 = t11
			C.a12 = t12
			C.a13 = t13
			C.a21 = t21
			C.a22 = t22
			C.a23 = t23
			C.a31 = t31
			C.a32 = t32
			C.a33 = t33
			return C
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
		 * Compute the inverse of the matrix A, place the result in C
		 */
		fun inverse(A: Matrix3x3, C: Matrix3x3 = Matrix3x3()): Matrix3x3 {
			val d =
				A.determinant() //(A.a31*A.a12*A.a23-A.a31*A.a13*A.a22-A.a21*A.a12*A.a33+A.a21*A.a13*A.a32+A.a11*A.a22*A.a33-A.a11*A.a23*A.a32);
			val t11 = (A.a22 * A.a33 - A.a23 * A.a32) / d
			val t12 = -(A.a12 * A.a33 - A.a13 * A.a32) / d
			val t13 = (A.a12 * A.a23 - A.a13 * A.a22) / d
			val t21 = -(-A.a31 * A.a23 + A.a21 * A.a33) / d
			val t22 = (-A.a31 * A.a13 + A.a11 * A.a33) / d
			val t23 = -(-A.a21 * A.a13 + A.a11 * A.a23) / d
			val t31 = (-A.a31 * A.a22 + A.a21 * A.a32) / d
			val t32 = -(-A.a31 * A.a12 + A.a11 * A.a32) / d
			val t33 = (-A.a21 * A.a12 + A.a11 * A.a22) / d
			C.a11 = t11
			C.a12 = t12
			C.a13 = t13
			C.a21 = t21
			C.a22 = t22
			C.a23 = t23
			C.a31 = t31
			C.a32 = t32
			C.a33 = t33
			return C
		}
	}
}
