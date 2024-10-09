package cc.lib.math

/**
 * Complex arithmetic.  Most functions are accounted for and gotten from web.
 * Some effort made to speed up computations, specifically
 * 1> Having 3 pow versions (int, double, complex) (fast, med, slow)
 * 2> No calls to 'new' user provides value for result to support caching
 *
 * @author chriscaron
 */
data class ComplexNumber(private var real: Double = 0.0, private var imag: Double = 0.0) {

	constructor(toCopy: ComplexNumber) : this(toCopy.real, toCopy.imag)

	fun getReal(): Double = real

	fun getImag(): Double = imag

	/**
	 *
	 * @param rhs
	 * @return
	 */
	fun add(rhs: ComplexNumber): ComplexNumber {
		return ComplexNumber(real + rhs.real, imag + rhs.imag)
	}

	/**
	 * Modifier, return this + rhs
	 * @param rhs
	 * @return
	 */
	fun addEq(rhs: ComplexNumber): ComplexNumber {
		return set(real + rhs.real, imag + rhs.imag)
	}

	/**
	 *
	 * @param rhs
	 * @return
	 */
	fun sub(rhs: ComplexNumber): ComplexNumber {
		return ComplexNumber(real - rhs.real, imag - rhs.imag)
	}

	/**
	 * Modifier, return this - rhs
	 * @param rhs
	 * @return
	 */
	fun subEq(rhs: ComplexNumber): ComplexNumber {
		return set(real - rhs.real, imag - rhs.imag)
	}

	/**
	 * Return negative this
	 * @param result
	 * @return
	 */
	fun negate(result: ComplexNumber): ComplexNumber {
		return result.set(-real, -imag)
	}

	fun negate(): ComplexNumber {
		return ComplexNumber(-real, -imag)
	}

	/**
	 * Modifier, return -this
	 * @return
	 */
	fun negateEq(): ComplexNumber {
		return set(-real, -imag)
	}

	/**
	 *
	 * @param scalar
	 * @return
	 */
	fun scale(scalar: Double): ComplexNumber {
		return ComplexNumber(real * scalar, imag * scalar)
	}

	/**
	 * Modifier, return this scaled by a double
	 * @param scalar
	 * @return
	 */
	fun scaleEq(scalar: Double): ComplexNumber {
		return set(scalar.let { real *= it; real }, scalar.let { imag *= it; imag })
	}

	/**
	 *
	 * @param rhs
	 * @return
	 */
	fun multiply(rhs: ComplexNumber): ComplexNumber {
		return ComplexNumber(real * rhs.real - imag * rhs.imag, imag * rhs.real + real * rhs.imag)
	}

	/**
	 *
	 * @param rhs
	 * @return
	 */
	fun multiplyEq(rhs: ComplexNumber): ComplexNumber {
		return set(real * rhs.real - imag * rhs.imag, imag * rhs.real + real * rhs.imag)
	}

	/**
	 * Return this / denom
	 * @param denom
	 * @return
	 */
	fun divide(denom: ComplexNumber): ComplexNumber {
		val den = Math.pow(denom.mod(), 2.0)
		return ComplexNumber((real * denom.real + imag * denom.imag) / den, (imag * denom.real - real * denom.imag) / den)
	}

	/**
	 * Synonym for mod
	 * @return
	 */
	fun abs(): Double {
		return mod()
	}

	/**
	 * Synonym for mod
	 * @return
	 */
	fun mag(): Double {
		return mod()
	}

	/**
	 * Return sqrt(this.real^2 + this.imag^2)
	 * @return
	 */
	fun mod(): Double {
		return if (real != 0.0 || imag != 0.0) Math.sqrt(real * real + imag * imag) else 0.0
	}

	/**
	 * Return atan2(this.imag, this.real)
	 * @return
	 */
	fun arg(): Double {
		return Math.atan2(imag, real)
	}

