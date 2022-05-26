package cc.applets.kaiser

import cc.game.kaiser.ai.PlayerBot
import cc.game.kaiser.core.*
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTKeyboardAnimationApplet
import cc.lib.utils.FileUtils
import cc.lib.utils.Lock
import cc.lib.utils.Reflector
import java.awt.event.MouseEvent
import java.io.File
import java.util.*

class KaiserApplet() : AWTKeyboardAnimationApplet() {
	private var tableImageId = -1
	private val cardDownImage = IntArray(4)
	private lateinit var kaiser: Kaiser
	private var running = false
	private val lock = Lock()

	internal enum class Angle(val degrees: Int) {
		ANG_0(0),
		ANG_90(90),
		ANG_180(180),
		ANG_270(270);
	}

	internal inner class CardImage(val rank: Rank, val suit: Suit, val fileName: String) {
		var imageId = IntArray(4)
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
		CardImage(Rank.FIVE, Suit.HEARTS, "39.png"),
		CardImage(Rank.THREE, Suit.CLUBS, "45.png"))
	private var cardWidth = 0f
	private var cardHeight = 0f
	val SAVE_FILE = File(FileUtils.getOrCreateSettingsDirectory(javaClass), "game.save")
	override fun doInitialization() {
		Kaiser.DEBUG_ENABLED = true
		Utils.setDebugEnabled()
		PlayerBot.ENABLE_AIDEBUG = true
		Reflector.THROW_ON_UNKNOWN = true
		kaiser = Kaiser()
		try {
			val game = Kaiser()
			game.loadFromFile(SAVE_FILE)
			kaiser.copyFrom(game)
			for (p: Player in kaiser.getPlayers()) {
				if (p is SwingPlayerUser) {
					p.applet = this
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			kaiser.newGame()
			kaiser.setPlayer(0, SwingPlayerUser("Chris", this))
			kaiser.setPlayer(1, SwingPlayer("AI 1"))
			kaiser.setPlayer(2, SwingPlayer("AI 2"))
			kaiser.setPlayer(3, SwingPlayer("AI 3"))
		}
	}

	fun loadImages(g: AGraphics) {
		if (tableImageId >= 0) {
			return
		}
		for (c: CardImage in cardImages) {
			c.imageId[Angle.ANG_0.ordinal] = g.loadImage(c.fileName)
			c.imageId[Angle.ANG_90.ordinal] = g.newRotatedImage(c.imageId[0], Angle.ANG_90.degrees)
			c.imageId[Angle.ANG_180.ordinal] = g.newRotatedImage(c.imageId[0], Angle.ANG_180.degrees)
			c.imageId[Angle.ANG_270.ordinal] = g.newRotatedImage(c.imageId[0], Angle.ANG_270.degrees)
		}
		tableImageId = g.loadImage("table.png")
		cardDownImage[2] = g.loadImage("b1fv.png")
		cardDownImage[0] = cardDownImage[2]
		cardDownImage[3] = g.loadImage("b1fh.png")
		cardDownImage[1] = cardDownImage[3]
		val image = g.getImage(cardImages[0].imageId[0])
		cardWidth = image.width
		cardHeight = image.height
	}

	fun startThread() {
		if (running) return
		running = true
		Thread(Runnable {
			try {
				var prevState: State? = State.GAME_OVER
				while (running) {
					kaiser.runGame()
					if (prevState !== kaiser.state) {
						println("went from state: " + prevState + " -> " + kaiser.state)
						prevState = kaiser.state
						kaiser.saveToFile(SAVE_FILE)
					}
					when (kaiser.state) {
						State.NEW_GAME, State.NEW_ROUND -> {
						}
						State.DEAL                      -> Thread.sleep(100)
						State.BID                       ->
							/** each player play's their card  */
							/** each player play's their card  */
							/** each player play's their card  */
							/** each player play's their card  */
							/** each player play's their card  */
							/** each player play's their card  */
							/** each player play's their card  */
							/** each player play's their card  */
							Thread.sleep(1000)
						State.TRICK                     -> Thread.sleep(1000)
						State.PROCESS_TRICK             -> Thread.sleep(3000)
						State.RESET_TRICK               -> {
						}
						State.PROCESS_ROUND             -> {
						}
						State.GAME_OVER                 -> {
						}
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
			running = false
		}).start()
	}

	override fun drawFrame(g: AGraphics) {
		this.clearScreen()
		g.drawImage(tableImageId, 0f, 0f, (screenHeight - 5).toFloat(), (screenHeight - 5).toFloat())
		if (!running) {
			drawGameReady(g)
			if (getMouseButtonClicked(0)) {
				startThread()
			}
		} else {
			when (kaiser.state) {
				State.NEW_GAME, State.NEW_ROUND, State.DEAL, State.BID, State.TRICK, State.PROCESS_TRICK, State.RESET_TRICK -> drawGame(g)
				State.PROCESS_ROUND -> drawTricksTaken(g)
				State.GAME_OVER -> if (getMouseButtonClicked(0)) {
					kaiser.newGame()
				}
			}
		}
	}

	private fun getPlayer(index: Int): SwingPlayer {
		return kaiser.getPlayer(index) as SwingPlayer
	}

	private fun drawTricksTaken(g: AGraphics) {
		var x = 15
		var y = 15
		for (i in 0 until Kaiser.numPlayers) {
			val p = kaiser.getPlayer(i)
			g.drawJustifiedString(x.toFloat(), y.toFloat(), Justify.LEFT, Justify.TOP, "${p.name} ${p.tricks.size} ${p.tricks.joinToString()}")
			y += g.textHeight.toInt()
			if (p.tricks.size === 0) continue
			val sx = x
			//for (ii in 0 until p.getNumTricks()) {
			for (trick in p.tricks) {
				val cards: List<Card> = trick.mCards
				val maxLenPixels = Math.round(cardWidth * 2)
				drawCards(g, cards, 0, cards.size, x, y, Angle.ANG_0, maxLenPixels, true)
				x += maxLenPixels + 5
				if (x + maxLenPixels > screenHeight) {
					x = sx
					y += (cardHeight + 2).toInt()
				}
			}
			x = sx
			y += (cardHeight + 5).toInt()
		}
	}

	private fun drawGame(g: AGraphics) {
		val padding: Int = 5
		val sh: Int = screenHeight
		val fh: Int = Math.round(g.getTextHeight() + 3)
		val x: FloatArray = floatArrayOf(
			padding * 2 + cardHeight,
			sh - cardHeight - padding,
			sh - (cardHeight * 2) - padding,
			padding
				.toFloat())
		val y: FloatArray = floatArrayOf(
			sh - cardHeight - (padding * 2),
			padding + cardHeight,
			padding.toFloat(),
			sh - (cardHeight * 2) - padding
		)
		// draw the trick cards that have been played
		val c: Int = sh / 2 // x/y center of board
		val d: Float = cardWidth / 2
		val tdx: FloatArray = floatArrayOf(-d, d, -d, -(d + cardHeight))
		val tdy: FloatArray = floatArrayOf(d, -d, -(d + cardHeight), -d)
		val maxCardsWidth: Int = screenHeight / 2
		val txtPadding: Int = 15
		val nx: FloatArray = floatArrayOf((screenHeight - txtPadding).toFloat(), (screenHeight - txtPadding).toFloat(), txtPadding.toFloat(), txtPadding.toFloat())
		val ny: FloatArray = floatArrayOf(y.get(0), txtPadding.toFloat(), txtPadding.toFloat(), y.get(0))
		val nhj: Array<Justify> = arrayOf(Justify.RIGHT, Justify.RIGHT, Justify.LEFT, Justify.LEFT)
		val nvj: Array<Justify> = arrayOf(Justify.TOP, Justify.TOP, Justify.TOP, Justify.TOP)
		for (i in 0..3) {
			drawPlayerHand(g, getPlayer(i), Math.round(x.get(i)), Math.round(y.get(i)), Angle.values().get(i), maxCardsWidth)
			var txt: String? = getPlayer(i).name
			if (kaiser.dealer == i) {
				txt += "\nDealer"
			}
			if (kaiser.startPlayer == i) {
				txt += "\nStart"
			}
			if (kaiser.state === State.PROCESS_TRICK && kaiser.trickWinnerIndex == i) {
				txt += "\nTrick Winner"
			}
			g.drawJustifiedString(nx.get(i), ny.get(i), nhj.get(i), nvj.get(i), txt)
			val trick: Card? = kaiser.getTrick(i)
			if (trick != null) {
				drawCard(g, trick, Math.round(c + tdx.get(i)), Math.round(c + tdy.get(i)), Angle.values().get(i), true)
			}
		}
		val t0: Team = kaiser.getTeam(0)
		val t1: Team = kaiser.getTeam(1)
		val text: String = ("Team ${t0.name} ${t0.bid}\n" +
			kaiser.getPlayer(t0.playerA).name + " " + kaiser.getPlayer(t0.playerB).name + "\n" +
			"\n" +
			"Points " + t0.totalPoints + "\n" +
			"Round  " + t0.totalPoints + "\n" +
			"\n" +
			"Team " + t1.name + " " + t1.bid + "\n" +
			kaiser.getPlayer(t1.playerA).name + " " + kaiser.getPlayer(t1.playerB).name + "\n" +
			"\n" +
			"Points " + t1.totalPoints + "\n" +
			"Round  " + t1.totalPoints + "\n")
		g.setColor(GColor.WHITE)
		val maxWidth: Int = screenWidth - screenHeight - 10
		val sx: Int = screenHeight + 5
		var sy: Float = 10f
		sy += g.drawWrapString(sx.toFloat(), sy, maxWidth.toFloat(), text).height
		val dy: Int = fh + 2
		sy += dy.toFloat()
		if (bidOptions.size > 0) {

			// draw the bid numbers
			var xx: Int = sx
			g.drawJustifiedString(sx.toFloat(), sy, Justify.LEFT, Justify.TOP, "Choose your bid")
			sy += dy.toFloat()
			val used: BooleanArray = BooleanArray(52)
			for (bid: Bid in bidOptions) {
				if (bid.numTricks <= 0) continue
				if (used[bid.numTricks]) continue
				used[bid.numTricks] = true
				xx += drawPickBidNumButton(g, xx.toFloat(), sy, bid.numTricks)
				if (xx >= screenWidth - 10) {
					xx = sx
					sy += dy.toFloat()
				}
			}
			sy += dy.toFloat()
			if (selectedBidNum > 0) {
				for (bid: Bid in bidOptions) {
					if (bid.numTricks == selectedBidNum) {
						if (drawPickBidSuitButton(g, sx.toFloat(), sy, bid.trump)) {
							pickedBid = bid
						}
						sy += dy.toFloat()
					}
				}
			}
			sy += dy.toFloat()
			if (selectedBidNum > 0 && selectedBidSuit != null) {
				if (drawPickButton(g, sx.toFloat(), sy, "BID " + selectedBidNum + " " + selectedBidSuit)) {
					for (bid: Bid in bidOptions) {
						if (bid.numTricks == selectedBidNum && bid.trump === selectedBidSuit) {
							pickedBid = bid
						}
					}
					lock.release()
				}
				sy += dy.toFloat()
			}
			if (drawPickButton(g, sx.toFloat(), sy, "NO BID")) {
				pickedBid = NO_BID
				lock.release()
			}
		}
	}

	private fun drawPickButton(g: AGraphics, x: Float, y: Float, text: String): Boolean {
		val padding = 2
		val wid = Math.round(g.getTextWidth(text))
		val hgt = g.textHeight
		val fontColor = GColor.WHITE
		var rectColor = GColor.CYAN
		var highlighted = false
		if (Utils.isPointInsideRect(mouseX.toFloat(), mouseY.toFloat(), x, y, (wid + padding * 2).toFloat(), hgt + padding * 2)) {
			rectColor = GColor.YELLOW
			highlighted = true
		}
		g.color = fontColor
		g.drawJustifiedString(x + padding, y + padding, Justify.LEFT, Justify.TOP, text)
		g.color = rectColor
		g.drawRect(x, y, (wid + padding * 2).toFloat(), hgt + padding * 2, 1f)
		return highlighted && getMouseButtonClicked(0)
	}

	private var selectedBidNum = -1
	private fun drawPickBidNumButton(g: AGraphics, x: Float, y: Float, num: Int): Int {
		val text = "" + num
		val padding = 2
		val wid = Math.round(g.getTextWidth("00"))
		val hgt = g.textHeight
		val fontColor = GColor.WHITE
		var rectColor = GColor.CYAN
		var highlighted = false
		if (num == selectedBidNum) {
			rectColor = GColor.RED
		} else if (Utils.isPointInsideRect(mouseX.toFloat(), mouseY.toFloat(), x, y, (wid + padding * 2).toFloat(), hgt + padding * 2)) {
			highlighted = true
			rectColor = GColor.YELLOW
		}
		g.color = fontColor
		g.drawJustifiedString(x + padding, y + padding, Justify.LEFT, Justify.TOP, text)
		g.color = rectColor
		g.drawRect(x, y, (wid + padding * 2).toFloat(), hgt + padding * 2, 1f)
		if (highlighted && getMouseButtonClicked(0)) {
			selectedBidNum = num
		}
		return wid + (padding * 2) + 2
	}

	private var selectedBidSuit: Suit? = null
	private fun drawPickBidSuitButton(g: AGraphics, x: Float, y: Float, suit: Suit): Boolean {
		val text = suit.name
		val padding = 2
		var wid = 0
		for (s: Suit in Suit.values()) {
			val w = Math.round(g.getTextWidth(s.name))
			if (w > wid) wid = w
		}
		//g.getTextWidth("WW");
		val hgt = g.textHeight
		val fontColor = GColor.WHITE
		var rectColor = GColor.CYAN
		var highlighted = false
		if (suit === selectedBidSuit) {
			rectColor = GColor.RED
		} else if (Utils.isPointInsideRect(mouseX.toFloat(), mouseY.toFloat(), x, y, (wid + padding * 2).toFloat(), hgt + padding * 2)) {
			highlighted = true
			rectColor = GColor.YELLOW
		}
		g.color = fontColor
		g.drawJustifiedString(x + padding, y + padding, Justify.LEFT, Justify.TOP, text)
		g.color = rectColor
		g.drawRect(x, y, (wid + padding * 2).toFloat(), hgt + padding * 2, 1f)
		if (highlighted && getMouseButtonClicked(0)) {
			selectedBidSuit = suit
			return true
		}
		return false
	}

	override fun mouseClicked(evt: MouseEvent) {
		when (kaiser.state) {
			State.NEW_GAME,
			State.NEW_ROUND,
			State.PROCESS_TRICK,
			State.RESET_TRICK,
			State.PROCESS_ROUND,
			State.GAME_OVER -> lock.releaseAll()
			else                                                                                                          -> {
			}
		}
		super.mouseClicked(evt)
	}

	private fun drawPlayerHand(g: AGraphics, player: SwingPlayer, x: Int, y: Int, angle: Angle, maxLenPixels: Int) {
		drawCards(g, player.hand, 0, player.hand.size, x, y, angle, maxLenPixels, player.isCardsShowing())
	}

	private fun drawGameReady(g: AGraphics) {
		val cards: List<Card> = kaiser.deck
		val x = 20
		var y = 10
		val width = screenWidth - 20
		drawCards(g, cards, 0, 8, x, y, Angle.ANG_0, width, true)
		y += (cardHeight + 5).toInt()
		drawCards(g, cards, 8, 8, x, y, Angle.ANG_0, width, true)
		y += (cardHeight + 5).toInt()
		drawCards(g, cards, 16, 8, x, y, Angle.ANG_0, width, true)
		y += (cardHeight + 5).toInt()
		drawCards(g, cards, 24, 8, x, y, Angle.ANG_0, width, true)
		g.color = GColor.BLUE
		g.drawJustifiedString((screenWidth / 2).toFloat(), (screenHeight / 2).toFloat(), Justify.CENTER, Justify.CENTER,
			"Click Mouse button to start")
	}

	private val pickableCards: MutableList<Card> = ArrayList()
	private var pickedCard = -1 //this is the index in pickableCards
	private val bidOptions: MutableList<Bid> = ArrayList()
	private var pickedBid: Bid? = null
	private fun getPickableIndex(card: Card): Int {
		return pickableCards.indexOf(card)
	}

	private fun drawCards(g: AGraphics, cards: List<Card>, offset: Int, len: Int, x: Int, y: Int, angle: Angle, maxLenPixels: Int, showing: Boolean) {
		var x = x
		var y = y
		if (len <= 0) return
		if (showing) pickedCard = -1
		var dx: Float = 0f
		var dy: Float = 0f
		when (angle) {
			Angle.ANG_0 -> {
				dx = (maxLenPixels - cardWidth) / len
				if (dx > cardWidth / 4) dx = cardWidth / 4
			}
			Angle.ANG_180 -> {
				dx = (maxLenPixels - cardWidth) / len
				if (dx > cardWidth / 4) dx = cardWidth / 4
				dx = -dx
			}
			Angle.ANG_90 -> {
				dy = (maxLenPixels - cardWidth) / len
				if (dy > cardWidth / 4) dy = cardWidth / 4
			}
			Angle.ANG_270 -> {
				dy = (maxLenPixels - cardWidth) / len
				if (dy > cardWidth / 4) dy = cardWidth / 4
				dy = -dy
			}
		}
		var picked: Int = -1
		if (showing) {
			// search backwards to see if a card is picked
			var sx: Float = x + dx * (len - 1)
			var sy: Float = y + dy * (len - 1)
			for (i in len - 1 downTo 0) {
				pickedCard = getPickableIndex(cards.get(i))
				if (pickedCard >= 0 && Utils.isPointInsideRect(getMouseX().toFloat(), getMouseY().toFloat(), sx, sy, cardWidth, cardHeight)) {
					picked = i
					break
				}
				pickedCard = -1
				sx -= dx
				sy -= dy
			}
		}

		//System.out.println("card picked1 = " + cardPicked);
		for (i in 0 until len) {
			var sy: Int = y
			if (i == picked) sy -= 20
			drawCard(g, cards.get(i + offset), x, sy, angle, showing)
			x += dx.toInt()
			y += dy.toInt()
		}
		if (pickedCard >= 0 && getMouseButtonClicked(0)) {
			lock.release()
		}
	}

	private fun drawCard(g: AGraphics, card: Card, x: Int, y: Int, angle: Angle, showing: Boolean) {
		var cw = cardWidth
		var ch = cardHeight
		when (angle) {
			Angle.ANG_90, Angle.ANG_270 -> {
				cw = cardHeight
				ch = cardWidth
			}
			else                        -> {
			}
		}
		if (showing) {
			val imgId = getCardImage(card.rank, card.suit, angle)
			if (imgId > 0)
				g.drawImage(imgId, x.toFloat(), y.toFloat(), cw, ch)
		} else g.drawImage(cardDownImage[angle.ordinal], x.toFloat(), y.toFloat(), cw, ch)
	}

	private fun getCardImage(rank: Rank, suit: Suit, angle: Angle): Int {
		for (c: CardImage in cardImages) {
			if (c.rank === rank && c.suit === suit) return c.imageId[angle.ordinal]
		}
		return 0
	}

	override fun onDimensionsChanged(g: AGraphics, width: Int, height: Int) {
		Utils.println("Dimensions changed to $width X $height")
		g.ortho()
		loadImages(g)
		//float aspectRatio = (float)cardWidth/cardHeight;
		val aspectRatio = cardHeight / cardWidth
		cardWidth = ((screenHeight / 3) / 3 - 3).toFloat()
		cardHeight = Math.round(cardWidth * aspectRatio).toFloat()
	}

	fun pickCard(options: Array<Card>): Card? {
		pickableCards.clear()
		pickableCards.addAll(options)
		lock.acquireAndBlock()
		pickableCards.clear()
		return if (pickedCard >= 0) {
			options.get(pickedCard)
		} else null
	}

	fun pickBid(options: Array<Bid>): Bid? {
		selectedBidNum = -1
		selectedBidSuit = null
		bidOptions.clear()
		bidOptions.addAll(options)
		lock.acquireAndBlock()
		bidOptions.clear()
		return pickedBid
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			//Utils.DEBUG_ENABLED = true;
			//Golf.DEBUG_ENABLED = true;
			//PlayerBot.DEBUG_ENABLED = true;
			val settings = FileUtils.getOrCreateSettingsDirectory(KaiserApplet::class.java)
			val frame = AWTFrame("Kaiser")
			val app: AWTKeyboardAnimationApplet = KaiserApplet()
			frame.add(app)
			app.init()
			if (!frame.loadFromFile(File(settings, "gui.properties"))) {
				frame.centerToScreen(800, 600)
			}
			app.start()
			app.setMillisecondsPerFrame(20)
		}
	}
}