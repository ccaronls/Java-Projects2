package cc.game.android.risk

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import cc.lib.android.DroidActivity
import cc.lib.android.DroidGraphics
import cc.lib.game.*
import cc.lib.math.Bezier
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.risk.*
import cc.lib.utils.Lock
import cc.lib.utils.prettify
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.roundToInt

class RiskActivity : DroidActivity(), OnItemClickListener {
	
	val lock = ReentrantLock()
	val cond = lock.newCondition()
	var message: String = ""

	lateinit var saveGame: File
	var game: RiskGame = object : RiskGame() {
		override fun onDiceRolled(attacker: Army, attackingDice: IntArray, defender: Army, defendingDice: IntArray, result: BooleanArray) {
			super.onDiceRolled(attacker, attackingDice, defender, defendingDice, result)
			if (getPlayerOrNull(attacker) is UIRiskPlayer || getPlayerOrNull(defender) is UIRiskPlayer) {
				DiceDialog(this@RiskActivity, attacker, defender, attackingDice, defendingDice, result)
			}
		}

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
					//drawRomanNumeral(g, interp.getAtPosition(position), r);
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
			if (delta.x > board.dimension.getWidth() / 2) {
				delta.subEq(board.dimension.getWidth(), 0f)
			} else if (delta.x < -board.dimension.getWidth() / 2) {
				delta.addEq(board.dimension.getWidth(), 0f)
			}
			if (delta.y > board.dimension.getHeight() / 2) {
				delta.subEq(0f, board.dimension.getHeight())
			} else if (delta.y < -board.dimension.getHeight() / 2) {
				delta.addEq(0f, board.dimension.getHeight())
			}
			val interp = Bezier.build(
				Vector2D(start), Vector2D(start).add(delta), .4f)
			addAnimation(object : RiskAnim(1000) {
				override fun draw(g: AGraphics, position: Float, dt: Float) {
					g.color = army.color
					//drawRomanNumeral(g, interp.getAtPosition(position), r);
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
				val dv: Vector2D = rect2.center.subEq(start)
				if (dv.x < -board.dimension.getWidth() / 2) {
					rect2.moveBy(board.dimension.getWidth(), 0f)
				} else if (dv.x > board.dimension.getWidth() / 2) {
					rect2.moveBy(-board.dimension.getWidth(), 0f)
				}
				if (dv.y < -board.dimension.getHeight() / 2) {
					rect2.moveBy(0f, board.dimension.getHeight())
				} else if (dv.y > board.dimension.getHeight() / 2) {
					rect2.moveBy(0f, -board.dimension.getHeight())
				}
				zoom.addEq(rect2)
			}
			zoom.aspect = startRect.aspect
			val endRect = GRectangle(zoom)
			val rectInterp = GRectangle.getInterpolator(startRect, endRect)
			val dragDeltaInterp = Vector2D.getLinearInterpolator(dragDelta, Vector2D.ZERO)
			addAnimation(object : RiskAnim(1000) {
				override fun draw(g: AGraphics, position: Float, dt: Float) {
					zoomRect = rectInterp.getAtPosition(position)
					dragDelta.set(dragDeltaInterp.getAtPosition(position))
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
						Log.d("Destroy Armies", "Release Lock")
						lock.release()
					}
				})
				Thread.sleep(500)
			}
		}

		override fun onAttackerGainedRegion(attacker: Army?, region: Region) {
			if (!running) return
			super.onAttackerGainedRegion(attacker, region)
			addOverlayAnimation(ExpandingTextOverlayAnimation(attacker!!.name + " CONTROLS " + region.name, attacker.color))
			Thread.sleep(1000)
		}

		override fun onGameOver(winner: Army) {
			super.onGameOver(winner)
			addOverlayAnimation(ExpandingTextOverlayAnimation(winner.name + " WINS!!!", GColor.BLUE)
				.setOscillating<AAnimation<AGraphics>>(true)
				.setRepeats(-1)
			)
		}
	}
	private val roman = RomanNumeral()
	private val animations: MutableList<RiskAnim> = ArrayList()
	private val highlightedCells: MutableList<Pair<Int?, GColor?>> = ArrayList()
	private val pickableTerritories: MutableList<Int> = ArrayList()
	private var running = false
	private var result: Any? = null
	private lateinit var listView: ListView
	private var zoomRect = GRectangle.EMPTY
	fun clearAnimations() {
		synchronized(animations) {
			for (a in animations) {
				a.stop()
			}
		}
	}

