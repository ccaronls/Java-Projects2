package cc.console

import cc.lib.game.Utils
import cc.lib.utils.getOrCreateSettingsDirectory
import cc.lib.yahtzee.Yahtzee
import cc.lib.yahtzee.YahtzeeRules
import cc.lib.yahtzee.YahtzeeSlot
import java.io.Console
import java.io.File
import java.util.Arrays
import java.util.Locale

class YahtzeeConsole internal constructor(var console: Console) : Yahtzee() {
	private var isRunning = true
	private fun checkQuit(input: String?) {
		var input = input
		input = input!!.trim { it <= ' ' }
		if (input.length > 0) {
			if (input.lowercase(Locale.getDefault())[0] == 'q') {
				isRunning = false
				System.exit(0)
			}
		}
	}

	private fun draw() {
		// Draw a divider
		console.printf("\n\n\n-----------------------------------------------------------------\n")

		// draw roll count
		console.printf("Roll " + rollCount + " of " + rules.numRollsPerRound)
		// draw the roll
		val dice = diceRoll
		val keepers = getKeepers()
		drawDice(*dice)
		console.printf("\n")
		// draw the keepers
		for (keep in keepers) {
			print(String.format("%-" + DICE_SPACING + "s", if (keep) " KEEP" else ""))
		}
		console.printf("\n")
		// draw the slots
		var index = 1
		for (slot in allSlots) {
			console.printf(String.format("%-2d %-20s %6s : %d", index++, slot.name, if (isSlotUsed(slot)) "CLOSED" else "", if (isSlotUsed(slot)) getSlotScore(slot) else slot.getScore(rules, *dice)))
		}
		console.printf("\n")
		// draw the score
		console.printf(String.format(
			"""
	        	Yahtzees     %-5d
	        	Upper Points %-5d
	        	Bonus Points %-5d
	        	Total        %-5d
	        	Top Score    %-5d
	        	""".trimIndent(), numYahtzees, upperPoints, bonusPoints, totalPoints, topScore))
	}

	public override fun onChooseKeepers(keeprs: BooleanArray): Boolean {
		print("\n\nChoose die nums to toggle seperated by a space or enter to continue\n> ")
		try {
			val line = console.readLine()
			if (line == null) {
				isRunning = false
				return false
			}
			if (line.length == 0) {
				return true
			}
			checkQuit(line)
			val parts = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			for (i in parts.indices) {
				if (parts[i].lowercase(Locale.getDefault())[0] == 'a') {
					Arrays.fill(keeprs, true)
					return true
				}
				val num = parts[i].toInt()
				if (num > 0 && num <= keeprs.size) {
					keeprs[num - 1] = !keeprs[num - 1]
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return false
	}

	override fun onGameOver() {
		try {
			print("G A M E    O V E R\nPress enter to start a new game or q to exit\n> ")
			val line = console.readLine()
			if (line == null) {
				System.exit(1)
			}
			checkQuit(line)
			reset()
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun onChooseSlotAssignment(choices: List<YahtzeeSlot>): YahtzeeSlot? {
		print("\n\nChoose slot num to assign\n> ")
		try {
			val line = console.readLine()
			if (line == null) {
				isRunning = false
				return null
			}
			if (line.length == 0) return null
			checkQuit(line)
			val num = line.trim { it <= ' ' }.toInt()
			return allSlots[num - 1]
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}

	fun drawDice(vararg roll: Int) {
		val diceSpacing = String.format("%" + (DICE_SPACING - diceEdge.length) + "s", " ")
		for (i in roll.indices) {
			print(String.format("%-" + DICE_SPACING + "s", "  [" + (i + 1) + "]"))
		}
		console.printf("\n")
		for (i in roll.indices) {
			print(diceEdge + diceSpacing)
		}
		console.printf("\n")
		for (ii in 0..2) {
			for (i in roll.indices) {
				print(diceMiddle[roll[i]][ii] + diceSpacing)
			}
			console.printf("\n")
		}
		for (i in roll.indices) {
			print(diceEdge + diceSpacing)
		}
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			val restoreFile = File(YahtzeeConsole::class.java.getOrCreateSettingsDirectory(), "yahtzee.sav")
			try {
				val console = System.console()
				val yc = YahtzeeConsole(console)
				do {
					console.printf("""
	N>  New game
	R>  Restore Game
	A>  New Alternate Game
	
	>
	""".trimIndent())
					val input = console.readLine()
					if (input.length == 0) {
						continue
					}
					when (input.lowercase(Locale.getDefault())[0]) {
						'n' -> {}
						'r' -> if (restoreFile.exists()) {
							try {
								yc.loadFromFile(restoreFile)
							} catch (e: Exception) {
								e.printStackTrace()
								continue
							}
						} else {
							System.err.println("Restore file $restoreFile not found")
							continue
						}

						'a' -> {
							val rules = YahtzeeRules()
							rules.isEnableAlternateVersion = true
							yc.reset(rules)
						}

						else -> {
							System.err.println("Invalid entry\n\n")
							continue
						}
					}
				} while (false)
				while (yc.isRunning) {
					yc.draw()
					yc.runGame()
					yc.saveToFile(restoreFile)
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}

		const val DICE_SPACING = 10
		const val diceEdge = "+-----+"
		val diceMiddle = arrayOf(arrayOf("|     |",
			"|     |",
			"|     |"), arrayOf("|     |",
			"|  o  |",
			"|     |"), arrayOf("|o    |",
			"|     |",
			"|    o|"), arrayOf("|o    |",
			"|  o  |",
			"|    o|"), arrayOf("|o   o|",
			"|     |",
			"|o   o|"), arrayOf("|o   o|",
			"|  o  |",
			"|o   o|"), arrayOf("|o   o|",
			"|o   o|",
			"|o   o|"), arrayOf("|o   o|",
			"|o o o|",
			"|o   o|"), arrayOf("|o o o|",
			"|o   o|",
			"|o o o|"), arrayOf("|o o o|",
			"|o o o|",
			"|o o o|"))
	}
}
