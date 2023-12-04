package cc.lib.probot

import cc.lib.reflector.Reflector

class Level : Reflector<Level>() {
	companion object {
		init {
			addAllFields(Level::class.java)
		}
	}

	@JvmField
    var label = "<UNNAMED>"
	@JvmField
    var info = "<EMPTY>"
	@JvmField
    var coins = arrayOf(arrayOf(Type.EM))
	@JvmField
    var lazers = arrayOf(true, true, true)
	@JvmField
    var numJumps = 0
	@JvmField
    var numTurns = -1
	@JvmField
    var numLoops = -1
}