	fun init() {
		clearAnimations()
		highlightedCells.clear()
		zoomRect = GRectangle.EMPTY
		message = ""
		pickableTerritories.clear()
		result = null
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		saveGame = File(filesDir, "save.game")
		try {
			assets.open("risk.board").use { `in` -> game.board.deserialize(`in`) }
		} catch (e: Exception) {
			e.printStackTrace()
			finish()
		}
		listView = findViewById(R.id.list_view)
		listView.setOnItemClickListener(this)
		hideNavigationBar()

		//getContent().setPinchZoomEnabled(true);
		//getContent().setZoomScaleBound(1, 5);
		instance = this
	}

	override fun getContentViewId(): Int {
		return R.layout.activity_main
	}

	override fun onStart() {
		super.onStart()
		try {
			if (saveGame.exists()) {
				val t = RiskGame()
				t.loadFromFile(saveGame)
				game.copyFrom(t)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			//saveGame.delete()
		}
		initHomeMenu()
	}

	override fun onStop() {
		super.onStop()
		stopGameThread()
	}

	internal enum class Buttons {
		NEW_GAME,
		RESUME,
		ABOUT
	}

	fun initHomeMenu() {
		if (saveGame.exists()) {
			initMenu(Utils.toList(*Buttons.values()))
		} else {
			initMenu(Utils.toList(Buttons.NEW_GAME, Buttons.ABOUT))
		}
	}

	override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
		Log.d("TAP", "onItemClick")
		val tag = view.tag ?: return
		if (tag is Action) {
			setGameResult(tag)
			return
		}
		if (tag is Buttons) {
			when (tag) {
				Buttons.NEW_GAME -> {
					PlayerChooserDialog(this)
				}
				Buttons.ABOUT    -> newDialogBuilder().setTitle("About")
					.setMessage("Game written by Chris Caron")
					.setNegativeButton(R.string.popup_button_close, null)
					.show()
				Buttons.RESUME   -> {
					if (game.tryLoadFromFile(saveGame)) {
						startGameThread()
					}
				}
			}
		}
	}

