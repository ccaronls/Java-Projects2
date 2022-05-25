package cc.lib.checkerboard

import cc.lib.utils.GException
import cc.lib.utils.Reflector

class Piece : Reflector<Piece> {
	companion object {
		init {
			addAllFields(Piece::class.java)
		}
	}

	private var type: PieceType

	@JvmField
    @Omit
	var numMoves = 0
	var isCaptured = false
	val position: Int
	var value = 0

	// pos 0 is closest to the top.
	private var stack = 0
	var stackSize = 0
		private set

	fun clear() {
		playerNum = -1
		type = PieceType.EMPTY
		stackSize = 0
		stack = 0
		isCaptured = false
		numMoves = 0
	}

	fun copyFrom(from: Piece) {
		setType(from.getType())
		stack = from.stack
		stackSize = from.stackSize
		isCaptured = from.isCaptured
		value = from.value
	}

	override fun equals(obj: Any?): Boolean {
		if (obj == null) return false
		if (obj === this) return true
		val p = obj as Piece
		return stack == p.stack && stackSize == p.stackSize && position == p.position && type === p.type
	}

	constructor() {
		stack = 0
		stackSize = 0
		type = PieceType.EMPTY
		position = -1
	}

	constructor(playerNum: Int, type: PieceType) : this(-1, -1, playerNum, type) {}
	constructor(rank: Int, col: Int, playerNum: Int=Game.NOP, type: PieceType=PieceType.EMPTY) {
		if (playerNum > 1) throw GException("Invaid player num")
		this.type = type
		position = rank shl 8 or col
		this.playerNum = playerNum
	}

	constructor(pos: Int, playerNum: Int, type: PieceType) : this(pos shl 8, pos and 0xff, playerNum, type) {}

	//return 0 == (stack & (1 << (numStacks-1))) ? 0 : 1;
	var playerNum: Int
		get() = if (stackSize == 0) -1 else stack and 1
		//return 0 == (stack & (1 << (numStacks-1))) ? 0 : 1;
		set(playerNum) {
			if (playerNum > 1) throw GException("Invalid player num")
			if (playerNum < 0) {
				stackSize = 0
				stack = 0
			} else if (stackSize <= 1) {
				stack = playerNum
				stackSize = 1
			} else {
				stack = stack and (1 shl stackSize - 1).inv()
				if (playerNum > 0) stack = stack or (1 shl stackSize - 1)
			}
		}

	fun getType(): PieceType {
		return type
	}

	fun setType(type: PieceType) {
		if (type === PieceType.EMPTY) throw GException("cannot set type to empty")
		this.type = type
	}

	val rank: Int
		get() = position shr 8
	val col: Int
		get() = position and 0xff
	val isStacked: Boolean
		get() = stackSize > 1

	fun addStackTop(n: Int) {
		stack = stack shl 1 or n
		stackSize++
	}

	fun addStackBottom(n: Int) {
		if (n < 0 || n > 1) throw GException("Invalid value for n: $n")
		if (stackSize >= 32) throw GException("Stack overflow")
		if (n > 0) stack = stack or (1 shl stackSize)
		stackSize++
	}

	fun removeStackTop(): Int {
		if (stackSize <= 0) throw GException("Empty stack")
		val n = stack and 0x1
		stack = stack shr 1
		stackSize--
		return n
	}

	fun removeStackBottom(): Int {
		if (stackSize <= 0) throw GException("Empty stack")
		val n = stack and (1 shl --stackSize)
		return if (n == 0) 0 else 1
	}

	// 0 is the top of the stack (closest to top piece)
	fun getStackAt(index: Int): Int {
		if (index < 0 || index >= stackSize) throw GException("Index ofut of bounds: Value '" + index + "' out of range of [0-" + stackSize + ")")
		return if (0 == stack and (1 shl index)) 0 else 1
	}

	fun setChecked(checked: Boolean) {
		when (type) {
			PieceType.CHECKED_KING -> if (!checked) type = PieceType.UNCHECKED_KING
			PieceType.CHECKED_KING_IDLE -> if (!checked) type = PieceType.UNCHECKED_KING_IDLE
			PieceType.UNCHECKED_KING -> if (checked) type = PieceType.CHECKED_KING
			PieceType.UNCHECKED_KING_IDLE -> if (checked) type = PieceType.CHECKED_KING_IDLE
			else                          -> throw GException("Unhandled case: $type")
		}
	}

	override fun toString(): String {
		return "Piece{" +
			"PNUM=" + playerNum +
			", " + type +  //                ", numMoves=" + numMoves +
			", pos= [" + rank +
			", " + col +
			"] captured=" + isCaptured +
			", value=" + value +
			(if (stackSize > 1) ", stacks=" + stackSize else "") +  //                ", stack=" + stack +
			'}'
	}
}