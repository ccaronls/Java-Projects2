package cc.lib.kreflector

import cc.lib.math.Vector2D
import cc.lib.utils.SomeEnum

class SmallReflector : Reflector<SmallReflector>() {
	var a = "hello"
	var b = "goodbye"
	var empty = ""
	var e = SomeEnum.ENUM1
	var vec = Vector2D(10, 20)

	companion object {
		init {
			addAllFields(SmallReflector::class.java)
		}
	}
}