package cc.game.monopoly

import cc.lib.game.Utils
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import java.io.File

class MonopolyTest : TestCase() {
	@Throws(Exception::class)
	override fun setUp() {
		super.setUp()
		Utils.setDebugEnabled()
		Utils.setRandomSeed(0)
	}

	fun testGame() {
		runBlocking {
			val monopoly = Monopoly()
			monopoly.newGame()
			monopoly.rules.startMoney = 500
			monopoly.addPlayer(Player())
			monopoly.addPlayer(Player())
			monopoly.addPlayer(Player())

			var i = 0
			while (i < 2000) {
				try {
					monopoly.runGame()
				} catch (e: Throwable) {
					monopoly.trySaveToFile(File("monopoly_crash.txt"))
					throw e
				}
				if (monopoly.isGameOver()) break
				i++
			}

			println("Stopped at " + i + " iterations")

			println("Players=" + monopoly.playersCopy)

			assertTrue(monopoly.isGameOver())
		}
	}

	fun testLotsOfGames() {
		var numSuccsess = 0
		try {
			for (i in 0..999) {
				testGame()
				numSuccsess++
			}
		} catch (t: Throwable) {
			throw t
		} finally {
			println("Num successfull games=" + numSuccsess)
		}
	}

	companion object {

		val CRASH_FILE = File("monopoly_crash.txt")

		@JvmStatic
		fun main(args: Array<String>) {
			runBlocking {
				val monopoly = Monopoly()
				monopoly.loadFromFile(File("Monopoly/monopoly_crash.txt"))
				while (!monopoly.isGameOver()) {
					monopoly.runGame()
				}
			}
		}
	}
}
