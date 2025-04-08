package cc.game.golf.swing

import cc.game.golf.ai.PlayerBot
import cc.game.golf.core.Card
import cc.game.golf.core.DrawType
import cc.game.golf.core.Golf
import cc.game.golf.core.Rank
import cc.game.golf.core.Rules
import cc.game.golf.core.Rules.KnockerBonusType
import cc.game.golf.core.Rules.KnockerPenaltyType
import cc.game.golf.core.Rules.WildCard
import cc.game.golf.core.State
import cc.game.golf.core.Suit
import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.lib.utils.FileUtils
import cc.lib.utils.KLock
import cc.lib.utils.launchIn
import kotlinx.coroutines.delay
import java.awt.Font
import java.io.File
import java.util.Arrays
import java.util.LinkedList

/**
 * GolfSwing (get it?)
 * @author ccaron
 */
class GolfSwing : AWTKeyboardAnimationApplet() {
	private var tableImageId = -1
	private val cardDownImage = IntArray(4)
	private val pickableCards: MutableList<Card> = ArrayList()
	private var pickedCard = -1
	private var highlightedCard = -1
	private var screenWidth = 0
	private var screenHeight = 0
	private var cardWidth = 0
	private var cardHeight = 0
	private var message: String? = ""
	private val animations: MutableList<AAnimation<AGraphics>> = LinkedList()
	private lateinit var game: IGolfGame
	private val lock = KLock()

	enum class Angle(val degrees: Int) {
		ANG_0(0),
		ANG_90(90),
		ANG_180(180),
		ANG_270(270)
	}

	internal inner class CardImage(//<CardImage> { - This is temping, but we need to be able to compare against integer
		val rank: Rank, val suit: Suit, val fileName: String
	) : Comparable<Any> {
		var imageId = IntArray(4)
		override fun compareTo(obj: Any): Int {
			val i0 = rank.ordinal shl 16 or suit.ordinal
			val i1: Int = if (obj is CardImage) {
				val c = obj
				c.rank.ordinal shl 16 or c.suit.ordinal
			} else {
				(obj as Int)
			}
			return i0 - i1
		}
	}

	private val cardImages = arrayOf<CardImage>(
		CardImage(Rank.ACE, Suit.CLUBS, "1.png"),
		CardImage(Rank.ACE, Suit.SPADES, "2.png"),
		CardImage(Rank.ACE, Suit.HEARTS, "3.png"),
		CardImage(Rank.ACE, Suit.DIAMONDS, "4.png"),
		CardImage(Rank.KING, Suit.CLUBS, "5.png"),
		CardImage(Rank.KING, Suit.SPADES, "6.png"),
		CardImage(Rank.KING, Suit.HEARTS, "7.png"),
		CardImage(Rank.KING, Suit.DIAMONDS, "8.png"),
		CardImage(Rank.QUEEN, Suit.CLUBS, "9.png"),
		CardImage(Rank.QUEEN, Suit.SPADES, "10.png"),
		CardImage(Rank.QUEEN, Suit.HEARTS, "11.png"),
		CardImage(Rank.QUEEN, Suit.DIAMONDS, "12.png"),
		CardImage(Rank.JACK, Suit.CLUBS, "13.png"),
		CardImage(Rank.JACK, Suit.SPADES, "14.png"),
		CardImage(Rank.JACK, Suit.HEARTS, "15.png"),
		CardImage(Rank.JACK, Suit.DIAMONDS, "16.png"),
		CardImage(Rank.TEN, Suit.CLUBS, "17.png"),
		CardImage(Rank.TEN, Suit.SPADES, "18.png"),
		CardImage(Rank.TEN, Suit.HEARTS, "19.png"),
		CardImage(Rank.TEN, Suit.DIAMONDS, "20.png"),
		CardImage(Rank.NINE, Suit.CLUBS, "21.png"),
		CardImage(Rank.NINE, Suit.SPADES, "22.png"),
		CardImage(Rank.NINE, Suit.HEARTS, "23.png"),
		CardImage(Rank.NINE, Suit.DIAMONDS, "24.png"),
		CardImage(Rank.EIGHT, Suit.CLUBS, "25.png"),
		CardImage(Rank.EIGHT, Suit.SPADES, "26.png"),
		CardImage(Rank.EIGHT, Suit.HEARTS, "27.png"),
		CardImage(Rank.EIGHT, Suit.DIAMONDS, "28.png"),
		CardImage(Rank.SEVEN, Suit.CLUBS, "29.png"),
		CardImage(Rank.SEVEN, Suit.SPADES, "30.png"),
		CardImage(Rank.SEVEN, Suit.HEARTS, "31.png"),
		CardImage(Rank.SEVEN, Suit.DIAMONDS, "32.png"),
		CardImage(Rank.SIX, Suit.CLUBS, "33.png"),
		CardImage(Rank.SIX, Suit.SPADES, "34.png"),
		CardImage(Rank.SIX, Suit.HEARTS, "35.png"),
		CardImage(Rank.SIX, Suit.DIAMONDS, "36.png"),
		CardImage(Rank.FIVE, Suit.CLUBS, "37.png"),
		CardImage(Rank.FIVE, Suit.SPADES, "38.png"),
		CardImage(Rank.FIVE, Suit.HEARTS, "39.png"),
		CardImage(Rank.FIVE, Suit.DIAMONDS, "40.png"),
		CardImage(Rank.FOUR, Suit.CLUBS, "41.png"),
		CardImage(Rank.FOUR, Suit.SPADES, "42.png"),
		CardImage(Rank.FOUR, Suit.HEARTS, "43.png"),
		CardImage(Rank.FOUR, Suit.DIAMONDS, "44.png"),
		CardImage(Rank.THREE, Suit.CLUBS, "45.png"),
		CardImage(Rank.THREE, Suit.SPADES, "46.png"),
		CardImage(Rank.THREE, Suit.HEARTS, "47.png"),
		CardImage(Rank.THREE, Suit.DIAMONDS, "48.png"),
		CardImage(Rank.TWO, Suit.CLUBS, "49.png"),
		CardImage(Rank.TWO, Suit.SPADES, "50.png"),
		CardImage(Rank.TWO, Suit.HEARTS, "51.png"),
		CardImage(Rank.TWO, Suit.DIAMONDS, "52.png"),
		CardImage(Rank.JOKER, Suit.BLACK, "53.png"),
		CardImage(Rank.JOKER, Suit.RED, "54.png"))

