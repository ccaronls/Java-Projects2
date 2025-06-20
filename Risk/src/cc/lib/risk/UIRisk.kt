package cc.lib.risk

import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.game.ChainInterpolator
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.IInterpolator
import cc.lib.game.IVector2D
import cc.lib.game.InterpolatorUtils
import cc.lib.game.Justify
import cc.lib.game.RomanNumeral
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.Bezier
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.Lock
import cc.lib.utils.launchIn
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.roundToInt

abstract class RiskAnim(durationMSecs: Long) : AAnimation<AGraphics>(durationMSecs) {
	var zOrder = 0
		private set

	fun setZOrder(order: Int): RiskAnim {
		zOrder = order
		return this
	}
}

class ExpandingTextOverlayAnimation(val text: String, color: GColor) : RiskAnim(2000) {
	val colorInterp: IInterpolator<GColor>
	val textSizeInterp: IInterpolator<Float>
	override fun draw(g: AGraphics, position: Float, dt: Float) {
		g.color = colorInterp.getAtPosition(position)
		g.pushTextHeight(textSizeInterp.getAtPosition(position), false)
		g.drawJustifiedString(g.viewport.center, Justify.CENTER, Justify.CENTER, text)
		g.popTextHeight()
	}

	init {
		val color2 = color.withAlpha(0)
		colorInterp = ChainInterpolator(color2.getInterpolator(color),
			color.getInterpolator(color2))
		textSizeInterp = InterpolatorUtils.linear(
			UIRisk.instance.textHeightOverlaySmall,
			UIRisk.instance.textHeightOverlayLarge)
	}
}

class UIRiskPlayer(army: Army=Army.BLUE) : RiskPlayer(army) {

	override fun pickTerritoryToClaim(game: RiskGame, options: List<Int>): Int? {
		return UIRisk.instance.pickTerritory(options, "$army Pick a territory to claim")
	}

	override fun pickTerritoryForArmy(game: RiskGame, options: List<Int>, remainingArmiesToPlace: Int, startArmiesToPlace: Int): Int? {
		return UIRisk.instance.pickTerritory(options, String.format("%s Pick territory to place the %d%s of %d armies", army, remainingArmiesToPlace + 1, Utils.getSuffix(remainingArmiesToPlace + 1), startArmiesToPlace))
	}

	override fun pickTerritoryForNeutralArmy(game: RiskGame, options: List<Int>): Int? {
		return UIRisk.instance.pickTerritory(options, "$army Pick territory to place a Neutral Army")
	}

	override fun pickTerritoryToAttackFrom(game: RiskGame, options: List<Int>): Int? {
		return UIRisk.instance.pickTerritory(options, "$army Pick territory from which to stage an attack")
	}

	override fun pickTerritoryToAttack(game: RiskGame, territoryAttackingFrom: Int, options: List<Int>): Int? {
		return UIRisk.instance.pickTerritory(options, "$army Pick Territory to Attack")
	}

	override fun pickTerritoryToMoveFrom(game: RiskGame, options: List<Int>): Int? {
		return UIRisk.instance.pickTerritory(options, "$army Pick territory from which to move a armys")
	}

	override fun pickTerritoryToMoveTo(game: RiskGame, territoryMovingFrom: Int, options: List<Int>): Int? {
		return UIRisk.instance.pickTerritory(options, "$army Pick Territory to Move an Army to")
	}

	override fun pickAction(game: RiskGame, actions: List<Action>, msg: String): Action? {
		return UIRisk.instance.pickAction(actions, msg)
	}
}

/**
 * Created by Chris Caron on 5/5/23.
 */
abstract class UIRisk(board : RiskBoard) : RiskGame(board) {

	init {
		instance = this
	}

	private val log = LoggerFactory.getLogger(UIRisk::class.java)

	private val roman = RomanNumeral()
	private val animations: MutableList<RiskAnim> = ArrayList()
	private val highlightedCells: MutableList<Pair<Int, GColor>> = ArrayList()
	private val pickableTerritories: MutableList<Int> = ArrayList()
	var running: Boolean = false
		private set
	private var result: Any? = null
	private var zoomRect = GRectangle.EMPTY
	private val lock = ReentrantLock()
	private val cond = lock.newCondition()
	private var message: String = ""

