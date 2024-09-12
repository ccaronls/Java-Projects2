package cc.lib.game

import junit.framework.TestCase

/**
 * Created by Chris Caron on 9/11/24.
 */
internal class DiceTest : TestCase() {

	fun test1() {

		for (d in 0..100)
			println(Dice(numPips = d))

		println(Dice.toString(3, Dice(5), Dice(12), Dice(3), Dice(9)))

	}


}