	suspend fun pickCard(options: List<Card>): Int {
		pickedCard = -1
		pickableCards.clear()
		pickableCards.addAll(options)
		lock.acquireAndBlock()
		pickableCards.clear()
		return pickedCard
	}

	override fun doInitialization() {
		newSinglePlayerGame()
	}

	private fun loadImages(g: AGraphics) {
		for (c in cardImages) {
			c.imageId[Angle.ANG_0.ordinal] = g.loadImage(c.fileName)
			c.imageId[Angle.ANG_90.ordinal] = g.newRotatedImage(c.imageId[0], Angle.ANG_90.degrees)
			c.imageId[Angle.ANG_180.ordinal] = g.newRotatedImage(c.imageId[0], Angle.ANG_180.degrees)
			c.imageId[Angle.ANG_270.ordinal] = g.newRotatedImage(c.imageId[0], Angle.ANG_270.degrees)
		}
		Arrays.sort(cardImages)
		tableImageId = g.loadImage("table.png")
		cardDownImage[2] = g.loadImage("b1fv.png")
		cardDownImage[0] = cardDownImage[2]
		cardDownImage[3] = g.loadImage("b1fh.png")
		cardDownImage[1] = cardDownImage[3]
		cardWidth = g.getImage(cardImages[0].imageId[0]).width.toInt()
		cardHeight = g.getImage(cardImages[0].imageId[0]).height.toInt()
		initTurnOverCardAnimations(g)
	}

	fun message(msg: String?) {
		println(msg)
	}

	private fun getCardWidth(ang: Angle): Int {
		return when (ang) {
			Angle.ANG_0, Angle.ANG_180 -> cardWidth
			Angle.ANG_90, Angle.ANG_270 -> cardHeight
		}
	}

	private fun getCardHeight(ang: Angle): Int {
		return when (ang) {
			Angle.ANG_0, Angle.ANG_180 -> cardHeight
			Angle.ANG_90, Angle.ANG_270 -> cardWidth
		}
	}

