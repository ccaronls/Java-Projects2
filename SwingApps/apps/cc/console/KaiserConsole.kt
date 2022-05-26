package cc.console

import cc.game.kaiser.ai.PlayerBot
import cc.game.kaiser.core.*
import cc.lib.game.Utils
import java.io.BufferedReader
import java.io.InputStreamReader

class KaiserConsole internal constructor() {
	fun chooseGameType() {
		var option: String? = null
		var name: String? = null
		while (true) {
			while (name == null || name.length <= 0) {
				printf("Enter your name >")
				name = readLine()!!.trim { it <= ' ' }
			}
			do {
				printf("\n\n")
				printf("Welcome %s\n", name)
				printf("S>   Single Player\n")
				printf("H>   Host Multi Player\n")
				printf("J>   Join Multi Player\n")
				printf("Q>   Quit")
				option = readLine()!!.trim { it <= ' ' }.toUpperCase()
			} while (option!!.length <= 0)
			when (option[0]) {
				'S' -> singlePlayerGame(name)
				'Q' -> System.exit(0)
				else -> printf("Unknown command: %c", option[0])
			}
		}
	}

	fun singlePlayerGame(name: String?) {
		val kaiser = Kaiser()
		var nm = 0
		val names = arrayOf("Simon", "Beth", "Max", "Joan")
		val random = Utils.rand() % 4
		var i = 0
		while (i < random) {
			kaiser.setPlayer(i, PlayerBot(names[nm++]))
			i++
		}
		kaiser.setPlayer(i++, ConsolePlayer(name))
		while (i < 4) {
			kaiser.setPlayer(i, PlayerBot(names[nm++]))
			i++
		}
		while (true) {
			drawGame(kaiser)
			kaiser.runGame()
			if (kaiser.isGameOver) {
				kaiser.newGame()
			}
		}
	}

	var thisPlayer: ConsolePlayer? = null
	var reader: BufferedReader? = null
	fun readLine(): String? {
		try {
			if (reader == null) reader = BufferedReader(InputStreamReader(System.`in`))
			return reader!!.readLine()
		} catch (e: Exception) {
			e.printStackTrace()
			System.exit(1)
		}
		return null
	}

	fun printf(fmt: String?, vararg args: Any?) {
		print(String.format(fmt!!, *args))
	}

	fun drawCards(hands: Array<Hand>, numHands: Int, faceUp: Boolean) {
		val CARD_HEIGHT = 3
		var i: Int
		var ii: Int
		if (numHands <= 0) {
			printf("\n<NONE>\n\n")
			return
		}
		val cardsSpacing = "     "
		val MAX_HANDS_PER_ROW = 3

		//int handsShown = 0;
		var start = 0
		while (start < numHands) {
			var end = numHands
			if (end - start > MAX_HANDS_PER_ROW) {
				end = start + MAX_HANDS_PER_ROW
			}
			ii = start
			while (ii < end) {
				i = 0
				while (i < hands[ii].size - 1) {
					printf("+--")
					i++
				}
				printf("+------+%s", cardsSpacing)
				ii++
			}
			printf("\n")
			ii = start
			while (ii < end) {
				i = 0
				while (i < hands[ii].size - 1) {
					printf("|%s", if (faceUp) hands[ii].get(i).rank.rankString else "  ")
					i++
				}
				printf("|%s    |%s", if (faceUp) hands[ii].get(i).rank.rankString else "  ", cardsSpacing)
				ii++
			}
			printf("\n")
			ii = start
			while (ii < end) {
				i = 0
				while (i < hands[ii].size - 1) {
					printf("|%c ", if (faceUp) hands[ii].get(i).suit.suitChar else ' ')
					i++
				}
				printf("|%c     |%s", if (faceUp) hands[ii].get(i).suit.suitChar else ' ', cardsSpacing)
				ii++
			}
			printf("\n")
			for (iii in 0 until CARD_HEIGHT) {
				ii = 0
				while (ii < end - start) {
					i = 0
					while (i < hands[ii].size - 1) {
						printf("|  ")
						i++
					}
					printf("|      |%s", cardsSpacing)
					ii++
				}
				printf("\n")
			}
			ii = start
			while (ii < end) {
				i = 0
				while (i < hands[ii].size - 1) {
					printf("+--")
					i++
				}
				printf("+------+%s", cardsSpacing)
				ii++
			}
			printf("\n")
			start = end
		}
	}

