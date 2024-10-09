package cc.lib.game

import cc.lib.ksp.mirror.DirtyType
import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import kotlin.math.roundToInt

@Mirror(dirtyType = DirtyType.ANY)
interface IColor : Mirrored {
	val argb: Int

	override fun isMutable() = false
}

/**
 * Abstract color class to support applet/android variations
 *
 * @author ccaron
 */
class GColor : ColorImpl {
	private var name: String? = null

	constructor()

	@JvmOverloads
	constructor(r: Int, g: Int, b: Int, a: Int = 255) {
		set(r, g, b, a)
	}

	constructor(r: Int, g: Int, b: Int, name: String?) : this(r, g, b) {
		this.name = name
	}

	constructor(argb: Int) {
		this.argb = argb
	}

	constructor(argb: Int, name: String?) : this(argb) {
		this.name = name
	}

	constructor(toCopy: GColor) : this(toCopy.argb)
	constructor(r: Float, g: Float, b: Float, a: Float) : this(g, g, b, a, null)
	constructor(r: Float, g: Float, b: Float, a: Float, name: String?) {
		this.name = name
		set(
			(r.coerceIn(0f, 1f) * 255).roundToInt(),
			(g.coerceIn(0f, 1f) * 255).roundToInt(),
			(b.coerceIn(0f, 1f) * 255).roundToInt(),
			(a.coerceIn(0f, 1f) * 255).roundToInt()
		)
	}

	val red: Float
		/**
		 * Return red component value between 0-1
		 *
		 * @return
		 */
		get() = red().toFloat() / 255
	val green: Float
		/**
		 * Return green component value between 0-1
		 * @return
		 */
		get() = green().toFloat() / 255
	val blue: Float
		/**
		 * Return blue component value between 0-1
		 * @return
		 */
		get() = blue().toFloat() / 255
	val alpha: Float
		/**
		 * Return alpha component value between 0-1
		 * @return
		 */
		get() = alpha().toFloat() / 255

	fun red(): Int {
		return argb shr 16 and 0xff
	}

	fun green(): Int {
		return argb shr 8 and 0xff
	}

	fun blue(): Int {
		return argb shr 0 and 0xff
	}

	fun alpha(): Int {
		return argb ushr 24 and 0xff
	}

	operator fun set(r: Int, g: Int, b: Int, a: Int) {
		argb = (a.coerceIn(0, 255) shl 24
			or (r.coerceIn(0, 255) shl 16)
			or (g.coerceIn(0, 255) shl 8)
			or b.coerceIn(0, 255))
	}

	/**
	 *
	 * @param amount value between 0-1 to indcate amount of RGB to remove
	 * @return
	 */
	fun darkened(amount: Float): GColor {
		if (amount < 0.01f) return this
		var R = amount * red
		var G = amount * green
		var B = amount * blue
		R = (red - R).coerceIn(0f, 255f)
		G = (green - G).coerceIn(0f, 255f)
		B = (blue - B).coerceIn(0f, 255f)
		return GColor(R, G, B, alpha)
	}

	/**
	 * Return new color that is lightened of this
	 *
	 * @param amount value between 0-1 to indcate amount of RGB to add
	 * @return
	 */
	fun lightened(amount: Float): GColor {
		if (amount < 0.01f) return this
		var R = amount * red
		var G = amount * green
		var B = amount * blue
		R = (red + R).coerceIn(0f, 255f)
		G = (green + G).coerceIn(0f, 255f)
		B = (blue + B).coerceIn(0f, 255f)
		return GColor(R, G, B, alpha)
	}

	/**
	 *
	 * @return
	 */
	fun toARGB(): Int {
		return argb
	}

	/**
	 *
	 * @return
	 */
	fun toRGB(): Int {
		return -0x1000000 or argb
	}

	/**
	 * This function added for AWT but AWT color is NOT in RGBA format even though input parameter suggests it is.
	 * @see java.awt.Color
	 * @return
	 */
	@Deprecated("")
	fun toRGBA(): Int {
		val alpha = argb ushr 24 and 0xff
		return argb shl 8 or alpha
	}

	override fun toString(): String {
		return name ?: String.format("ARGB[%d,%d,%d,%d]", alpha(), red(), green(), blue())
	}

	override fun equals(o: Any?): Boolean {
		if (o == null) return false
		if (o !is GColor) return false
		if (o === this) return true
		return argb == o.argb
	}

	fun equalsWithinThreshold(c: GColor?, threshold: Int): Boolean {
		if (c == null) return false
		if (c === this) return true
		if (Math.abs(alpha() - c.alpha()) > threshold) return false
		if (Math.abs(red() - c.red()) > threshold) return false
		if (Math.abs(green() - c.green()) > threshold) return false
		return if (Math.abs(blue() - c.blue()) > threshold) false else true
	}

