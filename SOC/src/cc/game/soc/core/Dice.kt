package cc.game.soc.core

import cc.lib.game.Utils
import cc.lib.reflector.Reflector

class Dice : Reflector<Dice> {
	companion object {
		init {
			addAllFields(Dice::class.java)
		}
	}

	var num = 0
		private set
	var type: DiceType = DiceType.WhiteBlack
	var isUserSet = false
		private set

	constructor() {}
	constructor(type: DiceType) : this(0, type) {}
	constructor(num: Int, type: DiceType) {
		this.num = num
		this.type = type
	}

	fun setNum(num: Int, userSet: Boolean) {
		this.num = num
		isUserSet = userSet
	}

	fun roll() {
		num = Utils.rand() % 6 + 1
	}
}