	inner class ConsolePlayer(nm: String?) : Player(nm!!) {
		override fun playTrick(kaiser: Kaiser, cards: Array<Card>): Card? {
			var card: Card? = null
			drawHeader(kaiser)
			drawTrick(kaiser, 0) //getPlayerNum());
			drawCards(arrayOf(hand), 1, true)
			for (i in 0 until numCards) {
				val inList = isInArray(getCard(i), cards)
				printf("%c%d%c", if (inList) '[' else ' ', i + 1, if (inList) ']' else ' ')
			}
			printf("\n\n")
			while (true) {
				printf("Choose card to play [1-%d]:", numCards)
				val line = readLine()
				var num = 0
				num = try {
					line!!.trim { it <= ' ' }.toInt()
				} catch (e: Exception) {
					printf("\n\nInvalid entry.\n\n")
					continue
				}
				if (num < 1 || num > numCards) {
					printf("\n\nInvalid Entry.\n\n")
					continue
				}
				card = getCard(num - 1)
				if (isInArray(card, cards)) break
				printf("\n\n%s is not a valid card to play.\n\n", card.toPrettyString())
			}
			printf("\n\n")
			return card
		}

		override fun makeBid(kaiser: Kaiser, options: Array<Bid>): Bid? {
			drawHeader(kaiser)
			drawCards(arrayOf(hand), 1, true)
			for (i in options.indices) {
				printf("%-2d - %-20s ", i + 1, bidToString(options[i]))
				if ((i + 1) % 2 == 1) printf("\n")
			}
			var op = 0
			while (true) {
				printf("\nChoose bid option:\n")
				val line = readLine()
				op = try {
					line!!.trim { it <= ' ' }.toInt()
				} catch (e: Exception) {
					printf("\n\nInvalid Entry\n\n")
					continue
				}
				if (op < 0 || op > options.size) {
					printf("\n\nNot an option\n\n")
					continue
				}
				break
			}
			return if (op == 0) null else options[op - 1]
		}
	}

	fun drawHeader(kaiser: Kaiser) {
		printf(
			"""
---------------------------------------------------
Round: %d      Dealer: %s    Trump: %s

           TEAM A           TEAM B
Player 1   %-10s       %s
Player 2   %-10s       %s
Bid        %-10s       %s
Total      %-10d       %d
Round      %-10d       %d

""", kaiser.numRounds,
			kaiser.getPlayer(kaiser.dealer).name,
			kaiser.trump.suitString,
			kaiser.getPlayer(kaiser.getTeam(0).playerA).name,
			kaiser.getPlayer(kaiser.getTeam(1).playerA).name,
			kaiser.getPlayer(kaiser.getTeam(0).playerB).name,
			kaiser.getPlayer(kaiser.getTeam(1).playerB).name,
			kaiser.getTeam(0).bid,
			kaiser.getTeam(1).bid,
			kaiser.getTeam(0).totalPoints,
			kaiser.getTeam(1).totalPoints,
			kaiser.getTeam(0).totalPoints,
			kaiser.getTeam(1).roundPoints
		)
	}

	fun drawRoundResult(kaiser: Kaiser) {
		printf("\n\nResults of Round %d\n\n", kaiser.numRounds)
		for (i in 0..3) {
			val p = kaiser.getPlayer(i)
			printf("\n%s (Team %s)\n", p.name, kaiser.getTeam(p.team).name)
			val tricks: Array<Hand> = p.tricks.toTypedArray()
			drawCards(tricks, p.tricks.size, true)
		}
	}

	fun getch() {
		readLine()
	}

	fun drawGame(kaiser: Kaiser) {
		when (kaiser.state) {
			State.NEW_GAME -> {
				printf("Press any key to start a new game\n")
				getch()
			}
			State.NEW_ROUND -> {
			}
			State.DEAL -> {
			}
			State.BID -> {
			}
			State.TRICK -> {
			}
			State.PROCESS_TRICK -> drawTrick(kaiser, 0)
			State.RESET_TRICK -> {
				printf("\n\nPress any key to continue\n\n")
				getch()
			}
			State.PROCESS_ROUND -> {
				drawHeader(kaiser)
				drawRoundResult(kaiser)
			}
			State.GAME_OVER -> printf("\n\n  G A M E   O V E R    \n\n")
		}
	}

	fun getPlayerHeader(kaiser: Kaiser, p: Player): String {
		val tw = kaiser.trickWinner
		var s = p.name
		if (kaiser.dealer == p.playerNum || kaiser.startPlayer == p.playerNum || tw === p) {
			s += " ["
			if (kaiser.dealer == p.playerNum) s += "D"
			if (kaiser.startPlayer == p.playerNum) s += "S"
			if (p === tw) s += "W"
			s += "]"
		}
		return s
	}

