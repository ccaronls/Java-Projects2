package cc.lib.game

enum class Justify {
	LEFT,
	RIGHT,
	TOP,
	BOTTOM,
	CENTER
	;

	companion object {
		open fun verticalEntries(): Array<Justify> = arrayOf(TOP, BOTTOM, CENTER)
		open fun horizontalEntries(): Array<Justify> = arrayOf(LEFT, RIGHT, CENTER)
	}
}
