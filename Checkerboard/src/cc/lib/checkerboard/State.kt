package cc.lib.checkerboard

import cc.lib.reflector.Reflector

class State @JvmOverloads constructor(val index: Int = -1, val moves: List<Move> = listOf()) : Reflector<State>() {
	companion object {
		init {
			addAllFields(State::class.java)
		}
	}

	override fun toString(): String {
		return "State{$move}"
	}

	val move: Move
		get() = moves[index]

}