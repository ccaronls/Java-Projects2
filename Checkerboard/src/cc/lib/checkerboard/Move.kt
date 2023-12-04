package cc.lib.checkerboard

import cc.lib.game.IMove
import cc.lib.reflector.Omit
import cc.lib.reflector.Reflector
import cc.lib.utils.GException

class Move @JvmOverloads constructor(val moveType: MoveType = MoveType.END, private val playerNum: Int = -1) : Reflector<Move>(), IMove, Comparable<Move> {
	companion object {
		fun toStr(pos: Int): String {
			val rnk = pos shr 8
			val col = pos and 0xff
			return String.format("%d,%d", rnk, col)
		}

		init {
			addAllFields(Move::class.java)
		}
	}

	// consider optimization here for better memory usage (Piece objects instead of arrays)
	var start = -1
		private set
	var end = -1
		private set
	var castleRookStart = -1
		private set
	var castleRookEnd = -1
		private set
	var opponentKingPos = -1
		private set
	var opponentKingTypeStart: PieceType? = null
		private set
	var opponentKingTypeEnd: PieceType? = null
		private set
	var startType: PieceType? = null
		private set
	var endType: PieceType? = null
		private set
	var enpassant = -1
	var capturedPosition = -1
		private set
	var capturedType: PieceType? = null
		private set
	var jumped = -1
		private set

	@Omit
	var parent: Move? = null

	@Omit
	var bestValue: Long = 0

	@Omit
	var path: Move? = null

	@Omit
	var maximize = 0

	@Omit
	var children: MutableList<Move>? = null

	@Omit
	private var compareValue = 0

	@Omit
	var jumpDepth = 0
	override fun compareTo(o: Move): Int {
		return o.compareValue - compareValue
	}

	fun getXmlStartTag(parent: Move?): String {
		if (parent == null) {
			return "<root value=\"$bestValue\" turn=\"$playerNum\">"
		}
		val isPath = parent.path === this
		if (maximize == 0) return "<leaf" + (if (isPath) " path=\"true\"" else "") + " value=\"" + bestValue + "\">" else if (maximize < 0) return "<min" + (if (isPath) " path=\"true\"" else "") + " value=\"" + bestValue + "\">"
		return "<max" + (if (isPath) " path=\"true\"" else "") + " value=\"" + bestValue + "\">"
	}

	fun getXmlEndTag(parent: Move?): String {
		if (parent == null) {
			return "</root>"
		}
		if (maximize == 0) return "</leaf>" else if (maximize < 0) return "</min>"
		return "</max>"
	}

	fun setStart(startRank: Int, startCol: Int, type: PieceType): Move {
		if (type === PieceType.EMPTY) throw GException("start type cannot be empty")
		start = startRank shl 8 or startCol
		startType = type
		compareValue += type.value
		return this
	}

	fun setEnd(endRank: Int, endCol: Int, type: PieceType?): Move {
		end = endRank shl 8 or endCol
		if (type == null) throw GException("type cannot be null")
		endType = type
		return this
	}

	fun setJumped(startRank: Int, startCol: Int): Move {
		jumped = startRank shl 8 or startCol
		return this
	}

	fun setCaptured(capturedRank: Int, capturedCol: Int, type: PieceType): Move {
		assert(0 == type.flag and PieceType.FLAG_KING)
		capturedPosition = capturedRank shl 8 or capturedCol
		capturedType = type
		compareValue += 100 + type.value
		return this
	}

	fun setCastle(castleRookStartRank: Int, castRookStartCol: Int, castleRookEndRank: Int, castleRookEndCol: Int): Move {
		castleRookStart = castleRookStartRank shl 8 or castRookStartCol
		castleRookEnd = castleRookEndRank shl 8 or castleRookEndCol
		return this
	}

	fun hasEnd(): Boolean {
		return end >= 0 && moveType !== MoveType.STACK
	}

	fun hasCaptured(): Boolean {
		return capturedPosition >= 0
	}

	override fun toString(): String {
		val str = StringBuffer(64)
		str.append(playerNum).append(":").append(moveType)
		if (start >= 0) {
			str.append(" ").append(startType).append(if (hasEnd()) " from:" else " at:").append(toStr(start))
		}
		if (end >= 0) {
			str.append(" to:").append(toStr(end))
			if (endType !== startType) str.append(" becomes:").append(endType)
		}
		if (hasCaptured()) {
			str.append(" cap:").append(toStr(capturedPosition)).append(" ").append(capturedType)
		}
		if (castleRookStart >= 0) {
			str.append(" castle st: ").append(toStr(castleRookStart)).append(" end: ").append(toStr(castleRookEnd))
		}
		if (opponentKingPos >= 0) { // && opponentKing[2] != opponentKing[3]) {
			str.append(" oppKing: ").append(toStr(opponentKingPos))
		}
		if (enpassant >= 0) {
			str.append(" enpassant:").append(toStr(enpassant))
		}
		return str.toString()
	}

	override fun getPlayerNum(): Int {
		return playerNum
	}

	fun setOpponentKingType(rank: Int, col: Int, opponentKingTypeStart: PieceType?, opponentKingTypeEnd: PieceType) {
		opponentKingPos = rank shl 8 or col
		this.opponentKingTypeStart = opponentKingTypeStart
		this.opponentKingTypeEnd = opponentKingTypeEnd
		compareValue += opponentKingTypeEnd.value
	}

	fun hasOpponentKing(): Boolean {
		return opponentKingPos >= 0
	}

	override fun equals(obj: Any?): Boolean {
		if (obj == null) return false
		if (obj === this) return true
		val mv = obj as Move
		return playerNum == mv.playerNum && moveType === mv.moveType && start == mv.start && end == mv.end && hasCaptured() == mv.hasCaptured() && hasOpponentKing() == mv.hasOpponentKing()
	}

	override fun getCompareValue(): Int {
		return compareValue
	}

	val pathString: String
		get() {
			val buf = StringBuffer()
			getPathStringR(buf, arrayOf(""))
			return buf.toString()
		}

	private fun getPathStringR(buf: StringBuffer, indent: Array<String>) {
		if (parent != null) parent!!.getPathStringR(buf, indent)
		buf.append("\n").append(indent[0]).append(toString())
		indent[0] += "  "
	}

	init { //}, Piece captured, PieceType nextType, int ... positions) {
		compareValue = moveType?.value ?: 0
	}
}