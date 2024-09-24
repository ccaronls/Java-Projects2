package cc.lib.monopoly

import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.IVector2D
import cc.lib.game.Justify
import cc.lib.logger.LoggerFactory
import cc.lib.math.Bezier
import cc.lib.math.CMath
import cc.lib.math.Matrix3x3
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.GException
import cc.lib.utils.Table
import cc.lib.utils.increment
import cc.lib.utils.prettify
import cc.lib.utils.random
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min
import kotlin.math.roundToInt

abstract class UIMonopoly : Monopoly() {
	
	private val lock = ReentrantLock()
	private val cond = lock.newCondition()
	
	var isGameRunning = false
		private set
	var W = 0
	var H = 0
	@JvmField
    var DIM = 0
	private lateinit var board: Board
	private var playerInfoWidth = 0f
	private var playerInfoHeight = 0f
	private val animations: MutableMap<String, MutableList<AAnimation<AGraphics>>> = HashMap()
	private val spriteMap: MutableMap<String, Sprite> = HashMap()

	/**
	 *
	 */
	abstract fun repaint()

	/**
	 *
	 * @param p
	 * @return
	 */
	abstract fun getImageId(p: Piece): Int

	/**
	 *
	 * @return
	 */
	abstract val boardImageId: Int

	/**
	 *
	 * @param player
	 * @param moves
	 * @return
	 */
	abstract fun showChooseMoveMenu(player: Player, moves: List<MoveType>): MoveType?

	/**
	 *
	 * @param player
	 * @param cards
	 * @param type
	 * @return
	 */
	abstract fun showChooseCardMenu(player: Player, cards: List<Card>, type: Player.CardChoiceType): Card?

	/**
	 *
	 * @param player
	 * @param trades
	 * @return
	 */
	abstract fun showChooseTradeMenu(player: Player, trades: List<Trade>): Trade?

	/**
	 *
	 * @param user
	 * @param sellable
	 * @return
	 */
	abstract fun showMarkSellableMenu(user: PlayerUser, sellable: List<Card>): Boolean