	/**
	 * Return [-this.imag, this.real]
	 * @return
	 */
	fun conj(): ComplexNumber {
		return ComplexNumber(-imag, real)
	}

	/**
	 * Return sqrt(this)
	 * @return
	 */
	fun sqrt(): ComplexNumber {
		val r = Math.sqrt(mod())
		val theta = arg() / 2
		return ComplexNumber(r * Math.cos(theta), r * Math.sin(theta))
	}

	/**
	 * Return this ^ int
	 * @param power
	 * @return
	 */
	fun powi(power: Int): ComplexNumber {
		val result = ComplexNumber()
		result[1.0] = 0.0
		for (i in 0 until power) {
			result.multiplyEq(this)
		}
		return result
	}

	/**
	 * Return this ^ double
	 * @param n
	 * @return
	 */
	fun powd(n: Double): ComplexNumber {
		val result = ComplexNumber()
		val real = n * Math.log(abs())
		val imag = n * arg()
		val scalar = Math.exp(real)
		return result.set(scalar * Math.cos(imag), scalar * Math.sin(imag))
	}

	/**
	 * Return [e^power.real * cos(power.imag), e^power.real * sin(power.imag)]
	 * @param power
	 * @return
	 */
	fun powc(power: ComplexNumber): ComplexNumber {
		return power.multiply(ln()).exp()
	}

	// Real cosh function (used to compute complex trig functions)
	private fun cosh(theta: Double): Double {
		return (Math.exp(theta) + Math.exp(-theta)) / 2
	}

	// Real sinh function (used to compute complex trig functions)
	private fun sinh(theta: Double): Double {
		return (Math.exp(theta) - Math.exp(-theta)) / 2
	}

	/**
	 * Return [fc(this.imag) * sin(this.real), fs(this.imag)*cos(this.real)]
	 * where fs(x) = (e^x - e^-x)/2
	 * and   fc(x) = (e^x + e^-x)/2
	 * @return
	 */
	fun sine(): ComplexNumber {
		return ComplexNumber(cosh(imag) * Math.sin(real), sinh(imag) * Math.cos(real))
	}

	/**
	 * Return [fs(this.real) * cos.(this.imag), fc(this.real)*sin(this.imag)]
	 * where fs(x) = (e^x - e^-x)/2
	 * and   fc(x) = (e^x + e^-x)/2
	 * @return
	 */
	fun sineh(): ComplexNumber {
		return ComplexNumber(sinh(real) * Math.cos(imag), cosh(real) * Math.sin(imag))
	}

	/**
	 * Return -I * ln((this*I) + sqrt(1-this^2))
	 * @return
	 */
	fun asine(): ComplexNumber {
		//return -I * ((this*I) + (1 - (this * this)).Sqrt()).Log();
		val negI = ComplexNumber(0.0, -1.0)
		val ttI = multiply(negI)
		val lhs = negI.multiply(ttI)
		val ttt = multiply(this)
		val omttt = ComplexNumber(1.0, 0.0).sub(ttt)
		val rhs = omttt.sqrt().ln()
		return lhs.add(rhs)
	}

	/**
	 * return ln(this + sqrt((this^2)+1))
	 *
	 * @return
	 */
	fun asineh(): ComplexNumber {
		//return (this + ((this*this) + 1).Sqrt()).Log();
		val tttpo = multiply(this)
		tttpo.real += 1.0
		val rhs = tttpo.sqrt().ln()
		return add(rhs)
	}

	fun cosine(): ComplexNumber {
		return ComplexNumber(cosh(imag) * Math.cos(real), -sinh(imag) * Math.sin(real))
	}

	/**
	 *
	 * @return
	 */
	fun cosineh(): ComplexNumber {
		return ComplexNumber(cosh(real) * Math.cos(imag), sinh(real) * Math.sin(imag))
	}

