package cc.lib.checkerboard

import cc.lib.game.Utils

class AIStats internal constructor() {
	val startTime: Long
    var prunes: Long = 0
    var evalCount: Long = 0
    var evalTimeTotalMSecs: Long = 0
    var pieceTypeCount = LongArray(PieceType.values().size) { 0 }
    var pieceTypeValue = DoubleArray(PieceType.values().size) { 0.0 }
	override fun toString(): String {
		var s = """
	    	Run Time: ${Utils.formatTime(System.currentTimeMillis() - startTime)}
	    	prunes=$prunes
	    	Pruned %${100f * prunes / evalCount}
	    	evalCount=$evalCount
	    	evalTimeTotalMSecs=$evalTimeTotalMSecs
	    	""".trimIndent()
		for (i in pieceTypeValue.indices) {
			if (pieceTypeCount[i] > 0) s += String.format("\n%-20s AVG: %5.3f", PieceType.values()[i], pieceTypeValue[i] / pieceTypeCount[i])
		}
		return s
	}

	init {
		startTime = System.currentTimeMillis()
	}
}