package cc.game.dominos.core

import cc.game.dominos.core.Board.Companion.drawDie
import cc.game.dominos.core.Board.Companion.drawTile
import cc.lib.annotation.Keep
import cc.lib.game.*
import cc.lib.logger.LoggerFactory
import cc.lib.math.Bezier
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.net.AGameServer
import cc.lib.utils.*
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Dominos game biznezz logic.
 *
 * Example usage:
 *
 * D = new Dominos();
 * D.setNumPlayers(4);
 * D.startNewGame(6, 150);
 *
 * ...
 *
 * From ender thread:
 * D.draw(g, mouseX, mouseY);
 *
 * on mouse click events:
 * D.onClick();
 *
 * on mouse move event:
 * D.redraw()
 *
 * on drag start event:
 * D.onDragStarted(mouseX, mouseY)
 *
 * on drag end event
 * D.endDrag(mouseX, mouseY)
 */
abstract class Dominos : Reflector<Dominos?>(), AGameServer.Listener {
	@Omit
	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		var SPACING = 6f
		var TEXT_SIZE = 20f
		const val DELAY_BETWEEN = 700

		init {
			addAllFields(Dominos::class.java)
		}
	}

	private var players: Array<Player> = arrayOf(PlayerUser(1))
	private val pool: LinkedList<Tile> = LinkedList()
	var maxPips = 0
		private set
	var maxScore = 0
		private set
	private var turn = 0
	var difficulty = 0
		private set
	var board = Board()

	@Omit
	private var selectedPlayerTile = -1

	@Omit
	private var highlightedPlayerTile = -1

	@Omit
	val gameLock = Lock()

	@Omit
	private var dragging = false

	/**
	 * Clear out tiles and board but keep playre instances with restted scores
	 */
	@Synchronized
	fun reset() {
		stopGameThread()
		clearHighlghts()
		pool.clear()
		board.clear()
		for (p in players) {
			p.reset()
		}
		anims.clear()
	}

	/**
	 * Same as reset but also set players count to 0
	 */
	@Synchronized
	fun clear() {
		reset()
		players = arrayOf()
	}

	fun setPlayers(vararg players: Player) {
		if (isGameRunning) 
			throw GException("Cant assign while game running")
		if (players.size <= 0 || players.size > 4) 
			throw GException("Range of players can be 1 to 4")
		this.players = arrayOf(*players)
	}

	@Omit
	var isGameRunning = false
		private set

	fun initGame(maxPipNum: Int, maxScore: Int, difficulty: Int) {
		if (isGameRunning) throw GException()
		maxPips = maxPipNum
		this.maxScore = maxScore
		this.difficulty = difficulty
	}

	fun startNewGame() {
		if (players.size == 0) throw RuntimeException("No players!")
		stopGameThread()
		newGame()
		//        server.broadcastCommand(new GameCommand(MPConstants.SVR_TO_CL_INIT_ROUND)
//                .setArg("dominos", this.toString()));
		redraw()
	}

	fun stopGameThread() {
		log.debug("stop game thread")
		if (isGameRunning) {
			isGameRunning = false
			gameLock.releaseAll()
		}
	}

	@Keep
	protected open fun onGameOver(playerNum: Int) {
		//server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, playerNum);
		val winner = players[playerNum]
		addAnimation(winner.getName() + "WINNER", object : AAnimation<AGraphics>(1000, -1, true) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				val c = GColor(position, 1 - position, position, position)
				g.color = c
				if (repeat % 4 < 2) 
					g.drawJustifiedString(0f, 0f, Justify.LEFT, "WINNER!") 
				else 
					g.drawJustifiedString(0f, 0f, Justify.LEFT, winner.getName())
			}
		}, false)
	}

	private fun startPlayerPtsAnim(p: Player, pts: Int) {
		addAnimation(p.getName() + "PTS", object : AAnimation<AGraphics>(2000) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				val curPts = p.score + (position * pts).roundToInt()
				g.drawJustifiedString(0f, 0f, Justify.RIGHT, curPts.toString())
			}

			override fun onDone() {
				gameLock.release()
			}
		}, false)
	}

	// return the height of used area (numLines * textHgt)
	private fun drawPlayerInfo(g: AGraphics, p: Player, maxWidth: Float): Float {
		var a = anims[p.getName() + "PTS"]
		g.color = GColor.BLACK
		if (a != null) {
			g.translate(maxWidth, 0f)
			a.update(g)
			g.translate(-maxWidth, 0f)
			g.color = GColor.TRANSPARENT
		}
		var dim = g.drawJustifiedString(maxWidth, 0f, Justify.RIGHT, "${p.score}")
		anims[p.getName() + "WINNER"]?.let { a ->
			a.update(g)
			return dim.height
		}
		anims[p.getName() + "KNOCK"]?.let { a ->
			a.update(g)
			return dim.height
		}
		g.color = GColor.BLUE
		dim = g.drawWrapString(0f, 0f, maxWidth - dim.width - SPACING, p.getName())
		return dim.height
	}

	fun isInitialized(): Boolean {
		if (players.size < 2)
			return false
		return getWinner() < 0
	}

	@Synchronized
	fun startGameThread() {
		if (isGameRunning) 
			return
		isGameRunning = true
		if (!isInitialized()) 
			throw GException("Game not initialized")
		log.debug("startGameThread, currently running=$isGameRunning")
		object : Thread() {
			override fun run() {
				log.debug("Thread starting.")
				isGameRunning = true
				while (isGameRunning && !isGameOver()) {
					synchronized(gameLock) {
						runGame()
						redraw()
						if (isGameRunning) {
							sleep(100)
						}
					}
				}
				log.debug("Thread done.")
				val w = getWinner()
				if (isGameRunning && w >= 0) {
					onGameOver(w)
				}
				isGameRunning = false
				gameLock.releaseAll()
			}
		}.start()
	}

	private fun initPool() {
		pool.clear()
		for (i in 1..maxPips) {
			for (ii in i..maxPips) {
				pool.add(Tile(i, ii))
			}
		}
	}

	private fun newGame() {
		anims.clear()
		pool.clear()
		for (p in players) {
			p.reset()
		}
		board.clear()
		initPool()
	}

	fun getTurn(): Int {
		return turn
	}

	@Keep
	@Synchronized
	fun setTurn(turn: Int) {
		//server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, turn);
		if (turn >= 0 && turn < players.size) {
			val fromPlayer = players[this.turn]
			val toPlayer = players[turn]
			addAnimation("TURN", object : AAnimation<AGraphics>(1000) {
				protected override fun draw(g: AGraphics, position: Float, dt: Float) {
					g.color = GColor.YELLOW
					g.drawRect(fromPlayer.outlineRect.getInterpolationTo(toPlayer.outlineRect, position), 3f)
				}

				public override fun onDone() {
					gameLock.release()
				}
			}, true)
		}
		this.turn = turn
	}

	fun getNumPlayers(): Int = players.size

	fun setNumPlayers(num: Int) {
		if (isGameRunning) 
			throw GException()
		var d = difficulty
		players = Array(num) {
			when (it) {
				0 -> PlayerUser(0)
				else -> Player(it).also { it.smart = true }
			}
		}
	}

	fun getPlayer(num: Int): Player {
		return players[num]
	}

	fun getCurPlayer() : Player = players[turn]

	fun getPool(): List<Tile> {
		return pool
	}

	private fun newRound() {
		pool.shuffle()
		val numPerPlayer = if (players.size == 2) 7 else 5
		for (p in players) {
			for (i in 0 until numPerPlayer) {
				val t = pool.first
				onTileFromPool(p.playerNum, t)
			}
		}
		if (!placeFirstTile()) {
			newRound()
		} else {
			nextTurn()
		}
	}

	private fun nextTurn() {
		setTurn((turn + 1) % players.size)
	}

	private fun placeFirstTile(): Boolean {
		for (i in maxPips downTo 1) {
			for (p in players.indices) {
				val t = players[p].findTile(i, i)
				if (t != null) {
					onPlaceFirstTile(p, t)
					setTurn(p)
					redraw()
					return true
				}
			}
		}
		return false
	}

	private fun computePlayerMoves(p: Player): MutableList<Move> {
		val moves: MutableList<Move> = ArrayList()
		p.tiles.forEach { pc ->
			moves.addAll(board.findMovesForPiece(pc))
		}
		return moves
	}

	@Keep
	protected fun onPlaceFirstTile(player: Int, t: Tile) {
		//server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player, t);
		players[player].tiles.remove(t)
		board.placeRootPiece(t)
	}

	fun runGame() {

		// make sure players are numbered corrcetly
		for (i in players.indices)
			if (players[i].playerNum != i)
				throw GException("Players not numbered correctly")
		if (board.root == null) {
			onNewRound()
		}
		val p = players[turn]
		val moves = computePlayerMoves(p)
		while (moves.size == 0) {
			if (pool.size == 0) {
				// see if any player can move, otherwise new round
				var canMove = false
				for (pp in players) {
					if (computePlayerMoves(pp).size > 0) {
						canMove = true
						break
					}
				}
				if (!canMove) {
					onNewRound()
					return
				} else {
					// player knocks
					onKnock(turn)
					break
				}
			}
			val pc = pool.first
			onTileFromPool(turn, pc)
			moves.addAll(board.findMovesForPiece(pc))
		}
		do { // sumtin to break out of
			if (moves.size > 0) {
				val mv = p.chooseMove(this, moves)
				if (mv == null || !isGameRunning) return
				onTilePlaced(turn, mv.piece, mv.endpoint, mv.placment)
				val pts = board.computeEndpointsTotal()
				if (pts > 0 && pts % 5 == 0) {
					onPlayerPoints(turn, pts)
					if (isGameOver()) {
						break
					}
				}
			}
			if (p.tiles.size == 0) {
				// end of round
				// this player gets remaining tiles points from all other players rounded to nearest 5
				var pts = 0
				for (pp in players) {
					for (t in pp.tiles) {
						pts += t.pip1 + t.pip2
					}
				}
				pts = 5 * ((pts + 4) / 5)
				if (pts > 0) {
					onPlayerEndRoundPoints(turn, pts)
					if (isGameOver()) {
						break
					}
				}
				onNewRound()
			} else if (getWinner() < 0) {
				nextTurn()
			}
		} while (false)
		redraw()
	}

	fun isGameOver(): Boolean = getWinner() >= 0

	@Keep
	protected fun onTilePlaced(player: Int, tile: Tile, endpoint: Int, placement: Int) {
		//server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player, tile, endpoint, placement);
		board.doMove(tile, endpoint, placement)
		players[player].tiles.remove(tile)
		redraw()
	}

	@Keep
	protected fun onTileFromPool(player: Int, pc: Tile) {
		//server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player, pc);
		val p = players[player]
		pool.remove(pc)
		addAnimation(p.getName() + "POOL", object : AAnimation<AGraphics>(700) {
			protected override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.pushMatrix()
				g.translate(0f, 0.5f)
				g.scale(1f, Math.max(0.1f, position))
				g.translate(0f, -0.5f)
				if (p.isPiecesVisible()) {
					drawTile(g, pc.pip1, pc.pip2, position / 2)
				} else {
					drawTile(g, 0, 0, 1f)
				}
				g.popMatrix()
			}

			public override fun onDone() {
				gameLock.release()
			}
		}, true)
		p.tiles.add(pc)
		redraw()
	}

	@Keep
	protected fun onKnock(player: Int) {
		//server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player);
		val p = players[player]
		addAnimation(p.getName() + "KNOCK", object : AAnimation<AGraphics>(1000) {
			protected override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.color = GColor.YELLOW.withAlpha(1f - position)
				g.drawJustifiedString(0f, 0f, Justify.CENTER, "KNOCK")
			}
		}, true)
		redraw()
	}

	@Omit
	private val anims = Collections.synchronizedMap(HashMap<String, AAnimation<AGraphics>>())
	fun addAnimation(id: String, a: AAnimation<AGraphics>, block: Boolean) {
		anims[id] = a
		a.start<AAnimation<AGraphics>>()
		redraw()
		if (block && a.duration > 0) {
			gameLock.acquireAndBlock(a.duration + 500)
		}
	}

	internal inner class StackTilesAnim(val tiles: List<Tile>, val p: Player, val pts: Int) : AAnimation<AGraphics>((tiles.size * Companion.DELAY_BETWEEN + 3000).toLong()) {
		var rows: Int
		var cols: Int
		var num: Int
		var scale = 0f
		override fun draw(g: AGraphics, position: Float, dt: Float) {
			scale = Math.min(boardDim / (rows + 2).toFloat(), boardDim / (cols * 2 + 2).toFloat())
			val numToShow = (elapsedTime / Companion.DELAY_BETWEEN).toInt().coerceIn(0 .. num)
			drawTiles(g, numToShow, 0f)
			redraw()
		}

		fun drawTiles(g: AGraphics, numToShow: Int, positionFromCenter: Float) {
			g.setPointSize(scale / 8)
			g.pushMatrix()
			g.translate(boardDim / 2, boardDim / 2)
			g.scale(scale, scale)
			val startY = -0.5f * (rows - 1)
			val pos = MutableVector2D((-(cols - 1)).toFloat(), startY)
			var n = 0
			for (i in 0 until cols) {
				for (ii in 0 until rows) {
					if (n < numToShow) {
						val t = tiles[n++]
						g.pushMatrix()
						g.translate(pos.scaledBy(1.0f - positionFromCenter))
						g.translate(-1f, -0.5f)
						g.scale(0.9f, 0.9f)
						drawTile(g, t.pip1, t.pip2, 1.0f - positionFromCenter)
						g.translate(1f, 0.5f)
						g.popMatrix()
						pos.addEq(0f, 1f)
					}
				}
				pos.y = startY
				pos.addEq(2f, 0f)
			}
			g.popMatrix()
		}

		override fun onDone() {
			addAnimation("TILES", object : AAnimation<AGraphics>(1000) {
				override fun draw(g: AGraphics, position: Float, dt: Float) {
					drawTiles(g, num, position)
				}

				override fun onDone() {
					startPlayerPtsBoardGraphicAnim(p, pts)
				}
			}, false)
		}

		init {
			num = tiles.size
			rows = tiles.size
			cols = 1
			if (rows > 5) {
				rows = 5
				cols = 1 + (num - 1) / 5
			}
		}
	}

	private fun startPlayerPtsBoardGraphicAnim(p: Player, pts: Int) {
		addAnimation("TILES", object : AAnimation<AGraphics>(2000) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				val hgtStart = boardDim / 12
				val hgtStop = boardDim / 6
				g.textHeight = hgtStart + (hgtStop - hgtStart) * position
				g.color = GColor.MAGENTA.withAlpha(1.0f - position)
				g.drawJustifiedString(boardDim / 2, boardDim / 2, Justify.CENTER, Justify.CENTER, "+$pts")
			}

			override fun onDone() {
				startPlayerPtsAnim(p, pts)
			}
		}, false)
	}

	@Keep
	protected fun onPlayerEndRoundPoints(player: Int, pts: Int) {
		//server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player, pts);
		val p = players[player]

		// figure out how many pieces are left
		val tiles: MutableList<Tile> = ArrayList()
		for (i in players.indices) {
			tiles.addAll(players[i].tiles)
		}
		addAnimation("TILES", StackTilesAnim(tiles, p, pts), false)
		gameLock.acquireAndBlock(30000)
		p.score += pts
	}

	internal open inner class GlowEndpointAnimation(val endpoint: Int) : AAnimation<AGraphics>(500, 1, true) {
		val boundingRect = arrayOfNulls<Vector2D>(2)
		override fun draw(g: AGraphics, position: Float, dt: Float) {
			g.pushMatrix()
			g.clearMinMax()
			board.transformToEndpointLastPiece(g, endpoint)
			g.translate(-1f, 0f)
			val pips = board.getOpenPips(endpoint)
			g.translate(0.5f, 0.5f)
			val scale = position / 4 + 1
			g.scale(scale, scale)
			g.translate(-0.5f, -0.5f)
			g.color = GColor.BLACK
			g.drawFilledRoundedRect(0f, 0f, 1f, 1f, 0.25f)
			g.color = GColor.WHITE.interpolateTo(GColor.YELLOW, position)
			// On Android the stroke width gets scaled by the current transform which makes this look huge so remove it
//            g.drawRoundedRect(0, 0, 1, 1, 2/*1+Math.round(position*3)*/, 0.25f);
			drawDie(g, 0f, 0f, pips)
			boundingRect[0] = g.minBoundingRect
			boundingRect[1] = g.maxBoundingRect
			g.popMatrix()
		}
	}

	/**
	 * Called when player was earned pts > 0
	 * @param player
	 * @param pts
	 */
	@Keep
	protected fun onPlayerPoints(player: Int, pts: Int) {
		//server.broadcastExecuteOnRemote(MPConstants.DOMINOS_ID, player,pts);
		val p = players[player]
		var delay: Long = 0
		for (i in 0..3) {
			val isFirst = i == 0
			if (board.getOpenPips(i) > 0) {
				board.addAnimation(object : GlowEndpointAnimation(i) {
					public override fun onDone() {
						if (isFirst) startPlayerPtsBoardGraphicAnim(p, pts)
					}
				}.start(delay))
				delay += 300
			}
		}
		redraw()
		gameLock.acquireAndBlock()
		p.score += pts
	}

	protected fun onNewRound() {
		log.debug("onNewRound")
		for (p in players) {
			p.tiles.clear()
		}
		board.clear()
		initPool()
		//server.broadcastCommand(new GameCommand(MPConstants.SVR_TO_CL_INIT_ROUND).setArg("dominos", this));
		startShuffleAnimation()
		newRound()
	}

	@Omit
	private var boardDim = 0f

	fun init(g: APGraphics) {
		val w = g.viewportWidth.toFloat()
		val h = g.viewportHeight.toFloat()
		val portrait = h > w
		boardDim = if (portrait) w else h
	}

	@Synchronized
	fun draw(g: APGraphics, pickX: Int, pickY: Int) {
		val w = g.viewportWidth.toFloat()
		val h = g.viewportHeight.toFloat()
		g.ortho(0f, w, 0f, h)

		// choose square for board and remainder to be used to display players stats
		val portrait = h > w
		boardDim = if (portrait) w else h
		g.clearScreen(GColor.GREEN)
		g.color = GColor.GREEN.darkened(0.2f)
		g.drawFilledRect(0f, 0f, boardDim, boardDim)

		//g.setClipRect(0, 0, boardDim, boardDim);
		var drawDragged = false
		var anim = anims["TILES"]
		if (anim != null) {
			board.draw(g, boardDim, boardDim, pickX, pickY, null)
			anim.update(g)
		} else {
			var dragging: Tile? = null
			if (this.dragging && selectedPlayerTile >= 0) {
				dragging = getUser().tiles[selectedPlayerTile]
			}
			drawDragged = true
			if (board.draw(g, boardDim, boardDim, pickX, pickY, dragging) >= 0) drawDragged = false
		}
		drawMenuButton(g, boardDim, boardDim, pickX, pickY)
		//g.clearClip();
		g.pushMatrix()
		g.textHeight = TEXT_SIZE
		if (portrait) {
			// draw the non-visible player stuff on lower LHS and visible player stuff on lower RHS
			g.translate(0f, boardDim)
			//setupTextHeight(g, w/3, (h-boardDim)/5);
			drawInfo(g, w / 3 - SPACING, h - boardDim)
			g.translate(w / 3, 0f)
			drawPlayer(g, w * 2 / 3, h - boardDim, pickX, pickY, drawDragged)
		} else {
			// draw the non-visible player stuff on lower half of rhs and visible player stuff on
			// upper half of RHS
			g.translate(boardDim, 0f)
			//setupTextHeight(g, w-boardDim, h/5);
			drawPlayer(g, w - boardDim, h * 2 / 3, pickX, pickY, drawDragged)
			g.translate(0f, h * 2 / 3 + SPACING)
			drawInfo(g, w - boardDim, h / 3 - SPACING)
		}
		g.popMatrix()
		anim = anims["TURN"]
		anim?.update(g)
		if (anim == null && turn >= 0 && turn < players.size) {
			g.color = GColor.YELLOW
			g.drawRect(players[turn].outlineRect, 3f)
		}
		synchronized(anims) {
			anims.removeAll {
				it.value.isDone
			}
		}
		if (anims.isNotEmpty() || board.animations.isNotEmpty())
			redraw()
	}

	private fun drawInfo(g: APGraphics, w: Float, h: Float) {
		val bk = arrayOf(
			GColor.GREEN.darkened(.25f),
			GColor.GREEN.darkened(.5f)
		)
		g.pushMatrix()
		var index = 0
		for (i in players.indices) {
			val p = players[i]
			if (p.isPiecesVisible()) continue
			g.color = bk[index]
			index = (index + 1) % bk.size
			g.drawFilledRect(0f, 0f, p.outlineRect.w, p.outlineRect.h)
			val outline1: Vector2D = g.transform(0f, 0f)
			val dy = drawPlayerInfo(g, p, w)
			g.translate(0f, dy)
			g.translate(0f, SPACING)
			val numTiles = p.tiles.size
			val tileDim = g.textHeight / 2
			val anim = anims[p.getName() + "POOL"]
			var width = tileDim * 2 * numTiles + (numTiles - 1) * SPACING
			if (anim != null) {
				width += tileDim * 2 + SPACING
			}
			g.pushMatrix()
			run {
				if (width > w) {
					val saveth = g.textHeight
					g.textHeight = saveth / 2
					g.pushAndRun()
					{
						g.scale(tileDim, tileDim)
						drawTile(g, 0, 0, 1f)
					}
					g.translate(tileDim * 2 + SPACING, 0f)
					if (anim != null) {
						g.pushAndRun()
						{
							g.scale(tileDim, tileDim)
							anim.update(g)
						}
						g.translate(tileDim * 2 + SPACING, 0f)
					}
					g.color = GColor.BLUE
					g.drawJustifiedString(0f, tileDim / 2, Justify.LEFT, Justify.CENTER, "x $numTiles")
					g.setTextHeight(saveth)
				} else {
					for (t in 0 until numTiles) {
						g.pushAndRun()
						{
							g.scale(tileDim, tileDim)
							drawTile(g, 0, 0, 1f)
						}
						g.translate(tileDim * 2 + SPACING, 0f)
					}
					anim?.let { anim ->
						g.pushAndRun() {
							g.scale(tileDim, tileDim)
							anim.update(g)
						}
					}
				}
			}
			g.popMatrix()
			g.translate(0f, tileDim + SPACING)
			val outline2: Vector2D = g.transform(w, 0f)
			p.outlineRect[outline1] = outline2
		}
		g.popMatrix()
		if (isInitialized()) {
			g.pushMatrix()
			val dim = g.textHeight
			g.translate(0f, h - dim)
			g.scale(dim, dim)
			drawTile(g, 0, 0, 1f)
			g.popMatrix()
			g.color = GColor.BLUE
			val a = anims["POOL"]
			g.pushMatrix()
			g.translate(dim * 2 + SPACING, h - dim)
			a?.update(g)
				?: g.drawJustifiedString(0f, 0f, Justify.LEFT, Justify.TOP, String.format("x %d", pool.size))
			g.popMatrix()
		}
	}

	private fun drawPlayer(g: APGraphics, w: Float, h: Float, pickX: Int, pickY: Int, drawDragged: Boolean) {
		var h = h
		g.pushMatrix()
		for (i in players.indices) {
			if (players[i].isPiecesVisible()) {
				val p = players[i]
				p.outlineRect[g.transform(0f, 0f)] = g.transform(w, h)
				var dy = drawPlayerInfo(g, p, w)
				dy += SPACING
				g.translate(0f, dy)
				h -= dy
				drawPlayerTiles(g, p, w, h, pickX, pickY, drawDragged)
				break
			}
		}
		g.popMatrix()
	}

	private fun drawMenuButton(g: APGraphics, w: Float, h: Float, pickX: Int, pickY: Int) {
		var infoStr = "MENU"
		if (anims["POOL"] != null) {
			infoStr = "SKIP"
		}
		val menuButtonPadding = 10
		g.color = GColor.WHITE
		val x = w - menuButtonPadding
		val y = menuButtonPadding.toFloat()
		g.textHeight = TEXT_SIZE
		var tw = g.drawJustifiedString(x, y, Justify.RIGHT, infoStr).width
		val rx = w - tw - menuButtonPadding * 2
		val ry = 0f
		val rw = tw + menuButtonPadding * 2
		val rh = g.textHeight + menuButtonPadding * 2
		g.drawRoundedRect(rx, ry, rw, rh, 5f, (menuButtonPadding / 2).toFloat())
		menuHighlighted = false
		if (pickX >= 0 && pickY >= 0) {
			g.begin()
			g.setName(1)
			g.vertex(rx, ry)
			g.vertex(rx + rw, ry + rh)
			val picked = g.pickRects(pickX, pickY)
			tw += tw / 4
			if (picked == 1) {
				menuHighlighted = true
				g.color = GColor.DARK_GRAY
				g.drawJustifiedString(x, y, Justify.RIGHT, infoStr)
			}
			g.end()
		}
	}

	@Omit
	private var menuHighlighted = false
	private fun getUser() : PlayerUser = players[0] as PlayerUser

	private fun drawPlayerTiles(g: APGraphics, p: Player, w: Float, h: Float, pickX: Int, pickY: Int, drawDragged: Boolean) {
		val numPlayerTiles = p.tiles.size
		//int numVirtualTiles = numPlayerTiles;
		var numDrawnTiles = numPlayerTiles
		var playertiles: List<Tile>
		synchronized(p.tiles) { playertiles = ArrayList(p.tiles) }
		val poolAnim = anims[p.getName() + "POOL"]
		if (poolAnim != null) {
			numDrawnTiles++
		}
		if (numDrawnTiles == 0) return
		val aspect = w / (h * 2)
		val cols = 2 * floor(sqrt((aspect * numDrawnTiles * 2).toDouble())).toInt().coerceIn(1 .. 100)
		val rows = ceil((2f * numDrawnTiles / cols).toDouble()).toInt().coerceIn(2 .. 100)
		if (rows * cols / 2 < numDrawnTiles) {
			System.err.println("oooops")
		}
		val tilesPerRow = cols / 2
		val tileD = Math.min(h / rows, w / cols)
		g.setPointSize(tileD / 8)
		highlightedPlayerTile = -1
		var tile = 0
		g.pushMatrix()
		g.scale(tileD, tileD)
		for (i in 0 until rows) {
			g.pushMatrix()
			for (ii in 0 until tilesPerRow) {
				if (tile < numDrawnTiles) {
					val anim = tile >= numPlayerTiles
					if (!anim) {
						val t = playertiles[tile]
						var available = false
						getUser().takeIf { it.usable.contains(t) }?.let { user ->
							available = true
							g.setName(tile)
							g.vertex(0f, 0f)
							g.vertex(2f, 1f)
							val picked = g.pickRects(pickX, pickY)
							if (picked >= 0) {
								highlightedPlayerTile = picked
							}
						}
						g.pushMatrix()
						g.translate(0.04f, 0.02f)
						g.scale(0.95f, 0.95f)
						val alpha = if (available) 1f else 0.5f
						if (tile == selectedPlayerTile && dragging) {
							if (drawDragged) {
								g.pushMatrix()
								g.setIdentity()
								g.translate(pickX.toFloat(), pickY.toFloat())
								g.translate(-tileD, -tileD / 2)
								g.scale(tileD, tileD)
								drawTile(g, t.pip1, t.pip2, 1f)
								g.popMatrix()
							}
						} else {
							drawTile(g, t.pip1, t.pip2, alpha)
						}
						g.popMatrix()
						if (tile == selectedPlayerTile) {
							g.color = GColor.RED
							g.drawRect(0f, 0f, 2f, 1f, 3f)
						} else if (tile == highlightedPlayerTile) {
							g.color = GColor.YELLOW
							g.drawRect(0f, 0f, 2f, 1f, 3f)
						}
					} else {
						poolAnim?.update(g)
					}
					g.translate(2f, 0f)
					tile++
				}
			}
			g.popMatrix()
			g.translate(0f, 1f)
		}
		g.popMatrix()
	}

	fun getWinner(): Int {
		for (i in players.indices) {
			val p = players[i]
			if (p.score >= maxScore) {
				return i
			}
		}
		return -1
	}

	protected open fun onMenuClicked() {
		startNewGame()
		startGameThread()
	}

	fun clearHighlghts() {
		menuHighlighted = false
		highlightedPlayerTile = -1
		selectedPlayerTile = -1
		board.clearSelection()
		board.highlightMoves(null)
	}

	@Synchronized
	fun onClick() {
		if (menuHighlighted) {
			onMenuClicked()
			clearHighlghts()
		}
		getUser().let { user ->
			if (highlightedPlayerTile >= 0) {
				selectedPlayerTile = highlightedPlayerTile
				val highlightedMoves: MutableList<Move> = ArrayList()
				for (m in user.moves) {
					if (m.piece === user.tiles[selectedPlayerTile]) {
						highlightedMoves.add(m)
					}
				}
				board.highlightMoves(highlightedMoves)
			} else if (board.selectedMove != null) {
				user.chosenMove = board.selectedMove
				gameLock.releaseAll()
				clearHighlghts()
			} else {
				clearHighlghts()
				log.debug("endpoints total: " + board.computeEndpointsTotal())
			}
			redraw()
		}
	}

	@Synchronized
	fun startDrag() {
		dragging = true
		if (selectedPlayerTile < 0 && highlightedPlayerTile >= 0) {
			getUser().let { user ->
				selectedPlayerTile = highlightedPlayerTile
				val highlightedMoves: MutableList<Move> = ArrayList()
				for (m in user.moves) {
					if (m.piece.equals(user.tiles[selectedPlayerTile])) {
						highlightedMoves.add(m)
					}
				}
				board.highlightMoves(highlightedMoves)
			}
		}
		redraw()
	}

	@Synchronized
	fun stopDrag() {
		if (dragging) {
			if (selectedPlayerTile >= 0 && board.selectedMove != null) {
				getUser().let { user ->
					user.chosenMove = board.selectedMove
					gameLock.releaseAll()
				}
			}
			dragging = false
		}
		board.clearSelection()
		selectedPlayerTile = -1
		highlightedPlayerTile = -1
		redraw()
	}

	fun startShuffleAnimation() {
		val gamePool: MutableList<Tile> = ArrayList(pool)
		addAnimation("POOL", object : AAnimation<AGraphics>(1000, -1) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.drawJustifiedString(0f, 0f, Justify.LEFT, Justify.TOP, String.format("x %d", gamePool.size))
			}
		}, false)
		board.addAnimation(ShuffleAnimation(gamePool).start())
		gameLock.acquireAndBlock()
		anims.remove("POOL")
	}

	internal inner class ShuffleAnimation(val gamePool: MutableList<Tile>) : AAnimation<AGraphics>(20, gamePool.size) {
		val pool: MutableList<Tile> = ArrayList()
		val rows // = (int)Math.round(Math.sqrt(pool.size()*2));
			: Int
		val cols // = 2 * (pool.size() / rows);
			: Int
		val positions: Array<MutableVector2D>
		override fun draw(g: AGraphics, position: Float, dt: Float) {
			val DIM = Math.min(board.boardHeight / (rows + 2), board.boardWidth / (cols + 2))
			g.setPointSize(DIM / 8)
			g.pushMatrix()
			g.setIdentity()
			g.scale(DIM, DIM)
			g.translate(1f, 0.5f)
			val repeats = repeat
			for (t in 0 until Math.min(pool.size, repeats)) {
				g.pushMatrix()
				g.translate(positions[t])
				g.scale(0.95f, 0.95f)
				g.translate(-1f, -0.5f)
				val tile = pool[t]
				drawTile(g, tile.pip1, tile.pip2, 1f)
				g.popMatrix()
			}
			g.popMatrix()
		}

		override fun onDone() {
			// flip each one over in succession with some overlapp
			// total time to flip should be 1 second with 25? ms inbetween starts
			var delay = 0
			val delayStep = 50
			val flipTime: Long = 500
			for (i in positions.indices) {
				val pos: Vector2D = positions[i]
				val tile = pool[i]
				val DIM = Math.min(board.boardHeight / (rows + 2), board.boardWidth / (cols + 2))
				val isLast = i == positions.size - 1
				val dur = flipTime + delayStep * (positions.size - 1 - i)
				board.addAnimation(object : AAnimation<AGraphics>(dur) {
					override fun drawPrestart(g: AGraphics) {
						g.setPointSize(DIM / 8)
						g.pushMatrix()
						g.setIdentity()
						g.scale(DIM, DIM)
						g.translate(1f, 0.5f)
						g.translate(pos)
						g.scale(0.95f, 0.95f)
						g.translate(-1f, -0.5f)
						drawTile(g, tile.pip1, tile.pip2, 1f)
						g.popMatrix()
					}

					override fun draw(g: AGraphics, position: Float, dt: Float) {
						g.setPointSize(DIM / 8)
						g.pushMatrix()
						g.setIdentity()
						g.scale(DIM, DIM)
						g.translate(1f, 0.5f)
						g.translate(pos)
						g.scale(0.95f, 0.95f)
						g.translate(-1f, -0.5f)
						val pos = elapsedTime.toFloat() / flipTime
						if (elapsedTime < flipTime / 2) {
							g.translate(0f, 0.5f)
							g.scale(1f, 1 - pos * 2)
							g.translate(0f, -0.5f)
							drawTile(g, tile.pip1, tile.pip2, 1f)
						} else if (elapsedTime < flipTime) {
							g.translate(0f, 0.5f)
							g.scale(1f, pos * 2 - 1)
							g.translate(0f, -0.5f)
							drawTile(g, 0, 0, 1f)
						} else {
							drawTile(g, 0, 0, 1f)
						}
						g.popMatrix()
					}

					override fun onDone() {
						if (isLast) {
							// start an animation of the tiles bouncing around the board edges
							val velocities = Array(positions.size) {
								val speed = 25f.random() + 25f
								MutableVector2D(
									if (Utils.flipCoin()) -speed else speed,
									if (Utils.flipCoin()) -speed else speed
								)
							}
							board.addAnimation(object : AAnimation<AGraphics>(5000) {
								override fun draw(g: AGraphics, position: Float, dt: Float) {
									g.setPointSize(DIM / 8)
									g.pushMatrix()
									g.setIdentity()
									g.scale(DIM, DIM)
									g.translate(1f, 0.5f)
									for (i in positions.indices) {
										val dv: Vector2D = velocities[i].scaledBy(dt)
										positions[i].addEq(dv)
										if (positions[i].x < 0 && dv.x < 0 ||
											positions[i].x > cols && dv.x > 0) {
											//positions[i].setX(positions[i].getX())
											positions[i].subEq(dv)
											velocities[i].scaleEq(-1f, 1f)
											positions[i].addEq(velocities[i].scaledBy(dt))
										}
										if (positions[i].y < 0 && dv.y < 0 ||
											positions[i].y > rows && dv.y > 0) {
											positions[i].subEq(dv)
											velocities[i].scaleEq(1f, -1f)
											positions[i].addEq(velocities[i].scaledBy(dt))
										}
										g.pushMatrix()
										g.translate(positions[i])
										g.scale(0.95f, 0.95f)
										g.translate(-1f, -0.5f)
										drawTile(g, 0, 0, 1f)
										g.popMatrix()
									}
									g.popMatrix()
									g.setIdentity()
									g.color = GColor.CYAN
									g.textHeight = board.boardHeight / 20
									g.drawJustifiedString(board.boardWidth / 2, board.boardHeight / 2, Justify.CENTER, Justify.CENTER, "SHUFFLING")
								}

								override fun onDone() {
									// finally shuffle all the tiles into the boneyard
									var delay = 0
									val delayStep = 30
									for (i in positions.indices) {
										val tile = pool[i]
										val pos: Vector2D = positions[i]
										val isLast = i == positions.size - 1
										board.addAnimation(object : AAnimation<AGraphics>(600) {
											override fun drawPrestart(g: AGraphics) {
												g.setPointSize(DIM / 8)
												g.pushMatrix()
												g.setIdentity()
												g.scale(DIM, DIM)
												g.translate(1f, 0.5f)
												g.translate(pos)
												g.scale(0.95f, 0.95f)
												g.translate(-1f, -0.5f)
												drawTile(g, 0, 0, 1f)
												g.popMatrix()
											}

											override fun draw(g: AGraphics, position: Float, dt: Float) {
												val boneyard: Vector2D
												boneyard = if (g.viewportWidth > g.viewportHeight) {
													// landscape
													Vector2D((rows + 3).toFloat(), 1f)
												} else {
													Vector2D(1f, (rows + 3).toFloat())
												}
												g.setPointSize(DIM / 8)
												g.pushMatrix()
												g.setIdentity()
												g.scale(DIM, DIM)
												g.translate(1f, 0.5f)
												val dv: Vector2D = boneyard.sub(pos)
												val p: Vector2D = pos.add(dv.scaledBy(position))
												g.translate(p)
												g.scale(1 - position, 1 - position)
												g.translate(-1f, -0.5f)
												drawTile(g, 0, 0, 1f)
												g.popMatrix()
											}

											override fun onDone() {
												gamePool.add(tile)
												if (isLast) {
													gameLock.releaseAll()
												}
											}
										}.start(delay.toLong()))
										delay += delayStep
									}
								}
							}.start())
						}
					}
				}.start(delay.toLong()))
				delay += delayStep
			}
		}

		init {
			pool.addAll(gamePool)
			gamePool.clear()
			rows = (Math.sqrt((pool.size * 2).toDouble())).roundToInt()
			cols = 2 * (pool.size / rows)
			var r = 0
			var c = 1
			positions = Array(pool.size) {
				MutableVector2D(c.toFloat(), r + 0.5f).also {
					c += 2
					if (c >= cols) {
						c = 1
						r++
					}
				}
			}
		}
	}

	internal open inner class IntroAnim : AAnimation<AGraphics>(8000, 1, true) {
		val dominosPositions = arrayOf(
			arrayOf(Vector2D(0.5f, 1f), 90, -1, -1),
			arrayOf(Vector2D(0.5f, 3f), 90, -1, -1),
			arrayOf(Vector2D(0.5f, 5f), 90, -1, -1),
			arrayOf(Vector2D(2f, 5.5f), 0, -1, -1),
			arrayOf(Vector2D(3.5f, 4f), 90, -1, -1),
			arrayOf(Vector2D(3.5f, 2f), 90, -1, -1),
			arrayOf(Vector2D(2f, 0.5f), 0, -1, -1),
			arrayOf(Vector2D(5f, 2f), 90, -1, -1),
			arrayOf(Vector2D(5f, 4f), 90, -1, -1),
			arrayOf(Vector2D(6f, 5.5f), 0, -1, -1),
			arrayOf(Vector2D(7f, 4f), 90, -1, -1),
			arrayOf(Vector2D(7f, 2f), 90, -1, -1),
			arrayOf(Vector2D(6f, 0.5f), 0, -1, -1),
			arrayOf(Vector2D(8.5f, 1f), 90, -1, -1),
			arrayOf(Vector2D(8.5f, 3f), 90, -1, -1),
			arrayOf(Vector2D(8.5f, 5f), 90, -1, -1),
			arrayOf(Vector2D(9.5f, 2f), 90, -1, -1),
			arrayOf(Vector2D(10.5f, 3f), 90, 3, 0),
			arrayOf(Vector2D(11.5f, 2f), 90, 5, 0),
			arrayOf(Vector2D(12.5f, 1f), 270, 3, 0),
			arrayOf(Vector2D(12.5f, 3f), 90, -1, -1),
			arrayOf(Vector2D(12.5f, 5f), 90, -1, -1),
			arrayOf(Vector2D(14.5f, 0.5f), 0, -1, -1),
			arrayOf(Vector2D(16.5f, 0.5f), 0, -1, -1),
			arrayOf(Vector2D(15.5f, 2f), 90, -1, -1),
			arrayOf(Vector2D(15.5f, 4f), 90, -1, -1),
			arrayOf(Vector2D(14.5f, 5.5f), 0, -1, -1),
			arrayOf(Vector2D(16.5f, 5.5f), 0, -1, -1),
			arrayOf(Vector2D(18.5f, 1f), 90, -1, -1),
			arrayOf(Vector2D(18.5f, 3f), 90, -1, -1),
			arrayOf(Vector2D(18.5f, 5f), 90, -1, -1),
			arrayOf(Vector2D(19.5f, 2f), 90, -1, -1),
			arrayOf(Vector2D(20.5f, 3f), 90, -1, -1),
			arrayOf(Vector2D(21.5f, 4f), 90, -1, -1),
			arrayOf(Vector2D(22.5f, 1f), 90, -1, -1),
			arrayOf(Vector2D(22.5f, 3f), 90, -1, -1),
			arrayOf(Vector2D(22.5f, 5f), 90, -1, -1),
			arrayOf(Vector2D(24f, 2f), 90, -1, -1),
			arrayOf(Vector2D(24f, 4f), 90, -1, -1),
			arrayOf(Vector2D(25f, 5.5f), 0, -1, -1),
			arrayOf(Vector2D(26f, 4f), 90, -1, -1),
			arrayOf(Vector2D(26f, 2f), 90, -1, -1),
			arrayOf(Vector2D(25f, 0.5f), 0, -1, -1),
			arrayOf(Vector2D(28.5f, 0.5f), 0, -1, -1),
			arrayOf(Vector2D(27.5f, 2f), 90, -1, -1),
			arrayOf(Vector2D(29f, 3f), 0, -1, -1),
			arrayOf(Vector2D(29.5f, 4.5f), 90, -1, -1),
			arrayOf(Vector2D(28f, 5.5f), 0, -1, -1))
		val old_dominosPositions = arrayOf(
			arrayOf(Vector2D(.5f, 2f), 90, 6, 6),
			arrayOf(Vector2D(.5f, 4f), 90, 6, 6),
			arrayOf(Vector2D(1f, 5.5f), 0, 6, 6),
			arrayOf(Vector2D(2.5f, 4.5f), 90, 6, 6),
			arrayOf(Vector2D(2.5f, 2.5f), 90, 6, 6),
			arrayOf(Vector2D(2f, 1f), 0, 6, 6),
			arrayOf(Vector2D(5f, 2.5f), 0, 6, 6),
			arrayOf(Vector2D(4f, 4f), 90, 6, 6),
			arrayOf(Vector2D(5f, 5.5f), 0, 6, 6),
			arrayOf(Vector2D(6f, 4f), 90, 6, 6),
			arrayOf(Vector2D(7.5f, 5f), 90, 6, 6),
			arrayOf(Vector2D(8f, 3.5f), 0, 6, 6),
			arrayOf(Vector2D(9.5f, 4.5f), 90, 5, 6),
			arrayOf(Vector2D(11f, 3.5f), 0, 6, 6),
			arrayOf(Vector2D(11.5f, 5f), 90, 6, 6),
			arrayOf(Vector2D(13f, 3f), 90, 9, 0),
			arrayOf(Vector2D(13f, 5f), 90, 6, 6),
			arrayOf(Vector2D(14.5f, 5f), 90, 6, 6),
			arrayOf(Vector2D(15f, 3.5f), 0, 6, 6),
			arrayOf(Vector2D(16.5f, 5f), 90, 6, 6),
			arrayOf(Vector2D(18f, 4f), 90, 6, 6),
			arrayOf(Vector2D(19f, 2.5f), 0, 6, 6),
			arrayOf(Vector2D(20f, 4f), 90, 6, 6),
			arrayOf(Vector2D(19f, 5.5f), 0, 6, 6),
			arrayOf(Vector2D(22.5f, 1.5f), 0, 6, 6),
			arrayOf(Vector2D(21.5f, 3f), 90, 6, 6),
			arrayOf(Vector2D(23f, 3.5f), 0, 6, 6),
			arrayOf(Vector2D(23.5f, 5f), 90, 6, 6),
			arrayOf(Vector2D(22f, 5.5f), 0, 6, 6))
		lateinit var starts: Array<Bezier>
		lateinit var angSpeeds: FloatArray
		lateinit var angles: FloatArray
		fun init(dim: Float, offset: Vector2D) {
			starts = Array(dominosPositions.size) { n ->
				Bezier().also {
					if (n < dominosPositions.size / 4)
						it.addPoint(-1f, dim.random())
							.addPoint(dim / 2 + (dim /2).random(), (dim /2).random())
							.addPoint(dim / 2 + (dim /2).random(), dim / 2 + (dim /2).random())
							.addPoint((dominosPositions[n][0] as Vector2D).add(offset))
					else if (n < dominosPositions.size / 2)
						it.addPoint(dim.random(), -1f)
							.addPoint(dim / 2 + (dim /2).random(), dim / 2 + (dim /2).random())
							.addPoint((dim /2).random(), dim / 2 + (dim /2).random())
							.addPoint((dominosPositions[n][0] as Vector2D).add(offset))
					else if (n < dominosPositions.size * 3 / 4)
						it.addPoint(dim + 1, dim.random())
							.addPoint((dim /2).random(), dim / 2 + (dim /2).random())
							.addPoint((dim /2).random(), (dim /2).random())
							.addPoint((dominosPositions[n][0] as Vector2D).add(offset))
					else
						it.addPoint((dim).random(), dim + 1)
							.addPoint((dim / 2).random(), (dim / 2).random())
							.addPoint(dim / 2 + (dim / 2).random(), dim.random())
							.addPoint((dominosPositions[n][0] as Vector2D).add(offset))
				}
			}
			angSpeeds = FloatArray(dominosPositions.size) {
				100 * 50f.randomSigned()
			}
			angles = FloatArray(dominosPositions.size) {
				(dominosPositions[it][1] as Int).toFloat() - angSpeeds[it]
			}
			for (i in dominosPositions.indices) {
				val o = dominosPositions[i]
				val pip1 = o[2] as Int
				val pip2 = o[3] as Int
				if (pip1 < 0) o[2] = random(1..6)
				if (pip2 < 0) o[3] = random(1..6)
			}
		}

		var dim: Float = 0f

		override fun onStarted(g: AGraphics) {
			val min = MutableVector2D(Vector2D.MAX)
			val max = MutableVector2D(Vector2D.MIN)
			dominosPositions.forEach {
				val v = it[0] as Vector2D
				min.minEq(v)
				max.maxEq(v)
			}
			dim = board.boardWidth / (max.x - min.x + 2)
			init(board.boardWidth / dim, Vector2D(.5f, 9f))
		}

		override fun draw(g: AGraphics, position: Float, dt: Float) {
			val pos = (position * 2).coerceIn(0f, 1f)
			g.setPointSize(dim / 8)
			g.pushMatrix()
			g.setIdentity()
			g.scale(dim, dim)
			//g.translate(.5f, 9);
			for (i in dominosPositions.indices) {
				val o = dominosPositions[i]
				val angle = o[1] as Int
				val pip1 = o[2] as Int
				val pip2 = o[3] as Int
				g.pushMatrix()
				//g.translate(dominosPositions[i]);
				g.translate(starts[i].getAtPosition(pos))
				//g.rotate(angle);
				val ang = angles[i] + pos * angSpeeds[i]
				g.rotate(ang)
				g.translate(-1f, -.5f)
				drawTile(g, pip1, pip2, 1f)
				g.popMatrix()
			}
			g.popMatrix()
		}

		override fun onDone() {
			board.addAnimation(IntroAnim().start(5000))
		}
	}

	fun startDominosIntroAnimation(onDoneRunnable: Runnable?) {
		board.animations.clear()
		board.addAnimation(object : IntroAnim() {
			override fun onDone() {
				onDoneRunnable?.run()
				super.onDone()
			}
		}.start())
	}

	abstract fun redraw()
}