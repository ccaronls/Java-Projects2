package cc.console

/**
 * Created by Chris Caron on 6/4/24.
 */
class MasterMindConsole {

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			println("Valid Chars: 12345")
			print("Press enter to accept or provide: ")
			val chars = readlnOrNull()?.takeIf { it.isNotBlank() } ?: "12345"
			println("Guess length: 4")
			print("Press enter to accept or provide: ")
			val len = readlnOrNull()?.toIntOrNull()?.takeIf { it in 1..10 } ?: 4
			run(chars, len)
		}

		fun run(validChars: String, guessLen: Int) {
			val solutionStr = String(CharArray(guessLen) { validChars.random() })

			var guesses = 0
			var guess = ""
			println("Guess my number:")
			start@ do {
				val solution = solutionStr.toCharArray()
				guesses++
				print("$guesses: ")
				guess = readlnOrNull() ?: ""
				if (guess.length != guessLen) {
					println("Guess must be $guessLen characters")
					continue@start
				}

				val record = IntArray(guessLen) { 0 }
				for (i in 0 until guessLen) {
					if (guess[i] == solution[i]) {
						record[i] = 1
						solution[i] = Char(0)
					} else if (guess[i] !in validChars) {
						println("Valid chars are '$validChars'")
						continue@start
					}
				}

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
					String(CharArray(record.count { it == 1 }) { 'o' }) + String(CharArray(record.count { it == -1 }) { 'x' })
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
		}
	}
}