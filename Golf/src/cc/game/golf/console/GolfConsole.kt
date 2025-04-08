package cc.game.golf.console

import cc.game.golf.ai.PlayerBot
import cc.game.golf.core.Card
import cc.game.golf.core.DrawType
import cc.game.golf.core.Golf
import cc.game.golf.core.Player
import cc.game.golf.core.Rules
import cc.game.golf.core.State
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class GolfConsole internal constructor() {
	val reader: BufferedReader by lazy {
		BufferedReader(InputStreamReader(System.`in`))
	}

	fun readLine(): String {
		try {
			printf("> ")
			return reader.readLine()
		} catch (e: IOException) {
			e.printStackTrace()
			System.exit(1)
			error("fatal")
		}
	}

	fun println(fmt: String?, vararg args: Any?) {
		kotlin.io.println(String.format(fmt!!, *args))
	}

	fun printf(fmt: String?, vararg args: Any?) {
		print(String.format(fmt!!, *args))
	}

	internal inner class ConsolePlayer : Player() {
		override suspend fun turnOverCard(golf: Golf, row: Int): Int {
			while (true) {
				val max = golf.rules.gameType.cols - 1
				println("Choose card from row " + (row + 1) + " to turn over (0-" + max + ")")
				try {
					val num = readLine().trim { it <= ' ' }.toInt()
					if (num in 0..max) {
						return num
					}
				} catch (e: Exception) {
				}
				println("Invalid entry")
			}
		}

		override suspend fun chooseDrawPile(golf: Golf): DrawType {
			while (true) {
				println("draw from (s)tack or (p)ile?")
				try {
					val s = readLine().trim { it <= ' ' }[0].code
					if (s == 's'.code) {
						return DrawType.DTStack
					} else if (s == 'p'.code) {
						return DrawType.DTDiscardPile
					}
				} catch (e: Exception) {
				}
				println("Invalid entry")
			}
		}

		override suspend fun chooseDiscardOrPlay(golf: Golf, drawCard: Card): Card? {
			while (true) {
				println("Pick card to swap (0-8) or (d)iscard")
				try {
					val s = readLine().trim { it <= ' ' }[0].code
					if (s == 'd'.code) {
						return drawCard
					}
					val n = s - '0'.code
					if (n in 0..7) {
						return this.getCard(n)
					}
				} catch (e: Exception) {
				}
				println("Invalid entry")
			}
		}

		override suspend fun chooseCardToSwap(golf: Golf, discardPileTop: Card): Card? {
			while (true) {
				println("Pick card to swap (0-8) or (d)iscard")
				try {
					val s = readLine().trim { it <= ' ' }[0].code
					val n = s - '0'.code
					if (n in 0..7) {
						return this.getCard(n)
					}
				} catch (e: Exception) {
				}
				println("Invalid entry")
			}
		}
	}

	var g = Golf()

	// heart diamond club spade
	var cardSymbols =
		arrayOf(
			arrayOf("  ^ ^ ", "  /\\  ", "   O  ", "   ^  "),
			arrayOf("  \\ / ", " /  \\ ", "  O O ", "  / \\ "),
			arrayOf("   v  ", " \\  / ", "   ^  ", "   ^  "),
			arrayOf("      ", "  \\/  ", "      ", "      ")
		)

	fun drawCards2(cards: List<Card?>) {
		for (c in cards) {
			if (c == null) break
			printf("  +------+")
		}
		printf("\n")
		for (c in cards) {
			if (c == null) break
			if (c.isShowing) printf("  |%s    |", c.rank.rankString) else printf("  |      |")
		}
		printf("\n")
		for (iii in 0..3) {
			for (c in cards) {
				if (c == null) break
				if (c.isShowing) printf("  |%s|", cardSymbols[iii][c.suit.ordinal]) else printf("  |      |")
			}
			printf("\n")
		}
		for (c in cards) {
			if (c == null) break
			printf("  +------+")
		}
		printf("\n")
	}

	fun drawCards(cards: List<Card>) {
		if (cards.size < 6) drawCardsWide(cards) else drawCardsCollapsed(cards)
	}

	fun drawCardsCollapsed(cards: List<Card>) {
		val maxCards = (80 - 6) / 3 // max number of cards we can display in 80 columns
		if (cards.size > maxCards) {
			drawCardsCollapsed(cards.subList(0, maxCards))
			drawCardsCollapsed(cards.subList(maxCards, cards.size))
		} else if (cards.size > 0) {
			for (i in 0 until cards.size - 1) {
				//Card c = cards.get(i);
				printf("+--")
			}
			printf("+----+\n")
			for (i in 0 until cards.size - 1) {
				val c = cards[i]
				printf("|%s", c!!.rank.rankString)
			}
			printf("|%s  |\n", cards[cards.size - 1]!!.rank.rankString)
			for (i in 0 until cards.size - 1) {
				val c = cards[i]
				printf("|%c ", c!!.suit.suitChar)
			}
			printf("|%c   |\n", cards[cards.size - 1]!!.suit.suitChar)
			for (i in 0 until cards.size - 1) {
				//Card c = cards.get(i);
				printf("|  ")
			}
			printf("|    |\n")
			for (i in 0 until cards.size - 1) {
				//Card c = cards.get(i);
				printf("+--")
			}
			printf("+----+\n")
		}
	}

	fun drawCardsWide(cards: List<Card?>) {
		for (c in cards) {
			if (c == null) break
			printf("  +----+")
		}
		printf("\n")
		for (c in cards) {
			if (c == null) break
			if (c.isShowing) printf("  |%s %c|", c.rank.rankString, c.suit.suitChar) else printf("  |    |")
		}
		printf("\n")
		for (i in 0..1) {
			for (c in cards) {
				if (c == null) break
				if (c.isShowing && g.rules.wildcard.isWild(c)) printf("  |Wild|") else printf("  |    |")
			}
			printf("\n")
		}
		for (c in cards) {
			if (c == null) break
			printf("  +----+")
		}
		printf("\n")
	}

	var temp: MutableList<Card> = ArrayList(2)
	fun drawHeader() {
		printf("------------------------------------------------------------\n")
		printf(g.state.toString() + " Current Player: " + g.currentPlayer + " Dealer: " + g.dealer + "\n")
	}

	fun drawGame() {
		val rows = g.rules.gameType.rows
		val cols = g.rules.gameType.cols
		val numHandCards = rows * cols
		for (i in 0 until g.numPlayers) {
			println("")
			println("  Player " + i + " Points Showing: " + g.getPlayer(i).getHandPoints(g) + " total points: " + g.getPlayer(i).points)
			var row = 0
			var ii = 0
			while (ii < numHandCards) {
				if (g.getPlayer(i).numCardsDealt > ii) drawCards(g.getPlayer(i).getRow(row++))
				ii += cols
			}
		}
		println("  Stack   Discard Pile")
		temp.clear()
		temp.add(Card(0))
		g.topOfDiscardPile?.let {
			temp.add(it)
		}
		drawCards(temp)
	}

	init {
		val SAVE_FILE = File("savedrules.txt")
		val rules = Rules()
		try {
			rules.loadFromFile(SAVE_FILE)
		} catch (e: Exception) {
			e.printStackTrace()
		}
		while (true) {
			println("Rules:\n$rules")
			printf("\nChange anything?\n>")
			val line = this.readLine()
			if (line.isEmpty()) continue
			if (line.trim { it <= ' ' } == "no") break
			try {
				rules.deserialize(line)
				rules.saveToFile(SAVE_FILE)
			} catch (e: Exception) {
				System.err.println(e.message)
			}
		}
		g.newGame(rules)
		g.addPlayer(ConsolePlayer())
		g.addPlayer(PlayerBot())
		//g.addPlayer(new PlayerBot());
		//g.addPlayer(new PlayerBot());
		g.newGame()
		drawCards(g.deck)
		runBlocking {
			while (g.state != State.GAME_OVER) {
				drawHeader()
				g.runGame()
				drawGame()
				//readLine();
			}
		}
		printf("GAME OVER\n")
		for (i in 0 until g.numPlayers) {
			val p = g.getPlayer(i)
			printf("Player " + i + " ends with " + p.points + "\n")
		}
		printf("Player " + g.winner + " is the winner")
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Golf.DEBUG_ENABLED = true
			GolfConsole()
		}
	}
}