	fun initMenu(buttons: List<*>) {
		listView.adapter = object : BaseAdapter() {
			override fun getCount(): Int {
				return buttons.size
			}

			override fun getItem(position: Int): Any? {
				return null
			}

			override fun getItemId(position: Int): Long {
				return 0
			}

			override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
				val convertView = convertView?:View.inflate(this@RiskActivity, R.layout.list_item, null)
				val b = convertView.findViewById<TextView>(R.id.text_view)
				b.tag = buttons[position]
				b.text = prettify(buttons[position]?:"")
				return b
			}
		}
	}

	lateinit var exploAnim: IntArray

	override fun onInit(g: DroidGraphics) {
		super.onInit(g)
		val cells = arrayOf(
			intArrayOf(57, 94, 115 - 57, 125 - 94), 
			intArrayOf(138, 73, 236 - 138, 125 - 73), 
			intArrayOf(252, 70, 370 - 252, 125 - 70), 
			intArrayOf(408, 48, 572 - 408, 125 - 48), 
			intArrayOf(47, 200, 187 - 47, 266 - 200), 
			intArrayOf(200, 177, 341 - 200, 266 - 177), 
			intArrayOf(353, 193, 400 - 353, 266 - 193), 
			intArrayOf(493, 197, 592 - 493, 266 - 197))
		exploAnim = g.loadImageCells(R.drawable.blowup_anim, cells)
	}
	
	
	override fun onDraw(g: DroidGraphics) {
		//        g.getPaint().setStrokeWidth(getResources().getDimension(R.dimen.roman_number_thickness));
		val board = game.board
		val imageRect = GRectangle(board.dimension)
		val boardRect = if (zoomRect.isEmpty()) imageRect else zoomRect
		g.ortho(boardRect)
		g.pushMatrix()
		run {
			dragDelta.wrap(Vector2D.ZERO, Vector2D(imageRect.w, imageRect.h))
			g.translate(dragDelta)
			g.pushMatrix()
			run {
				g.translate(0f, -imageRect.h)
				for (ii in 0..2) {
					g.pushMatrix()
					g.translate(-imageRect.w, 0f)
					for (i in 0..2) {
						// dont render if target rect is not visible
						val tl = imageRect.topLeft
						val br = imageRect.bottomRight
						g.transform(tl)
						g.transform(br)
						if (tl.x >= g.viewportWidth || tl.y >= g.viewportHeight || br.x <= 0 || br.y <= 0) {
							// dont render - offscreen
						} else {
							g.drawImage(R.drawable.risk_board, imageRect)
							pickCell(g, board)
							drawCells(g, board)
							drawHighlightedCells(g, board)
						}
						drawAnimations(g, ZORDER_GAME)
						g.translate(imageRect.w, 0f)
					}
					g.popMatrix()
					g.translate(0f, imageRect.h)
				}
			}
			g.popMatrix()
		}
		g.popMatrix()
		g.ortho()
		g.color = GColor.CYAN
		g.textHeight = resources.getDimension(R.dimen.text_height_info)
		if (game.numPlayers > 0)
			game.summary.draw(g, 10f, (g.viewportHeight - 10).toFloat(), Justify.LEFT, Justify.BOTTOM)
		if (message.isNotEmpty()) {
			g.drawJustifiedStringOnBackground((g.viewportWidth - 20).toFloat(), 20f, Justify.RIGHT, Justify.TOP, message, GColor.TRANSLUSCENT_BLACK, 10f, 5f)
		}
		drawAnimations(g, ZORDER_OVERLAY)
	}

	fun drawHighlightedCells(g: AGraphics, board: RiskBoard) {
		for (pair in highlightedCells) {
			val cell = board.getCell(pair.first!!)
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

	/*
    void drawRomanNumeral(AGraphics g, IVector2D cell, String numerals) {
        float thickness = getResources().getDimension(R.dimen.roman_number_thickness);
        AGraphics.Border [] borders = {
                new AGraphics.Border(AGraphics.BORDER_FLAG_NORTH, thickness, thickness, 0, -thickness/2, thickness*2/3),
                new AGraphics.Border(AGraphics.BORDER_FLAG_SOUTH, thickness, thickness, 0, -thickness/2, 0),
        };
        g.drawJustifiedStringBordered(cell, Justify.CENTER, Justify.CENTER, numerals, borders);
    }*/
	// EXPERIMENTAL
	fun drawArmy(g: AGraphics, cell: IVector2D?, army: Army, numTroops: Int) {
		val numerals = roman.toRoman(numTroops)
		val thickness = resources.getDimension(R.dimen.roman_number_thickness)
		val borders = arrayOf(
			AGraphics.Border(AGraphics.BORDER_FLAG_NORTH, thickness, thickness, 0f, -thickness / 2, thickness * 2 / 3),
			AGraphics.Border(AGraphics.BORDER_FLAG_SOUTH, thickness, thickness, 0f, -thickness / 2, 0f))
		g.color = GColor.BLACK
		val th = resources.getDimension(R.dimen.text_height_roman)
		g.textHeight = th + 4
		g.setTextStyles(AGraphics.TextStyle.BOLD)
		g.drawJustifiedStringBordered(cell, Justify.CENTER, Justify.CENTER, numerals, *borders)
		g.textHeight = th
		g.setTextStyles(AGraphics.TextStyle.NORMAL)
		g.color = army.color
		g.drawJustifiedStringBordered(cell, Justify.CENTER, Justify.CENTER, numerals, *borders)
	}

	fun drawCells(g: AGraphics, board: RiskBoard) {
		for (cell in board.cells) {
			cell.occupier?.let { occupier ->
				drawArmy(g, cell, occupier, cell.numArmies)
				/*
                String numerals = roman.toRoman(cell.getNumArmies());
                g.setColor(GColor.BLACK);
                float th = getResources().getDimension(R.dimen.text_height_roman);
                g.setTextHeight(th + 4);
                g.setTextStyles(AGraphics.TextStyle.BOLD);
                drawRomanNumeral(g, cell, numerals);
                g.setTextHeight(th);
                g.setTextStyles(AGraphics.TextStyle.NORMAL);
                g.setColor(cell.occupier.getColor());
                drawRomanNumeral(g, cell, numerals);
                 */
			}

			/* DRAW THE CELL OUTLINES
            board.renderCell(cell, g, 1);
            g.setColor(cell.getRegion().getColor());
            g.drawLineLoop(getResources().getDimension(R.dimen.cell_width));
            g.begin();
             */
			/* DRAW CONNECTION BETWEEN THE CELLS
            for (int idx : cell.getConnectedCells()) {
                MutableVector2D v0 = new MutableVector2D(cell);
                MutableVector2D v1 = new MutableVector2D(board.getCell(idx));
                float maxW = boardWidth / 2;
                if (v0.sub(v1).magSquared() > maxW * maxW) {
                    if (v1.getX() < boardWidth /2)
                        v1.subEq(boardWidth, 0);
                    else {
                        v0.addEq(boardWidth, 0);
                        g.vertex(v0);
                        g.vertex(v1);
                        g.setColor(GColor.CYAN);
                        g.drawLines(getResources().getDimension(R.dimen.cell_width));
                    }
                }
            }*/
		}
	}

	fun pickCell(g: AGraphics, board: RiskBoard) {
		//Log.d("TAP", "Viewport: " + g.getViewport());
		//Log.d("TAP", "BoardDim: " + board.getDimension());
		//Log.d("TAP", "tap Befone: " + tapPos);
		//Log.d("TAP", "DragDelta: " + dragDelta);
		val tap = MutableVector2D(tapPos)
		g.screenToViewport(tap)
		//Log.d("TAP", "Tap Viewport: " + tap);
		g.color = GColor.WHITE
		for (idx in pickableTerritories) {
			val cell = board.getCell(idx)
			board.renderCell(cell, g)
			g.drawLineLoop(resources.getDimension(R.dimen.cell_width))
			if (tapped) {
				if (board.isPointInsideCell(tap, idx)) {
					tapped = false
					tapPos.zero()
					setGameResult(idx)
					break
				}
			}
		}
	}

	var dragStart = MutableVector2D()
	var dragDelta = MutableVector2D()
	var tapped = false
	val tapPos = MutableVector2D()
	override fun onTap(x: Float, y: Float) {
		Log.d("TAP", "onTap ($x,$y)")
		tapPos[x] = y
		tapped = true
	}

	override fun onDragStart(x: Float, y: Float) {
		dragStart[x] = y
	}

	override fun onDrag(x: Float, y: Float) {
		if (zoomRect.isEmpty) {
			dragDelta.addEq(Vector2D(x, y).sub(dragStart))
			dragStart[x] = y
		}
	}

	override fun onBackPressed() {
		if (running) {
			stopGameThread()
		} else {
			super.onBackPressed()
		}
	}

	fun stopGameThread() {
		running = false
		setGameResult(null)
		if (!isFinishing) {
			runOnUiThread { initHomeMenu() }
		}
	}

	@Synchronized
	fun startGameThread() {
		if (running) return
		init()
		clearAnimations()
		initMenu(emptyList<Any>())
		running = true
		object : Thread() {
			override fun run() {
				while (running && !game.isDone) {
					try {
						game.runGame()
						redraw()
						if (result != null)
							game.trySaveToFile(saveGame)
						result = null
					} catch (e: Exception) {
						e.printStackTrace()
						break
					}
				}
				running = false
				runOnUiThread { initHomeMenu() }
				log.debug("game thread EXIT")
			}
		}.start()
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
		runOnUiThread { initMenu(emptyList<Any>()) }
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
		runOnUiThread { initMenu(listOf(Action.CANCEL)) }
		return waitForUser(Int::class.javaObjectType)
	}

	fun pickAction(options: List<Action>, msg: String): Action? {
		message = msg
		runOnUiThread { initMenu(options) }
		return waitForUser(Action::class.java)
	}

	fun startGame(players: List<RiskPlayer>) {
		init()
		game.clear()
		Utils.shuffle(players)
		for (pl in players) game.addPlayer(pl)
		startGameThread()
	}

	companion object {
		const val ZORDER_GAME = 0
		const val ZORDER_OVERLAY = 5
		lateinit var instance: RiskActivity
	}
}