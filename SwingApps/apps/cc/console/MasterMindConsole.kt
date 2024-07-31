package cc.console

import cc.lib.utils.FileUtils
import cc.lib.utils.asString
import cc.lib.utils.trimmedToSize
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.serialization.Serializable
import java.io.File
import java.text.SimpleDateFormat
import kotlin.random.Random

@Serializable
data class Score(
	val initials: String,
	val score: Int,
	val level: Int,
	val date: Long
)

@Serializable
data class SaveData(
	val scores: MutableList<Score> = mutableListOf(),
	var validChars: String = "12345",
	var numChars: Int = 4
) {

	val highScore: Int
		get() = scores.sortedBy { it.score }.firstOrNull()?.score ?: 1000

	val level: Int
		get() = numChars * validChars.length

	fun print(max: Int) {
		val format = SimpleDateFormat("MMM dd,yy")
		val sep = "+-----------------------+"
		println(sep)
		println("+     HIGH   SCORES     +")
		println(sep)
		scores.map {
			it.level to it
		}.groupBy {
			it.first
		}.toSortedMap(compareByDescending { it }).entries.forEach {
			println(String.format("+ Level             %3d +", it.key))
			println(sep)
			it.value.map { it.second }.sortedBy {
				it.score
			}.take(max).forEach {
				println(String.format("+ %-3s     %3d|%-8s +", it.initials, it.score, format.format(it.date)))
			}
			println(sep)
		}

	}
}


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
	var saveData = SaveData()
	val gson: Gson
	val random = Random(System.currentTimeMillis())

	init {
		settings = FileUtils.getOrCreateSettingsDirectory(MasterMindConsole::class.java)
		saveFile = File(settings, "game.save")
		gson = GsonBuilder().setPrettyPrinting().create()
		try {
			saveFile.bufferedReader().use {
				saveData = gson.fromJson(it, SaveData::class.java)
			}
		} catch (e: Exception) {
			saveFile.delete()
		}
	}

	fun go() {

		println("+------------+")
		println("| MASTERMIND |")
		println("+------------+")
		println()
		println("o = a character is correct in the correct position")
		println("x = a character is correct in the wrong position")
		println()
		println("Valid Chars: ${saveData.validChars}")
		print("Press enter to accept or provide: ")
		readlnOrNull()?.takeIf { it.isNotBlank() }?.let {
			saveData.validChars = it
		}
		println("Guess length: ${saveData.numChars}")
		print("Press enter to accept or provide: ")
		readlnOrNull()?.toIntOrNull()?.takeIf { it in 1..10 }?.let {
			saveData.numChars = it
		}
		do {
			saveData.print(10)
			println()
			run(saveData.validChars, saveData.numChars)
			println()
			println("Play again? y/n")
		} while (readlnOrNull()?.startsWith("y") == true)
		println("Thanks for playing!")
	}

	fun run(validChars: String, guessLen: Int) {
		val solutionStr = String(CharArray(guessLen) { validChars.random(random) })

		var guesses = 0
		var guess = ""
		println("Level : ${saveData.level}")
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
		if (guesses < saveData.highScore) {
			println("New high score!")
		}

		print("Enter your initials: ")
		val initials = readlnOrNull()?.takeIf { it.isNotBlank() }?.toUpperCase()?.trimmedToSize(3) ?: "AAA"
		saveData.scores.add(Score(initials, guesses, saveData.level, System.currentTimeMillis()))
		saveFile.bufferedWriter().use {
			gson.toJson(saveData, it)
		}
	}
}