	override fun drawFrame(g: AGraphics) {
		try {
			clearScreen()
			g.drawImage(tableImageId, 0f, 0f, (screenHeight - 5).toFloat(), (screenHeight - 5).toFloat())
			if (!game.isRunning) {
				drawGameReady(g)
			} else {
				drawGame(g)
				drawPlayersStatus(g)
			}
			getMouseButtonClicked(0)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	private fun <T> incrementRule(currentValue: T, vararg options: T): T {
		var index = 0
		while (index < options.size) {
			if (currentValue === options[index]) break
			index++
		}
		index = (index + 1) % options.size
		return options[index]
	}

	private fun drawGameReady(g: AGraphics) {
		val deck = game.deck
		val cards = deck.toTypedArray<Card>()
		var start = 0
		val x = 10
		var y = 10
		val width = screenHeight - 25
		val cardsPerRow = cards.size / 4
		while (start < cards.size) {
			drawCards_old(g, cards, start, cardsPerRow, x, y, Angle.ANG_0, width)
			start += cardsPerRow
			y += cardHeight + 5
		}
		val buttonX = screenHeight + 10
		var buttonY = 20
		val buttonDY = g.textHeight + 10
		val buttonWidth = screenWidth - 20 - buttonX
		if (game.canResume()) {
			if (drawPickButton(g, buttonX, buttonY, buttonWidth, "RESUME")) {
				try {
					game.resume()
				} catch (e: Exception) {
					//SAVE_FILE.delete();
					message("Failed to resume: " + e.message)
				}
			}
		}
		buttonY = (buttonY + buttonDY).toInt()
		if (drawPickButton(g, buttonX, buttonY, buttonWidth, "NEW GAME")) {
			game.startNewGame()
		}
		buttonY = (buttonY + buttonDY).toInt()
		if (drawPickButton(g, buttonX, buttonY, buttonWidth, "JOIN MULTIPLAYER")) {
			/*
            try {
                IGolfGame multiplayerGame = new MultiPlayerGolfGame(this, "Chris");
                multiplayerGame.startNewGame();
                game = multiplayerGame;
            } catch (Exception e) {
                setMessage("Failed to connect to server");
            }*/
		}
		val buttonWidth2 = buttonWidth / 3
		val buttonX2 = buttonX + buttonWidth - buttonWidth2
		val rules = game.rules
		buttonY = (buttonY + buttonDY).toInt()
		g.color = GColor.WHITE
		buttonY = (buttonY + buttonDY).toInt()
		g.color = GColor.WHITE
		g.drawJustifiedString(buttonX, buttonY + 5, Justify.LEFT, Justify.TOP, "Game Type")
		if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, rules.gameType.name)) {
			rules.gameType = incrementRule(rules.gameType, *Rules.GameType.entries.toTypedArray())
			game.updateRules()
		}
		buttonY = (buttonY + buttonDY).toInt()
		g.color = GColor.WHITE
		g.drawJustifiedString(buttonX, buttonY + 5, Justify.LEFT, Justify.TOP, "Num Holes")
		if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.numHoles + " ")) {
			rules.numHoles = incrementRule(rules.numHoles, 18, 9)
			game.updateRules()
		}
		if (rules.gameType != Rules.GameType.NineCard && rules.gameType != Rules.GameType.FourCard) {
			buttonY = (buttonY + buttonDY).toInt()
			g.color = GColor.WHITE
			g.drawJustifiedString(buttonX, buttonY + 5, Justify.LEFT, Justify.TOP, "Num Decks")
			if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.numDecks + " ")) {
				rules.numDecks = incrementRule(rules.numDecks, 1, 2)
				game.updateRules()
			}
		}
		buttonY = (buttonY + buttonDY).toInt()
		g.color = GColor.WHITE
		g.drawJustifiedString(buttonX, buttonY + 5, Justify.LEFT, Justify.TOP, "Wild Card")
		if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, rules.wildcard.name)) {
			rules.wildcard = incrementRule(rules.wildcard, *WildCard.entries.toTypedArray())
			game.updateRules()
		}
		if (rules.gameType != Rules.GameType.NineCard) {
			buttonY = (buttonY + buttonDY).toInt()
			g.color = GColor.WHITE
			g.drawJustifiedString(buttonX, buttonY + 5, Justify.LEFT, Justify.TOP, "Num Jokers")
			if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.numJokers + " ")) {
				rules.numJokers = incrementRule(rules.numJokers, 0, 2, 4, 8)
				game.updateRules()
			}
			buttonY = (buttonY + buttonDY).toInt()
			g.color = GColor.WHITE
			g.drawJustifiedString(buttonX, buttonY + 5, Justify.LEFT, Justify.TOP, "Jokers Paired")
			if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.jokerValuePaired + " ")) {
				rules.jokerValuePaired = incrementRule(rules.jokerValuePaired, -5, -4)
				game.updateRules()
			}
			buttonY = (buttonY + buttonDY).toInt()
			g.color = GColor.WHITE
			g.drawJustifiedString(buttonX, buttonY + 5, Justify.LEFT, Justify.TOP, "Jokers Unpaired")
			if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.jokerValueUnpaired + " ")) {
				rules.jokerValueUnpaired = incrementRule(rules.jokerValueUnpaired, 15, 0)
				game.updateRules()
			}
		}
		buttonY = (buttonY + buttonDY).toInt()
		g.color = GColor.WHITE
		g.drawJustifiedString(buttonX, buttonY + 5, Justify.LEFT, Justify.TOP, "Knocker penalty")
		if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, rules.knockerPenaltyType.name)) {
			rules.knockerPenaltyType = incrementRule(rules.knockerPenaltyType, *KnockerPenaltyType.entries.toTypedArray())
			game.updateRules()
		}
		buttonY = (buttonY + buttonDY).toInt()
		g.color = GColor.WHITE
		g.drawJustifiedString(buttonX, buttonY + 5, Justify.LEFT, Justify.TOP, "Knocker bonus")
		if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, rules.knockerBonusType.name)) {
			rules.knockerBonusType = incrementRule(rules.knockerBonusType, *KnockerBonusType.entries.toTypedArray())
			game.updateRules()
		}
		buttonY = (buttonY + buttonDY).toInt()
		g.color = GColor.WHITE
		g.drawJustifiedString(buttonX, buttonY + 5, Justify.LEFT, Justify.TOP, "Four of a Kind Bonus")
		if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.fourOfAKindBonus + " ")) {
			rules.fourOfAKindBonus = incrementRule(rules.fourOfAKindBonus, -50, -40, -30, -20, -10, 0)
			game.updateRules()
		}
	}

	private fun drawMessage(g: AGraphics, x: Int, y: Int, width: Int, msg: String): Int {
		var y = y
		val ty = g.textHeight
		y = (y + ty).toInt()
		g.color = GColor.GRAY
		g.generateWrappedLines(msg, width.toFloat()).forEach { l ->
			y = (y + ty).toInt()
			g.drawJustifiedString(x, y, Justify.LEFT, Justify.TOP, l)
		}
		return y
	}

	private fun drawPlayersStatus(g: AGraphics) {
		g.color = GColor.WHITE
		val statusWidth = screenWidth - screenHeight - 20
		val lx = screenHeight + 10
		val rx = screenWidth - 20
		val cx = (lx + rx) / 2
		var y = 10
		val ty = g.textHeight
		val by = ty + 10 //3;
		g.drawJustifiedString(cx, y, Justify.CENTER, Justify.TOP, "ROUND " + game.numRounds)
		y = (y + ty).toInt()
		for (i in 0 until game.numPlayers) {
			y = (y + ty).toInt()
			val lText = game.getPlayerName(i) + if (game.dealer == i) "(Dealer)" else ""
			g.drawJustifiedString(lx, y, Justify.LEFT, Justify.TOP, lText)
			val rText = "" + game.getPlayerPoints(i) + " Points"
			g.drawJustifiedString(rx, y, Justify.RIGHT, Justify.TOP, rText)
		}
		y = (y + by).toInt()
		if (drawPickButton(g, cx - statusWidth / 2, y, statusWidth, "QUIT")) {
			game.quit()
			newSinglePlayerGame()
			lock.release()
			return
		}
		y = (y + by).toInt()
		val playerName = game.getPlayerName(game.currentPlayer)
		g.color = GColor.WHITE
		when (game.state) {
			State.INIT, State.SHUFFLE -> {}
			State.DEAL -> {}
			State.TURN_OVER_CARDS -> {
				y = drawMessage(g, lx, y, statusWidth, "Waiting for $playerName to turn over a card")
			}

			State.TURN -> {
				y = drawMessage(g, lx, y, statusWidth, "Waiting for $playerName to choose from stack or discard pile")
			}

			State.PLAY -> {
				y = drawMessage(g, lx, y, statusWidth, "Waiting for $playerName to choose card from their hand to swap")
			}

			State.DISCARD_OR_PLAY -> {
				y =
					drawMessage(g, lx, y, statusWidth, "Waiting for $playerName to discard or choose a card from their hand to swap")
			}

			State.SETUP_DISCARD_PILE -> {}
			State.GAME_OVER -> {

				//y += by;
				g.drawJustifiedString(cx, y, Justify.CENTER, Justify.TOP, "G A M E   O V E R")
				y = (y + ty).toInt()
				//SwingPlayer winner = (SwingPlayer)golf.getPlayer(golf.getWinner());
				val winner = game.winner
				g.drawJustifiedString(cx, y, Justify.CENTER, Justify.TOP, game.getPlayerName(winner) + " Wins!")
				y = (y + by).toInt()
				if (drawPickButton(g, cx - statusWidth / 2, y, statusWidth, "Play Agian?")) {
					lock.release()
				}
			}

			State.PROCESS_ROUND -> {}
			State.END_ROUND -> {
				y = (y + by).toInt()
				if (drawPickButton(g, cx - statusWidth / 2, y, statusWidth, "CONTINUE")) {
					lock.release()
				}
			}
		}
		message?.let {
			y = drawMessage(g, lx, y, statusWidth, it)
		}
	}

	class CardLayout(val angle: Angle, rows: Int, cols: Int, width: Int, height: Int) {
		val x: Array<IntArray>
		val y: Array<IntArray>
		val width: Int
		val height: Int

		init {
			x = Array(rows) { IntArray(cols) }
			y = Array(rows) { IntArray(cols) }
			when (angle) {
				Angle.ANG_0, Angle.ANG_180 -> {
					this.width = width
					this.height = height
				}

				else -> {
					this.width = height
					this.height = width
				}
			}
		}
	}

	val cardsLayout by lazy {
		val padding = 3
		val z0 = cardHeight * 2 + padding * 3
		val z1 = screenHeight - cardHeight * 2 - padding * 3
		val numRows = game.rules.gameType.rows
		assert(numRows > 1)
		val numCols = game.rules.gameType.cols
		Array<CardLayout>(4) {
			CardLayout(Angle.entries[it], numRows, numCols, z1 - z0, getCardHeight(Angle.ANG_0))
		}
	}

	private fun drawGame(g: AGraphics) {
		val padding = 3
		val z0 = cardHeight * 2 + padding * 3
		val z1 = screenHeight - cardHeight * 2 - padding * 3
		//final int txtHeight = g.getTextHeight();
		val xTop = intArrayOf(z0 + padding, z0 / 2 + padding, z0 + padding, z1 + padding)
		val yTop = intArrayOf(z1 + padding, z0 + padding, z0 / 2 + padding, z0 + padding)
		val xBottom = intArrayOf(xTop[0], padding, xTop[2], z1 + cardHeight + 2 * padding)
		val yBottom = intArrayOf(z1 + cardHeight + 2 * padding, yTop[3], padding, yTop[1])

		// draw piles
		val cz = screenHeight / 2 + padding
		val xTxt = intArrayOf(
			cz, z0 / 2, cz, z1 + z0 / 2
		)
		val yTxt = intArrayOf(
			yTop[0] - padding * 2, yTop[3] - padding, yBottom[3] + padding, yTop[1] - padding
		)
		val vTxt = arrayOf(Justify.BOTTOM, Justify.BOTTOM, Justify.TOP, Justify.BOTTOM)
		val numRows = game.rules.gameType.rows
		assert(numRows > 1)
		val numCols = game.rules.gameType.cols
		highlightedCard = -1
		for (i in 0 until game.numPlayers) {
			val playerIndex = (game.frontPlayer + i) % game.numPlayers
			cardsLayout[i] = CardLayout(Angle.entries[i], numRows, numCols, z1 - z0, getCardHeight(Angle.ANG_0))
			val layout = cardsLayout[i]
			//SwingPlayer p = (SwingPlayer)golf.getPlayer(i);
			for (row in 0 until numRows) {
				val xscale = (xBottom[i] - xTop[i]).toFloat() / (numRows - 1)
				val yscale = (yBottom[i] - yTop[i]).toFloat() / (numRows - 1)
				val x = xTop[i] + Math.round(xscale * row)
				val y = yTop[i] + Math.round(yscale * row)
				//                drawCards(g, p.getRow(row).toArray(new Card[numInRow]), 0, numInRow, x, y, Angle.values()[i], z1 - z0);
				layoutCards(layout.x[row], layout.y[row], numCols, x, y, Angle.entries[i], z1 - z0)
			}
			drawCards(g, layout, game.getPlayerCards(playerIndex))
			var text = game.getPlayerName(playerIndex) + " Showing:" + game.getHandPoints(playerIndex)
			if (playerIndex == game.knocker) text += " KNOCKED"
			g.color = GColor.WHITE
			g.drawJustifiedString(xTxt[i], yTxt[i], Justify.CENTER, vTxt[i], text)
		}
		val piles = arrayOf(
			game.topOfDeck,
			game.topOfDiscardPile).filterNotNull()
		val stackAngle = cardsLayout[game.frontPlayer].angle
		layoutCards(stackLayout.x[0], stackLayout.y[0], 2, cz - cardWidth, cz - cardHeight / 2, stackAngle, cardWidth * 2 + padding)
		drawCards(g, stackLayout, arrayOf(piles.toTypedArray()))
		processAnimations(g)
		if (AGraphics.DEBUG_ENABLED) {
			g.color = GColor.WHITE
			g.drawCircle(mouseX.toFloat(), mouseY.toFloat(), 5f)
		}
	}

	val stackLayout by lazy {
		val padding = 3
		val stackAngle = cardsLayout[game.frontPlayer].angle
		CardLayout(stackAngle, 1, 2, getCardWidth(Angle.ANG_0) * 2 + padding, getCardHeight(Angle.ANG_0))
	}

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		Utils.println("Dimensions changed to $width X $height")
		screenWidth = width
		screenHeight = height
		g.ortho(0f, screenWidth.toFloat(), 0f, screenHeight.toFloat())
		if (tableImageId < 0) {
			loadImages(g)
		}
		//float aspectRatio = (float)cardWidth/cardHeight;
		val aspectRatio = cardHeight.toFloat() / cardWidth
		cardWidth = screenHeight / 3 / 3 - 3
		cardHeight = Math.round(cardWidth.toFloat() * aspectRatio)
	}

	private fun getPickableIndex(card: Card): Int {
		return pickableCards.indexOf(card)
	}

	// complex function  
	// draw an array of cards spaced and centered within the specified rectangle 
	// with a specified orientation.
	// handles picking of cards as well.
	private fun layoutCards(outX: IntArray, outY: IntArray, numCards: Int, x: Int, y: Int, angle: Angle, maxLenPixels: Int) {
		var x = x
		var y = y
		var dx = 0
		var dy = 0
		val maxDx = cardWidth + cardWidth / 6
		if (numCards > 1) {
			when (angle) {
				Angle.ANG_0, Angle.ANG_180 -> {
					dx = (maxLenPixels - cardWidth) / (numCards - 1)
					if (dx > maxDx) {
						dx = maxDx
						val spacing = dx - cardWidth
						x = x + maxLenPixels / 2 - cardWidth * numCards / 2 - spacing * (numCards - 1) / 2
					}
				}

				Angle.ANG_90, Angle.ANG_270 -> {
					dy = (maxLenPixels - cardWidth) / (numCards - 1)
					if (dy > maxDx) {
						dy = maxDx
						val spacing = dy - cardWidth
						y = y + maxLenPixels / 2 - cardWidth * numCards / 2 - spacing * (numCards - 1) / 2
					}
				}
			}
		}

		//System.out.println("card picked1 = " + cardPicked);
		for (i in 0 until numCards) {
			outX[i] = x
			outY[i] = y
			x += dx
			y += dy
		}
	}

	private fun drawCards(g: AGraphics, layout: CardLayout, cards: Array<Array<Card?>>) {
		if (AGraphics.DEBUG_ENABLED) {
			g.color = GColor.RED
			for (i in layout.x.indices) g.drawRect(layout.x[i][0].toFloat(), layout.y[i][0].toFloat(), layout.width.toFloat(), layout.height.toFloat())
		}
		var highlighted: Card? = null
		val cardWidth = getCardWidth(layout.angle)
		val cardHeight = getCardHeight(layout.angle)
		for (r in cards.indices) {
			for (c in cards[r].indices) {
				cards[r][c]?.let { card ->
					if (getPickableIndex(card) >= 0 && Utils.isPointInsideRect(mouseX.toFloat(), mouseY.toFloat(), layout.x[r][c].toFloat(), layout.y[r][c].toFloat(), cardWidth.toFloat(), cardHeight.toFloat())) {
						highlighted = card
					}
				}
			}
		}

		// now draw the cards
		for (r in cards.indices) {
			for (c in cards[r].indices) {
				cards[r][c]?.let { card ->
					val dy = 0
					//if (highlighted != null && card.equals(highlighted))
					//    dy = 20;
					drawCard(g, card, layout.x[r][c], layout.y[r][c] - dy, layout.angle)
					if (highlighted != null && card == highlighted) {
						g.color = GColor.RED
						g.drawRect((layout.x[r][c] - 2).toFloat(), (layout.y[r][c] - 2).toFloat(), (cardWidth + 4).toFloat(), (cardHeight + 4).toFloat(), 3f)
					}
				}
			}
		}
		highlighted?.takeIf { getMouseButtonPressed(0) }?.let {
			pickedCard = getPickableIndex(it)
			println("Picked the " + pickableCards[pickedCard])
			lock.release()
		}
	}

	// complex function  
	// draw an array of cards spaced and centered within the specified rectangle 
	// with a specified orientation.
	// handles picking of cards as well.
	private fun drawCards_old(g: AGraphics, cards: Array<Card>, offset: Int, len: Int, x: Int, y: Int, angle: Angle, maxLenPixels: Int) {
		var len = len
		var x = x
		var y = y
		if (AGraphics.DEBUG_ENABLED) {
			g.color = GColor.RED
			if (angle == Angle.ANG_0 || angle == Angle.ANG_180) g.drawRect(x.toFloat(), y.toFloat(), maxLenPixels.toFloat(), cardHeight.toFloat()) else g.drawRect(x.toFloat(), y.toFloat(), cardHeight.toFloat(), maxLenPixels.toFloat())
		}

		if (len <= 0) return
		if (offset + len >= cards.size) len = cards.size - offset
		var dx = 0
		var dy = 0
		val maxDx = cardWidth + cardWidth / 6
		if (len > 1) {
			when (angle) {
				Angle.ANG_0, Angle.ANG_180 -> {
					dx = (maxLenPixels - cardWidth) / (len - 1)
					if (dx > maxDx) {
						dx = maxDx
						val spacing = dx - cardWidth
						x = x + maxLenPixels / 2 - cardWidth * len / 2 - spacing * (len - 1) / 2
					}
				}

				Angle.ANG_90, Angle.ANG_270 -> {
					dy = (maxLenPixels - cardWidth) / (len - 1)
					if (dy > maxDx) {
						dy = maxDx
						val spacing = dy - cardWidth
						y = y + maxLenPixels / 2 - cardWidth * len / 2 - spacing * (len - 1) / 2
					}
				}
			}
		}

		// search backwards to see if a card is picked
		var picked = -1
		var sx = x + dx * (len - 1)
		var sy = y + dy * (len - 1)
		if (highlightedCard < 0) {
			for (i in len - 1 downTo 0) {
				picked = getPickableIndex(cards[i])
				if (picked >= 0 && Utils.isPointInsideRect(mouseX.toFloat(), mouseY.toFloat(), sx.toFloat(), sy.toFloat(), cardWidth.toFloat(), cardHeight.toFloat())) {
					//picked = i;
					//System.out.println("picked = " + picked);
					break
				}
				picked = -1
				sx -= dx
				sy -= dy
			}
		}

		//System.out.println("card picked1 = " + cardPicked);
		for (i in 0 until len) {
			sy = y
			if (highlightedCard < 0 && picked >= 0 && cards[i] == pickableCards[picked]) {
				highlightedCard = picked
				sy -= 20
			}
			drawCard(g, cards[i + offset], x, sy, angle)
			x += dx
			y += dy
		}
		if (getMouseButtonPressed(0)) {
			if (picked >= 0) {
				pickedCard = picked
				println("Picked the " + pickableCards[pickedCard])
			}
			lock.release()
		}
	}

	private fun drawCard(g: AGraphics, card: Card, x: Int, y: Int, angle: Angle) {
		val cw = getCardWidth(angle)
		val ch = getCardHeight(angle)
		g.drawImage(getCardImage(card, angle), x.toFloat(), y.toFloat(), cw.toFloat(), ch.toFloat())
		if (card.isShowing && game.rules.wildcard.isWild(card)) {
			g.color = GColor(0, 0, 0, 64)
			g.drawFilledRect(x.toFloat(), y.toFloat(), cw.toFloat(), ch.toFloat())
			val cx = x + cw / 2
			val cy = y + ch / 2
			g.color = GColor.BLACK
			g.drawJustifiedString(cx + 2, cy + 2, Justify.CENTER, Justify.CENTER, "WILD")
			g.color = GColor.YELLOW
			g.drawJustifiedString(cx, cy, Justify.CENTER, Justify.CENTER, "WILD")
		}
	}

	private fun getCardImage(card: Card, angle: Angle): Int {
		return if (!card.isShowing) cardDownImage[angle.ordinal] else getCardImage(card.rank, card.suit, angle)
	}

	private fun getCardImage(rank: Rank?, suit: Suit?, angle: Angle): Int {
		if (rank == null || suit == null) return cardDownImage[angle.ordinal]
		val key = rank.ordinal shl 16 or suit.ordinal
		val index = Arrays.binarySearch(cardImages, key)
		val card = cardImages[index]
		if (card.rank != rank || card.suit != suit) throw RuntimeException("No image for card $rank $suit")
		return card.imageId[angle.ordinal] //index;
		//        for (CardImage c : cardImages) {
//            if (c.rank == rank && c.suit == suit)
//                return c.imageId[angle.ordinal()];
//        }
//        throw new RuntimeException("No image for card " + rank + " " + suit);
	}

	private fun drawPickButton(g: AGraphics, x: Int, y: Int, minWidth: Int, text: String): Boolean {
		val padding = 2
		var wid = Math.round(g.getTextWidth(text))
		if (wid < minWidth) wid = minWidth
		val hgt = g.textHeight
		val fontColor = GColor.WHITE
		var rectColor = GColor.CYAN
		var highlighted = false
		if (Utils.isPointInsideRect(mouseX.toFloat(), mouseY.toFloat(), x.toFloat(), y.toFloat(), (wid + padding * 2).toFloat(), hgt + padding * 2)) {
			rectColor = GColor.YELLOW
			highlighted = true
		}
		g.color = fontColor
		g.drawJustifiedString(x + padding, y + padding, Justify.LEFT, Justify.TOP, text)
		g.color = rectColor
		g.drawRect(x.toFloat(), y.toFloat(), (wid + padding * 2).toFloat(), hgt + padding * 2, 1f)
		val pressed = getMouseButtonPressed(0)
		if (pressed) {
			println("PRESSED")
		}
		return highlighted && pressed
	}

	override fun getDefaultFont(): Font {
		return Font("Arial", Font.BOLD, 12)
	}

	fun processAnimations(g: AGraphics) {
		if (animations.size <= 0) return
		val it = animations.iterator()
		while (it.hasNext()) {
			val a = it.next()
			a.update(g)
			if (a.isDone) {
				it.remove()
			}
		}
	}

	internal inner class MoveCardAnimation(val card: Card, val sx: Float, val sy: Float, val ex: Float, val ey: Float, val angle: Angle, duration: Long) :
		AAnimation<AGraphics>(duration, 0, false) {
		init {
			start<AAnimation<*>>()
		}

		override fun draw(g: AGraphics, position: Float, dt: Float) {
			val x = sx + (ex - sx) * position
			val y = sy + (ey - sy) * position
			var cw = cardWidth.toFloat()
			var ch = cardHeight.toFloat()
			val xi = Math.round(x - cw / 2)
			val yi = Math.round(y - ch / 2)
			when (angle) {
				Angle.ANG_90, Angle.ANG_270 -> {
					cw = cardHeight.toFloat()
					ch = cardWidth.toFloat()
				}

				Angle.ANG_0, Angle.ANG_180 -> {}
			}
			drawCard(g, card, xi, yi, angle)
		}
	}

	var turnOverCardImages = Array(4) { IntArray(10) }
	fun initTurnOverCardAnimations(g: AGraphics) {
		for (i in 0..9) {
			val id = g.loadImage("turnoverblue$i.png")
			val idRot = g.newRotatedImage(id, 90)
			turnOverCardImages[Angle.ANG_180.ordinal][i] = id
			turnOverCardImages[Angle.ANG_0.ordinal][i] = turnOverCardImages[Angle.ANG_180.ordinal][i]
			turnOverCardImages[Angle.ANG_270.ordinal][i] = idRot
			turnOverCardImages[Angle.ANG_90.ordinal][i] = turnOverCardImages[Angle.ANG_270.ordinal][i]
		}
	}

	internal inner class TurnOverCardAnimation(duration: Long, val x: Int, val y: Int, val card: Card, val angle: Angle) :
		AAnimation<AGraphics>(duration, 0, false) {
		val w: Int
		val h: Int
		val images: IntArray

		init {
			w = getCardWidth(angle)
			h = getCardHeight(angle)
			images = turnOverCardImages[angle.ordinal]
			start<AAnimation<*>>()
		}

		override fun draw(g: AGraphics, position: Float, dt: Float) {
			var index = Math.round(position * images.size)
			//System.out.println("index=" + index);
			if (index < 0) index = 0
			if (index >= images.size) index = images.size - 1
			g.drawImage(images[index], x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())
		}
	}

	fun startSwapCardAnimation(player: Int, type: DrawType, drawn: Card, row: Int, col: Int) {
		val layout = cardsLayout[(player - game.frontPlayer + game.numPlayers) % game.numPlayers]
		val a = if (type == DrawType.DTStack) {
			newMoveCardAnimation(drawn, stackLayout, 0, 0, layout, row, col, 1000)
		} else {
			newMoveCardAnimation(drawn, stackLayout, 0, 1, layout, row, col, 1000)
		}
		game.getPlayerCard(player, row, col)?.let { card ->
			val b = newMoveCardAnimation(card, layout, row, col, stackLayout, 0, 1, 1000)
			launchIn {
				animations.add(a)
				animations.add(b)
				repaint()
				delay(1000)
			}
		}
	}

	fun startDiscardDrawnCardAnimation(card: Card) {
		launchIn {
			animations.add(newMoveCardAnimation(card, stackLayout, 0, 0, stackLayout, 0, 1, 1000))
			delay(1000)
		}
	}

	private fun newMoveCardAnimation(c: Card, src: CardLayout, srcRow: Int, srcCol: Int, dst: CardLayout, dstRow: Int, dstCol: Int, time: Long): AAnimation<AGraphics> {
		val sx = src.x[srcRow][srcCol] + getCardWidth(src.angle) / 2
		val sy = src.y[srcRow][srcCol] + getCardHeight(src.angle) / 2
		val ex = dst.x[dstRow][dstCol] + getCardWidth(dst.angle) / 2
		val ey = dst.y[dstRow][dstCol] + getCardHeight(dst.angle) / 2
		return MoveCardAnimation(c, sx.toFloat(), sy.toFloat(), ex.toFloat(), ey.toFloat(), src.angle, time)
	}

	// called from seperated thread
	fun startDealCardAnimation(player: Int, c: Card, row: Int, col: Int) {
		val layout = cardsLayout[(player - game.frontPlayer + game.numPlayers) % game.numPlayers]
		launchIn {
			animations.add(newMoveCardAnimation(c, stackLayout, 0, 0, layout, row, col, 500))
			repaint()
			delay(200)
		}
	}

	val TIME_TO_TURN_OVER_CARD = 1500
	fun startTurnOverCardAnimationStack() {
		game.topOfDeck?.let { top ->
			launchIn {
				animations.add(TurnOverCardAnimation(TIME_TO_TURN_OVER_CARD.toLong(), stackLayout.x[0][0], stackLayout.y[0][0], top, stackLayout.angle))
				repaint()
				delay(TIME_TO_TURN_OVER_CARD.toLong())
			}
		}
	}

	fun startTurnOverCardAnimation(player: Int, card: Card, row: Int, col: Int) {
		val layout = cardsLayout[(player - game.frontPlayer + game.numPlayers) % game.numPlayers]
		launchIn {
			animations.add(TurnOverCardAnimation(TIME_TO_TURN_OVER_CARD.toLong(), layout.x[row][col], layout.y[row][col], card, layout.angle))
			repaint()
			delay(TIME_TO_TURN_OVER_CARD.toLong())
		}
	}

	fun setMessage(msg: String?) {
		message = msg
	}

	fun newSinglePlayerGame() {
		game = SinglePlayerGolfGame(this)
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			AGraphics.DEBUG_ENABLED = true
			Utils.setDebugEnabled()
			Golf.DEBUG_ENABLED = true
			PlayerBot.DEBUG_ENABLED = true
			val frame = AWTFrame("Golf Card Game")
			val app: AWTKeyboardAnimationApplet = GolfSwing()
			frame.add(app)
			if (!frame.loadFromFile(File(FileUtils.getOrCreateSettingsDirectory(GolfSwing::class.java), "gui.properties"))) {
				frame.centerToScreen(800, 600)
			}
			app.init()
			app.start()
			app.setTargetFPS(30)
		}
	}
}
