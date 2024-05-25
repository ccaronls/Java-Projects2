package cc.lib.reflectortest

import cc.lib.ksp.reflector.Alternates
import cc.lib.ksp.reflector.Omit
import cc.lib.ksp.reflector.Reflect

@Reflect
class Ref0 : Ref0Reflector() {
	override var a = 0
	override var b = "zero"
}

@Reflect
class Ref1 : Ref1Reflector() {

	override var xi = 0
	override var xf = 0f
	override var xd = 0.0
	override var xs = "hello"
	override var xb = true
	override var xc = listOf(0, 1, 2)
	override var xl = 0L

	@Alternates("xnalt1")
	override var xin: Int? = null

	@Alternates("ynalt1", "ynalt2")
	override var xfn: Float? = null
	override var xsn: String? = null
	override var xcn: List<Int>? = null

	override var xon: Ref0? = null

	@Omit
	var xo = 1
}