	/**
	 * Return -I * ln(this+I * sqrt(1-(this^2))
	 * @return
	 */
	fun acosine(): ComplexNumber {
		// return -I * (this + I * (1 - (this*this)).Sqrt()).Log();
		val negI = ComplexNumber(0.0, -1.0)
		val ttt = multiply(this)
		val omttt = ComplexNumber(1.0, 0.0).subEq(ttt)
		val rhs = add(ComplexNumber(0.0, 1.0).multiply(omttt.sqrt().ln()))
		return negI.multiply(rhs)
	}

	/**
	 * Return 2 * ln(sqrt((this+1)/2) + sqrt(((this-1)/2))
	 * @return
	 */
	fun acosineh(): ComplexNumber {
		//return 2d * (((this+1d) / 2d).Sqrt() + ((this-1) / 2d).Sqrt()).Log();
		val tp1d2 = ComplexNumber((real + 1) / 2, imag / 2)
		val tm1d2 = ComplexNumber((real - 1) / 2, imag / 2)
		val lhs = tp1d2.sqrt().scaleEq(2.0)
		val rhs = tm1d2.sqrt().ln()
		return lhs.add(rhs)
	}

	/**
	 * Return sin(this) / cosine(this)
	 * @return
	 */
	fun tangent(): ComplexNumber {
		val x = sine()
		val y = cosine()
		return x.divide(y)
	}

	fun tangenth(): ComplexNumber {
		val x = sineh()
		val y = cosineh()
		return x.divide(y)
	}

	fun atangent(): ComplexNumber {
		//return -I/2 * ((I - this)/(I + this)).Log();
		val negI2 = ComplexNumber(0.0, -0.5)
		val Imt = ComplexNumber(0 - real, 1 - imag)
		val Ipt = ComplexNumber(0 + real, 1 + imag)
		val rhs = Imt.divide(Ipt).ln()
		return negI2.multiply(rhs)
	}

	fun atangenth(): ComplexNumber {
		// return ((1+this) / (1-this)).Log() / 2d;
		val opt = ComplexNumber(real + 1, imag)
		val omt = ComplexNumber(1 - real, imag)
		return opt.divide(omt).ln().scaleEq(0.5)
	}

	fun ln(): ComplexNumber {
		return ComplexNumber(Math.log(mod()), arg())
	}

	fun exp(): ComplexNumber {
		val ex = Math.exp(real)
		return ComplexNumber(ex * Math.cos(imag), ex * Math.sin(imag))
	}

	operator fun set(a: Double, b: Double): ComplexNumber {
		real = a
		imag = b
		return this
	}

	fun copy(rhs: ComplexNumber): ComplexNumber {
		return set(rhs.real, rhs.imag)
	}

	override fun equals(o: Any?): Boolean {
		val c = o as ComplexNumber?
		return c!!.real == real && c.imag == imag
	}

	val isNaN: Boolean
		get() = java.lang.Double.isNaN(real) || java.lang.Double.isNaN(imag)
	val isInfinity: Boolean
		get() = java.lang.Double.isInfinite(real) || java.lang.Double.isInfinite(imag)

	fun isReal(): Boolean {
		return Math.abs(imag) <= 0.0000001
	}

	protected val isImmutable: Boolean
		protected get() = true

	override fun toString(): String {
		if (isNaN) return "NaN"
		if (isInfinity) return "INF"
		return if (isReal()) formatDouble(real) else "[" + formatDouble(real) + "," + formatDouble(
			imag
		) + "]"
	}

	companion object {
		// TODO: Move to Utils
		fun formatDouble(d: Double): String {
			val opt1 = d.toString()
			var opt2 = String.format("%.6f", d)
			while (opt2.length > 2 && opt2[opt2.length - 1] == '0' && opt2[opt2.length - 2] != '.') {
				opt2 = opt2.substring(0, opt2.length - 1)
			}
			return if (opt1.length < opt2.length) opt1 else opt2
		}
	}
} // end ComplexNumber class