	abstract val saveGame: File
	abstract val storedGames: Map<String, File>
	override fun onDiceRolled(
		attacker: Army,
		attackingDice: IntArray,
		defender: Army,
		defendingDice: IntArray,
		result: BooleanArray
	) {
		super.onDiceRolled(attacker, attackingDice, defender, defendingDice, result)
		if (getPlayerOrNull(attacker) is UIRiskPlayer || getPlayerOrNull(defender) is UIRiskPlayer) {
			showDiceDialog(attacker, attackingDice, defender, defendingDice, result)
			waitForUser(Void::class.java)
		}
	}

	abstract fun showDiceDialog(attacker: Army, attackingDice: IntArray, defender: Army, defendingDice: IntArray, result: BooleanArray)

	override fun onPlaceArmy(army: Army, cellIdx: Int) {
		if (!running) return
		val cell = board.getCell(cellIdx)
		val rect = GRectangle(board.dimension)
		val start: Vector2D = arrayOf(rect.bottomLeft,rect.bottomRight).random()
		val interp = Bezier.build(
			Vector2D(start), Vector2D(cell), .4f)
		addAnimation(object : RiskAnim(1000) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.color = army.color
				drawArmy(g, interp.getAtPosition(position), army, 1)
			}
		})
		Thread.sleep(500)
	}

	override fun onMoveTroops(startIdx: Int, endIdx: Int, numTroops: Int) {
		if (!running)
			return
		val start = board.getCell(startIdx)
		val army: Army = requireNotNull(start.occupier)
		val endV = MutableVector2D(board.getCell(endIdx))
		val delta = endV.sub(start)
		if (delta.x > board.dimension.width / 2) {
			delta.subEq(board.dimension.width, 0f)
		} else if (delta.x < -board.dimension.width / 2) {
			delta.addEq(board.dimension.width, 0f)
		}
		if (delta.y > board.dimension.height / 2) {
			delta.subEq(0f, board.dimension.height)
		} else if (delta.y < -board.dimension.height / 2) {
			delta.addEq(0f, board.dimension.height)
		}
		val interp = Bezier.build(
			Vector2D(start), Vector2D(start).add(delta), .4f)
		addAnimation(object : RiskAnim(1000) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.color = army.color
				drawArmy(g, interp.getAtPosition(position), army, numTroops)
			}
		})
		Thread.sleep(500)
	}

	override fun onStartAttackTerritoryChosen(cellIdx: Int) {
		if (!running) return
		val start = board.getCell(cellIdx)
		val zoom = board.getCellBoundingRect(cellIdx)
		val startRect = GRectangle(board.dimension)
		val all = start.getAllConnectedCells()
		for (idx in all) {
			val rect2 = board.getCellBoundingRect(idx)
			val dv: Vector2D = rect2.center.sub(start)
			if (dv.x < -board.dimension.width / 2) {
				rect2.moveBy(board.dimension.width, 0f)
			} else if (dv.x > board.dimension.width / 2) {
				rect2.moveBy(-board.dimension.width, 0f)
			}
			if (dv.y < -board.dimension.height / 2) {
				rect2.moveBy(0f, board.dimension.height)
			} else if (dv.y > board.dimension.height / 2) {
				rect2.moveBy(0f, -board.dimension.height)
			}
			zoom.addEq(rect2)
		}
		zoom.setAspect(startRect.aspect)
		val endRect = GRectangle(zoom)
		val rectInterp = GRectangle.getInterpolator(startRect, endRect)
		val dragDeltaInterp = Vector2D.getLinearInterpolator(dragDelta, Vector2D.ZERO)
		addAnimation(object : RiskAnim(1000) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				zoomRect = rectInterp.getAtPosition(position)
				dragDelta.assign(dragDeltaInterp.getAtPosition(position))
			}
		})
		highlightedCells.add(Pair(cellIdx, GColor.RED))
		Thread.sleep(1000)
	}

	override fun onEndAttackTerritoryChosen(startIdx: Int, endIdx: Int) {
		highlightedCells.add(Pair(endIdx, GColor.GOLD))
		super.onEndAttackTerritoryChosen(startIdx, endIdx)
	}

	override fun onBeginTurn(army: Army) {
		if (!running) return
		zoomRect = GRectangle.EMPTY
		highlightedCells.clear()
		addOverlayAnimation(ExpandingTextOverlayAnimation("$army's Turn", army.color))
		Thread.sleep(1000)
	}

	override fun onBeginMove() {
		zoomRect = GRectangle.EMPTY
		highlightedCells.clear()
	}

	override fun onStartMoveTerritoryChosen(cellIdx: Int) {
		if (!running) return
		highlightedCells.add(Pair(cellIdx, GColor.CYAN))
	}

	override fun onArmiesDestroyed(attackerIdx: Int, attackersLost: Int, defenderIdx: Int, defendersLost: Int) {
		if (!running) return
		val lock = Lock()
		destroyArmies(lock, attackerIdx, attackersLost)
		destroyArmies(lock, defenderIdx, defendersLost)
		lock.block()
	}

	private fun destroyArmies(lock: Lock, cellIdx: Int, num: Int) {
		val rects = board.getCellBoundingRect(cellIdx).scaledBy(.6f).subDivide(2, 2)
		rects.shuffle()
		for (i in 0 until num) {
			lock.acquire()
			val exploLoc = rects[i].scaledBy(.5f).randomPointInside
			addAnimation(object : RiskAnim(2000) {
				override fun draw(g: AGraphics, position: Float, dt: Float) {
					val step = 1f / exploAnim.size
					val idx = (position / step).roundToInt().coerceIn(exploAnim.indices)
					//val img = g.getImage(exploAnim[idx])
					g.drawImage(exploAnim[idx], exploLoc, Justify.CENTER, Justify.BOTTOM, .3f)
				}

				override fun onDone() {
					super.onDone()
					log.debug("Destroy Armies", "Release Lock")
					lock.release()
				}
			})
			Thread.sleep(500)
		}
	}

	override fun onAttackerGainedRegion(attacker: Army, region: Region) {
		if (!running) return
		super.onAttackerGainedRegion(attacker, region)
		addOverlayAnimation(ExpandingTextOverlayAnimation(attacker.name + " CONTROLS " + region.name, attacker.color))
		Thread.sleep(1000)
	}

	override fun onGameOver(winner: Army) {
		super.onGameOver(winner)
		addOverlayAnimation(ExpandingTextOverlayAnimation(winner.name + " WINS!!!", GColor.BLUE)
			.setOscillating<AAnimation<AGraphics>>(true)
			.setRepeats(-1)
		)
	}

	fun clearAnimations() {
		synchronized(animations) {
			for (a in animations) {
				a.stop()
			}
		}
	}

	override fun reset() {
		super.reset()
		init()
	}

	fun init() {
		clearAnimations()
		highlightedCells.clear()
		zoomRect = GRectangle.EMPTY
		message = ""
		pickableTerritories.clear()
		result = null
	}

	enum class Buttons {
		NEW_GAME {
			override fun isEnabled(game: UIRisk): Boolean = true
		},
		RESUME {
			override fun isEnabled(game: UIRisk): Boolean = game.saveGame.exists()
		},
		SAVE {
			override fun isEnabled(game: UIRisk): Boolean = game.saveGame.exists()
		},
		LOAD {
			override fun isEnabled(game: UIRisk): Boolean = game.storedGames.isNotEmpty()
		},
		ABOUT {
			override fun isEnabled(game: UIRisk): Boolean = true
		};

		open fun isEnabled(game: UIRisk): Boolean = false
	}

	fun initHomeMenu() {
		initMenu(Buttons.values().filter { it.isEnabled(this) })
	}

	abstract fun initMenu(buttons: List<*>)

	private lateinit var exploAnim: IntArray

	fun loadExplodAnim(g: AGraphics, sourceBitmapId : Int) {
		val cells = arrayOf(
			intArrayOf(57, 94, 115 - 57, 125 - 94),
			intArrayOf(138, 73, 236 - 138, 125 - 73),
			intArrayOf(252, 70, 370 - 252, 125 - 70),
			intArrayOf(408, 48, 572 - 408, 125 - 48),
			intArrayOf(47, 200, 187 - 47, 266 - 200),
			intArrayOf(200, 177, 341 - 200, 266 - 177),
			intArrayOf(353, 193, 400 - 353, 266 - 193),
			intArrayOf(493, 197, 592 - 493, 266 - 197))
		exploAnim = g.loadImageCells(sourceBitmapId, cells)
	}

	abstract val boardImageId : Int

	fun onDraw(g: AGraphics) {
		val imageRect = GRectangle(board.dimension)
		val boardRect = if (zoomRect.isEmpty) imageRect else zoomRect
		g.ortho(boardRect)
		g.pushMatrix()
		run {
			dragDelta.wrap(Vector2D.ZERO, Vector2D(imageRect.width, imageRect.height))
			g.translate(dragDelta)
			g.pushMatrix()
			run {
				g.translate(0f, -imageRect.height)
				for (ii in 0..2) {
					g.pushMatrix()
					g.translate(-imageRect.width, 0f)
					for (i in 0..2) {
						// dont render if target rect is not visible
						val tl = imageRect.topLeft
						val br = imageRect.bottomRight
						g.transform(tl)
						g.transform(br)
						if (tl.x >= g.viewportWidth || tl.y >= g.viewportHeight || br.x <= 0 || br.y <= 0) {
							// dont render - offscreen
						} else {
							g.drawImage(boardImageId, imageRect)
							pickCell(g, board)
							drawCells(g, board)
							drawHighlightedCells(g, board)
						}
						drawAnimations(g, ZORDER_GAME)
						g.translate(imageRect.width, 0f)
					}
					g.popMatrix()
					g.translate(0f, imageRect.height)
				}
			}
			g.popMatrix()
		}
		g.popMatrix()
		g.ortho()
		g.color = GColor.CYAN
		g.setTextHeight(textHeightInfo, false)
		if (numPlayers > 0)
			summary.draw(g, 10f, (g.viewportHeight - 10).toFloat(), Justify.LEFT, Justify.BOTTOM)
		if (message.isNotEmpty()) {
			g.drawJustifiedStringOnBackground((g.viewportWidth - 20).toFloat(), 20f, Justify.RIGHT, Justify.TOP, message, GColor.TRANSLUSCENT_BLACK, 10f, 5f)
		}
		drawAnimations(g, ZORDER_OVERLAY)
	}

	fun drawHighlightedCells(g: AGraphics, board: RiskBoard) {
		for (pair in highlightedCells.toList()) {
			val cell = board.getCell(pair.first)
			board.renderCell(cell, g, 1f)
			g.color = pair.second
			g.drawLineLoop(4f)
		}
	}

	@Synchronized
	fun addAnimation(a: RiskAnim) {
		if (running) {
			animations.add(a.start())
			redraw()
		}
	}

	@Synchronized
	fun addOverlayAnimation(a: RiskAnim) {
		animations.add(a.setZOrder(5).start())
		redraw()
	}

	@Synchronized
	fun drawAnimations(g: AGraphics, zOrder: Int) {
		val it = animations.iterator()
		while (it.hasNext()) {
			val a = it.next()
			if (a.zOrder != zOrder) continue
			if (a.isDone) {
				it.remove()
				continue
			}
			a.update(g)
		}
		if (animations.size > 0) {
			redraw()
		}
	}

	val circleColor = GColor(255, 255, 255, 64)

	fun drawArmy(g: AGraphics, cell: IVector2D?, army: Army, numTroops: Int) {
		val numerals = roman.toRoman(numTroops)
		val thickness = romanNumberThickness
		val borders = arrayOf(
			AGraphics.Border(AGraphics.BORDER_FLAG_NORTH, thickness, thickness, 0f, -thickness / 2, thickness * 2 / 3),
			AGraphics.Border(AGraphics.BORDER_FLAG_SOUTH, thickness, thickness, 0f, -thickness / 2, 0f)
		)
		val th = textHeightRoman
		val htPix = g.setTextHeight(th + 4, false)
		g.color = circleColor
		g.drawFilledCircle(cell, htPix / 2)
		g.color = GColor.BLACK
		g.setTextStyles(AGraphics.TextStyle.BOLD)
		g.drawJustifiedStringBordered(cell, Justify.CENTER, Justify.CENTER, numerals, *borders)
		g.setTextHeight(th, false)
		g.setTextStyles(AGraphics.TextStyle.NORMAL)
		g.color = army.color
		g.drawJustifiedStringBordered(cell, Justify.CENTER, Justify.CENTER, numerals, *borders)
	}

	fun drawCells(g: AGraphics, board: RiskBoard) {
		for (cell in board.cells) {
			cell.occupier?.let { occupier ->
				drawArmy(g, cell, occupier, cell.numArmies)
			}
		}
	}

	fun pickCell(g: AGraphics, board: RiskBoard) {
		//log.debug("TAP", "Viewport: " + g.getViewport());
		//log.debug("TAP", "BoardDim: " + board.getDimension());
		//log.debug("TAP", "tap Befone: " + tapPos);
		//log.debug("TAP", "DragDelta: " + dragDelta);
		val tap = MutableVector2D(tapPos)
		g.screenToViewport(tap)
		//log.debug("TAP", "Tap Viewport: " + tap);
		var highlightedIdx = -1
		g.color = GColor.WHITE
		for (idx in pickableTerritories) {
			val cell = board.getCell(idx)
			board.renderCell(cell, g)
			if (board.isPointInsideCell(tap, idx)) {
				highlightedIdx = idx
				continue
			}
			g.drawLineLoop(cellLineThickness)
		}
		if (highlightedIdx >= 0) {
			if (tapped) {
				tapped = false
				tapPos.zeroEq()
				setGameResult(highlightedIdx)
			} else {
				g.color = GColor.RED.withAlpha(.5f)
				val cell = board.getCell(highlightedIdx)
				board.renderCell(cell, g)
				g.drawLineLoop(cellLineThickness + 2)
			}
		}
	}

	private var dragStart = MutableVector2D()
	private var dragDelta = MutableVector2D()
	private var tapped = false
	private val tapPos = MutableVector2D()

	fun onTap(x: Float, y: Float) {
		log.debug("onTap ($x,$y)")
		tapPos.assign(x, y)
		tapped = true
	}

	fun onMouse(x: Float, y: Float) {
		log.debug("onMouse ($x,$y)")
		tapPos.assign(x, y)
	}

	fun onDragStart(x: Float, y: Float) {
		dragStart.assign(x, y)
	}

	fun onDrag(x: Float, y: Float) {
		if (zoomRect.isEmpty) {
			dragDelta.addEq(Vector2D(x, y).sub(dragStart))
			dragStart.assign(x, y)
		}
	}

	fun stopGameThread() {
		running = false
		setGameResult(null)
		onGameThreadStopped()
	}

	open fun onGameThreadStopped() = Unit

	@Synchronized
	fun startGameThread() {
		if (running) return
		init()
		initMenu(emptyList<Any>())
		val gameDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
		running = true
		launchIn(gameDispatcher) {
			while (running && !isDone) {
				try {
					runGame()
					redraw()
					result = null
				} catch (e: Exception) {
					e.printStackTrace()
					break
				}
			}
			initHomeMenu()
			log.debug("game thread EXIT")
		}
	}

	override fun onNextPlayer(currentPlayer: Int) {
		if (!running) return
		zoomRect = GRectangle.EMPTY
		highlightedCells.clear()
		val army = getPlayer(currentPlayer).army
		addOverlayAnimation(ExpandingTextOverlayAnimation("$army's Turn", army.color))
		Thread.sleep(1000)
	}

	fun <T> waitForUser(expectedType: Class<T>): T? {
		result = null
		redraw()
		lock.withLock {
			cond.await()
		}
		result?.let {
			if (expectedType.isAssignableFrom(it.javaClass))
				return result as T
		}
		return null
	}

	fun setGameResult(result: Any?) {
		this.result = result
		initMenu(emptyList<Any>())
		message = ""
		pickableTerritories.clear()
		lock.withLock {
			cond.signal()
		}
	}

	fun pickTerritory(options: List<Int>, msg: String): Int? {
		message = msg
		pickableTerritories.clear()
		pickableTerritories.addAll(options)
		initMenu(listOf(Action.CANCEL))
		return waitForUser(Int::class.javaObjectType)
	}

	fun pickAction(options: List<Action>, msg: String): Action? {
		trySaveToFile(saveGame)
		message = msg
		initMenu(options)
		return waitForUser(Action::class.java)
	}

	fun startGame(players: List<RiskPlayer>) {
		reset()
		Utils.shuffle(players)
		for (pl in players) addPlayer(pl)
		startGameThread()
	}

	abstract fun redraw()

	open val textHeightOverlaySmall : Float = 10f
	open val textHeightOverlayLarge : Float = 20f
	open val textHeightInfo: Float = 12f
	open val romanNumberThickness: Float = 3f
	open val textHeightRoman: Float = 12f
	open val cellLineThickness : Float = 3f

	companion object {
		const val ZORDER_GAME = 0
		const val ZORDER_OVERLAY = 5
		lateinit var instance: UIRisk
	}

}