	/**
	 *
	 */
	override fun onDiceRolled() {
		addAnimation("GAME", object : AAnimation<AGraphics>(2000) {
			var delay: Long = 10
			var die1 = random(1..6)
			var die2 = random(1..6)
			override fun onStarted(g: AGraphics) {
				object : Thread() {
					override fun run() {
						while (!isDone) {
							sleep(delay)
							delay += 20
							die1 = random(1..6)
							die2 = random(1..6)
						}
					}
				}.start()
			}

			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.pushMatrix()
				g.translate((W / 2).toFloat(), (H / 8).toFloat())
				drawDice(g, die1, die2, (DIM / 10).toFloat())
				g.popMatrix()
			}

			public override fun onDone() {
				lock.withLock { 
					cond.signal()
				}
			}
		}.start())
		lock.withLock { 
			cond.await(3, TimeUnit.SECONDS)
		}
		super.onDiceRolled()
	}

	override fun onPlayerMove(playerNum: Int, numSquares: Int, next: Square) {
		setSpriteAnim(getPlayer(playerNum).piece.name, JumpAnimation(playerNum, numSquares).start())
		lock.withLock { 
			cond.await(numSquares*600L, TimeUnit.MILLISECONDS)
		}
		super.onPlayerMove(playerNum, numSquares, next)
	}

	private fun renderMessage(g: AGraphics, title: String, txt: String, render: Boolean): GRectangle {
		val border = 5f
		val width = DIM / 3 + border * 2
		val lines = g.generateWrappedLines(txt, width - border * 2)
		//GDimension dim = //g.drawWrapStringOnBackground(DIM/2, DIM/2, DIM/3, Justify.CENTER, Justify.CENTER, txt, GColor.WHITE, 5);
		val txtHgt = g.textHeight
		var height = txtHgt * lines.size
		height += txtHgt + 4 * border
		var x = DIM / 2 - width / 2
		var y = DIM / 2 - height / 2
		if (render) {
			g.color = GColor.BLACK
			g.drawFilledRect(x - border, y - border, width + border * 2, height + border * 2)
			g.color = GColor.WHITE
			g.drawFilledRect(x, y, width, height)
			g.color = GColor.BLACK
			y += border
			x = (DIM / 2).toFloat() // center
			g.drawJustifiedString(x, y, Justify.CENTER, Justify.TOP, title)
			y += txtHgt
			y += border
			g.drawLine(x - width / 2, y, x + width / 2, y, 4f)
			y += border
			for (line in lines) {
				g.drawJustifiedString(x, y, Justify.CENTER, Justify.TOP, line)
				y += txtHgt
			}
		}
		return GRectangle(x, y, width, height)
	}

	private fun showMessage(title: String, txt: String) {
		addAnimation("BOARD", object : AAnimation<AGraphics>(4000) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				renderMessage(g, title, txt, true)
			}

			override fun onDone() {
				lock.withLock { 
					cond.signal()
				}
			}
		}.start())
		lock.withLock { 
			cond.await(5, TimeUnit.SECONDS)
		}
	}

	internal inner class TurnOverCardAnim(val start: Array<Vector2D>, val color: GColor, val title: String, val msg: String) : AAnimation<AGraphics>(1500) {
		lateinit var dest: Array<Vector2D>

		override fun onStarted(g: AGraphics) {
			super.onStarted(g)
			val r = renderMessage(g, title, msg, false)
			dest = arrayOf(
				Vector2D(r.x, r.y + r.h),
				Vector2D(r.x + r.w, r.y + r.h),
				Vector2D(r.x + r.w, r.y),
				Vector2D(r.x, r.y)
			)
		}

		override fun draw(g: AGraphics, position: Float, dt: Float) {
			g.pushMatrix()
			if (position < 0.5f) g.color = color else g.color = GColor.WHITE
			g.begin()
			for (i in 0..3) {
				val s: Vector2D = start[i].scaledBy(board.scale)
				g.vertex(s.add(dest[i].sub(s).scaledBy(position)))
			}
			g.drawTriangleFan()
			g.popMatrix()
		}

		override fun onDone() {
			lock.withLock { cond.signal() }
		}
	}

	override fun onPlayerDrawsChance(playerNum: Int, chance: CardActionType) {
		addAnimation("BOARD", TurnOverCardAnim(Board.CHANCE_RECT, Board.CHANCE_ORANGE, "Chance", chance.description).start())
		lock.withLock { cond.await(3000, TimeUnit.MILLISECONDS) }
		showMessage("Chance", chance.description)
		super.onPlayerDrawsChance(playerNum, chance)
	}

	override fun onPlayerDrawsCommunityChest(playerNum: Int, commChest: CardActionType) {
		addAnimation("BOARD", TurnOverCardAnim(Board.COMM_CHEST_RECT, Board.COMM_CHEST_BLUE, "Community Chest", commChest.description).start())
		lock.withLock { cond.await(3000, TimeUnit.MILLISECONDS) }
		showMessage("Community Chest", commChest.description)
		super.onPlayerDrawsCommunityChest(playerNum, commChest)
	}

	override fun onPlayerGotPaid(playerNum: Int, amt: Int) {
		setSpriteAnim("PLAYER$playerNum", MoneyAnim(getPlayer(playerNum).money, amt).start())
		lock.withLock { cond.await(MONEY_PAUSE, TimeUnit.MILLISECONDS) }
		super.onPlayerGotPaid(playerNum, amt)
	}

	override fun onPlayerReceiveMoneyFromAnother(playerNum: Int, giverNum: Int, amt: Int) {
		setSpriteAnim("PLAYER$playerNum", MoneyAnim(getPlayer(playerNum).money, amt).start())
		setSpriteAnim("PLAYER$giverNum", MoneyAnim(getPlayer(giverNum).money, -amt).start())
		lock.withLock { cond.await(MONEY_PAUSE, TimeUnit.MILLISECONDS) }
		super.onPlayerReceiveMoneyFromAnother(playerNum, giverNum, amt)
	}

	override fun onPlayerPayMoneyToKitty(playerNum: Int, amt: Int) {
		setSpriteAnim("PLAYER$playerNum", MoneyAnim(getPlayer(playerNum).money, -amt).start())
		setSpriteAnim(Square.FREE_PARKING.name, MoneyAnim(kitty, amt).start())
		lock.withLock { cond.await(MONEY_PAUSE, TimeUnit.MILLISECONDS) }
		super.onPlayerPayMoneyToKitty(playerNum, amt)
	}

	override fun onPlayerGoesToJail(playerNum: Int) {
		val p = getPlayer(playerNum)
		val start = board.getPiecePlacement(playerNum, p.square)
		val end = board.getPiecePlacementJail(playerNum)
		addAnimation("PLAYER$playerNum", JailedAnim(playerInfoWidth, playerInfoHeight).start<AAnimation<AGraphics>>())
		setSpriteAnim(p.piece.name, object : AAnimation<Sprite>(2000) {
			val curve = Bezier()
			override fun onStarted(g: Sprite) {
				val s = start.topLeft
				val e = end.topLeft
				curve.addPoint(s)
				val dv = e.sub(s)
				val len = dv.mag()
				val n = MutableVector2D(0f, len / 3)
				curve.addPoint(s.add(dv.scaledBy(.33f)).add(n))
				curve.addPoint(s.add(dv.scaledBy(.66f)).add(n))
				curve.addPoint(e)
			}

			override fun draw(s: Sprite, position: Float, dt: Float) {
				s.M.setTranslate(curve.getAtPosition(position))
			}

			override fun onDone() {
				lock.withLock { cond.signal() }
			}
		}.start())
		lock.withLock { cond.await(5000, TimeUnit.MILLISECONDS) }
		super.onPlayerGoesToJail(playerNum)
	}

	override fun onPlayerOutOfJail(playerNum: Int) {
		setSpriteAnim("PLAYER$playerNum", object : AAnimation<Sprite>(500) {
			lateinit var start: Vector2D
			lateinit var end: Vector2D

			override fun onStarted(g: Sprite) {
				start = board.getPiecePlacementJail(playerNum).topLeft
				end = board.getPiecePlacement(playerNum, Square.VISITING_JAIL).topLeft
			}

			override fun draw(g: Sprite, position: Float, dt: Float) {
				g.M.translate(start.add(end.sub(start).scaledBy(position)))
			}
		}.start())
		addAnimation("PLAYER$playerNum", JailedAnim(playerInfoWidth, playerInfoHeight).startReverse<AAnimation<AGraphics>>())
		lock.withLock { cond.await(5000, TimeUnit.MILLISECONDS) }
		super.onPlayerOutOfJail(playerNum)
	}

	override fun onPlayerPaysRent(playerNum: Int, renterNum: Int, amt: Int) {
		setSpriteAnim("PLAYER$playerNum", MoneyAnim(getPlayer(playerNum).money, -amt).start())
		setSpriteAnim("PLAYER$renterNum", MoneyAnim(getPlayer(renterNum).money, amt).start())
		lock.withLock { cond.await(MONEY_PAUSE, TimeUnit.MILLISECONDS) }
		super.onPlayerPaysRent(playerNum, renterNum, amt)
	}

	override fun onPlayerMortgaged(playerNum: Int, property: Square, amt: Int) {
		setSpriteAnim("PLAYER$playerNum", MoneyAnim(getPlayer(playerNum).money, amt).start())
		lock.withLock { cond.await(MONEY_PAUSE, TimeUnit.MILLISECONDS) }
		super.onPlayerMortgaged(playerNum, property, amt)
	}

	override fun onPlayerUnMortgaged(playerNum: Int, property: Square, amt: Int) {
		setSpriteAnim("PLAYER$playerNum", MoneyAnim(getPlayer(playerNum).money, -amt).start())
		lock.withLock { cond.await(MONEY_PAUSE, TimeUnit.MILLISECONDS) }
		super.onPlayerUnMortgaged(playerNum, property, amt)
	}

	internal inner class PropertyAnimation(val property: Square, val buyerNum: Int) : AAnimation<AGraphics>(6000) {
		override fun draw(g: AGraphics, position: Float, dt: Float) {
			val maxWidth = g.viewport.minLength()/3
			val topY = g.viewport.minLength()/5
			drawPropertyCard(g, maxWidth, topY, property, if (elapsedTime > 1000) getPlayerName(buyerNum) else null)
		}
	}

	override fun onPlayerPurchaseProperty(playerNum: Int, property: Square) {
		setSpriteAnim("PLAYER$playerNum", MoneyAnim(getPlayer(playerNum).money, -property.price).start())
		addAnimation("BOARD", PropertyAnimation(property, playerNum).start<AAnimation<AGraphics>>())
		lock.withLock { cond.await(5000, TimeUnit.MILLISECONDS) }
		super.onPlayerPurchaseProperty(playerNum, property)
	}

	override fun onPlayerTrades(buyer: Int, seller: Int, property: Square, amount: Int) {
		setSpriteAnim("PLAYER$seller", MoneyAnim(getPlayer(seller).money, amount).start())
		addAnimation("BOARD", PropertyAnimation(property, buyer).start<AAnimation<AGraphics>>())
		//onPlayerPurchaseProperty(buyer, property);
		setSpriteAnim("PLAYER$buyer", MoneyAnim(getPlayer(buyer).money, -amount).start())
		super.onPlayerTrades(buyer, seller, property, amount)
	}

	override fun onPlayerBoughtHouse(playerNum: Int, property: Square, amt: Int) {
		addAnimation("BOARD", object : AAnimation<AGraphics>(HOUSE_PAUSE) {
			lateinit var v0: Vector2D
			lateinit var v1: Vector2D
			override fun onStarted(g: AGraphics) {
				v0 = Vector2D((DIM / 2).toFloat(), (DIM / 2).toFloat())
				v1 = board.getInnerEdge(property)
			}

			override fun draw(g: AGraphics, position: Float, dt: Float) {
				val v: Vector2D = v0.add(v1.sub(v0).scaledBy(position))
				val s = (DIM / 20).toFloat()
				g.pushMatrix()
				g.translate(v)
				g.color = HOUSE_COLOR
				g.scale(s * (.2f+(1.0f - position)))
				drawHouse(g)
				g.popMatrix()
			}

			override fun onDone() {
				setSpriteAnim("PLAYER$playerNum", MoneyAnim(getPlayer(playerNum).money, -amt).start())
				setSpriteAnim(property.name, ErectHouseAnim().start())
			}
		}.start())
		lock.withLock { cond.await(10000, TimeUnit.MILLISECONDS) }
		super.onPlayerBoughtHouse(playerNum, property, amt)
	}

	override fun onPlayerBoughtHotel(playerNum: Int, property: Square, amt: Int) {
		addAnimation("BOARD", object : AAnimation<AGraphics>(HOUSE_PAUSE) {
			lateinit var v0: Vector2D
			lateinit var v1: Vector2D
			override fun onStarted(g: AGraphics) {
				v0 = Vector2D((DIM / 2).toFloat(), (DIM / 2).toFloat())
				v1 = board.getInnerEdge(property)
			}

			override fun draw(g: AGraphics, position: Float, dt: Float) {
				val v: Vector2D = v0.add(v1.sub(v0).scaledBy(position))
				val sx = (DIM / 12).toFloat()
				val sy = (DIM / 10).toFloat()
				g.pushMatrix()
				g.translate(v)
				g.color = HOTEL_COLOR
				g.scale(sx, sy)
				g.scale(1.0f - position)
				drawHouse(g)
				g.popMatrix()
			}

			override fun onDone() {
				setSpriteAnim("PLAYER$playerNum", MoneyAnim(getPlayer(playerNum).money, -amt).start())
				setSpriteAnim(property.name, ErectHouseAnim().start())
			}
		}.start())
		lock.withLock { cond.await(10000, TimeUnit.MILLISECONDS) }
		super.onPlayerBoughtHotel(playerNum, property, amt)
	}

	override fun onPlayerBankrupt(playerNum: Int) {
		addAnimation("PLAYER$playerNum", object : AAnimation<AGraphics>(1000) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				val rect = g.clipRect
				g.color = GColor.TRANSLUSCENT_BLACK
				g.drawFilledRect(rect.x + (1 - position) * rect.w / 2, rect.y, rect.w * position, rect.h)
			}

			override fun onDone() {
				lock.withLock { cond.signal() }
			}
		}.start())
		lock.withLock { cond.await(2000, TimeUnit.MILLISECONDS) }
		super.onPlayerBankrupt(playerNum)
	}

	override fun onPlayerWins(playerNum: Int) {
		showMessage("WINNER", getPlayerName(playerNum) + " IS THE WINNER!")
		addAnimation("PLAYER$playerNum", object : AAnimation<AGraphics>(500, -1) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				val r = g.clipRect
				g.setColor(random(256), random(256), random(256), 255)
				g.drawRect(r, 3f)
			}
		}.start())
		super.onPlayerWins(playerNum)
	}

	fun addAnimation(key: String, a: AAnimation<AGraphics>) {
		if (!isGameRunning) return
		synchronized(animations) {
			var list = animations[key]
			if (list == null) {
				list = LinkedList()
				animations[key] = list
			}
			list.add(a)
		}
		repaint()
	}

	fun stopAnimations() {
		synchronized(animations) { animations.clear() }
		for (sprite in spriteMap.values) {
			sprite.animation?.kill()
		}
	}

	fun setSpriteAnim(key: String, anim: AAnimation<Sprite>) {
		if (!isGameRunning) return
		spriteMap[key]?.animation = anim
		repaint()
	}

	fun onClick() {
		stopAnimations()
		lock.withLock { cond.signal() }
	}

	fun startDrag() {}
	fun stopDrag() {}

	/**
	 * Sprites solve the problem of rendering something that can be animated or static.
	 * Transforms:
	 * - position
	 * - orientation
	 * - scale
	 * - color
	 *
	 */
	abstract inner class Sprite {
		var animation: AAnimation<Sprite>? = null
		val M = Matrix3x3.newIdentity()
		var color = GColor.BLACK
		var data1 = 0
		var data2 = 0
		fun animateAndDraw(g: AGraphics, w: Float, h: Float) {
			animation?.takeIf { !it.isDone }?.update(this)
			g.pushMatrix()
			g.multMatrix(M)
			val c = g.color
			g.color = color
			draw(g, w, h)
			g.color = c
			g.popMatrix()
			if (isAnimating) repaint()
		}

		val isAnimating: Boolean
			get() = animation?.isDone==false

		/**
		 * Draw the sprite at origin facing at angle 0. Translations will orient and move according
		 * @param g
		 * @param w
		 * @param h
		 */
		abstract fun draw(g: AGraphics, w: Float, h: Float)
	}

	fun initSprites() {
		// the pieces on the board can be animated
		for (p in Piece.values()) {
			spriteMap[p.name] = object : Sprite() {
				override fun draw(g: AGraphics, w: Float, h: Float) {
					g.drawImage(getImageId(p), 0f, 0f, w, h)
				}
			}
		}

		// a players current money can be animated
		for (i in 0 until MAX_PLAYERS) {
			spriteMap["PLAYER$i"] = object : Sprite() {
				override fun draw(g: AGraphics, w: Float, h: Float) {
					val p = getPlayer(i)
					val value = p.cardsForMortgage.sumBy { it.property.getMortgageValue(it.houses) }
					var money = p.money
					animation?.let {
						money = data1
						val textHeight = g.textHeight
						val amt = if (data2 < 0) data2.toString() else "+$data2"
						val delta = Vector2D(0f, textHeight * it.position)
						g.pushMatrix()
						g.translate(delta)
						g.color = (if (data2 > 0) Board.GREEN else Board.RED).withAlpha(it.position)
						g.drawJustifiedString(0f, textHeight, Justify.RIGHT, Justify.CENTER, amt)
						g.popMatrix()
					}
					g.color = color
					g.drawJustifiedString(0f, 0f, Justify.RIGHT, Justify.CENTER, "($$value)\n$$money")
				}
			}
		}

		// The kitty can be animated
		spriteMap[Square.FREE_PARKING.name] = object : Sprite() {
			override fun draw(g: AGraphics, w: Float, h: Float) {
				if (kitty > 0 || isAnimating) {
					g.pushTextHeight(TEXT_HEIGHT_SM, false)
					g.pushColor(GColor.TRANSPARENT)
					val dim = g.drawWrapString(0f, 0f, w, Justify.CENTER, Justify.CENTER, """
					    Kitty
					    $${kitty}
					    """.trimIndent())
					g.color = GColor.TRANSLUSCENT_BLACK
					g.drawFilledRect(-dim.width / 2 - 5, -dim.height / 2 - 5, dim.width + 10, dim.height + 10)
					var money: Int = kitty
					animation?.takeIf { !it.isDone }?.let { animation ->
						money = data1
						val textHeight = g.textHeight
						val amt = if (data2 < 0) data2.toString() else "+$data2"
						animation.position
						val dir = CMath.signOf(-data2.toFloat())
						val delta = Vector2D(0f, textHeight * animation.position * dir)
						g.pushMatrix()
						g.translate(0f, textHeight * dir)
						g.translate(delta)
						g.color = (if (data2 > 0) GColor.GREEN else GColor.RED).withAlpha(1f - animation.position)
						g.drawJustifiedString(0f, 0f, Justify.RIGHT, Justify.CENTER, amt)
						g.popMatrix()
					}
					g.color = color
					g.drawWrapString(0f, 0f, w, Justify.CENTER, Justify.CENTER, "Kitty\n$$money")
					g.popTextHeight()
					g.popColor()
				}
			}
		}

		// the property squares can animate building houses
		for (sq in Square.values()) {
			if (sq.isProperty) {
				spriteMap[sq.name] = object : Sprite() {
					override fun draw(g: AGraphics, w: Float, h: Float) {
						val r = board.getSqaureBounds(sq)
						val houseScale: Float = min(r.w, r.h) / 15
						g.pushMatrix()
						val houses = data1
						var v: Vector2D? = null
						var angle = 0
						when (board.getSquarePosition(sq)) {
							Board.Position.TOP -> {
								v = Vector2D(r.x + r.w / 2, r.y + r.h - houseScale)
								angle = 0 //, houseScale, houses);
							}
							Board.Position.RIGHT -> {
								v = Vector2D(r.x + houseScale, r.y + r.h / 2)
								angle = 270 //, houseScale, houses);
							}
							Board.Position.BOTTOM -> {
								v = Vector2D(r.x + r.w / 2, r.y + houseScale)
								angle = 0 //, houseScale, houses);
							}
							Board.Position.LEFT -> {
								v = Vector2D(r.x + r.w - houseScale, r.y + r.h / 2)
								angle = 90 //, houseScale, houses);
							}
							else -> Unit
						}
						g.translate(v)
						g.rotate(angle.toFloat())
						g.multMatrix(M)
						animation?.takeIf { !it.isDone }?.also { animation ->
							log.debug("${sq.name} : houses = $houses")
							when (houses) {
								0, 1, 2, 3 -> {
									g.scale(houseScale * 2)
									g.color = HOUSE_COLOR
									g.translate(-HOUSE_RADIUS * (houses - 1), 0f)
									g.translate(-HOUSE_RADIUS * animation.position, 0f)
									for (i in 0 until houses) {
										drawHouse(g)
										g.translate(HOUSE_RADIUS * 2, 0f)
									}
									g.scale(animation.position)
									drawHouse(g)
								}
								4 -> {
									g.pushMatrix()
									g.scale(houseScale * 2, houseScale * 2 * (1.0f - animation.position))
									g.color = HOUSE_COLOR
									g.translate(-HOUSE_RADIUS * (houses - 1), 0f)
									for (i in 0 until houses) {
										drawHouse(g)
										g.translate(HOUSE_RADIUS * 2, 0f)
									}
									g.popMatrix()
									g.scale(animation.position * houseScale * 4)
									g.color = HOTEL_COLOR
									drawHouse(g)
								}
							}
						}?:run {
							when (houses) {
								5    -> {
									g.scale(houseScale * 4)
									g.color = HOTEL_COLOR
									drawHouse(g)
								}
								else -> {
									g.color = HOUSE_COLOR
									g.scale(houseScale * 2)
									g.translate(-HOUSE_RADIUS * (houses - 1), 0f)
									for (i in 0 until houses) {
										drawHouse(g)
										g.translate(HOUSE_RADIUS * 2, 0f)
									}
								}
							}
						}
						g.popMatrix()
					}
				}
			}
		}
	}

	internal inner class ErectHouseAnim() : AAnimation<Sprite>(HOUSE_PAUSE) {
		override fun draw(g: Sprite, position: Float, dt: Float) {}

		override fun onDone() {
			lock.withLock { cond.signal() }
		}
	}

	internal inner class JailedAnim(val width: Float, val height: Float) : AAnimation<AGraphics>(2000) {
		override fun draw(g: AGraphics, position: Float, dt: Float) {
			drawJail(g, width, height, position)
		}

		override fun onDone() {
			lock.withLock { cond.signal() }
		}
	}

	private fun drawJail(g: AGraphics, w: Float, h: Float, position: Float) {
		val barWidth = 5f
		val barSpacing = 20f
		var x = 5f
		while (x < w) {
			g.color = GColor.BLACK
			g.drawFilledRect(x, 0f, barWidth, h * position)
			x += barSpacing + barWidth
		}
	}

	internal inner class MoneyAnim(startMoney: Int, val delta: Int) : AAnimation<Sprite>(2500) {
		val startMoney: Float
		override fun draw(g: Sprite, position: Float, dt: Float) {
			g.data1 = (startMoney + position * delta).roundToInt()
			g.data2 = delta
		}

		init {
			this.startMoney = startMoney.toFloat()
		}
	}

	internal inner class JumpAnimation(var playerNum: Int, jumps: Int) : AAnimation<Sprite>(500) {
		var start: Square
		var jumps: Int
		var dir: Int
		var curve: Bezier
		fun init() {
			val r0 = board.getPiecePlacement(playerNum, start)
			var steps = 1
			if (jumps > 5) {
				steps = 5
				start = start.increment(5) // TEST BEFORE CHECKING IN!, Square.values()) // make bigger steps when a long way to jump
			} else if (jumps < 0) {
				start = start.increment(-1) // TEST BEFORE CHECKING IN!
			} else {
				start = start.increment(1) // TEST BEFORE CHECKING IN!
			}
			val r1 = board.getPiecePlacement(playerNum, start)
			curve.reset()
			curve.addPoint(r0.x, r0.y)
			val dx = r1.x - r0.x
			val dy = r1.y - r0.y
			curve.addPoint(r0.x + dx / 3, (r0.y + dy * 0.33f + dx / 6).coerceIn(0f, DIM.toFloat()))
			curve.addPoint(r0.x + dx * 2 / 3, (r0.y + dy * 0.66f + dx / 6).coerceIn(0f, DIM.toFloat()))
			curve.addPoint(r1.x, r1.y)
			jumps -= dir * steps
		}

		override fun draw(sp: Sprite, position: Float, dt: Float) {
			sp.M.setTranslate(curve.getAtPosition(position))
		}

		override fun onDone() {
			if (jumps != 0) {
				init()
				start<AAnimation<Sprite>>()
			} else {
				lock.withLock { cond.signal() }
			}
		}

		init {
			start = getPlayer(playerNum).square
			this.jumps = jumps
			dir = CMath.signOf(jumps.toFloat())
			curve = Bezier()
			init()
		}
	}

	@Synchronized
	fun startGameThread() {
		if (isGameRunning)
			return
		log.debug("startGameThread")
		isGameRunning = true
		object : Thread() {
			override fun run() {
				log.debug("ENTER game thread")
				while (isGameRunning && winner < 0) {
					try {
						runGame()
					} catch (t: Throwable) {
						onError(t)
						break
					}
					repaint()
					sleep(100)
				}
				log.debug("EXIT game thread")
				isGameRunning = false
			}
		}.start()
	}

	protected open fun onError(t: Throwable) {
		throw RuntimeException(t)
	}

	fun stopGameThread() {
		log.debug("stopGameThread")
		isGameRunning = false
		stopAnimations()
		lock.withLock { 
			cond.signalAll()
		}
		log.debug("GAME THREAD STOPPED")
	}

	fun initPlayers(num: Int, pc: Piece) {
		clear()
		val user = PlayerUser()
		user.piece = pc
		addPlayer(user)
		for (i in 1 until num)
			addPlayer(Player(unusedPieces.random()))
	}

	fun paint(g: APGraphics, mouseX: Int, mouseY: Int) {
		g.clearScreen(Board.BOARD_COLOR)
		W = g.viewportWidth
		H = g.viewportHeight
		DIM = W.coerceAtMost(H)
		board = Board(DIM.toFloat())
		g.setTextStyles(AGraphics.TextStyle.BOLD)
		if (W > H) {
			drawLandscape(g, mouseX, mouseY)
		} else {
			drawPortrait(g, mouseX, mouseY)
		}
	}

	private fun drawPlayerInfo(g: APGraphics, playerNum: Int, w: Float, h: Float) {
		g.color = Board.BOARD_COLOR
		g.drawFilledRect(0f, 0f, w, h)
		g.color = GColor.BLACK
		g.drawRect(0f, 0f, w, h)
		g.setClipRect(0f, 0f, w, h)
		playerInfoWidth = w
		val p = getPlayer(playerNum)
		val pcId = getImageId(p.piece)
		val dim = Math.min(w / 2, h / 4)
		playerInfoHeight = dim
		val border = 5f
		g.drawImage(pcId, 0f, 0f, dim, dim)
		if (currentPlayerNum == playerNum) {
			g.color = GColor.CYAN
			g.drawRect(0f, 0f, dim, dim, 2f)
			animations["GAME"]?.takeIf { it.isNotEmpty() }?.let { anim ->
				g.pushMatrix()
				val dieDim = dim / 2 - g.textHeight / 2
				g.translate(w - dieDim, 0f)
				drawDice(g, die1, die2, dieDim)
				g.popMatrix()
			}
		}
		spriteMap["PLAYER$playerNum"]?.let { sp ->
			sp.M.setTranslate(w - PADDING, dim / 2)
			sp.color = GColor.BLACK
			sp.animateAndDraw(g, 0f, 0f)
			g.pushTextHeight(TEXT_HEIGHT_MED, false)
			if (p.isBankrupt) {
				g.pushTextHeight(TEXT_HEIGHT_LARGE, false)
				g.color = GColor.TRANSLUSCENT_BLACK
				g.drawFilledRect(0f, 0f, w, h)
				g.color = GColor.RED
				g.drawWrapString(w / 2, h / 2, w, Justify.CENTER, Justify.CENTER, "BANKRUPT")
				g.popTextHeight()
			} else {
				var sy = dim
				var bkColor = GColor.TRANSPARENT
				var txtColor = GColor.BLACK
				if (p.isInJail) {
					drawJail(g, w, dim, 1f)
					sy += drawWrapStringOnBackground(g, 0f, sy, w, "IN JAIL", bkColor, txtColor, border)
				} else {
					if (p.square.canPurchase()) {
						bkColor = p.square.color
						txtColor = chooseContrastColor(bkColor)
					}
					var sqStr = p.square.name.prettify()
					while (Character.isDigit(sqStr[sqStr.length - 1])) {
						sqStr = sqStr.substring(0, sqStr.length - 1)
					}
					sy += drawWrapStringOnBackground(g, 0f, sy, w, sqStr, bkColor, txtColor, border)
				}
				var num: Int
				if (p.numGetOutOfJailFreeCards.also { num = it } > 0) {
					g.color = GColor.BLACK
					sy += drawWrapStringOnBackground(g, 0f, sy, w, "Get out of Jail FREE x $num", GColor.WHITE, GColor.BLACK, border)
				}
				if (p.numRailroads.also { num = it } > 0) {
					g.color = GColor.BLACK
					sy += drawWrapStringOnBackground(g, 0f, sy, w, "Railroads x $num", GColor.BLACK, GColor.WHITE, border)
				}
				if (p.numUtilities.also { num = it } > 0) {
					g.color = GColor.BLACK
					sy += drawWrapStringOnBackground(g, 0f, sy, w, "Utilities x $num", GColor.WHITE, GColor.BLACK, border)
				}
				val map = p.propertySets
				for ((key, value) in map) {
					val txt = String.format("%d of %d", value.size, value[0].property.numForSet)
					bkColor = key
					txtColor = chooseContrastColor(bkColor)
					sy += drawWrapStringOnBackground(g, 0f, sy, w, txt, bkColor, txtColor, border)
				}
			}
			g.popTextHeight()
		}
		drawAnimations(g, "PLAYER$playerNum")
		g.clearClip()
	}

	fun drawWrapStringOnBackground(g: AGraphics, x: Float, y: Float, width: Float, txt: String?, bkColor: GColor, txtColor: GColor, border: Float): Float {
		val d = g.drawWrapString(x + border, y + border, width - border * 2, txt)
		g.color = bkColor
		g.drawFilledRect(x, y, width, d.height + 2 * border)
		g.color = txtColor
		g.drawWrapString(x + border, y + border, width - border * 2, txt)
		return d.height + 2 * border
	}

	fun getPropertyDetails(property: Square) : String {
		return when (property.type) {
			SquareType.PROPERTY -> """
	            	PRICE $${property.price} RENT $${property.getRent(0)}

	            	Rent With:
	            	1 House $${property.getRent(1)}
	            	2 Houses $${property.getRent(2)}
	            	3 Houses $${property.getRent(3)}
	            	4 Houses $${property.getRent(4)}
	            	Hotel $${property.getRent(5)}

	            	Upgrade $${property.unitPrice}
	            	MORTGAGE: $${property.getMortgageValue(0)}
	            	""".trimIndent()
			SquareType.UTILITY -> """
					PRICE $${property.price}
				
					If One Utility is owned then 4 times the amount shown on dice.
					If two Utilities owned then 10 times the amount shown on Dice
				
					Mortgage $${property.getMortgageValue(0)}
					""".trimIndent()
			SquareType.RAIL_ROAD -> """
	            	PRICE $${property.price}

	            	RENT $${RAILROAD_RENT * (1 shl 0)}
	            	If 2 R.R. Owned $${RAILROAD_RENT * (1 shl 1)}
	            	If 3 R.R. Owned $${RAILROAD_RENT * (1 shl 2)}
	            	If 4 R.R. Owned $${RAILROAD_RENT * (1 shl 3)}

	            	MORTGAGE $${property.getMortgageValue(0)}
	            	""".trimIndent()
			else -> throw AssertionError("Dont know how to draw: $property")
		}
	}

	open fun drawPropertyCard(g: AGraphics, maxWidth: Float, topY: Float, property: Square, buyer: String?) {

		val buyerStr = buyer?.let {
			"\nSOLD TO $it\n"
		}?:run {
			"\n\n\n"
		}
		val txtLrg = g.textHeight
		val txtMed = txtLrg * .75f
		val table = Table().addColumn(
			property.name.prettify(), getPropertyDetails(property), buyerStr).setModel(object : Table.Model {
			override fun getCornerRadius(): Float {
				return txtLrg / 2
			}

			override fun getBorderColor(g: AGraphics): GColor {
				return property.color
			}

			override fun getHeaderColor(g: AGraphics): GColor {
				return if (property.color != GColor.WHITE) property.color else GColor.BLACK
			}

			override fun getBackgroundColor(): GColor {
				return GColor.WHITE
			}

			override fun getBorderWidth(): Int {
				return (txtLrg/4).roundToInt()
			}

			override fun getCellColor(g: AGraphics, row: Int, col: Int): GColor {
				return when (row) {
					1->GColor.RED
					else->g.getColor()
				}
			}

			override fun getTextAlignment(row: Int, col: Int): Justify {
				return when (row) {
					1->Justify.CENTER
					else->Justify.LEFT
				}
			}

			override fun getHeaderTextHeight(g: AGraphics): Float {
				return txtLrg
			}

			override fun getCellTextHeight(g: AGraphics): Float {
				return txtMed
			}

			override fun getHeaderJustify(col: Int): Justify {
				return Justify.CENTER
			}
		})

		val dim = g.viewport.minLength()
		g.color = GColor.BLACK
		table.draw(g, dim/2, topY, Justify.CENTER, Justify.TOP)
	}

	fun chooseContrastColor(c: GColor): GColor {
		var amt = c.red + c.blue + c.green
		amt *= c.alpha
		return if (amt > 1.5f) GColor.BLACK else GColor.WHITE
	}

	private fun drawPortrait(g: APGraphics, mx: Int, my: Int) {
		drawBoard(g)
		g.pushMatrix()
		g.translate((W / 2).toFloat(), (H / 8).toFloat())
		drawDice(g, die1, die2, (DIM / 10).toFloat())
		g.popMatrix()
		val w: Float = (W - PADDING * (numPlayers - 1)) / numPlayers
		for (i in 0 until numPlayers) {
			g.pushMatrix()
			g.translate((w + PADDING) * i, DIM + PADDING)
			drawPlayerInfo(g, i, w, H - DIM - PADDING)
			g.popMatrix()
		}
		drawAnimations(g, "GAME")
	}

	private fun drawLandscape(g: APGraphics, mx: Int, my: Int) {
		g.pushMatrix()
		g.translate((W / 2 - DIM / 2).toFloat(), (H / 2 - DIM / 2).toFloat())
		drawBoard(g)
		g.popMatrix()
		val w = ((W - DIM) / 2).toFloat()
		val h1 = H.toFloat()
		val h2 = H / 2 - PADDING / 2
		val numP: Int = numPlayers
		for (i in 0 until numPlayers) {
			var h = h1
			g.pushMatrix()
			when (i) {
				0 -> if (numP == 4) h = h2
				1 -> {
					if (numP >= 3) h = h2
					g.translate(W - w + PADDING, 0f)
				}
				2 -> {
					h = h2
					g.translate(W - w + PADDING, H / 2 + PADDING / 2)
				}
				3 -> {
					h = h2
					g.translate(0f, H / 2 + PADDING)
				}
			}
			drawPlayerInfo(g, i, w - PADDING, h)
			g.popMatrix()
		}
		drawAnimations(g, "GAME")
	}

	private fun drawAnimations(g: AGraphics, key: String) {
		synchronized(animations) {
			animations[key]?.let { list ->
				val it = list.iterator()
				while (it.hasNext()) {
					val a = it.next()
					g.pushMatrix()
					a.update(g)
					g.popMatrix()
					if (a.isDone) it.remove()
				}
				if (list.size > 0) repaint()
			}
		}
	}

	// draw house with center at 0,0.
	fun drawHouse(g: AGraphics) {
		val color = g.color
		val roofR = color.darkened(0.2f)
		val front = color.darkened(0.5f)
		g.color = front
		g.begin()
		g.vertexArray(HOUSE_PTS[8], HOUSE_PTS[9], HOUSE_PTS[6], HOUSE_PTS[7])
		g.drawQuadStrip()
		g.begin()
		g.color = color
		g.vertexArray(HOUSE_PTS[0], HOUSE_PTS[1], HOUSE_PTS[2], HOUSE_PTS[3])
		g.drawQuadStrip()
		g.begin()
		g.color = roofR
		g.vertexArray(HOUSE_PTS[2], HOUSE_PTS[3], HOUSE_PTS[4], HOUSE_PTS[5])
		g.drawQuadStrip()
		g.color = color // restore color
	}

	private fun drawBoard(g: AGraphics) {
		g.drawImage(boardImageId, 0f, 0f, DIM.toFloat(), DIM.toFloat())
		run {
			spriteMap[Square.FREE_PARKING.name]?.let { kitty ->
				val r = board.getSqaureBounds(Square.FREE_PARKING)
				kitty.M.setTranslate(r.center)
				kitty.color = GColor.GREEN
				kitty.animateAndDraw(g, r.w, r.h)
			}
		}
		/*
        if (kitty > 0) {
            GRectangle r = board.getSqaureBounds(Square.FREE_PARKING);
            Vector2D cntr = r.getCenter();
            g.setColor(GColor.GREEN);
            GDimension dim = g.drawWrapString(cntr.X(), cntr.Y(), r.w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + kitty);
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledRect(cntr.X() - dim.width/2 - 5, cntr.Y() - dim.height/2 - 5, dim.width + 10, dim.height + 10);
            g.setColor(GColor.GREEN);
            g.drawWrapString(cntr.X(), cntr.Y(), r.w, Justify.CENTER, Justify.CENTER, "Kitty\n$" + kitty);
        }*/
		for (i in 0 until numPlayers) {
			val p = getPlayer(i)
			if (p.isBankrupt) continue
			val pcId = getImageId(p.piece)
			val targetDim: Float = board.pieceDimension
			for (c in ArrayList(p.cards)) {
				val r = board.getSqaureBounds(c.property)
				when (board.getSquarePosition(c.property)) {
					Board.Position.TOP    -> g.drawImage(pcId, r.x + r.w / 2 - targetDim / 2, r.y + r.h - targetDim / 3, targetDim, targetDim)
					Board.Position.RIGHT  -> g.drawImage(pcId, r.x - targetDim * 2 / 3, r.y + r.h / 2 - targetDim / 2, targetDim, targetDim)
					Board.Position.BOTTOM -> g.drawImage(pcId, r.x + r.w / 2 - targetDim / 2, r.y - targetDim * 2 / 3, targetDim, targetDim)
					Board.Position.LEFT -> g.drawImage(pcId, r.x + r.w - targetDim / 3, r.y + r.h / 2 - targetDim / 2, targetDim, targetDim)
					else -> Unit
				}
				val sp = spriteMap[c.property.name]
				if (sp != null) {
					sp.data1 = c.houses
					//sp.M.setTranslate(r.x, r.y);
					sp.animateAndDraw(g, r.w, r.h)
				}
				if (c.isMortgaged) {
					g.color = GColor.TRANSLUSCENT_BLACK
					g.drawFilledRect(r)
					g.color = GColor.RED
					val v: IVector2D = r.center
					g.drawWrapString(v.x, v.y, r.w, Justify.CENTER, Justify.CENTER,
						"MORTGAGED")
				}
			}
			run {

				// draw player piece on the board
				val r = if (p.isInJail) board.getPiecePlacementJail(i) else board.getPiecePlacement(i, p.square)
				spriteMap[p.piece.name]?.let { sp ->
					sp.M.setTranslate(r.x, r.y)
					sp.animateAndDraw(g, r.w, r.h)
				}
			}

			// Mark all sellable properties
			for (t in p.getTrades()) {
				val v = board.getInnerEdge(t.card.property)
				val offset: Float = board.pieceDimension
				//GRectangle r = board.getPiecePlacement(getPlayerNum(t.getTrader()), t.card.property);
				val pos = board.getSquarePosition(t.card.property)
				g.color = GColor.YELLOW
				when (pos) {
					Board.Position.TOP -> g.drawJustifiedStringOnBackground(v.X(), v.Y() + offset, Justify.CENTER, Justify.TOP, "$" + t.price, GColor.TRANSLUSCENT_BLACK, 5f, 5f)
					Board.Position.RIGHT -> g.drawJustifiedStringOnBackground(v.X() - offset, v.Y(), Justify.RIGHT, Justify.CENTER, "$" + t.price, GColor.TRANSLUSCENT_BLACK, 5f, 5f)
					Board.Position.BOTTOM -> g.drawJustifiedStringOnBackground(v.X(), v.Y() - offset, Justify.CENTER, Justify.BOTTOM, "$" + t.price, GColor.TRANSLUSCENT_BLACK, 5f, 5f)
					Board.Position.LEFT -> g.drawJustifiedStringOnBackground(v.X() + offset, v.Y(), Justify.LEFT, Justify.CENTER, "$" + t.price, GColor.TRANSLUSCENT_BLACK, 5f, 5f)
					else                  -> throw GException("Unhandled case")
				}
			}
		}
		drawAnimations(g, "BOARD")
	}

	/**
	 * Draw dice vert centered TOP and horz centered at ORIGIN with maxheight=dim and maxWidth<dim></dim>*2
	 * @param g
	 * @param die1
	 * @param die2
	 * @param dieDim
	 */
	fun drawDice(g: AGraphics, die1: Int, die2: Int, dieDim: Float) {
		val padding = (dieDim / 4).toInt() + 1
		if (die1 > 0 && die2 > 0) {
			g.pushMatrix()
			g.translate(-dieDim + padding / 2, (padding / 2).toFloat())
			drawDie(g, dieDim - padding, GColor.WHITE, GColor.BLACK, die1)
			g.popMatrix()
			g.pushMatrix()
			g.translate((+padding / 2).toFloat(), (padding / 2).toFloat())
			drawDie(g, dieDim - padding, GColor.WHITE, GColor.BLACK, die2)
			g.popMatrix()
		}
	}

	fun drawDie(g: AGraphics, dim: Float, dieColor: GColor?, dotColor: GColor?, numDots: Int) {
		g.color = dieColor
		val arc = dim / 4
		g.drawFilledRoundedRect(0f, 0f, dim, dim, arc)
		g.color = dotColor
		val dd2 = dim / 2
		val dd4 = dim / 4
		val dd34 = dim * 3 / 4
		val dotSize = dim / 8
		val oldDotSize = g.setPointSize(dotSize)
		g.begin()
		when (numDots) {
			1 -> g.vertex(dd2, dd2)
			2 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
			}
			3 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd2, dd2)
				g.vertex(dd34, dd34)
			}
			4 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
			}
			5 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd2, dd2)
			}
			6 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd4, dd2)
				g.vertex(dd34, dd2)
			}
			else -> throw GException("Unsupported dot num $numDots")
		}
		g.drawPoints()
		g.setPointSize(oldDotSize)
	}

	companion object {
		private val log = LoggerFactory.getLogger(UIMonopoly::class.java)
		const val HOUSE_RADIUS = 1f
		const val MONEY_PAUSE = 1000L
		const val HOUSE_PAUSE = 2000L
		const val PURCHASE_PAUSE = 10000L
		const val MESSAGE_PAUSE = 6000L
		const val PADDING = 5f
		val HOUSE_COLOR = GColor.GREEN.darkened(.6f)
		val HOTEL_COLOR = GColor.RED
		private val HOUSE_PTS = arrayOf(
			Vector2D(-1f, -0.8f),
			Vector2D(-1f, .6f),
			Vector2D(0f, -1f),
			Vector2D(0f, .2f),
			Vector2D(1f, -0.8f),
			Vector2D(1f, .6f),
			Vector2D(-0.8f, 1f),
			Vector2D(.8f, 1f),
			Vector2D(-.8f, 0f),
			Vector2D(.8f, 0f))

		var TEXT_HEIGHT_LARGE = 20f
		var TEXT_HEIGHT_MED   = 16f
		var TEXT_HEIGHT_SM    = 12f
	}

	init {
		initSprites()
	}
}