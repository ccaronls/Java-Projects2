package cc.console

import cc.lib.utils.FileUtils
import cc.lib.utils.asString
import cc.lib.utils.trimmedToSize
import java.io.File

/**
 * Created by Chris Caron on 6/4/24.
 */
class MasterMindConsole {

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			MasterMindConsole().go()

		}
	}

	val settings: File
	val saveFile: File
	val scores = mutableListOf<Pair<String, Int>>()
	var highScore = 0

	init {
		settings = FileUtils.getOrCreateSettingsDirectory(MasterMindConsole::class.java)
		saveFile = File(settings, "hs.properties")
		try {
			saveFile.bufferedReader().lines().forEach {
				it.split("=").takeIf { it.size == 2 && it[1].toIntOrNull() != null }?.let {
					scores.add(it.first() to it[1].toInt())
				}
			}
		} catch (e: Exception) {
			saveFile.delete()
		}
		highScore = scores.sortedBy { it.second }.firstOrNull()?.second ?: 1000
	}

	fun top10() {
		println("+-------------+")
		println("+ HIGH SCORES +")
		println("+             +")
		scores.sortedBy {
			it.second
		}.take(10).forEach { (nm, score) ->
			println(String.format("+ %-3s     %3d +", nm, score))
		}
		println("+-------------+")
	}

	fun go() {

		println("+------------+")
		println("| MASTERMIND |")
		println("+------------+")
		println()
		println("o = a character is correct in the correct position")
		println("x = a character is correct in the wrong position")
		println()
		println("Valid Chars: 12345")
		print("Press enter to accept or provide: ")
		val chars = readlnOrNull()?.takeIf { it.isNotBlank() } ?: "12345"
		println("Guess length: 4")
		print("Press enter to accept or provide: ")
		val len = readlnOrNull()?.toIntOrNull()?.takeIf { it in 1..10 } ?: 4
		do {
			top10()
			println()
			run(chars, len)
			println()
			println("Play again? y/n")
		} while (readlnOrNull()?.startsWith("y") == true)
		println("Thanks for playing!")
	}

	fun run(validChars: String, guessLen: Int) {
		val solutionStr = String(CharArray(guessLen) { validChars.random() })

		var guesses = 0
		var guess = ""
		println("Guess my number:")
		start@ do {
			val solution = solutionStr.toCharArray()
			print("${guesses + 1}: ")
			guess = readlnOrNull() ?: ""

			val record = IntArray(guessLen) { 0 }
			if (guess.length != guessLen) {
				println("Guess must be $guessLen characters and consist of only $validChars")
				continue@start
			}
			for (i in 0 until guessLen) {
				if (guess[i] == solution[i]) {
					record[i] = 1
					solution[i] = Char(0)
				} else if (guess[i] !in validChars) {
					println("Guess must be $guessLen characters and consist of only $validChars")
					continue@start
				}
			}
			guesses++

			for (i in 0 until guessLen) {
				if (record[i] == 0) {
					val idx = solution.indexOf(guess[i])
					if (idx >= 0) {
						record[i] = -1
						solution[idx] = Char(0)
					}
				}
			}

			var feedback: String =
				CharArray(record.count { it == 1 }) {
					'o'
				}.asString() + CharArray(record.count { it == -1 }) {
					'x'
				}.asString()
			if (false)
				for (i in 0 until guessLen) {
					feedback += when (record[i]) {
						1 -> guess[i]
						-1 -> 'x'
						else -> '.'
					}
				}

			println(feedback)

		} while (guess != solutionStr)

		println("You guessed in $guesses tries")
		if (guesses < highScore) {
			highScore = guesses
			println("New high score!")
		}

		print("Enter your initials: ")
		val initials = readlnOrNull()?.toUpperCase()?.trimmedToSize(3) ?: "AAA"
		scores.add(initials to guesses)
		saveFile.bufferedWriter().use { writer ->
			scores.forEach { (nm, score) ->
				writer.write("$nm=$score")
				writer.newLine()
			}
		}
	}
}