	/**
	 * Return new color that is interpolation between this and parameter
	 * @param target
	 * @param factor
	 * @return
	 */
	fun interpolateTo(target: GColor, factor: Float): GColor {
		if (factor > 0.99) return this
		if (factor < 0.01) return target
		val R = (red * factor + target.red * (1.0f - factor)).coerceIn(0f, 1f)
		val G = (green * factor + target.green * (1.0f - factor)).coerceIn(0f, 1f)
		val B = (blue * factor + target.blue * (1.0f - factor)).coerceIn(0f, 1f)
		val A = (alpha * factor + target.alpha * (1.0f - factor)).coerceIn(0f, 1f)
		return GColor(R, G, B, A)
	}

	/**
	 * Return a new color instance with RGB components of this but specified alpha
	 *
	 * @param alpha
	 * @return
	 */
	fun withAlpha(alpha: Float): GColor {
		return GColor(red(), green(), blue(), Math.round(alpha * 255))
	}

	/**
	 * Return a new color instance with RGB components of this and the specified alpha
	 *
	 * @param alpha
	 * @return
	 */
	fun withAlpha(alpha: Int): GColor {
		return GColor(red(), green(), blue(), alpha)
	}

	/**
	 * return a color with its components summed.
	 *
	 * @param other
	 * @return
	 */
	fun add(other: GColor): GColor {
		return GColor(
			1f.coerceAtMost(red + other.red),
			1f.coerceAtMost(green + other.green),
			1f.coerceAtMost(blue + other.blue),
			1f.coerceAtMost(alpha + other.alpha)
		)
	}

	/**
	 * Return color with RGB components equal to 1-RGB. [.5,.5,.5] will be unchanged.
	 * @return
	 */
	fun inverted(): GColor {
		return GColor(1f - red, 1f - green, 1f - blue, alpha)
	}

	fun getInterpolator(target: GColor): IInterpolator<GColor> {
		return IInterpolator { position: Float -> interpolateTo(target, position) }
	}

	companion object {

		val BLACK = GColor(0f, 0f, 0f, 1f, "BLACK")

		val WHITE = GColor(1f, 1f, 1f, 1f, "WHITE")

		val RED = GColor(1f, 0f, 0f, 1f, "RED")

		val BLUE = GColor(0f, 0f, 1f, 1f, "BLUE")

		val GREEN = GColor(0f, 1f, 0f, 1f, "GREEN")

		val CYAN = GColor(0f, 1f, 1f, 1f, "CYAN")

		val MAGENTA = GColor(1f, 0f, 1f, 1f, "MAGENTA")

		val YELLOW = GColor(1f, 1f, 0f, 1f, "YELLOW")
		val ORANGE = GColor(1f, 0.4f, 0f, 1f, "ORANGE")
		val GRAY = GColor(0.6f, 0.6f, 0.6f, 1f, "GRAY")

		val LIGHT_GRAY = GColor(0.8f, 0.8f, 0.8f, 1f, "LIGHT_GRAY")

		val DARK_GRAY = GColor(0.4f, 0.4f, 0.4f, 1f, "DARK_GRAY")
		val PINK = GColor(255, 175, 175, "PINK")

		val BROWN = GColor(165, 42, 42, "BROWN")
		val CHOCOLATE = GColor(210, 105, 30, "CHOCOLATE")

		val TRANSPARENT = GColor(0f, 0f, 0f, 0f, "TRANSPARENT")

		val TRANSLUSCENT_BLACK = GColor(0f, 0f, 0f, .5f, "TRANSLUCENT_BLACK")
		val DARK_OLIVE = GColor(-0x7f8000, "DARK_OLIVE")
		val LIGHT_OLIVE = GColor(-0x323300, "LIGHT_OLIVE")
		val GOLD = GColor(-0x2900, "GOLD")
		val SLIME_GREEN = GColor(-0x5e1dfd, "SLLIME_GREEN")
		val SKY_BLUE = GColor(-0x783115, "SKY_BLLUE")
		val TRUE_BLUE = GColor(-0xff8c31, "TRUE_BLUE")
		fun fromRGB(rgb: Int): GColor {
			return GColor(-0x1000000 or rgb)
		}

		fun fromARGB(argb: Int): GColor {
			return GColor(argb)
		}

		/**
		 * Parses string of pattern [(a,)?r,g,b] into a color object
		 * @param str
		 * @return null of string not identified as a color
		 */
		@JvmStatic
		@Throws(NumberFormatException::class)
		fun fromString(str: String): GColor {
			try {
				val parts: Array<String>
				parts = if (str.startsWith("ARGB[") && str.endsWith("]")) {
					str.substring(5, str.length - 1).split("[,]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				} else if (str.startsWith("[") && str.endsWith("]")) {
					str.substring(1, str.length - 1).split("[,]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				} else throw Exception("string '$str' not in form '[(a,)?r,g,b]'")
				if (parts.size == 3) {
					return GColor(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
				} else if (parts.size == 4) {
					return GColor(parts[1].toInt(), parts[2].toInt(), parts[3].toInt(), parts[0].toInt())
				}
				throw Exception("string '$str' not in form '[(a,)?r,g,b]'")
			} catch (e: NumberFormatException) {
				throw e
			} catch (e: Exception) {
				throw NumberFormatException(e.message)
			}
		}
	}
}
