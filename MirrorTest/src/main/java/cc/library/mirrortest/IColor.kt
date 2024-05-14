package cc.library.mirrortest

import cc.lib.ksp.mirror.DirtyType
import cc.lib.ksp.mirror.Mirror
import cc.lib.ksp.mirror.Mirrored
import java.util.Objects

/**
 * Created by Chris Caron on 11/16/23.
 */
@Mirror(DirtyType.NEVER)
interface IColor : Mirrored {
	var nm: String
	var a: Byte
	var r: Byte
	var g: Byte
	var b: Byte
}

class Color(name: String = "", red: Byte = -127, grn: Byte = -127, blu: Byte = -127, alpha: Byte = 127) : ColorImpl() {
	init {
		nm = name
		r = red
		g = grn
		b = blu
		a = alpha
	}

	override fun toString(buffer: StringBuffer, indent: String) {
		buffer.append(indent).append(nm).append("\n")
	}

	override fun hashCode(): Int {
		return Objects.hash(r, g, b, a)
	}

	override fun equals(other: Any?): Boolean {
		return contentEquals(other)
	}
}

val RED = Color("RED", 127, 0, 0)
val GREEN = Color("GREEN", 0, 127, 0)
val BLUE = Color("BLUE", 0, 0, 127)
val BLACK = Color("BLACK", 0, 0, 0)
val WHITE = Color("WHITE", 127, 127, 127)