package cc.lib.checkerboard

/**
 * Also known as Russian Checkers
 */
class Shashki : Checkers() {
	override fun canJumpSelf(): Boolean {
		return false
	}

	override fun canMenJumpBackwards(): Boolean {
		return true
	}

	override val isJumpsMandatory: Boolean
		get() = true
	override val isMaxJumpsMandatory: Boolean
		get() = true
	override val isCaptureAtEndEnabled: Boolean
		get() = false
	override val isFlyingKings: Boolean
		get() = true
	override val isKingPieces: Boolean
		get() = true
}