package cc.lib.yahtzee

import kotlin.math.max

enum class YahtzeeSlot(val niceName: String, val isUpper: Boolean) {
	ONES("Ones", true),
	TWOS("Twos", true),
	THREES("Threes", true),
	FOURS("Fours", true),
	FIVES("Fives", true),
	SIXES("Sixes", true),
	ONE_PAIR("One Pair", false),
	TWO_PAIR("Two Pair", false),
	THREE_OF_A_KIND("Three of a Kind", false),
	FOUR_OF_A_KIND("Four of a Kind", false),
	SMALL_STRAIGHT("Small Straight", false),
	LARGE_STRAIGHT("Large Straight", false),
	FULL_HOUSE("Full House", false),
	YAHZEE("Yahtzee", false),
	CHANCE("Chance", false);

	fun getScore(yahtzee: Yahtzee): Int {
		return getScore(yahtzee.rules, *yahtzee.diceRoll)
	}

	fun getScore(rules: YahtzeeRules, vararg roll: Int): Int {
		var score = 0
		when (this) {
			ONES, TWOS, THREES, FOURS, FIVES, SIXES -> for (i in roll) {
				if (i == ordinal + 1) {
					score += ordinal + 1
				}
			}
			ONE_PAIR                                -> {
				val counts = occurances(rules, *roll)
				var i = rules.numDiceSides
				while (i > 0) {
					if (counts[i] >= 2) {
						score = 2 * i
						break
					}
					i--
				}
			}
			TWO_PAIR -> {
				var high = 0
				var low = 0
				val counts = occurances(rules, *roll)
				var i = rules.numDiceSides
				while (i > 0) {
					if (counts[i] >= 2) {
						if (high == 0) {
							high = 2 * i
						} else {
							low = 2 * i
							break
						}
					}
					i--
				}
				if (high > 0 && low > 0) {
					score = high + low
				}
			}
			THREE_OF_A_KIND -> {
				val counts = occurances(rules, *roll)
				var i = rules.numDiceSides
				while (i > 0) {
					if (counts[i] >= 3) {
						score = sum(*roll)
						break
					}
					i--
				}
			}
			FOUR_OF_A_KIND -> {
				val counts = occurances(rules, *roll)
				var i = rules.numDiceSides
				while (i > 0) {
					if (counts[i] >= 4) {
						score = sum(*roll)
						break
					}
					i--
				}
			}
			SMALL_STRAIGHT                          -> {
				if (getStraightLength(rules, roll) >= 4) score = 30
			}
			LARGE_STRAIGHT                          -> {
				if (getStraightLength(rules, roll) >= 5) score = 40
			}
			FULL_HOUSE -> {
				val counts = occurances(rules, *roll)
				var have2 = false
				var have3 = false
				var i = 0
				while (i < counts.size) {
					if (counts[i] == 2) have2 = true
					if (counts[i] == 3) have3 = true
					i++
				}
				if (have2 && have3) score = 25
			}
			YAHZEE -> {
				val counts = occurances(rules, *roll)
				var i = 1
				while (i < counts.size) {
					if (counts[i] >= 5) {
						score = 50
						break
					}
					i++
				}
			}
			CHANCE -> score = sum(*roll)
		}
		return score
	}

	companion object {
		fun getStraightLength(rules: YahtzeeRules, dice: IntArray): Int {
			val counts = occurances(rules, *dice)
			var max = 0
			var len = 0
			for (i in 1 until counts.size) {
				if (counts[i] > 0) len++ else len = 0
				max = max(max, len)
			}
			return max
		}

		fun occurances(rules: YahtzeeRules, vararg dice: Int): IntArray {
			val count = IntArray(rules.numDiceSides + 1)
			for (i in dice) {
				count[i]++
			}
			return count
		}

		fun sum(vararg dice: Int): Int {
			var sum = 0
			for (i in dice) {
				sum += i
			}
			return sum
		}
	}
}