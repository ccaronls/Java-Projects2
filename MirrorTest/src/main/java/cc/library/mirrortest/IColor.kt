package cc.library.mirrortest

import cc.lib.mirror.annotation.DirtyType
import cc.lib.mirror.annotation.Mirror
import cc.lib.mirror.context.Mirrored

/**
 * Created by Chris Caron on 11/16/23.
 */
@Mirror("cc.library.mirrortest", DirtyType.NEVER)
interface IColor : Mirrored {
	var a: Byte
	var r: Byte
	var g: Byte
	var b: Byte
}

class Color(val x: String, red: Byte, grn: Byte, blu: Byte, alpha: Byte = 127) : ColorImpl() {
	init {
		r = red
		g = grn
		b = blu
		a = alpha
	}

	override fun toString(buffer: StringBuffer, indent: String) {
		buffer.append(indent).append(x).append("\n")
	}
}

val RED = Color("RED", 127, 0, 0)
val GREEN = Color("GREEN", 0, 127, 0)
val BLUE = Color("BLUE", 0, 0, 127)