	// heart diamond club spade
	var cardSymbols = arrayOf(arrayOf("  ^ ^ ", "  /\\  ", "   O  ", "   ^  "), arrayOf("  \\ / ", " /  \\ ", "  O O ", "  / \\ "), arrayOf("   v  ", " \\  / ", "   ^  ", "   ^  "), arrayOf("      ", "  \\/  ", "      ", "      "))
	fun drawTrick(kaiser: Kaiser, frontPlayer: Int) {
		// arrange the played cards as follows:
		/*
                     Name(fp+2)
                    +----+
                    |    |
                    |    |
                    +----+
             Name(fp+1)         Name(fp+3)
            +----+          +----+
            |    |          |    |
            |    |          |    |
            +----+          +----+
                     you(fp)
                    +----+
                    |    |
                    |    |
                    +----+

        */
		val backPlayer = (frontPlayer + 2) % 4
		val leftPlayer = (frontPlayer + 1) % 4
		val rightPlayer = (frontPlayer + 3) % 4
		var card = kaiser.getTrick(backPlayer)
		var indentStr = "                    "
		var indent = indentStr
		printf(
			"""
	        	%s  %s
	        	%s+------+
	        	%s|%s    |
	        	%s|%s|
	        	%s|%s|
	        	%s|%s|
	        	%s|%s|
	        	%s+------+

	        	""".trimIndent(), indent, getPlayerHeader(kaiser, kaiser.getPlayer(backPlayer)), indent, indent, card?.rank?.rankString
			?: "  " //,indent, card == null ? ' ' : card.suit.getSuitChar()
			, indent, if (card == null) "      " else cardSymbols[0][card.suit.ordinal], indent, if (card == null) "      " else cardSymbols[1][card.suit.ordinal], indent, if (card == null) "      " else cardSymbols[2][card.suit.ordinal], indent, if (card == null) "      " else cardSymbols[3][card.suit.ordinal], indent
		)
		indentStr = "          "
		indent = indentStr
		val spacingStr = "            "
		val card0 = kaiser.getTrick(leftPlayer)
		val card1 = kaiser.getTrick(rightPlayer)
		printf(
			"""
	        	%s %-8s%s %s
	        	%s+------+%s+------+
	        	%s|%s    |%s|%s    |
	        	%s|%s|%s|%s|
	        	%s|%s|%s|%s|
	        	%s|%s|%s|%s|
	        	%s|%s|%s|%s|
	        	%s+------+%s+------+

	        	""".trimIndent(), indent, getPlayerHeader(kaiser, kaiser.getPlayer(leftPlayer)),
			spacingStr, getPlayerHeader(kaiser, kaiser.getPlayer(rightPlayer)), indent, spacingStr, indent, card0?.rank?.rankString
			?: "  ", spacingStr, card1?.rank?.rankString
			?: "  " //,indent, card0 == null ? ' ' : card0.suit.getSuitChar(), spacing, card1 == null ? ' ' : card1.suit.getSuitChar()
			, indent, if (card0 == null) "      " else cardSymbols[0][card0.suit.ordinal], spacingStr, if (card1 == null) "      " else cardSymbols[0][card1.suit.ordinal], indent, if (card0 == null) "      " else cardSymbols[1][card0.suit.ordinal], spacingStr, if (card1 == null) "      " else cardSymbols[1][card1.suit.ordinal], indent, if (card0 == null) "      " else cardSymbols[2][card0.suit.ordinal], spacingStr, if (card1 == null) "      " else cardSymbols[2][card1.suit.ordinal], indent, if (card0 == null) "      " else cardSymbols[3][card0.suit.ordinal], spacingStr, if (card1 == null) "      " else cardSymbols[3][card1.suit.ordinal], indent, spacingStr
		)
		card = kaiser.getTrick(frontPlayer)
		indentStr = "                    "
		indent = indentStr
		printf(
			"""
	        	%s  %s
	        	%s+------+
	        	%s|%s    |
	        	%s|%s|
	        	%s|%s|
	        	%s|%s|
	        	%s|%s|
	        	%s+------+

	        	""".trimIndent(), indent, getPlayerHeader(kaiser, kaiser.getPlayer(frontPlayer)), indent, indent, card?.rank?.rankString
			?: "  " //,indent, card == null ? ' ' : card.suit.getSuitChar()
			, indent, if (card == null) "      " else cardSymbols[0][card.suit.ordinal], indent, if (card == null) "      " else cardSymbols[1][card.suit.ordinal], indent, if (card == null) "      " else cardSymbols[2][card.suit.ordinal], indent, if (card == null) "      " else cardSymbols[3][card.suit.ordinal], indent)
	}

	fun bidToString(bid: Bid): String {
		var s = ""
		if (bid.numTricks == 0) s = "No Bid" else {
			s += bid.numTricks.toString()
			s += " tricks "
			s += bid.trump.suitString
		}
		return s
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			val game = KaiserConsole()
			game.chooseGameType()
		}

		const val PORT = 32323
		const val VERSION = "KaiserConsole"
		fun <T> isInArray(value: T, array: Array<T>): Boolean {
			for (i in array.indices) if (value == array[i]) return true
			return false
		}
	}
}