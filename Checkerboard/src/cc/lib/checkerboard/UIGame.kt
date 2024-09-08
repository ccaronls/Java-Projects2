package cc.lib.checkerboard

import cc.lib.checkerboard.AIPlayer.Companion.cancel
import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.IVector2D
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.Bezier
import cc.lib.math.Vector2D
import cc.lib.utils.Lock
import cc.lib.utils.Table
import java.io.File
import java.util.Collections

abstract class UIGame : Game() {
	lateinit var saveFile: File
	var isGameRunning = false
	val runLock = Lock()
	val animLock = Lock()
	var mode = 0
	var draggingPos = -1
	var highlightedPos = -1
	val pickablePieces: MutableList<Piece> = ArrayList()
	val pickableMoves: MutableList<Move> = ArrayList()
	var showInstructions = true
	var SQ_DIM = 0f
	var PIECE_RADIUS = 0f
	var BORDER_WIDTH = 0f
	var SCREEN_DIM: GDimension? = null
	var BOARD_DIM: GDimension? = null
	val DAMA_BACKGROUND_COLOR = GColor(-0x21657)
	val BORDER_COLOR = GColor(-0x2d4b74)

	//final List<PieceAnim> animations = Collections.synchronizedList(new ArrayList<>());
	val animations = Collections.synchronizedMap(HashMap<Int, PieceAnim>())
	fun init(saveFile: File): Boolean {
		this.saveFile = saveFile
		try {
			loadFromFile(saveFile)
			countPieceMoves()
			return true
		} catch (e: Exception) {
			e.printStackTrace()
			setRules(Chess())
			setPlayer(NEAR, UIPlayer(UIPlayer.Type.USER))
			setPlayer(FAR, UIPlayer(UIPlayer.Type.AI))
			newGame()
		}
		return false
	}

	override fun newGame() {
		super.newGame()
		pickablePieces.clear()
		pickableMoves.clear()
		animations.clear()
		mode = 0
		draggingPos = -1
		highlightedPos = -1
		showInstructions = true
	}

	fun undoAndRefresh(): Move {
		val m = super.undo()
		pickablePieces.clear()
		pickableMoves.clear()
		runLock.release()
		repaint(0)
		return m
	}

	abstract fun repaint(delayMs: Long)
	@Synchronized
	fun startGameThread() {
		if (isGameRunning) return
		isGameRunning = true
		Thread {
			Thread.currentThread().name = "runGame"
			try {
				while (isGameRunning) {
					log.debug("run game")
					runGame()
					if (isGameRunning) {
						if (isGameOver()) break
						trySaveToFile(saveFile)
						repaint(0)
					} else {
						log.debug("leaving")
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
			repaint(0)
			isGameRunning = false
			log.debug("game thread stopped")
		}.start()
	}

	@Synchronized
	fun stopGameThread() {
		if (isGameRunning) {
			cancel()
			isGameRunning = false
			runLock.releaseAll()
		}
	}

	@Synchronized
	fun draw(g: AGraphics, mx: Int, my: Int) {
		g.clearScreen(GColor.GRAY)
		SCREEN_DIM = GDimension(g.viewportWidth.toFloat(), g.viewportHeight.toFloat())
		if (getRules() is DragonChess) {
			// always landscape using the portion of the screen not used by the board
			SQ_DIM = Math.min(SCREEN_DIM!!.getWidth(), SCREEN_DIM!!.getHeight()) / Math.min(ranks + 1, columns)
			BOARD_DIM = GDimension(SQ_DIM * columns, SQ_DIM * ranks)
			g.pushMatrix()
			g.translate(0f, SQ_DIM)
			drawDragonChess(g, mx, my)
			g.popMatrix()
		} else if (SCREEN_DIM!!.aspect > 1) {
			// landscape draws board on the left and captured pieces on the right
			SQ_DIM = Math.min(SCREEN_DIM!!.getWidth(), SCREEN_DIM!!.getHeight()) / Math.min(ranks, columns)
			BOARD_DIM = GDimension(SQ_DIM * columns, SQ_DIM * ranks)
			drawLandscape(g, mx, my)
		} else {
			// portrait draws board in center and captured pieces in front of each player
			SQ_DIM = Math.min(SCREEN_DIM!!.getWidth(), SCREEN_DIM!!.getHeight()) / Math.min(ranks, columns)
			BOARD_DIM = GDimension(SQ_DIM * columns, SQ_DIM * ranks)
			drawPortrait(g, mx, my)
		}
	}

	private fun drawPortrait(g: AGraphics, mx: Int, my: Int) {
		g.pushMatrix()
		val infoHgt = SCREEN_DIM!!.getHeight() / 2 - BOARD_DIM!!.getHeight() / 2
		val infoDim = GDimension(SCREEN_DIM!!.getWidth(), infoHgt)
		drawPlayer(getPlayer(FAR) as UIPlayer?, g, infoDim)
		g.translate(0f, infoHgt)
		drawBoard(g, mx, my)
		g.translate(0f, BOARD_DIM!!.getHeight())
		drawPlayer(getPlayer(NEAR) as UIPlayer?, g, infoDim)
		g.popMatrix()
	}

	private fun drawDragonChess(g: AGraphics, mx: Int, my: Int) {
		val info = GDimension(SQ_DIM * 3 + SCREEN_DIM!!.getWidth() - BOARD_DIM!!.getWidth(), SQ_DIM * 3)
		drawBoard(g, mx, my)
		g.pushMatrix()
		g.translate(BOARD_DIM!!.getWidth() - SQ_DIM * 3, 0f)
		drawPlayer(getPlayer(FAR) as UIPlayer?, g, info)
		g.translate(0f, info.getHeight() + SQ_DIM * 4)
		drawPlayer(getPlayer(NEAR) as UIPlayer?, g, info)
		g.popMatrix()
	}

	private fun drawLandscape(g: AGraphics, mx: Int, my: Int) {
		val info = GDimension(SCREEN_DIM!!.getWidth() - BOARD_DIM!!.getWidth(), SCREEN_DIM!!.getHeight() / 2)
		drawBoard(g, mx, my)
		g.pushMatrix()
		g.translate(BOARD_DIM!!.getWidth(), 0f)
		drawPlayer(getPlayer(FAR) as UIPlayer?, g, info)
		g.translate(0f, info.getHeight())
		drawPlayer(getPlayer(NEAR) as UIPlayer?, g, info)
		g.popMatrix()
	}

	private fun drawPlayer(player: UIPlayer?, g: AGraphics, info: GDimension) {
		var info = info
		if (player == null) return
		val BORDER = 5
		if (player.playerNum == turn) {
			g.color = GColor.CYAN
			g.drawRect(info, 3f)
		}
		g.pushMatrix()
		g.translate(BORDER.toFloat(), BORDER.toFloat())
		info = info.adjustedBy((-BORDER * 2).toFloat(), (-BORDER * 2).toFloat())
		drawCapturedPieces(g, info, opponent, getCapturedPieces(player.playerNum))

		// draw player info
		var txt = player.type.name
		if (player.type === UIPlayer.Type.AI) {
			txt += " ("
			txt += when (player.maxSearchDepth) {
				1 -> "Easy"
				2 -> "Medium"
				3 -> "Hard"
				else -> "MaxDepth: " + player.maxSearchDepth
			}
			txt += ")"
		}
		if (player.isThinking) {
			var secs = player.thinkingTimeSecs
			val mins = secs / 60
			secs -= mins * 60
			txt += String.format("\nThinking %d:%02d", mins, secs)
			repaint(1000)
		}
		g.color = GColor.WHITE
		g.drawJustifiedString(info.getWidth(), info.getHeight(), Justify.RIGHT, Justify.BOTTOM, txt)
		g.popMatrix()
	}

	private fun drawCapturedPieces(g: AGraphics, dim: GDimension, playerNum: Int, pieces: List<PieceType?>?) {
		if (getRules() is Columns) return
		if (pieces == null) return
		//        g.setClipRect(0, 0, width, height);
		var x = PIECE_RADIUS
		var y = PIECE_RADIUS * 3
		for (pt in pieces) {
			g.pushMatrix()
			g.translate(x, y)
			val color = getPlayer(playerNum)!!.color
			drawPiece(g, pt, playerNum) //.getDisplayType(), color, PIECE_RADIUS*2, PIECE_RADIUS*2, null);
			g.popMatrix()
			x += PIECE_RADIUS * 2
			if (x >= dim.getWidth()) {
				x = PIECE_RADIUS
				y += PIECE_RADIUS * 2
			}
		}

//        g.clearClip();
	}

	private fun drawCheckerboardImage(g: AGraphics, id: Int, boardImageDim: Float, boardImageBorder: Float) {
		BORDER_WIDTH = boardImageBorder * BOARD_DIM!!.getWidth() / boardImageDim
		val boardWidth = BOARD_DIM!!.getWidth() - 2 * BORDER_WIDTH
		val boardHeight = BOARD_DIM!!.getHeight() - 2 * BORDER_WIDTH
		SQ_DIM = boardWidth / columns
		g.drawImage(id, 0f, 0f, BOARD_DIM!!.getWidth(), BOARD_DIM!!.getHeight())
		g.translate(BORDER_WIDTH, BORDER_WIDTH)
		if (DEBUG) {
			g.color = GColor.RED
			g.drawRect(0f, 0f, boardWidth, boardHeight)
		}
	}

	protected abstract val checkerboardImageId: Int
	private fun drawCheckerboard8x8(g: AGraphics) {
		drawCheckerboardImage(g, checkerboardImageId, 545f, 26f)
	}

	private fun drawCheckboardBoard(g: AGraphics, dark: GColor, light: GColor) {
		val color = arrayOf(
			dark, light
		)
		var colorIdx = 0
		BORDER_WIDTH = SQ_DIM / 2
		val boardWidth = BOARD_DIM!!.getWidth() - 2 * BORDER_WIDTH
		val boardHeight = BOARD_DIM!!.getHeight() - 2 * BORDER_WIDTH
		SQ_DIM = boardWidth / columns
		g.color = BORDER_COLOR
		g.drawFilledRoundedRect(0f, 0f, BOARD_DIM!!.getWidth(), BOARD_DIM!!.getHeight(), BORDER_WIDTH / 2)
		g.translate(BORDER_WIDTH, BORDER_WIDTH)
		g.pushMatrix()
		for (i in 0 until ranks) {
			g.pushMatrix()
			for (ii in 0 until columns) {
				g.color = color[colorIdx]
				colorIdx = (colorIdx + 1) % 2
				if (isOnBoard(i, ii)) g.drawFilledRect(0f, 0f, SQ_DIM, SQ_DIM)
				g.translate(SQ_DIM, 0f)
			}
			g.popMatrix()
			g.translate(0f, SQ_DIM)
			colorIdx = (colorIdx + 1) % 2
		}
		if (DEBUG) {
			g.color = GColor.RED
			g.drawRect(0f, 0f, boardWidth, boardHeight)
		}
		g.popMatrix()
	}

	protected abstract val kingsCourtBoardId: Int
	private fun drawKingsCourtBoard(g: AGraphics) {
		drawCheckerboardImage(g, kingsCourtBoardId, 206f, 7f)
	}

	private fun drawDamaBoard(g: AGraphics) {
		g.color = DAMA_BACKGROUND_COLOR
		g.drawFilledRect(0f, 0f, BOARD_DIM!!.getWidth(), BOARD_DIM!!.getHeight())
		g.color = GColor.BLACK
		for (i in 0..ranks) {
			g.drawLine(i * SQ_DIM, 0f, i * SQ_DIM, BOARD_DIM!!.getHeight(), 3f)
		}
		for (i in 0..columns) {
			g.drawLine(0f, i * SQ_DIM, BOARD_DIM!!.getWidth(), i * SQ_DIM, 3f)
		}
	}

	@Synchronized
	private fun drawBoard(g: AGraphics, _mx: Int, _my: Int) {
		log.verbose("drawBoard $_mx x $_my highlighted=$highlightedPos selected=$selectedPiece")
		g.pushMatrix()
		if (getRules() is KingsCourt && kingsCourtBoardId != 0) {
			drawKingsCourtBoard(g)
		} else if (getRules() is Dama) {
			drawDamaBoard(g)
		} else if (ranks == 8 && columns == 8 && checkerboardImageId != 0) {
			drawCheckerboard8x8(g)
		} else {
			drawCheckboardBoard(g, GColor.BLACK, GColor.LIGHT_GRAY)
		}
		val cw = SQ_DIM
		val ch = SQ_DIM
		PIECE_RADIUS = SQ_DIM / 3
		highlightedPos = -1
		var _selectedPiece: Piece? = null
		val _pickablePieces: MutableList<Piece> = ArrayList()
		val _pickableMoves: MutableList<Move> = ArrayList()
		val isUser = isCurrentPlayerUser
		if (isUser && !isGameOver()) {
			synchronized(this) {
				_selectedPiece = selectedPiece
				_pickablePieces.addAll(pickablePieces)
				_pickableMoves.addAll(pickableMoves)
			}
		}
		if (_selectedPiece != null && mode == 0 && animations.size == 0) {
			g.color = GColor.GREEN
			val x = _selectedPiece!!.col * cw + cw / 2
			val y = _selectedPiece!!.rank * ch + ch / 2
			g.drawFilledCircle(x, y, PIECE_RADIUS + 5)
		}
		val mv = g.screenToViewport(_mx, _my)
		val mx = mv.x
		val my = mv.y
		for (r in 0 until ranks) {
			for (c in 0 until columns) {
				val x = c * cw + cw / 2
				val y = r * ch + ch / 2
				if (Utils.isPointInsideRect(mx, my, c * cw, r * ch, cw, ch)) {
					if (isUser) {
						g.color = GColor.CYAN
						g.drawRect(c * cw + 1, r * ch + 1, cw - 2, ch - 2)
						highlightedPos = r shl 8 or c
					}
					if (DEBUG) {
						g.color = GColor.YELLOW
						g.drawJustifiedStringOnBackground(mv, Justify.CENTER, Justify.CENTER, String.format("%d,%d\n%s", r, c, getPiece(r, c).getType()), GColor.BLACK, 5f, 0f)
					}
				}
			}
		}
		for (p in getPieces(-1)) {
			if (animations.containsKey(p.position)) continue
			val r = p.rank
			val c = p.col
			val x = c * cw + cw / 2
			val y = r * ch + ch / 2
			if (mode == 0) {
				for (pp in _pickablePieces) {
					if (pp.rank == r && pp.col == c) {
						g.color = GColor.CYAN
						g.drawFilledCircle(x, y, PIECE_RADIUS + 5)
						break
					}
				}
			}
			g.pushMatrix()
			if (p.isCaptured) {
				g.color = GColor.RED
				g.drawFilledCircle(x, y, PIECE_RADIUS + 5)
			}
			if (p.position == draggingPos) {
				g.translate(mx, my)
			} else {
				g.translate(x, y)
			}
			drawPiece(g, p)
			g.popMatrix()
		}
		for (m in _pickableMoves) {
			if (m.moveType === MoveType.END && _selectedPiece != null) {
				if (highlightedPos == _selectedPiece!!.position) {
					g.color = GColor.YELLOW
				} else {
					g.color = GColor.GREEN
				}
				val x = _selectedPiece!!.col * cw + cw / 2
				val y = _selectedPiece!!.rank * ch + ch / 2
				//g.setTextHeight(PIECE_RADIUS / 2);
				g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, "END")
			} else if (m.end >= 0) {
				g.color = GColor.CYAN
				val x = (m.end and 0xff) * cw + cw / 2
				val y = (m.end shr 8) * ch + ch / 2
				g.drawCircle(x, y, PIECE_RADIUS)
			}
		}
		synchronized(animations) {
			val it: MutableIterator<Map.Entry<Int, PieceAnim>> = animations.entries.iterator()
			while (it.hasNext()) {
				val a = it.next().value
				if (a.isDone) {
					it.remove()
				} else {
					a.update(g)
					repaint(0)
				}
			}
		}
		g.popMatrix()
		val cx = BOARD_DIM!!.getWidth() / 2
		val cy = BOARD_DIM!!.getHeight() / 2
		if (isGameOver()) {
			if (winner != null) {
				val txt = """G A M E   O V E R
${winner!!.color} Wins!"""
				g.color = GColor.CYAN
				g.drawJustifiedStringOnBackground(cx, cy, Justify.CENTER, Justify.CENTER, txt, GColor.TRANSLUSCENT_BLACK, 20f)
			} else {
				val txt = "D R A W   G A M E"
				g.color = GColor.CYAN
				g.drawJustifiedStringOnBackground(cx, cy, Justify.CENTER, Justify.CENTER, txt, GColor.TRANSLUSCENT_BLACK, 20f)
			}
		} else if (!isGameRunning) {
			g.color = GColor.YELLOW
			if (showInstructions) {
				val tab = getRules().instructions
				tab.setModel(instructionsModel)
				tab.draw(g, cx, cy, Justify.CENTER, Justify.CENTER)
				showInstructions = false
			} else {
				val txt = "P A U S E D"
				g.color = GColor.CYAN
				g.drawJustifiedStringOnBackground(cx, cy, Justify.CENTER, Justify.CENTER, txt, GColor.TRANSLUSCENT_BLACK, 3f)
			}
		}
	}

	val instructionsModel: Table.Model = object : Table.Model {
		override fun getBorderColor(g: AGraphics): GColor {
			return GColor.WHITE
		}

		override fun getBackgroundColor(): GColor {
			return GColor.BLACK
		}
	}

	fun doClick() {
		log.verbose("do Click")
		mode = MODE_CLICK
		runLock.release()
	}

	fun startDrag() {
		log.verbose("start drag")
		mode = MODE_DRAG_START
		runLock.release()
	}

	fun stopDrag() {
		log.verbose("stop drag")
		mode = MODE_DRAG_STOP
		draggingPos = -1
		runLock.release()
	}

	val isCurrentPlayerUser: Boolean
		get() = (currentPlayer as UIPlayer).type === UIPlayer.Type.USER

	fun choosePieceToMove(pieces: List<Piece>): Piece? {
		if (!isGameRunning) return null
		//Utils.println("choosePieceToMove: " + pieces);
		mode = 0
		synchronized(this) {
			pickableMoves.clear()
			pickablePieces.clear()
			pickablePieces.addAll(pieces)
		}
		repaint(0)
		runLock.acquireAndBlock()
		when (mode) {
			MODE_CLICK -> {
				mode = 0
				draggingPos = -1
				for (p in pieces) {
					if (p.position == highlightedPos) {
						return p
					}
				}
			}
			MODE_DRAG_START -> {
				for (p in pieces) {
					if (p.position == highlightedPos) {
						draggingPos = p.position
						return p
					}
				}
			}
			MODE_DRAG_STOP -> {
			}
		}
		return null
	}

	fun chooseMoveForPiece(moves: List<Move>): Move? {
		synchronized(this) {
			pickableMoves.clear()
			pickablePieces.clear()
			pickableMoves.addAll(moves)
		}
		repaint(0)
		runLock.acquireAndBlock()
		when (mode) {
			MODE_CLICK -> {
				mode = 0
				run {
					for (m in moves) {
						when (m.moveType) {
							MoveType.SWAP, MoveType.END                                                          -> if (selectedPiece != null && selectedPiece!!.position == highlightedPos) {
								return m
							}
							MoveType.SLIDE, MoveType.FLYING_JUMP, MoveType.JUMP, MoveType.CASTLE, MoveType.STACK -> if (m.end == highlightedPos) {
								return m
							}
						}
					}
				}
			}
			MODE_DRAG_STOP -> {
				for (m in moves) {
					when (m.moveType) {
						MoveType.SWAP, MoveType.END                                                          -> if (selectedPiece != null && selectedPiece!!.position == highlightedPos) {
							return m
						}
						MoveType.SLIDE, MoveType.FLYING_JUMP, MoveType.JUMP, MoveType.CASTLE, MoveType.STACK -> if (m.end == highlightedPos) {
							return m
						}
					}
				}
			}
		}
		return null
	}

	private fun drawPiece(g: AGraphics, pc: Piece) {
		if (pc.playerNum < 0) return
		g.pushMatrix()
		g.translate(0f, PIECE_RADIUS)
		val d = PIECE_RADIUS * 2
		if (pc.isStacked) {
			for (s in pc.stackSize - 1 downTo 0) {
				drawPiece(g, PieceType.CHECKER, getPlayer(pc.getStackAt(s))!!.color, d, d, null)
				g.translate(0f, -d / 5)
			}
		} else {
			drawPiece(g, pc.getType(), pc.playerNum)
		}
		g.popMatrix()
	}

	private fun drawPiece(g: AGraphics, pt: PieceType?, playerNum: Int) {
		val d = PIECE_RADIUS * 2
		val color = getPlayer(playerNum)!!.color
		g.pushMatrix()
		when (pt) {
			null,
			PieceType.BLOCKED,
			PieceType.EMPTY -> Unit
			PieceType.PAWN, PieceType.PAWN_IDLE, PieceType.PAWN_ENPASSANT, PieceType.PAWN_TOSWAP -> drawPiece(g, PieceType.PAWN, color, d, d, null)
			PieceType.BISHOP, PieceType.QUEEN, PieceType.KNIGHT_L, PieceType.KNIGHT_R -> drawPiece(g, pt, color, d, d, null)
			PieceType.ROOK, PieceType.ROOK_IDLE -> drawPiece(g, PieceType.ROOK, color, d, d, null)
			PieceType.DRAGON_R, PieceType.DRAGON_L, PieceType.DRAGON_IDLE_R, PieceType.DRAGON_IDLE_L -> drawPiece(g, pt, color, d, d, null)
			PieceType.CHECKED_KING, PieceType.CHECKED_KING_IDLE -> drawPiece(g, PieceType.KING, color, d, d, GColor.RED)
			PieceType.UNCHECKED_KING, PieceType.UNCHECKED_KING_IDLE -> drawPiece(g, PieceType.KING, color, d, d, null)
			PieceType.KING, PieceType.FLYING_KING, PieceType.DAMA_KING -> {
				drawPiece(g, PieceType.CHECKER, color, d, d, null)
				g.translate(0f, -d / 5)
				drawPiece(g, PieceType.CHECKER, color, d, d, null)
			}
			PieceType.CHIP_4WAY, PieceType.DAMA_MAN, PieceType.CHECKER -> drawPiece(g, PieceType.CHECKER, color, d, d, null)
		}
		g.popMatrix()
	}

	fun drawPiece(g: AGraphics, p: PieceType, color: Color, w: Float, h: Float, outlineColor: GColor?) {
		var h = h
		val id = getPieceImageId(p, color)
		if (id > 0) {
			//Matrix3x3 M = new Matrix3x3();
			//g.getTransform(M);
			val img = g.getImage(id)
			val a = w / h
			val aa = img.aspect
			//w = w * aa / a;
			h = h * a / aa
			val imgWidth = img.width
			val imgHeight = img.height
			val xScale = w / imgWidth
			val yScale = h / imgHeight
			g.pushMatrix()
			if (p.drawFlipped()) {
				g.translate(w / 2, -h)
				g.scale(-xScale, yScale)
			} else {
				g.translate(-w / 2, -h)
				g.scale(xScale, yScale)
			}
			g.drawImage(id) //-w/2, -h/2, w, h);
			g.popMatrix()
		} else {
			g.color = color.color
			g.drawFilledCircle(0f, -h / 2, w / 2)
			g.pushTextHeight(SQ_DIM / 2, true)
			g.color = color.color.inverted()
			g.drawJustifiedString(0f, -h / 2, Justify.CENTER, Justify.CENTER, p.abbrev)
			g.popTextHeight()
		}
		if (outlineColor != null) {
			g.color = outlineColor
			g.drawCircle(0f, -h / 2, w / 2, 3f)
		}
	}

	override fun onGameOver(winner: Player?) {
		super.onGameOver(winner)
	}

	override fun onMoveChosen(m: Move?) {
		if (mode == MODE_DRAG_STOP && m!!.moveType !== MoveType.STACK) {
			mode = 0
			return  // no animations when drag-n-drop
		}
		val pc = getPiece(m!!.start)
		when (m.moveType) {
			MoveType.END -> {
			}
			MoveType.SLIDE -> {
				animations[pc.position] = SlideAnim(m.start, m.end).start()
			}
			MoveType.FLYING_JUMP, MoveType.JUMP -> animations[pc.position] = JumpAnim(m.start, m.end).start()
			MoveType.STACK -> animations[pc.position] = StackAnim(m.start).start()
			MoveType.SWAP -> {
			}
			MoveType.CASTLE -> {
				animations[pc.position] = SlideAnim(m.start, m.end).start()
				animations[pc.position] = JumpAnim(m.castleRookStart, m.castleRookEnd).start()
			}
		}
		repaint(0)
		animLock.block()
	}

	override fun onPieceSelected(p: Piece?) {
		// TODO: Animation
		super.onPieceSelected(p)
	}

	/**
	 * Return < 0 for no image or > 0 for a valid image id.
	 * @param p
	 * @param color
	 * @return
	 */
	abstract fun getPieceImageId(p: PieceType, color: Color): Int

	abstract inner class PieceAnim(val start: Int, ex: Float, ey: Float, durationMSecs: Long) : AAnimation<AGraphics>(durationMSecs) {
		val sx: Float
		val sy: Float
		val ex: Float
		val ey: Float

		constructor(start: Int, end: Int, durationMSecs: Long) : this(start, SQ_DIM * (end and 0xff) + SQ_DIM / 2, SQ_DIM * (end shr 8) + SQ_DIM / 2, durationMSecs) {}

		override fun onDone() {
			animLock.release()
		}

		init {
			sx = SQ_DIM * (start and 0xff) + SQ_DIM / 2
			sy = SQ_DIM * (start shr 8) + SQ_DIM / 2
			this.ex = ex
			this.ey = ey
			animLock.acquire()
		}
	}

	internal open inner class SlideAnim(start: Int, end: Int) : PieceAnim(start, end, 1000) {
		override fun draw(g: AGraphics, position: Float, dt: Float) {
			val x = sx + (ex - sx) * position
			val y = sy + (ey - sy) * position
			g.pushMatrix()
			g.translate(x, y)
			drawPiece(g, getPiece(start))
			g.popMatrix()
		}
	}

	internal inner class JumpAnim(start: Int, end: Int) : SlideAnim(start, end) {
		val curve: Bezier
		private fun computeJumpPoints(playerNum: Int): Array<IVector2D> {
			val midx1 = sx + (ex - sx) / 3
			val midx2 = sx + (ex - sx) * 2 / 3
			val midy1 = sy + (ey - sy) / 3
			val midy2 = sy + (ey - sy) * 2 / 3
			val dist = -SQ_DIM //-1;//getDir(playerNum);
			return arrayOf(
				Vector2D(sx, sy),
				Vector2D(midx1, midy1 + dist),
				Vector2D(midx2, midy2 + dist),
				Vector2D(ex, ey))
		}

		override fun draw(g: AGraphics, position: Float, dt: Float) {
			val v: IVector2D = curve.getAtPosition(position)
			g.pushMatrix()
			g.translate(v)
			drawPiece(g, getPiece(start))
			g.popMatrix()
		}

		init {
			curve = Bezier(computeJumpPoints(getPiece(start).playerNum))
		}
	}

	internal inner class StackAnim(pos: Int) : PieceAnim(pos, pos, 1000) {
		val ssy: Float
		val eey: Float
		override fun draw(g: AGraphics, position: Float, dt: Float) {
			val p = getPiece(start)
			g.pushMatrix()
			g.translate(sx, sy)
			drawPiece(g, p)
			g.popMatrix()
			val scale = 1f + (1f - position)
			val x = sx + (ex - sx) * position
			val y = ssy + (eey - ssy) * position
			g.pushMatrix()
			g.translate(x, y)
			g.scale(scale, scale)
			drawPiece(g, getPiece(start))
			g.popMatrix()
		}

		init {
			ssy = ey - SQ_DIM
			eey = ey - PIECE_RADIUS / 2
		}
	}

	companion object {
		var log = LoggerFactory.getLogger(UIGame::class.java)
		private const val DEBUG = false
		const val MODE_CLICK = 1
		const val MODE_DRAG_START = 2
		const val MODE_DRAG_STOP = 3
	}
}